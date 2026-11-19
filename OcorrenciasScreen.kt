package br.com.fiap.zelabairro.ui.screens.ocorrencias

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.zelabairro.model.Categoria
import br.com.fiap.zelabairro.model.StatusOcorrencia
import br.com.fiap.zelabairro.ui.AppViewModelProvider
import br.com.fiap.zelabairro.ui.components.EstadoVazio
import br.com.fiap.zelabairro.ui.components.OcorrenciaCard

/** Funcionalidade 2 - Lista de ocorrências com busca, filtro por status e por categoria. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcorrenciasScreen(
    onAbrirOcorrencia: (Long) -> Unit,
    viewModel: OcorrenciasViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val filtro by viewModel.filtro.collectAsStateWithLifecycle()
    val ocorrencias by viewModel.ocorrencias.collectAsStateWithLifecycle()
    val temFiltro = filtro.status != null || filtro.categoria != null || filtro.busca.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acompanhar ocorrências") },
                actions = {
                    if (temFiltro) TextButton(onClick = viewModel::limparFiltros) { Text("Limpar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = filtro.busca,
                onValueChange = viewModel::atualizarBusca,
                label = { Text("Buscar por título, bairro ou protocolo") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filtro.busca.isNotEmpty()) {
                        IconButton(onClick = { viewModel.atualizarBusca("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filtro por status
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filtro.status == null,
                        onClick = { viewModel.selecionarStatus(null) },
                        label = { Text("Todos") }
                    )
                }
                items(StatusOcorrencia.entries, key = { it.name }) { status ->
                    FilterChip(
                        selected = filtro.status == status,
                        onClick = {
                            viewModel.selecionarStatus(if (filtro.status == status) null else status)
                        },
                        label = { Text(status.rotulo) }
                    )
                }
            }

            // Filtro por categoria
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Categoria.entries, key = { it.name }) { categoria ->
                    FilterChip(
                        selected = filtro.categoria == categoria,
                        onClick = {
                            viewModel.selecionarCategoria(if (filtro.categoria == categoria) null else categoria)
                        },
                        label = { Text(categoria.rotulo) }
                    )
                }
            }

            val lista = ocorrencias
            when {
                lista == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                lista.isEmpty() -> EstadoVazio(
                    titulo = "Nenhuma ocorrência encontrada",
                    descricao = if (temFiltro) "Tente remover algum filtro." else "Registre o primeiro problema do seu bairro."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "${lista.size} ocorrência(s) • ordenadas por prioridade",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // key estável: o Compose reaproveita os itens e só redesenha o que mudou
                    items(lista, key = { it.id }) { ocorrencia ->
                        OcorrenciaCard(
                            ocorrencia = ocorrencia,
                            onClick = { onAbrirOcorrencia(ocorrencia.id) },
                            onApoiar = { viewModel.alternarApoio(ocorrencia.id) }
                        )
                    }
                }
            }
        }
    }
}
