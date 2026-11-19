package br.com.fiap.zelabairro.ui.screens.nova

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.zelabairro.data.repository.OcorrenciaRepository
import br.com.fiap.zelabairro.model.Categoria
import br.com.fiap.zelabairro.model.NovaOcorrencia
import br.com.fiap.zelabairro.model.Prioridade
import br.com.fiap.zelabairro.model.RegraPrioridade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado do formulário de registro. As validações ficam aqui, fora da tela. */
data class FormularioState(
    val titulo: String = "",
    val descricao: String = "",
    val categoria: Categoria? = null,
    val bairro: String = "",
    val endereco: String = "",
    val pontoReferencia: String = "",
    val afetaAcessibilidade: Boolean = false,
    val tentouEnviar: Boolean = false,
    val salvando: Boolean = false,
    val protocoloGerado: String? = null,
    val erro: String? = null
) {
    val tituloValido get() = titulo.trim().length >= 5
    val descricaoValida get() = descricao.trim().length >= 10
    val categoriaValida get() = categoria != null
    val bairroValido get() = bairro.isNotBlank()
    val enderecoValido get() = endereco.trim().length >= 5
    val valido get() = tituloValido && descricaoValida && categoriaValida && bairroValido && enderecoValido

    /** Prévia da prioridade mostrada antes do envio (transparência para o cidadão). */
    val prioridadeEstimada: Prioridade?
        get() = categoria?.let { RegraPrioridade.calcular(it, afetaAcessibilidade, 0) }
}

/** Funcionalidade 1 - Registrar problema em espaço público. */
class NovaOcorrenciaViewModel(private val repository: OcorrenciaRepository) : ViewModel() {

    private val _form = MutableStateFlow(FormularioState())
    val form: StateFlow<FormularioState> = _form.asStateFlow()

    fun onTitulo(v: String) = _form.update { it.copy(titulo = v.take(80)) }
    fun onDescricao(v: String) = _form.update { it.copy(descricao = v.take(500)) }
    fun onCategoria(v: Categoria) = _form.update { it.copy(categoria = v) }
    fun onBairro(v: String) = _form.update { it.copy(bairro = v.take(60)) }
    fun onEndereco(v: String) = _form.update { it.copy(endereco = v.take(120)) }
    fun onReferencia(v: String) = _form.update { it.copy(pontoReferencia = v.take(120)) }
    fun onAcessibilidade(v: Boolean) = _form.update { it.copy(afetaAcessibilidade = v) }

    fun enviar() {
        val atual = _form.value
        if (atual.salvando) return
        if (!atual.valido) {
            _form.update { it.copy(tentouEnviar = true) }
            return
        }
        _form.update { it.copy(salvando = true, erro = null) }
        viewModelScope.launch {
            try {
                val protocolo = repository.registrar(
                    NovaOcorrencia(
                        titulo = atual.titulo,
                        descricao = atual.descricao,
                        categoria = requireNotNull(atual.categoria),
                        bairro = atual.bairro,
                        endereco = atual.endereco,
                        pontoReferencia = atual.pontoReferencia,
                        afetaAcessibilidade = atual.afetaAcessibilidade
                    )
                )
                // Limpa o formulário e guarda apenas o protocolo para o diálogo de sucesso
                _form.value = FormularioState(protocoloGerado = protocolo)
            } catch (e: Exception) {
                _form.update {
                    it.copy(salvando = false, erro = "Não foi possível salvar. Tente novamente.")
                }
            }
        }
    }

    fun protocoloVisto() = _form.update { it.copy(protocoloGerado = null) }

    fun erroVisto() = _form.update { it.copy(erro = null) }
}
