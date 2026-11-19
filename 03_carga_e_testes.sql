-- =====================================================================
-- ZelaBairro - Global Solution 2 (CitizenTech)
-- Script 03: Carga inicial + demonstração das rotinas PL/SQL
-- Pré-requisitos: 01_ddl.sql e 02_plsql.sql
-- Dica: no SQL Developer, abra a aba "Saída DBMS" para ver as mensagens.
-- =====================================================================

SET SERVEROUTPUT ON SIZE UNLIMITED;

-- ---------------------------------------------------------------------
-- 1) Dados de apoio (órgãos, categorias, bairros, cidadãos)
-- ---------------------------------------------------------------------
INSERT INTO T_ZB_ORGAO (nm_orgao, ds_email, nr_prazo_dias_sla) VALUES ('Departamento de Iluminação Pública', 'ilume@prefeitura.exemplo.gov.br', 5);
INSERT INTO T_ZB_ORGAO (nm_orgao, ds_email, nr_prazo_dias_sla) VALUES ('Secretaria de Obras', 'obras@prefeitura.exemplo.gov.br', 15);
INSERT INTO T_ZB_ORGAO (nm_orgao, ds_email, nr_prazo_dias_sla) VALUES ('Secretaria da Pessoa com Deficiência', 'smped@prefeitura.exemplo.gov.br', 10);
INSERT INTO T_ZB_ORGAO (nm_orgao, ds_email, nr_prazo_dias_sla) VALUES ('Defesa Civil / Drenagem Urbana', 'defesacivil@prefeitura.exemplo.gov.br', 3);
INSERT INTO T_ZB_ORGAO (nm_orgao, ds_email, nr_prazo_dias_sla) VALUES ('Serviço de Limpeza Urbana', 'limpeza@prefeitura.exemplo.gov.br', 4);
INSERT INTO T_ZB_ORGAO (nm_orgao, ds_email, nr_prazo_dias_sla) VALUES ('Companhia de Engenharia de Tráfego', 'transito@prefeitura.exemplo.gov.br', 7);
INSERT INTO T_ZB_ORGAO (nm_orgao, ds_email, nr_prazo_dias_sla) VALUES ('Secretaria do Verde e Meio Ambiente', 'verde@prefeitura.exemplo.gov.br', 20);

-- Os códigos (cd_categoria) são os mesmos do enum Categoria do aplicativo
INSERT INTO T_ZB_CATEGORIA (cd_categoria, nm_categoria, nr_peso, id_orgao)
VALUES ('ILUMINACAO', 'Iluminação pública', 3, (SELECT id_orgao FROM T_ZB_ORGAO WHERE nm_orgao = 'Departamento de Iluminação Pública'));
INSERT INTO T_ZB_CATEGORIA (cd_categoria, nm_categoria, nr_peso, id_orgao)
VALUES ('BURACO_VIA', 'Buraco na via', 3, (SELECT id_orgao FROM T_ZB_ORGAO WHERE nm_orgao = 'Secretaria de Obras'));
INSERT INTO T_ZB_CATEGORIA (cd_categoria, nm_categoria, nr_peso, id_orgao)
VALUES ('CALCADA_ACESSIBILIDADE', 'Calçada e acessibilidade', 4, (SELECT id_orgao FROM T_ZB_ORGAO WHERE nm_orgao = 'Secretaria da Pessoa com Deficiência'));
INSERT INTO T_ZB_CATEGORIA (cd_categoria, nm_categoria, nr_peso, id_orgao)
VALUES ('ALAGAMENTO', 'Alagamento e drenagem', 4, (SELECT id_orgao FROM T_ZB_ORGAO WHERE nm_orgao = 'Defesa Civil / Drenagem Urbana'));
INSERT INTO T_ZB_CATEGORIA (cd_categoria, nm_categoria, nr_peso, id_orgao)
VALUES ('LIXO_ENTULHO', 'Lixo e entulho', 2, (SELECT id_orgao FROM T_ZB_ORGAO WHERE nm_orgao = 'Serviço de Limpeza Urbana'));
INSERT INTO T_ZB_CATEGORIA (cd_categoria, nm_categoria, nr_peso, id_orgao)
VALUES ('SINALIZACAO', 'Sinalização de trânsito', 2, (SELECT id_orgao FROM T_ZB_ORGAO WHERE nm_orgao = 'Companhia de Engenharia de Tráfego'));
INSERT INTO T_ZB_CATEGORIA (cd_categoria, nm_categoria, nr_peso, id_orgao)
VALUES ('AREA_VERDE', 'Praças e áreas verdes', 1, (SELECT id_orgao FROM T_ZB_ORGAO WHERE nm_orgao = 'Secretaria do Verde e Meio Ambiente'));

