package br.com.fiap.zelabairro.ui.screens.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.zelabairro.data.local.CategoriaContagem
import br.com.fiap.zelabairro.data.local.IndicadoresResumo
import br.com.fiap.zelabairro.data.local.OcorrenciaEntity
import br.com.fiap.zelabairro.data.repository.OcorrenciaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado da tela inicial (Funcionalidade 4 - Painel do bairro). */
data class InicioUiState(
    val carregando: Boolean = true,
    val resumo: IndicadoresResumo? = null,
    val pendentesPorCategoria: List<CategoriaContagem> = emptyList(),
    val maisApoiadas: List<OcorrenciaEntity> = emptyList()
)

class InicioViewModel(private val repository: OcorrenciaRepository) : ViewModel() {

    /**
     * Junta três fluxos do banco em um único estado.
     * WhileSubscribed(5000): para de escutar o banco 5s depois que a tela sai de cena
     * (economiza bateria/memória), mas sobrevive a uma rotação de tela.
     */
    val uiState: StateFlow<InicioUiState> = combine(
        repository.observarIndicadores(),
        repository.observarPendentesPorCategoria(),
        repository.observarMaisApoiadas()
    ) { resumo, porCategoria, maisApoiadas ->
        InicioUiState(
            carregando = false,
            resumo = resumo,
            pendentesPorCategoria = porCategoria,
            maisApoiadas = maisApoiadas
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InicioUiState()
    )

    fun alternarApoio(id: Long) {
        viewModelScope.launch { repository.alternarApoio(id) }
    }
}
