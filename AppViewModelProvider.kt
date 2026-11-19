package br.com.fiap.zelabairro.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.fiap.zelabairro.ZelaBairroApp
import br.com.fiap.zelabairro.ui.screens.detalhe.DetalheOcorrenciaViewModel
import br.com.fiap.zelabairro.ui.screens.inicio.InicioViewModel
import br.com.fiap.zelabairro.ui.screens.nova.NovaOcorrenciaViewModel
import br.com.fiap.zelabairro.ui.screens.ocorrencias.OcorrenciasViewModel

/**
 * Fábrica central de ViewModels: entrega o repositório para cada ViewModel
 * sem que as telas precisem conhecer o banco de dados.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { InicioViewModel(zelaBairroApp().container.repository) }
        initializer { OcorrenciasViewModel(zelaBairroApp().container.repository) }
        initializer { NovaOcorrenciaViewModel(zelaBairroApp().container.repository) }
        initializer {
            DetalheOcorrenciaViewModel(
                createSavedStateHandle(),
                zelaBairroApp().container.repository
            )
        }
    }
}

private fun CreationExtras.zelaBairroApp(): ZelaBairroApp =
    this[APPLICATION_KEY] as ZelaBairroApp