INSERT INTO T_ZB_BAIRRO (nm_bairro, nm_cidade, sg_uf) VALUES ('Vila Mariana', 'São Paulo', 'SP');
INSERT INTO T_ZB_BAIRRO (nm_bairro, nm_cidade, sg_uf) VALUES ('Jabaquara', 'São Paulo', 'SP');
INSERT INTO T_ZB_BAIRRO (nm_bairro, nm_cidade, sg_uf) VALUES ('Saúde', 'São Paulo', 'SP');

INSERT INTO T_ZB_CIDADAO (nm_cidadao, ds_email, nr_telefone, id_bairro)
VALUES ('Ana Souza', 'ana.souza@email.com', '11990000001', (SELECT id_bairro FROM T_ZB_BAIRRO WHERE nm_bairro = 'Vila Mariana'));
INSERT INTO T_ZB_CIDADAO (nm_cidadao, ds_email, nr_telefone, id_bairro)
VALUES ('Bruno Lima', 'bruno.lima@email.com', '11990000002', (SELECT id_bairro FROM T_ZB_BAIRRO WHERE nm_bairro = 'Vila Mariana'));
INSERT INTO T_ZB_CIDADAO (nm_cidadao, ds_email, nr_telefone, id_bairro)
VALUES ('Carla Mendes', 'carla.mendes@email.com', '11990000003', (SELECT id_bairro FROM T_ZB_BAIRRO WHERE nm_bairro = 'Jabaquara'));
INSERT INTO T_ZB_CIDADAO (nm_cidadao, ds_email, nr_telefone, id_bairro)
VALUES ('Diego Rocha', 'diego.rocha@email.com', '11990000004', (SELECT id_bairro FROM T_ZB_BAIRRO WHERE nm_bairro = 'Saúde'));
INSERT INTO T_ZB_CIDADAO (nm_cidadao, ds_email, nr_telefone, id_bairro)
VALUES ('Elisa Prado', 'elisa.prado@email.com', '11990000005', (SELECT id_bairro FROM T_ZB_BAIRRO WHERE nm_bairro = 'Saúde'));

COMMIT;

