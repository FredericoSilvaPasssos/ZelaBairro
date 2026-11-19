package br.com.fiap.zelabairro.model

/**
 * Categorias de problemas urbanos que o cidadão pode registrar.
 *
 * @property rotulo texto exibido na interface.
 * @property peso gravidade base usada no cálculo de prioridade (1 a 4).
 * @property orgaoResponsavel órgão que recebe a ocorrência daquela categoria.
 */
enum class Categoria(val rotulo: String, val peso: Int, val orgaoResponsavel: String) {
    ILUMINACAO("Iluminação pública", 3, "Departamento de Iluminação Pública"),
    BURACO_VIA("Buraco na via", 3, "Secretaria de Obras"),
    CALCADA_ACESSIBILIDADE("Calçada e acessibilidade", 4, "Secretaria da Pessoa com Deficiência"),
    ALAGAMENTO("Alagamento e drenagem", 4, "Defesa Civil / Drenagem Urbana"),
    LIXO_ENTULHO("Lixo e entulho", 2, "Serviço de Limpeza Urbana"),
    SINALIZACAO("Sinalização de trânsito", 2, "Companhia de Engenharia de Tráfego"),
    AREA_VERDE("Praças e áreas verdes", 1, "Secretaria do Verde e Meio Ambiente")
}

/**
 * Ciclo de vida de uma ocorrência. A ordem dos valores é a ordem natural do atendimento.
 */
enum class StatusOcorrencia(val rotulo: String) {
    REGISTRADA("Registrada"),
    EM_ANALISE("Em análise"),
    EM_ATENDIMENTO("Em atendimento"),
    RESOLVIDA("Resolvida");

    /** Próxima etapa do fluxo de atendimento, ou null se já estiver resolvida. */
    fun proximo(): StatusOcorrencia? = when (this) {
        REGISTRADA -> EM_ANALISE
        EM_ANALISE -> EM_ATENDIMENTO
        EM_ATENDIMENTO -> RESOLVIDA
        RESOLVIDA -> null
    }

    /** Indica se a ocorrência ainda precisa de ação do poder público. */
    val pendente: Boolean get() = this != RESOLVIDA
}

/** Prioridade de atendimento calculada automaticamente (ver [RegraPrioridade]). */
enum class Prioridade(val rotulo: String) {
    ALTA("Alta"),
    MEDIA("Média"),
    BAIXA("Baixa")
}

/**
 * Regra de negócio de priorização. É a MESMA regra implementada no Oracle
 * (FN_ZB_CALCULAR_PONTUACAO e FN_ZB_CLASSIFICAR_PRIORIDADE), garantindo que app e
 * banco classifiquem a ocorrência da mesma forma.
 *
 * pontuação = peso da categoria + 3 (se afeta acessibilidade) + apoios (limitado a 10)
 * ALTA >= 10 | MÉDIA >= 6 | BAIXA < 6
 */
object RegraPrioridade {
    const val BONUS_ACESSIBILIDADE = 3
    const val LIMITE_APOIOS = 10
    const val CORTE_ALTA = 10
    const val CORTE_MEDIA = 6

    fun pontuacao(categoria: Categoria, afetaAcessibilidade: Boolean, apoios: Int): Int =
        categoria.peso +
            (if (afetaAcessibilidade) BONUS_ACESSIBILIDADE else 0) +
            apoios.coerceIn(0, LIMITE_APOIOS)

    fun calcular(categoria: Categoria, afetaAcessibilidade: Boolean, apoios: Int): Prioridade {
        val pontos = pontuacao(categoria, afetaAcessibilidade, apoios)
        return when {
            pontos >= CORTE_ALTA -> Prioridade.ALTA
            pontos >= CORTE_MEDIA -> Prioridade.MEDIA
            else -> Prioridade.BAIXA
        }
    }
}

/** Filtros aplicados na tela de acompanhamento de ocorrências. */
data class FiltroOcorrencias(
    val status: StatusOcorrencia? = null,
    val categoria: Categoria? = null,
    val busca: String = ""
)

/** Dados digitados pelo cidadão ao registrar um novo problema. */
data class NovaOcorrencia(
    val titulo: String,
    val descricao: String,
    val categoria: Categoria,
    val bairro: String,
    val endereco: String,
    val pontoReferencia: String,
    val afetaAcessibilidade: Boolean
)
