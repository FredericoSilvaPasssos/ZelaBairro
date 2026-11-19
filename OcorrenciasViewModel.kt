package br.com.fiap.zelabairro.ui.screens.ocorrencias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.zelabairro.data.local.OcorrenciaEntity
import br.com.fiap.zelabairro.data.repository.OcorrenciaRepository
import br.com.fiap.zelabairro.model.Categoria
import br.com.fiap.zelabairro.model.FiltroOcorrencias
import br.com.fiap.zelabairro.model.StatusOcorrencia
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Funcionalidade 2 - Acompanhar ocorrências com busca e filtros. */
class OcorrenciasViewModel(private val repository: OcorrenciaRepository) : ViewModel() {

    private val _filtro = MutableStateFlow(FiltroOcorrencias())
    val filtro: StateFlow<FiltroOcorrencias> = _filtro.asStateFlow()

    /**
     * Lista reativa: muda sozinha quando o filtro ou o banco mudam.
     * - debounce(250): espera o usuário parar de digitar antes de consultar o banco.
     * - flatMapLatest: cancela a consulta anterior quando o filtro muda (sem trabalho inútil).
     * - null significa "carregando".
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val ocorrencias: StateFlow<List<OcorrenciaEntity>?> = _filtro
        .debounce(250)
        .flatMapLatest { repository.observarOcorrencias(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun atualizarBusca(texto: String) = _filtro.update { it.copy(busca = texto.take(60)) }

    fun selecionarStatus(status: StatusOcorrencia?) = _filtro.update { it.copy(status = status) }

    fun selecionarCategoria(categoria: Categoria?) = _filtro.update { it.copy(categoria = categoria) }

    fun limparFiltros() = _filtro.update { FiltroOcorrencias() }

    fun alternarApoio(id: Long) {
        viewModelScope.launch { repository.alternarApoio(id) }
    }
}