-- ---------------------------------------------------------------------
-- 2) Fluxo completo: registro, apoios, mudanças de status e reabertura
-- ---------------------------------------------------------------------
DECLARE
    TYPE t_ids IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    v_oc    t_ids;
    v_id    NUMBER;
    v_prot  VARCHAR2(20);

    v_ana   NUMBER; v_bruno NUMBER; v_carla NUMBER; v_diego NUMBER; v_elisa NUMBER;
    v_vm    NUMBER; v_jb    NUMBER; v_sd    NUMBER;

    FUNCTION cidadao (p_email VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        SELECT id_cidadao INTO v FROM T_ZB_CIDADAO WHERE ds_email = p_email;
        RETURN v;
    END;

    FUNCTION bairro (p_nome VARCHAR2) RETURN NUMBER IS
        v NUMBER;
    BEGIN
        SELECT id_bairro INTO v FROM T_ZB_BAIRRO WHERE nm_bairro = p_nome;
        RETURN v;
    END;
BEGIN
    v_ana := cidadao('ana.souza@email.com');     v_bruno := cidadao('bruno.lima@email.com');
    v_carla := cidadao('carla.mendes@email.com'); v_diego := cidadao('diego.rocha@email.com');
    v_elisa := cidadao('elisa.prado@email.com');
    v_vm := bairro('Vila Mariana'); v_jb := bairro('Jabaquara'); v_sd := bairro('Saúde');

    -- Funcionalidade 1: registro (protocolo, prioridade e histórico automáticos)
    PKG_ZELABAIRRO.registrar_ocorrencia(v_ana, 'ILUMINACAO', v_vm, 'Poste apagado há uma semana',
        'Rua fica totalmente escura à noite, moradores com medo de voltar do ponto de ônibus.',
        'Rua Domingos de Morais, 2100', 'Em frente à padaria', 'N', v_id, v_prot);
    v_oc(1) := v_id; DBMS_OUTPUT.PUT_LINE('Registrada: ' || v_prot);

    PKG_ZELABAIRRO.registrar_ocorrencia(v_bruno, 'CALCADA_ACESSIBILIDADE', v_vm, 'Calçada quebrada impede cadeirante',
        'Raízes levantaram o piso e não há rebaixamento na esquina. Cadeirantes precisam ir pela rua.',
        'Rua França Pinto, 450', 'Esquina com a Rua Joaquim Távora', 'S', v_id, v_prot);
    v_oc(2) := v_id; DBMS_OUTPUT.PUT_LINE('Registrada: ' || v_prot);

    PKG_ZELABAIRRO.registrar_ocorrencia(v_carla, 'ALAGAMENTO', v_jb, 'Bueiro entupido causa alagamento',
        'Qualquer chuva forte alaga a rua e a água invade as casas mais baixas.',
        'Av. Eng. Armando de Arruda Pereira, 3000', 'Perto do terminal', 'S', v_id, v_prot);
    v_oc(3) := v_id; DBMS_OUTPUT.PUT_LINE('Registrada: ' || v_prot);

    PKG_ZELABAIRRO.registrar_ocorrencia(v_diego, 'LIXO_ENTULHO', v_sd, 'Entulho descartado na praça',
        'Restos de obra jogados no canteiro da praça, atraindo ratos.',
        'Praça Santa Rita de Cássia', 'Ao lado do parquinho', 'N', v_id, v_prot);
    v_oc(4) := v_id; DBMS_OUTPUT.PUT_LINE('Registrada: ' || v_prot);

    PKG_ZELABAIRRO.registrar_ocorrencia(v_elisa, 'BURACO_VIA', v_sd, 'Buraco grande na faixa da direita',
        'Buraco com cerca de 1 metro de diâmetro, motos já caíram no local.',
        'Av. Jabaquara, 1200', 'Sentido centro', 'N', v_id, v_prot);
    v_oc(5) := v_id; DBMS_OUTPUT.PUT_LINE('Registrada: ' || v_prot);

    PKG_ZELABAIRRO.registrar_ocorrencia(v_carla, 'SINALIZACAO', v_jb, 'Semáforo de pedestres sem som',
        'O sinal sonoro para pessoas com deficiência visual parou de funcionar.',
        'Rua das Rosas, 80', 'Travessia da escola estadual', 'S', v_id, v_prot);
    v_oc(6) := v_id; DBMS_OUTPUT.PUT_LINE('Registrada: ' || v_prot);

    -- Simula que as ocorrências foram abertas em dias anteriores (para SLA e indicadores)
    UPDATE T_ZB_OCORRENCIA SET dt_abertura = SYSDATE - 12 WHERE id_ocorrencia = v_oc(1);
    UPDATE T_ZB_OCORRENCIA SET dt_abertura = SYSDATE - 10 WHERE id_ocorrencia = v_oc(2);
    UPDATE T_ZB_OCORRENCIA SET dt_abertura = SYSDATE - 6  WHERE id_ocorrencia = v_oc(3);
    UPDATE T_ZB_OCORRENCIA SET dt_abertura = SYSDATE - 3  WHERE id_ocorrencia = v_oc(4);
    UPDATE T_ZB_OCORRENCIA SET dt_abertura = SYSDATE - 35 WHERE id_ocorrencia = v_oc(5);
    UPDATE T_ZB_OCORRENCIA SET dt_abertura = SYSDATE - 2  WHERE id_ocorrencia = v_oc(6);
    UPDATE T_ZB_HISTORICO_STATUS h
       SET dt_alteracao = (SELECT o.dt_abertura FROM T_ZB_OCORRENCIA o WHERE o.id_ocorrencia = h.id_ocorrencia);
    COMMIT;

    -- Funcionalidade 3: apoios (trigger atualiza contador e prioridade)
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(1), v_bruno);
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(1), v_carla);
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(1), v_diego);
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(1), v_elisa);
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(2), v_ana);
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(2), v_carla);
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(2), v_diego);
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(3), v_ana);
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(3), v_diego);
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(3), v_elisa);
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(5), v_diego);

    -- Remoção de apoio (trigger decrementa o contador)
    PKG_ZELABAIRRO.apoiar_ocorrencia(v_oc(4), v_ana);
    PKG_ZELABAIRRO.remover_apoio(v_oc(4), v_ana);

    -- Atendimento pelo órgão público (trigger grava o histórico)
    PKG_ZELABAIRRO.atualizar_status(v_oc(1), 'EM_ANALISE');
    PKG_ZELABAIRRO.atualizar_status(v_oc(1), 'EM_ATENDIMENTO', 'Troca da lâmpada agendada para sexta-feira.');
    PKG_ZELABAIRRO.atualizar_status(v_oc(2), 'EM_ANALISE');
    PKG_ZELABAIRRO.atualizar_status(v_oc(4), 'EM_ANALISE');
    PKG_ZELABAIRRO.atualizar_status(v_oc(4), 'EM_ATENDIMENTO');
    PKG_ZELABAIRRO.atualizar_status(v_oc(4), 'RESOLVIDA', 'Entulho removido e área limpa.');
    PKG_ZELABAIRRO.atualizar_status(v_oc(5), 'EM_ANALISE');
    PKG_ZELABAIRRO.atualizar_status(v_oc(5), 'EM_ATENDIMENTO');
    PKG_ZELABAIRRO.atualizar_status(v_oc(5), 'RESOLVIDA');

    -- Cidadã informa que o buraco continua -> volta para análise
    PKG_ZELABAIRRO.reabrir_ocorrencia(v_oc(5), v_elisa, 'Taparam só metade do buraco.');

    DBMS_OUTPUT.PUT_LINE('Fluxo de demonstração concluído.');
