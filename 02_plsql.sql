-- =====================================================================
-- ZelaBairro - Global Solution 2 (CitizenTech)
-- Script 02: Rotinas PL/SQL de automação
--   * Funções de regra de negócio (pontuação e prioridade)
--   * Procedure de log de erros (transação autônoma)
--   * Package PKG_ZELABAIRRO (registro, apoio, status, indicadores, relatórios)
--   * Triggers (prioridade automática, histórico automático, contador de apoios)
-- Pré-requisito: 01_ddl.sql
-- =====================================================================

SET SERVEROUTPUT ON;

-- ---------------------------------------------------------------------
-- 1) FUNÇÃO: pontuação de prioridade
--    Mesma regra do app (RegraPrioridade.kt):
--    peso da categoria + 3 se afeta acessibilidade + apoios (máximo 10)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_ZB_CALCULAR_PONTUACAO (
    p_peso            IN NUMBER,
    p_acessibilidade  IN CHAR,
    p_apoios          IN NUMBER
) RETURN NUMBER
DETERMINISTIC
IS
    c_bonus_acessibilidade CONSTANT NUMBER := 3;
    c_limite_apoios        CONSTANT NUMBER := 10;
BEGIN
    RETURN NVL(p_peso, 0)
         + CASE WHEN p_acessibilidade = 'S' THEN c_bonus_acessibilidade ELSE 0 END
         + LEAST(GREATEST(NVL(p_apoios, 0), 0), c_limite_apoios);
END FN_ZB_CALCULAR_PONTUACAO;
/

-- ---------------------------------------------------------------------
-- 2) FUNÇÃO: classifica a pontuação em ALTA / MEDIA / BAIXA
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_ZB_CLASSIFICAR_PRIORIDADE (
    p_pontuacao IN NUMBER
) RETURN VARCHAR2
DETERMINISTIC
IS
BEGIN
    RETURN CASE
               WHEN p_pontuacao >= 10 THEN 'ALTA'
               WHEN p_pontuacao >= 6  THEN 'MEDIA'
               ELSE 'BAIXA'
           END;
END FN_ZB_CLASSIFICAR_PRIORIDADE;
/

-- ---------------------------------------------------------------------
-- 3) PROCEDURE: grava erro no log mesmo que a transação principal sofra
--    ROLLBACK (PRAGMA AUTONOMOUS_TRANSACTION).
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE PRC_ZB_REGISTRAR_LOG (
    p_rotina   IN VARCHAR2,
    p_codigo   IN NUMBER,
    p_mensagem IN VARCHAR2
) IS
    PRAGMA AUTONOMOUS_TRANSACTION;
BEGIN
    INSERT INTO T_ZB_LOG_ERRO (nm_rotina, cd_erro, ds_erro)
    VALUES (p_rotina, p_codigo, SUBSTR(p_mensagem, 1, 4000));
    COMMIT;
END PRC_ZB_REGISTRAR_LOG;
/

