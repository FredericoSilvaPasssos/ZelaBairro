package br.com.fiap.zelabairro.data.repository

import br.com.fiap.zelabairro.data.local.CategoriaContagem
import br.com.fiap.zelabairro.data.local.DadosIniciais
import br.com.fiap.zelabairro.data.local.HistoricoStatusEntity
import br.com.fiap.zelabairro.data.local.IndicadoresResumo
import br.com.fiap.zelabairro.data.local.OcorrenciaDao
import br.com.fiap.zelabairro.data.local.OcorrenciaEntity
import br.com.fiap.zelabairro.model.FiltroOcorrencias
import br.com.fiap.zelabairro.model.NovaOcorrencia
import br.com.fiap.zelabairro.model.RegraPrioridade
import br.com.fiap.zelabairro.model.StatusOcorrencia
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Repositório: única porta de entrada dos ViewModels para os dados.
 * Concentra as regras de negócio (protocolo, prioridade, transições de status),
 * deixando DAO e telas simples.
 *
 * @param relogio fonte de tempo injetável (facilita testes).
 */
class OcorrenciaRepository(
    private val dao: OcorrenciaDao,
    private val relogio: () -> Long = System::currentTimeMillis
) {

    fun observarOcorrencias(filtro: FiltroOcorrencias): Flow<List<OcorrenciaEntity>> =
        dao.observarFiltradas(filtro.status, filtro.categoria, filtro.busca.trim())

    fun observarOcorrencia(id: Long): Flow<OcorrenciaEntity?> = dao.observarPorId(id)

    fun observarHistorico(id: Long): Flow<List<HistoricoStatusEntity>> = dao.observarHistorico(id)

    fun observarIndicadores(): Flow<IndicadoresResumo> = dao.observarIndicadores()

    fun observarPendentesPorCategoria(): Flow<List<CategoriaContagem>> =
        dao.observarPendentesPorCategoria()

    fun observarMaisApoiadas(limite: Int = 3): Flow<List<OcorrenciaEntity>> =
        dao.observarMaisApoiadas(limite)

    /**
     * Funcionalidade 1 - Registrar ocorrência.
     * Gera protocolo, calcula a prioridade inicial e grava o primeiro histórico.
     * @return protocolo gerado para o cidadão acompanhar a solicitação.
     */
    suspend fun registrar(nova: NovaOcorrencia): String {
        val agora = relogio()
        val protocolo = gerarProtocolo(agora)
        val entidade = OcorrenciaEntity(
            protocolo = protocolo,
            titulo = nova.titulo.trim(),
            descricao = nova.descricao.trim(),
            categoria = nova.categoria,
            bairro = nova.bairro.trim(),
            endereco = nova.endereco.trim(),
            pontoReferencia = nova.pontoReferencia.trim(),
            afetaAcessibilidade = nova.afetaAcessibilidade,
            status = StatusOcorrencia.REGISTRADA,
            qtdApoios = 0,
            apoiadaPorMim = false,
            prioridade = RegraPrioridade.calcular(nova.categoria, nova.afetaAcessibilidade, 0),
            criadaEm = agora,
            atualizadaEm = agora,
            resolvidaEm = null
        )
        dao.registrarComHistorico(
            entidade,
            DadosIniciais.mensagemPadrao(StatusOcorrencia.REGISTRADA, nova.categoria)
        )
        return protocolo
    }

    /**
     * Funcionalidade 3 - Apoio comunitário ("também sou afetado").
     * Alterna o apoio do usuário e recalcula a prioridade.
     * @return true se o usuário passou a apoiar; false se removeu o apoio ou não foi possível.
     */
    suspend fun alternarApoio(id: Long): Boolean {
        val atual = dao.buscarPorId(id) ?: return false
        if (!atual.status.pendente) return false

        val passaAApoiar = !atual.apoiadaPorMim
        val apoios = (if (passaAApoiar) atual.qtdApoios + 1 else atual.qtdApoios - 1).coerceAtLeast(0)
        dao.atualizar(
            atual.copy(
                apoiadaPorMim = passaAApoiar,
                qtdApoios = apoios,
                prioridade = RegraPrioridade.calcular(atual.categoria, atual.afetaAcessibilidade, apoios),
                atualizadaEm = relogio()
            )
        )
        return passaAApoiar
    }

    /**
     * Avança a ocorrência para a próxima etapa (usado no modo demonstração, simulando
     * o retorno do órgão responsável - no sistema real isso vem do Oracle/PL-SQL).
     * @return novo status ou null se já estava resolvida.
     */
    suspend fun avancarStatus(id: Long): StatusOcorrencia? {
        val atual = dao.buscarPorId(id) ?: return null
        val proximo = atual.status.proximo() ?: return null
        val agora = relogio()
        dao.atualizarComHistorico(
            atual.copy(
                status = proximo,
                atualizadaEm = agora,
                resolvidaEm = if (proximo == StatusOcorrencia.RESOLVIDA) agora else null
            ),
            DadosIniciais.mensagemPadrao(proximo, atual.categoria)
        )
        return proximo
    }

    /**
     * O cidadão informa que um problema marcado como resolvido continua acontecendo.
     * A ocorrência volta para "Em análise" e o motivo fica registrado no histórico.
     */
    suspend fun reabrir(id: Long, motivo: String): Boolean {
        val atual = dao.buscarPorId(id) ?: return false
        if (atual.status != StatusOcorrencia.RESOLVIDA) return false
        dao.atualizarComHistorico(
            atual.copy(
                status = StatusOcorrencia.EM_ANALISE,
                atualizadaEm = relogio(),
                resolvidaEm = null
            ),
            "Reaberta pelo cidadão: ${motivo.trim().ifBlank { "o problema continua no local." }}"
        )
        return true
    }

    private fun gerarProtocolo(agora: Long): String {
        val data = SimpleDateFormat("yyMMdd", Locale.ROOT).format(Date(agora))
        return "ZB-$data-${Random.nextInt(1000, 10000)}"
    }
}
