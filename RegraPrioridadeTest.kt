package br.com.fiap.zelabairro

import br.com.fiap.zelabairro.model.Categoria
import br.com.fiap.zelabairro.model.Prioridade
import br.com.fiap.zelabairro.model.RegraPrioridade
import br.com.fiap.zelabairro.model.StatusOcorrencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Testes da regra de priorização (deve bater com FN_ZB_CALCULAR_PONTUACAO no Oracle). */
class RegraPrioridadeTest {

    @Test
    fun `area verde sem apoios e baixa`() {
        assertEquals(Prioridade.BAIXA, RegraPrioridade.calcular(Categoria.AREA_VERDE, false, 0))
    }

    @Test
    fun `calcada com acessibilidade comeca como media`() {
        // 4 (peso) + 3 (acessibilidade) = 7
        assertEquals(7, RegraPrioridade.pontuacao(Categoria.CALCADA_ACESSIBILIDADE, true, 0))
        assertEquals(Prioridade.MEDIA, RegraPrioridade.calcular(Categoria.CALCADA_ACESSIBILIDADE, true, 0))
    }

    @Test
    fun `apoios da comunidade elevam para alta`() {
        // 3 + 0 + 7 = 10
        assertEquals(Prioridade.ALTA, RegraPrioridade.calcular(Categoria.ILUMINACAO, false, 7))
    }

    @Test
    fun `apoios sao limitados a dez pontos`() {
        assertEquals(
            RegraPrioridade.pontuacao(Categoria.LIXO_ENTULHO, false, 10),
            RegraPrioridade.pontuacao(Categoria.LIXO_ENTULHO, false, 500)
        )
    }

    @Test
    fun `fluxo de status termina em resolvida`() {
        assertEquals(StatusOcorrencia.EM_ANALISE, StatusOcorrencia.REGISTRADA.proximo())
        assertEquals(StatusOcorrencia.RESOLVIDA, StatusOcorrencia.EM_ATENDIMENTO.proximo())
        assertNull(StatusOcorrencia.RESOLVIDA.proximo())
    }
}