-- =====================================================================
-- 4) PACKAGE PKG_ZELABAIRRO - especificação (o que fica visível)
-- =====================================================================
CREATE OR REPLACE PACKAGE PKG_ZELABAIRRO IS

    -- Status do ciclo de vida da ocorrência
    gc_registrada     CONSTANT VARCHAR2(20) := 'REGISTRADA';
    gc_em_analise     CONSTANT VARCHAR2(20) := 'EM_ANALISE';
    gc_em_atendimento CONSTANT VARCHAR2(20) := 'EM_ATENDIMENTO';
    gc_resolvida      CONSTANT VARCHAR2(20) := 'RESOLVIDA';

    -- Exceções de negócio
    e_transicao_invalida EXCEPTION;
    PRAGMA EXCEPTION_INIT(e_transicao_invalida, -20010);

    /* Observação que a trigger de histórico grava na próxima mudança de status. */
    FUNCTION fn_observacao_atual RETURN VARCHAR2;

    /* Regras do fluxo: REGISTRADA > EM_ANALISE > EM_ATENDIMENTO > RESOLVIDA (> EM_ANALISE se reaberta). */
    FUNCTION fn_transicao_valida (p_atual IN VARCHAR2, p_novo IN VARCHAR2) RETURN BOOLEAN;

    /* Funcionalidade 1: registra a ocorrência, gera protocolo e devolve o id. */
    PROCEDURE registrar_ocorrencia (
        p_id_cidadao       IN  T_ZB_OCORRENCIA.id_cidadao%TYPE,
        p_cd_categoria     IN  T_ZB_CATEGORIA.cd_categoria%TYPE,
        p_id_bairro        IN  T_ZB_OCORRENCIA.id_bairro%TYPE,
        p_titulo           IN  T_ZB_OCORRENCIA.ds_titulo%TYPE,
        p_descricao        IN  T_ZB_OCORRENCIA.ds_descricao%TYPE,
        p_endereco         IN  T_ZB_OCORRENCIA.ds_endereco%TYPE,
        p_ponto_referencia IN  T_ZB_OCORRENCIA.ds_ponto_referencia%TYPE DEFAULT NULL,
        p_acessibilidade   IN  CHAR DEFAULT 'N',
        o_id_ocorrencia    OUT T_ZB_OCORRENCIA.id_ocorrencia%TYPE,
        o_protocolo        OUT T_ZB_OCORRENCIA.cd_protocolo%TYPE
    );

    /* Funcionalidade 3: apoio comunitário ("também sou afetado"). */
    PROCEDURE apoiar_ocorrencia (p_id_ocorrencia IN NUMBER, p_id_cidadao IN NUMBER);
    PROCEDURE remover_apoio     (p_id_ocorrencia IN NUMBER, p_id_cidadao IN NUMBER);

    /* Acompanhamento do atendimento pelo órgão público. */
    PROCEDURE atualizar_status (
        p_id_ocorrencia IN NUMBER,
        p_novo_status   IN VARCHAR2,
        p_observacao    IN VARCHAR2 DEFAULT NULL
    );

    /* O cidadão informa que o problema "resolvido" continua. */
    PROCEDURE reabrir_ocorrencia (p_id_ocorrencia IN NUMBER, p_id_cidadao IN NUMBER, p_motivo IN VARCHAR2);

    /* Funcionalidade 2: consulta com filtros opcionais (SQL dinâmico + OPEN FOR). */
    FUNCTION listar_ocorrencias (
        p_status       IN VARCHAR2 DEFAULT NULL,
        p_id_bairro    IN NUMBER   DEFAULT NULL,
        p_cd_categoria IN VARCHAR2 DEFAULT NULL,
        p_ordenacao    IN VARCHAR2 DEFAULT 'PRIORIDADE'
    ) RETURN SYS_REFCURSOR;

    /* Funcionalidade 4: consolida indicadores do mês (MERGE). Formato AAAAMM. */
    PROCEDURE gerar_indicadores_mensais (p_ano_mes IN NUMBER DEFAULT TO_NUMBER(TO_CHAR(SYSDATE, 'YYYYMM')));

    /* Percentual de ocorrências resolvidas em um bairro. */
    FUNCTION fn_taxa_resolucao_bairro (p_id_bairro IN NUMBER) RETURN NUMBER;

    /* Relatório (DBMS_OUTPUT) das ocorrências pendentes que já passaram do prazo do órgão. */
    PROCEDURE relatorio_fora_do_prazo;

END PKG_ZELABAIRRO;
/