END;
/

-- ---------------------------------------------------------------------
-- 3) Testes de regras de negócio (devem ser BLOQUEADOS)
-- ---------------------------------------------------------------------
DECLARE
    v_id    NUMBER;
    v_autor NUMBER;
    v_prot  VARCHAR2(20);
BEGIN
    SELECT id_ocorrencia, id_cidadao INTO v_id, v_autor
      FROM T_ZB_OCORRENCIA WHERE ds_titulo LIKE 'Bueiro%';

    -- a) Pular etapas: REGISTRADA -> RESOLVIDA
    BEGIN
        PKG_ZELABAIRRO.atualizar_status(v_id, 'RESOLVIDA');
    EXCEPTION
        WHEN PKG_ZELABAIRRO.e_transicao_invalida THEN
            DBMS_OUTPUT.PUT_LINE('[OK] Bloqueado: ' || SQLERRM);
    END;

    -- b) Autor apoiando a própria ocorrência
    BEGIN
        PKG_ZELABAIRRO.apoiar_ocorrencia(v_id, v_autor);
    EXCEPTION
        WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('[OK] Bloqueado: ' || SQLERRM);
    END;

    -- c) Categoria inexistente
    BEGIN
        PKG_ZELABAIRRO.registrar_ocorrencia(v_autor, 'FOGUETE', 1, 'Teste inválido', 'Descrição de teste inválido',
                                            'Rua X, 1', NULL, 'N', v_id, v_prot);
    EXCEPTION
        WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('[OK] Bloqueado: ' || SQLERRM);
    END;
END;
/

-- ---------------------------------------------------------------------
-- 4) Indicadores mensais (mês atual e anterior)
-- ---------------------------------------------------------------------
BEGIN
    PKG_ZELABAIRRO.gerar_indicadores_mensais;
    PKG_ZELABAIRRO.gerar_indicadores_mensais(TO_NUMBER(TO_CHAR(ADD_MONTHS(SYSDATE, -1), 'YYYYMM')));
END;
/

