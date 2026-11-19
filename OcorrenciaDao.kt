package br.com.fiap.zelabairro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.fiap.zelabairro.model.Categoria
import br.com.fiap.zelabairro.model.StatusOcorrencia
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) das ocorrências.
 *
 * - Métodos que retornam Flow mantêm um canal aberto entre o banco e a interface:
 *   qualquer inserção/atualização emite automaticamente a nova lista para a UI.
 * - Métodos suspend rodam fora da thread principal (o Room usa um executor próprio).
 */
@Dao
abstract class OcorrenciaDao {

    /**
     * Lista ocorrências aplicando filtros opcionais (null = sem filtro).
     * Ordena por prioridade, depois por número de apoios e data.
     */
    @Query(
        """
        SELECT * FROM ocorrencias
        WHERE (:status IS NULL OR status = :status)
          AND (:categoria IS NULL OR categoria = :categoria)
          AND (:busca = '' OR titulo LIKE '%' || :busca || '%'
               OR bairro LIKE '%' || :busca || '%'
               OR protocolo LIKE '%' || :busca || '%')
        ORDER BY CASE prioridade WHEN 'ALTA' THEN 0 WHEN 'MEDIA' THEN 1 ELSE 2 END,
                 qtdApoios DESC,
                 criadaEm DESC
        """
    )
    abstract fun observarFiltradas(
        status: StatusOcorrencia?,
        categoria: Categoria?,
        busca: String
    ): Flow<List<OcorrenciaEntity>>

    @Query("SELECT * FROM ocorrencias WHERE id = :id")
    abstract fun observarPorId(id: Long): Flow<OcorrenciaEntity?>

    @Query("SELECT * FROM ocorrencias WHERE id = :id")
    abstract suspend fun buscarPorId(id: Long): OcorrenciaEntity?

    @Query("SELECT * FROM historico_status WHERE ocorrenciaId = :ocorrenciaId ORDER BY dataHora ASC, id ASC")
    abstract fun observarHistorico(ocorrenciaId: Long): Flow<List<HistoricoStatusEntity>>

    /** Ocorrências pendentes com mais apoios da comunidade (destaque do painel). */
    @Query(
        """
        SELECT * FROM ocorrencias
        WHERE status != 'RESOLVIDA'
        ORDER BY qtdApoios DESC, criadaEm DESC
        LIMIT :limite
        """
    )
    abstract fun observarMaisApoiadas(limite: Int): Flow<List<OcorrenciaEntity>>

    /** Indicadores do painel calculados direto no banco (sem carregar a lista inteira). */
    @Query(
        """
        SELECT COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN status IN ('REGISTRADA', 'EM_ANALISE') THEN 1 ELSE 0 END), 0) AS emAberto,
               COALESCE(SUM(CASE WHEN status = 'EM_ATENDIMENTO' THEN 1 ELSE 0 END), 0) AS emAtendimento,
               COALESCE(SUM(CASE WHEN status = 'RESOLVIDA' THEN 1 ELSE 0 END), 0) AS resolvidas,
               COALESCE(SUM(qtdApoios), 0) AS totalApoios,
               AVG(CASE WHEN resolvidaEm IS NOT NULL THEN resolvidaEm - criadaEm END) AS tempoMedioResolucaoMs
        FROM ocorrencias
        """
    )
    abstract fun observarIndicadores(): Flow<IndicadoresResumo>

    @Query(
        """
        SELECT categoria, COUNT(*) AS quantidade
        FROM ocorrencias
        WHERE status != 'RESOLVIDA'
        GROUP BY categoria
        ORDER BY quantidade DESC
        """
    )
    abstract fun observarPendentesPorCategoria(): Flow<List<CategoriaContagem>>

    @Insert
    abstract suspend fun inserir(ocorrencia: OcorrenciaEntity): Long

    @Update
    abstract suspend fun atualizar(ocorrencia: OcorrenciaEntity)

    @Insert
    abstract suspend fun inserirHistorico(historico: HistoricoStatusEntity): Long

    /** Insere a ocorrência e o primeiro registro do histórico na mesma transação. */
    @Transaction
    open suspend fun registrarComHistorico(ocorrencia: OcorrenciaEntity, observacao: String): Long {
        val id = inserir(ocorrencia)
        inserirHistorico(
            HistoricoStatusEntity(
                ocorrenciaId = id,
                status = ocorrencia.status,
                observacao = observacao,
                dataHora = ocorrencia.criadaEm
            )
        )
        return id
    }

    /** Atualiza a ocorrência e grava a mudança de status no histórico (atômico). */
    @Transaction
    open suspend fun atualizarComHistorico(ocorrencia: OcorrenciaEntity, observacao: String) {
        atualizar(ocorrencia)
        inserirHistorico(
            HistoricoStatusEntity(
                ocorrenciaId = ocorrencia.id,
                status = ocorrencia.status,
                observacao = observacao,
                dataHora = ocorrencia.atualizadaEm
            )
        )
    }
}