-- =====================================================================
-- 5) PACKAGE PKG_ZELABAIRRO - corpo (implementação)
-- =====================================================================
CREATE OR REPLACE PACKAGE BODY PKG_ZELABAIRRO IS

    -- Variável privada lida pela trigger de histórico
    g_observacao VARCHAR2(300);

    FUNCTION fn_observacao_atual RETURN VARCHAR2 IS
    BEGIN
        RETURN g_observacao;
    END fn_observacao_atual;

    FUNCTION fn_transicao_valida (p_atual IN VARCHAR2, p_novo IN VARCHAR2) RETURN BOOLEAN IS
    BEGIN
        RETURN (p_atual = gc_registrada     AND p_novo = gc_em_analise)
            OR (p_atual = gc_em_analise     AND p_novo = gc_em_atendimento)
            OR (p_atual = gc_em_atendimento AND p_novo = gc_resolvida)
            OR (p_atual = gc_resolvida      AND p_novo = gc_em_analise);   -- reabertura
    END fn_transicao_valida;

    -- Mensagem padrão do histórico para cada etapa
    FUNCTION fn_mensagem_padrao (p_status IN VARCHAR2, p_orgao IN VARCHAR2) RETURN VARCHAR2 IS
    BEGIN
        RETURN CASE p_status
                   WHEN gc_em_analise     THEN p_orgao || ' recebeu a solicitação e está avaliando o local.'
                   WHEN gc_em_atendimento THEN 'Equipe designada. Serviço programado ou em execução.'
                   WHEN gc_resolvida      THEN 'Serviço concluído pelo órgão responsável.'
                   ELSE 'Status atualizado.'
               END;
    END fn_mensagem_padrao;

    -- -----------------------------------------------------------------
    PROCEDURE registrar_ocorrencia (
        p_id_cidadao       IN  T_ZB_OCORRENCIA.id_cidadao%TYPE,
        p_cd_categoria     IN  T_ZB_CATEGORIA.cd_categoria%TYPE,
        p_id_bairro        IN  T_ZB_OCORRENCIA.id_bairro%TYPE,
        p_titulo           IN  T_ZB_OCORRENCIA.ds_titulo%TYPE,
        p_descricao        IN  T_ZB_OCORRENCIA.ds_descricao%TYPE,
        p_endereco         IN  T_ZB_OCORRENCIA.ds_endereco%TYPE,
        p_ponto_referencia IN  T_ZB_OCORRENCIA.ds_ponto_referencia%TYPE DEFAULT NULL,
        p_acessibilidade   IN  CHAR DEFAULT 'N',
        o_id_ocorrencia    OUT T_ZB_OCORRENCIA.id_ocorrencia%TYPE,
        o_protocolo        OUT T_ZB_OCORRENCIA.cd_protocolo%TYPE
    ) IS
        v_id_categoria T_ZB_CATEGORIA.id_categoria%TYPE;
        v_nm_orgao     T_ZB_ORGAO.nm_orgao%TYPE;
    BEGIN
        -- Validações de entrada
        IF p_titulo IS NULL OR LENGTH(TRIM(p_titulo)) < 5 THEN
            RAISE_APPLICATION_ERROR(-20002, 'O título deve ter pelo menos 5 caracteres.');
        END IF;
        IF p_descricao IS NULL OR LENGTH(TRIM(p_descricao)) < 10 THEN
            RAISE_APPLICATION_ERROR(-20003, 'A descrição deve ter pelo menos 10 caracteres.');
        END IF;
        IF NVL(p_acessibilidade, 'X') NOT IN ('S', 'N') THEN
            RAISE_APPLICATION_ERROR(-20004, 'Informe S ou N para acessibilidade.');
        END IF;

        -- Descobre a categoria e o órgão que receberá a ocorrência
        BEGIN
            SELECT c.id_categoria, o.nm_orgao
              INTO v_id_categoria, v_nm_orgao
              FROM T_ZB_CATEGORIA c
              JOIN T_ZB_ORGAO o ON o.id_orgao = c.id_orgao
             WHERE c.cd_categoria = UPPER(p_cd_categoria);
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20001, 'Categoria inexistente: ' || p_cd_categoria);
        END;

        o_protocolo := 'ZB-' || TO_CHAR(SYSDATE, 'YYMMDD') || '-' ||
                       LPAD(SEQ_ZB_PROTOCOLO.NEXTVAL, 6, '0');

        g_observacao := 'Ocorrência registrada e encaminhada para ' || v_nm_orgao || '.';

        -- Pontuação, prioridade e histórico são preenchidos pelas triggers
        INSERT INTO T_ZB_OCORRENCIA (
            cd_protocolo, ds_titulo, ds_descricao, ds_endereco, ds_ponto_referencia,
            fl_acessibilidade, st_ocorrencia, id_cidadao, id_categoria, id_bairro
        ) VALUES (
            o_protocolo, TRIM(p_titulo), TRIM(p_descricao), TRIM(p_endereco), TRIM(p_ponto_referencia),
            p_acessibilidade, gc_registrada, p_id_cidadao, v_id_categoria, p_id_bairro
        )
        RETURNING id_ocorrencia INTO o_id_ocorrencia;

        g_observacao := NULL;
        COMMIT;
    EXCEPTION
        WHEN OTHERS THEN
            g_observacao := NULL;
            ROLLBACK;
            PRC_ZB_REGISTRAR_LOG('PKG_ZELABAIRRO.REGISTRAR_OCORRENCIA', SQLCODE, SQLERRM);
            RAISE;
    END registrar_ocorrencia;

    -- -----------------------------------------------------------------
    PROCEDURE apoiar_ocorrencia (p_id_ocorrencia IN NUMBER, p_id_cidadao IN NUMBER) IS
        v_status  T_ZB_OCORRENCIA.st_ocorrencia%TYPE;
        v_autor   T_ZB_OCORRENCIA.id_cidadao%TYPE;
    BEGIN
        SELECT st_ocorrencia, id_cidadao
          INTO v_status, v_autor
          FROM T_ZB_OCORRENCIA
         WHERE id_ocorrencia = p_id_ocorrencia;

        IF v_status = gc_resolvida THEN
            RAISE_APPLICATION_ERROR(-20020, 'Não é possível apoiar uma ocorrência já resolvida.');
        END IF;
        IF v_autor = p_id_cidadao THEN
            RAISE_APPLICATION_ERROR(-20021, 'O autor da ocorrência não pode apoiá-la.');
        END IF;

        -- O contador qt_apoios e a nova prioridade são atualizados por trigger
        INSERT INTO T_ZB_APOIO (id_ocorrencia, id_cidadao) VALUES (p_id_ocorrencia, p_id_cidadao);
        COMMIT;
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20022, 'Este cidadão já apoiou a ocorrência.');
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20023, 'Ocorrência não encontrada: ' || p_id_ocorrencia);
        WHEN OTHERS THEN
            ROLLBACK;
            PRC_ZB_REGISTRAR_LOG('PKG_ZELABAIRRO.APOIAR_OCORRENCIA', SQLCODE, SQLERRM);
            RAISE;
    END apoiar_ocorrencia;

    -- -----------------------------------------------------------------
    PROCEDURE remover_apoio (p_id_ocorrencia IN NUMBER, p_id_cidadao IN NUMBER) IS
    BEGIN
        DELETE FROM T_ZB_APOIO
         WHERE id_ocorrencia = p_id_ocorrencia
           AND id_cidadao = p_id_cidadao;

        IF SQL%ROWCOUNT = 0 THEN
            RAISE_APPLICATION_ERROR(-20024, 'Apoio não encontrado.');
        END IF;
        COMMIT;
    END remover_apoio;

    -- -----------------------------------------------------------------
    PROCEDURE atualizar_status (
        p_id_ocorrencia IN NUMBER,
        p_novo_status   IN VARCHAR2,
        p_observacao    IN VARCHAR2 DEFAULT NULL
    ) IS
        v_atual  T_ZB_OCORRENCIA.st_ocorrencia%TYPE;
        v_orgao  T_ZB_ORGAO.nm_orgao%TYPE;
        v_novo   VARCHAR2(20) := UPPER(p_novo_status);
    BEGIN
        -- FOR UPDATE: bloqueia a linha para evitar duas atualizações simultâneas
        SELECT o.st_ocorrencia, org.nm_orgao
          INTO v_atual, v_orgao
          FROM T_ZB_OCORRENCIA o
          JOIN T_ZB_CATEGORIA c  ON c.id_categoria = o.id_categoria
          JOIN T_ZB_ORGAO org    ON org.id_orgao = c.id_orgao
         WHERE o.id_ocorrencia = p_id_ocorrencia
           FOR UPDATE OF o.st_ocorrencia;

        IF NOT fn_transicao_valida(v_atual, v_novo) THEN
            RAISE_APPLICATION_ERROR(-20010, 'Transição inválida: ' || v_atual || ' -> ' || v_novo);
        END IF;

        g_observacao := NVL(p_observacao, fn_mensagem_padrao(v_novo, v_orgao));

        UPDATE T_ZB_OCORRENCIA
           SET st_ocorrencia = v_novo
         WHERE id_ocorrencia = p_id_ocorrencia;

        g_observacao := NULL;
        COMMIT;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            g_observacao := NULL;
            RAISE_APPLICATION_ERROR(-20023, 'Ocorrência não encontrada: ' || p_id_ocorrencia);
        WHEN OTHERS THEN
            g_observacao := NULL;
            ROLLBACK;
            PRC_ZB_REGISTRAR_LOG('PKG_ZELABAIRRO.ATUALIZAR_STATUS', SQLCODE, SQLERRM);
            RAISE;
    END atualizar_status;

    -- -----------------------------------------------------------------
    PROCEDURE reabrir_ocorrencia (p_id_ocorrencia IN NUMBER, p_id_cidadao IN NUMBER, p_motivo IN VARCHAR2) IS
        v_nome   T_ZB_CIDADAO.nm_cidadao%TYPE;
        v_status T_ZB_OCORRENCIA.st_ocorrencia%TYPE;
    BEGIN
        SELECT st_ocorrencia INTO v_status FROM T_ZB_OCORRENCIA WHERE id_ocorrencia = p_id_ocorrencia;
        IF v_status <> gc_resolvida THEN
            RAISE_APPLICATION_ERROR(-20011, 'Só é possível reabrir ocorrências resolvidas.');
        END IF;

        SELECT nm_cidadao INTO v_nome FROM T_ZB_CIDADAO WHERE id_cidadao = p_id_cidadao;
        atualizar_status(
            p_id_ocorrencia,
            gc_em_analise,
            SUBSTR('Reaberta por ' || v_nome || ': ' || NVL(p_motivo, 'o problema continua no local.'), 1, 300)
        );
    END reabrir_ocorrencia;

    -- -----------------------------------------------------------------
    -- SQL dinâmico: o WHERE e o ORDER BY são montados em tempo de execução.
    -- Os valores entram SEMPRE por variável de ligação (bind) -> sem SQL Injection.
    -- Quando um filtro não é informado, usamos ":b IS NULL" para manter a
    -- mesma quantidade de binds no USING.
    -- -----------------------------------------------------------------
    FUNCTION listar_ocorrencias (
        p_status       IN VARCHAR2 DEFAULT NULL,
        p_id_bairro    IN NUMBER   DEFAULT NULL,
        p_cd_categoria IN VARCHAR2 DEFAULT NULL,
        p_ordenacao    IN VARCHAR2 DEFAULT 'PRIORIDADE'
    ) RETURN SYS_REFCURSOR IS
        v_sql    VARCHAR2(2000);
        v_cursor SYS_REFCURSOR;
    BEGIN
        v_sql := 'SELECT o.cd_protocolo, o.ds_titulo, c.nm_categoria, b.nm_bairro, '
              || '       o.st_ocorrencia, o.ds_prioridade, o.qt_apoios, o.dt_abertura '
              || '  FROM T_ZB_OCORRENCIA o '
              || '  JOIN T_ZB_CATEGORIA c ON c.id_categoria = o.id_categoria '
              || '  JOIN T_ZB_BAIRRO b    ON b.id_bairro = o.id_bairro '
              || ' WHERE 1 = 1';

        v_sql := v_sql || CASE WHEN p_status IS NULL
                               THEN ' AND :b_status IS NULL'
                               ELSE ' AND o.st_ocorrencia = :b_status' END;
        v_sql := v_sql || CASE WHEN p_id_bairro IS NULL
                               THEN ' AND :b_bairro IS NULL'
                               ELSE ' AND o.id_bairro = :b_bairro' END;
        v_sql := v_sql || CASE WHEN p_cd_categoria IS NULL
                               THEN ' AND :b_categoria IS NULL'
                               ELSE ' AND c.cd_categoria = :b_categoria' END;

        -- Ordenação só aceita valores de uma lista fechada (whitelist)
        v_sql := v_sql || CASE UPPER(p_ordenacao)
                              WHEN 'PRIORIDADE' THEN ' ORDER BY o.nr_pontuacao DESC, o.dt_abertura'
                              WHEN 'APOIOS'     THEN ' ORDER BY o.qt_apoios DESC, o.dt_abertura'
                              WHEN 'DATA'       THEN ' ORDER BY o.dt_abertura DESC'
                          END;

        IF UPPER(p_ordenacao) NOT IN ('PRIORIDADE', 'APOIOS', 'DATA') THEN
            RAISE_APPLICATION_ERROR(-20030, 'Ordenação inválida. Use PRIORIDADE, APOIOS ou DATA.');
        END IF;

        OPEN v_cursor FOR v_sql USING UPPER(p_status), p_id_bairro, UPPER(p_cd_categoria);
        RETURN v_cursor;
    END listar_ocorrencias;

    -- -----------------------------------------------------------------
    PROCEDURE gerar_indicadores_mensais (p_ano_mes IN NUMBER DEFAULT TO_NUMBER(TO_CHAR(SYSDATE, 'YYYYMM'))) IS
        v_inicio DATE;
        v_fim    DATE;
    BEGIN
        v_inicio := TO_DATE(TO_CHAR(p_ano_mes) || '01', 'YYYYMMDD');
        v_fim    := ADD_MONTHS(v_inicio, 1);

        MERGE INTO T_ZB_INDICADOR_MENSAL d
        USING (
            SELECT o.id_bairro,
                   o.id_categoria,
                   COUNT(*) AS qt_registradas,
                   SUM(CASE WHEN o.st_ocorrencia = 'RESOLVIDA' THEN 1 ELSE 0 END) AS qt_resolvidas,
                   SUM(CASE WHEN NVL(o.dt_resolucao, SYSDATE) - o.dt_abertura > org.nr_prazo_dias_sla
                            THEN 1 ELSE 0 END) AS qt_fora_prazo,
                   SUM(o.qt_apoios) AS qt_apoios,
                   ROUND(AVG(CASE WHEN o.dt_resolucao IS NOT NULL
                                  THEN (o.dt_resolucao - o.dt_abertura) * 24 END), 2) AS tempo_medio_horas
              FROM T_ZB_OCORRENCIA o
              JOIN T_ZB_CATEGORIA c ON c.id_categoria = o.id_categoria
              JOIN T_ZB_ORGAO org   ON org.id_orgao = c.id_orgao
             WHERE o.dt_abertura >= v_inicio
               AND o.dt_abertura <  v_fim
             GROUP BY o.id_bairro, o.id_categoria
        ) s
        ON (d.nr_ano_mes = p_ano_mes AND d.id_bairro = s.id_bairro AND d.id_categoria = s.id_categoria)
        WHEN MATCHED THEN UPDATE SET
            d.qt_registradas       = s.qt_registradas,
            d.qt_resolvidas        = s.qt_resolvidas,
            d.qt_fora_prazo        = s.qt_fora_prazo,
            d.qt_apoios            = s.qt_apoios,
            d.nr_tempo_medio_horas = s.tempo_medio_horas,
            d.pc_resolucao         = ROUND(s.qt_resolvidas * 100 / s.qt_registradas, 2),
            d.dt_geracao           = SYSDATE
        WHEN NOT MATCHED THEN INSERT (
            nr_ano_mes, id_bairro, id_categoria, qt_registradas, qt_resolvidas,
            qt_fora_prazo, qt_apoios, nr_tempo_medio_horas, pc_resolucao
        ) VALUES (
            p_ano_mes, s.id_bairro, s.id_categoria, s.qt_registradas, s.qt_resolvidas,
            s.qt_fora_prazo, s.qt_apoios, s.tempo_medio_horas,
            ROUND(s.qt_resolvidas * 100 / s.qt_registradas, 2)
        );

        DBMS_OUTPUT.PUT_LINE('Indicadores de ' || p_ano_mes || ': ' || SQL%ROWCOUNT || ' linha(s) consolidada(s).');
        COMMIT;
    EXCEPTION
        WHEN OTHERS THEN
            ROLLBACK;
            PRC_ZB_REGISTRAR_LOG('PKG_ZELABAIRRO.GERAR_INDICADORES_MENSAIS', SQLCODE, SQLERRM);
            RAISE;
    END gerar_indicadores_mensais;

    -- -----------------------------------------------------------------
    FUNCTION fn_taxa_resolucao_bairro (p_id_bairro IN NUMBER) RETURN NUMBER IS
        v_total      NUMBER;
        v_resolvidas NUMBER;
    BEGIN
        SELECT COUNT(*),
               SUM(CASE WHEN st_ocorrencia = gc_resolvida THEN 1 ELSE 0 END)
          INTO v_total, v_resolvidas
          FROM T_ZB_OCORRENCIA
         WHERE id_bairro = p_id_bairro;

        IF v_total = 0 THEN
            RETURN 0;
        END IF;
        RETURN ROUND(v_resolvidas * 100 / v_total, 2);
    END fn_taxa_resolucao_bairro;

    -- -----------------------------------------------------------------
    PROCEDURE relatorio_fora_do_prazo IS
        CURSOR c_atrasadas IS
            SELECT o.cd_protocolo,
                   o.ds_titulo,
                   b.nm_bairro,
                   org.nm_orgao,
                   o.ds_prioridade,
                   o.qt_apoios,
                   TRUNC(SYSDATE - o.dt_abertura) AS dias_abertos,
                   org.nr_prazo_dias_sla
              FROM T_ZB_OCORRENCIA o
              JOIN T_ZB_BAIRRO b    ON b.id_bairro = o.id_bairro
              JOIN T_ZB_CATEGORIA c ON c.id_categoria = o.id_categoria
              JOIN T_ZB_ORGAO org   ON org.id_orgao = c.id_orgao
             WHERE o.st_ocorrencia <> 'RESOLVIDA'
               AND SYSDATE - o.dt_abertura > org.nr_prazo_dias_sla
             ORDER BY o.nr_pontuacao DESC, dias_abertos DESC;

        v_qtd NUMBER := 0;
    BEGIN
        DBMS_OUTPUT.PUT_LINE('===== OCORRÊNCIAS FORA DO PRAZO =====');
        FOR r IN c_atrasadas LOOP
            v_qtd := v_qtd + 1;
            DBMS_OUTPUT.PUT_LINE(
                r.cd_protocolo || ' | ' || RPAD(r.ds_titulo, 35) || ' | ' || r.nm_bairro ||
                ' | ' || r.nm_orgao || ' | prioridade ' || r.ds_prioridade ||
                ' | ' || r.qt_apoios || ' apoio(s) | ' || r.dias_abertos ||
                ' dias (prazo ' || r.nr_prazo_dias_sla || ')'
            );
        END LOOP;
        IF v_qtd = 0 THEN
            DBMS_OUTPUT.PUT_LINE('Nenhuma ocorrência fora do prazo.');
        ELSE
            DBMS_OUTPUT.PUT_LINE('Total: ' || v_qtd || ' ocorrência(s) atrasada(s).');
        END IF;
    END relatorio_fora_do_prazo;