-- ---------------------------------------------------------------------
-- 5) Consulta dinâmica (SYS_REFCURSOR) e relatório de SLA
-- ---------------------------------------------------------------------
DECLARE
    v_cursor     SYS_REFCURSOR;
    v_protocolo  VARCHAR2(20);
    v_titulo     VARCHAR2(80);
    v_categoria  VARCHAR2(60);
    v_bairro     VARCHAR2(60);
    v_status     VARCHAR2(20);
    v_prioridade VARCHAR2(5);
    v_apoios     NUMBER;
    v_abertura   DATE;
    v_jabaquara  NUMBER;
    v_saude      NUMBER;

    PROCEDURE imprimir (p_titulo VARCHAR2) IS
    BEGIN
        DBMS_OUTPUT.PUT_LINE(CHR(10) || '--- ' || p_titulo || ' ---');
        LOOP
            FETCH v_cursor INTO v_protocolo, v_titulo, v_categoria, v_bairro,
                                v_status, v_prioridade, v_apoios, v_abertura;
            EXIT WHEN v_cursor%NOTFOUND;
            DBMS_OUTPUT.PUT_LINE(v_protocolo || ' | ' || RPAD(v_titulo, 35) || ' | ' || v_bairro ||
                                 ' | ' || v_status || ' | ' || v_prioridade || ' | apoios: ' || v_apoios);
        END LOOP;
        CLOSE v_cursor;
    END;
BEGIN
    -- Em PL/SQL não é permitido subquery como parâmetro: buscamos os ids antes
    SELECT id_bairro INTO v_jabaquara FROM T_ZB_BAIRRO WHERE nm_bairro = 'Jabaquara';
    SELECT id_bairro INTO v_saude     FROM T_ZB_BAIRRO WHERE nm_bairro = 'Saúde';

    v_cursor := PKG_ZELABAIRRO.listar_ocorrencias(p_ordenacao => 'PRIORIDADE');
    imprimir('Todas, por prioridade');

    v_cursor := PKG_ZELABAIRRO.listar_ocorrencias(p_status => 'EM_ANALISE', p_ordenacao => 'APOIOS');
    imprimir('Somente EM_ANALISE, por apoios');

    v_cursor := PKG_ZELABAIRRO.listar_ocorrencias(p_id_bairro => v_jabaquara, p_ordenacao => 'DATA');
    imprimir('Bairro Jabaquara, mais recentes');

    DBMS_OUTPUT.PUT_LINE(CHR(10) || 'Taxa de resolução Saúde: ' ||
        PKG_ZELABAIRRO.fn_taxa_resolucao_bairro(v_saude) || '%');

    DBMS_OUTPUT.PUT_LINE('');
    PKG_ZELABAIRRO.relatorio_fora_do_prazo;
END;
/

-- ---------------------------------------------------------------------
-- 6) Conferência dos resultados
-- ---------------------------------------------------------------------
SELECT o.cd_protocolo, o.ds_titulo, c.cd_categoria, o.fl_acessibilidade,
       o.qt_apoios, o.nr_pontuacao, o.ds_prioridade, o.st_ocorrencia,
       TO_CHAR(o.dt_abertura, 'DD/MM/YYYY') AS abertura,
       TO_CHAR(o.dt_resolucao, 'DD/MM/YYYY') AS resolucao
  FROM T_ZB_OCORRENCIA o
  JOIN T_ZB_CATEGORIA c ON c.id_categoria = o.id_categoria
 ORDER BY o.nr_pontuacao DESC;

SELECT o.cd_protocolo, h.st_anterior, h.st_novo, h.ds_observacao,
       TO_CHAR(h.dt_alteracao, 'DD/MM/YYYY HH24:MI') AS quando
  FROM T_ZB_HISTORICO_STATUS h
  JOIN T_ZB_OCORRENCIA o ON o.id_ocorrencia = h.id_ocorrencia
 ORDER BY o.cd_protocolo, h.id_historico;

SELECT i.nr_ano_mes, b.nm_bairro, c.nm_categoria, i.qt_registradas, i.qt_resolvidas,
       i.qt_fora_prazo, i.qt_apoios, i.nr_tempo_medio_horas, i.pc_resolucao
  FROM T_ZB_INDICADOR_MENSAL i
  JOIN T_ZB_BAIRRO b    ON b.id_bairro = i.id_bairro
  JOIN T_ZB_CATEGORIA c ON c.id_categoria = i.id_categoria
 ORDER BY i.nr_ano_mes DESC, b.nm_bairro;

SELECT nm_rotina, cd_erro, ds_erro, TO_CHAR(dt_erro, 'DD/MM/YYYY HH24:MI') AS quando
  FROM T_ZB_LOG_ERRO
 ORDER BY id_log;
