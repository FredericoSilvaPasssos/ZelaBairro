package br.com.fiap.zelabairro.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.fiap.zelabairro.model.Categoria
import br.com.fiap.zelabairro.model.RegraPrioridade
import br.com.fiap.zelabairro.model.StatusOcorrencia
import java.util.concurrent.TimeUnit

/**
 * Ocorrências de exemplo inseridas na primeira execução, para que o painel
 * e a lista já mostrem dados reais na demonstração.
 */
internal object DadosIniciais {

    private data class Exemplo(
        val protocolo: String,
        val titulo: String,
        val descricao: String,
        val categoria: Categoria,
        val bairro: String,
        val endereco: String,
        val referencia: String,
        val acessibilidade: Boolean,
        val statusFinal: StatusOcorrencia,
        val apoios: Int,
        val diasAtras: Long,
        val horasAteResolver: Long = 0
    )

    private val exemplos = listOf(
        Exemplo(
            "ZB-261101-1024", "Poste apagado há uma semana",
            "Rua fica totalmente escura à noite, moradores com medo de voltar do ponto de ônibus.",
            Categoria.ILUMINACAO, "Vila Mariana", "Rua Domingos de Morais, 2100",
            "Em frente à padaria", false, StatusOcorrencia.EM_ATENDIMENTO, 7, 12
        ),
        Exemplo(
            "ZB-261103-2048", "Calçada quebrada impede cadeirante",
            "Raízes levantaram o piso e não há rebaixamento na esquina. Cadeirantes precisam ir pela rua.",
            Categoria.CALCADA_ACESSIBILIDADE, "Vila Mariana", "Rua França Pinto, 450",
            "Esquina com a Rua Joaquim Távora", true, StatusOcorrencia.EM_ANALISE, 5, 10
        ),
        Exemplo(
            "ZB-261105-3072", "Bueiro entupido causa alagamento",
            "Qualquer chuva forte alaga a rua e a água invade as casas mais baixas.",
            Categoria.ALAGAMENTO, "Jabaquara", "Av. Engenheiro Armando de Arruda Pereira, 3000",
            "Perto do terminal", true, StatusOcorrencia.REGISTRADA, 3, 6
        ),
        Exemplo(
            "ZB-261020-4096", "Entulho descartado na praça",
            "Restos de obra jogados no canteiro da praça, atraindo ratos.",
            Categoria.LIXO_ENTULHO, "Saúde", "Praça Santa Rita de Cássia",
            "Ao lado do parquinho", false, StatusOcorrencia.RESOLVIDA, 2, 30, 72
        ),
        Exemplo(
            "ZB-261025-5120", "Buraco grande na faixa da direita",
            "Buraco com cerca de 1 metro, motos já caíram no local.",
            Categoria.BURACO_VIA, "Saúde", "Av. Jabaquara, 1200",
            "Sentido centro", false, StatusOcorrencia.RESOLVIDA, 4, 25, 120
        ),
        Exemplo(
            "ZB-261108-6144", "Semáforo de pedestres sem som",
            "O sinal sonoro para pessoas com deficiência visual parou de funcionar.",
            Categoria.SINALIZACAO, "Jabaquara", "Rua das Rosas, 80",
            "Travessia da escola estadual", true, StatusOcorrencia.REGISTRADA, 0, 2
        )
    )

    fun popular(db: SupportSQLiteDatabase) {
        val agora = System.currentTimeMillis()
        exemplos.forEach { e ->
            val criadaEm = agora - TimeUnit.DAYS.toMillis(e.diasAtras)
            val resolvidaEm = if (e.statusFinal == StatusOcorrencia.RESOLVIDA) {
                criadaEm + TimeUnit.HOURS.toMillis(e.horasAteResolver)
            } else null
            val prioridade = RegraPrioridade.calcular(e.categoria, e.acessibilidade, e.apoios)

            val valores = ContentValues().apply {
                put("protocolo", e.protocolo)
                put("titulo", e.titulo)
                put("descricao", e.descricao)
                put("categoria", e.categoria.name)
                put("bairro", e.bairro)
                put("endereco", e.endereco)
                put("pontoReferencia", e.referencia)
                put("afetaAcessibilidade", if (e.acessibilidade) 1 else 0)
                put("status", e.statusFinal.name)
                put("qtdApoios", e.apoios)
                put("apoiadaPorMim", 0)
                put("prioridade", prioridade.name)
                put("criadaEm", criadaEm)
                put("atualizadaEm", resolvidaEm ?: criadaEm)
                if (resolvidaEm != null) put("resolvidaEm", resolvidaEm) else putNull("resolvidaEm")
            }
            val id = db.insert("ocorrencias", SQLiteDatabase.CONFLICT_NONE, valores)

            // Gera a linha do tempo até o status atual do exemplo
            val etapas = StatusOcorrencia.entries.takeWhile { it.ordinal <= e.statusFinal.ordinal }
            etapas.forEachIndexed { indice, status ->
                val dataEtapa = if (status == StatusOcorrencia.RESOLVIDA && resolvidaEm != null) {
                    resolvidaEm
                } else {
                    criadaEm + TimeUnit.HOURS.toMillis(indice * 20L)
                }
                db.insert(
                    "historico_status",
                    SQLiteDatabase.CONFLICT_NONE,
                    ContentValues().apply {
                        put("ocorrenciaId", id)
                        put("status", status.name)
                        put("observacao", mensagemPadrao(status, e.categoria))
                        put("dataHora", dataEtapa)
                    }
                )
            }
        }
    }

    /** Textos padrão do histórico, reutilizados pelo repositório. */
    fun mensagemPadrao(status: StatusOcorrencia, categoria: Categoria): String = when (status) {
        StatusOcorrencia.REGISTRADA ->
            "Ocorrência registrada pelo cidadão e encaminhada para ${categoria.orgaoResponsavel}."
        StatusOcorrencia.EM_ANALISE ->
            "${categoria.orgaoResponsavel} recebeu a solicitação e está avaliando o local."
        StatusOcorrencia.EM_ATENDIMENTO ->
            "Equipe designada. O serviço está programado ou em execução."
        StatusOcorrencia.RESOLVIDA ->
            "Serviço concluído pelo órgão responsável. Confirme se o problema foi resolvido."
    }
}