END PKG_ZELABAIRRO;
/

-- =====================================================================
-- 6) TRIGGERS
-- =====================================================================

-- Recalcula pontuação/prioridade e controla datas sempre que algo que
-- influencia a prioridade muda (inclusive o contador de apoios).
CREATE OR REPLACE TRIGGER TRG_ZB_OCORRENCIA_PRIORIDADE
BEFORE INSERT OR UPDATE OF qt_apoios, fl_acessibilidade, id_categoria, st_ocorrencia
ON T_ZB_OCORRENCIA
FOR EACH ROW
DECLARE
    v_peso T_ZB_CATEGORIA.nr_peso%TYPE;
BEGIN
    SELECT nr_peso INTO v_peso
      FROM T_ZB_CATEGORIA
     WHERE id_categoria = :NEW.id_categoria;

    :NEW.nr_pontuacao  := FN_ZB_CALCULAR_PONTUACAO(v_peso, :NEW.fl_acessibilidade, :NEW.qt_apoios);
    :NEW.ds_prioridade := FN_ZB_CLASSIFICAR_PRIORIDADE(:NEW.nr_pontuacao);
    :NEW.dt_atualizacao := SYSDATE;

    IF :NEW.st_ocorrencia = 'RESOLVIDA' AND NVL(:OLD.st_ocorrencia, '-') <> 'RESOLVIDA' THEN
        :NEW.dt_resolucao := SYSDATE;
    ELSIF :NEW.st_ocorrencia <> 'RESOLVIDA' THEN
        :NEW.dt_resolucao := NULL;
    END IF;
