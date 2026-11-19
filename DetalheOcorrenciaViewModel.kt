package br.com.fiap.zelabairro.ui.screens.detalhe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.zelabairro.data.local.HistoricoStatusEntity
import br.com.fiap.zelabairro.data.local.OcorrenciaEntity
import br.com.fiap.zelabairro.data.repository.OcorrenciaRepository
import br.com.fiap.zelabairro.ui.navigation.Rotas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalheUiState(
    val carregando: Boolean = true,
    val ocorrencia: OcorrenciaEntity? = null,
    val historico: List<HistoricoStatusEntity> = emptyList()
)

/** Detalhe de uma ocorrência: linha do tempo, apoio e confirmação de resolução. */
class DetalheOcorrenciaViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: OcorrenciaRepository
) : ViewModel() {

    /** O id chega pela rota de navegação e sobrevive à recriação do processo. */
    private val id: Long = checkNotNull(savedStateHandle[Rotas.ARG_ID])

    val uiState: StateFlow<DetalheUiState> = combine(
        repository.observarOcorrencia(id),
        repository.observarHistorico(id)
    ) { ocorrencia, historico ->
        DetalheUiState(carregando = false, ocorrencia = ocorrencia, historico = historico)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalheUiState())

    private val _mensagem = MutableStateFlow<String?>(null)
    /** Mensagens curtas exibidas em Snackbar após uma ação. */
    val mensagem: StateFlow<String?> = _mensagem.asStateFlow()

    fun alternarApoio() {
        viewModelScope.launch {
            val apoiou = repository.alternarApoio(id)
            _mensagem.value = if (apoiou) {
                "Obrigado! Seu apoio aumenta a prioridade desta ocorrência."
            } else {
                "Apoio removido."
            }
        }
    }

    fun avancarStatus() {
        viewModelScope.launch {
            val novo = repository.avancarStatus(id)
            _mensagem.value = novo?.let { "Status atualizado para: ${it.rotulo}" }
        }
    }

    fun reabrir(motivo: String) {
        viewModelScope.launch {
            if (repository.reabrir(id, motivo)) {
                _mensagem.value = "Ocorrência reaberta. O órgão responsável será avisado."
            }
        }
    }

    fun mensagemExibida() {
        _mensagem.value = null
    }
}