END;
/

-- Grava automaticamente a linha do tempo (histórico) da ocorrência.
CREATE OR REPLACE TRIGGER TRG_ZB_OCORRENCIA_HISTORICO
AFTER INSERT OR UPDATE OF st_ocorrencia
ON T_ZB_OCORRENCIA
FOR EACH ROW
BEGIN
    IF INSERTING THEN
        INSERT INTO T_ZB_HISTORICO_STATUS (id_ocorrencia, st_anterior, st_novo, ds_observacao)
        VALUES (:NEW.id_ocorrencia, NULL, :NEW.st_ocorrencia,
                NVL(PKG_ZELABAIRRO.fn_observacao_atual, 'Ocorrência registrada.'));
    ELSIF :OLD.st_ocorrencia <> :NEW.st_ocorrencia THEN
        INSERT INTO T_ZB_HISTORICO_STATUS (id_ocorrencia, st_anterior, st_novo, ds_observacao)
        VALUES (:NEW.id_ocorrencia, :OLD.st_ocorrencia, :NEW.st_ocorrencia,
                NVL(PKG_ZELABAIRRO.fn_observacao_atual, 'Status alterado diretamente no banco.'));
    END IF;
END;
/

-- Mantém o contador de apoios sincronizado com a tabela associativa.
-- O UPDATE dispara a trigger de prioridade, que recalcula a prioridade.
CREATE OR REPLACE TRIGGER TRG_ZB_APOIO_CONTADOR
AFTER INSERT OR DELETE
ON T_ZB_APOIO
FOR EACH ROW
BEGIN
    IF INSERTING THEN
        UPDATE T_ZB_OCORRENCIA
           SET qt_apoios = qt_apoios + 1
         WHERE id_ocorrencia = :NEW.id_ocorrencia;
    ELSE
        UPDATE T_ZB_OCORRENCIA
           SET qt_apoios = GREATEST(qt_apoios - 1, 0)
         WHERE id_ocorrencia = :OLD.id_ocorrencia;
    END IF;
END;
/

-- Verificação: lista objetos inválidos (deve retornar zero linhas)
SELECT object_name, object_type, status
  FROM user_objects
 WHERE (object_name LIKE '%ZB%' OR object_name = 'PKG_ZELABAIRRO')
   AND status = 'INVALID';
