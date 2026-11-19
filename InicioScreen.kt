package br.com.fiap.zelabairro.ui.screens.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.zelabairro.ui.AppViewModelProvider
import br.com.fiap.zelabairro.ui.components.BarraProporcional
import br.com.fiap.zelabairro.ui.components.IndicadorCard
import br.com.fiap.zelabairro.ui.components.OcorrenciaCard
import br.com.fiap.zelabairro.ui.components.TituloSecao
import br.com.fiap.zelabairro.ui.components.formatarDuracao
import br.com.fiap.zelabairro.ui.theme.AzulInfo
import br.com.fiap.zelabairro.ui.theme.VerdeSucesso
import br.com.fiap.zelabairro.ui.theme.VermelhoAlerta

/** Tela inicial: chamada para registrar + Painel do bairro (Funcionalidade 4). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    onAbrirOcorrencia: (Long) -> Unit,
    onRegistrar: () -> Unit,
    onVerTodas: () -> Unit,
    viewModel: InicioViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    // collectAsStateWithLifecycle: só coleta enquanto a tela está visível
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ZelaBairro", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (state.carregando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CartaoChamada(onRegistrar) }

            item { TituloSecao("Painel do bairro") }

            state.resumo?.let { r ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IndicadorCard("Aguardando análise", "${r.emAberto}", VermelhoAlerta, Modifier.weight(1f))
                        IndicadorCard("Em atendimento", "${r.emAtendimento}", AzulInfo, Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IndicadorCard("Resolvidas", "${r.resolvidas}", VerdeSucesso, Modifier.weight(1f))
                        IndicadorCard(
                            "Apoios de vizinhos",
                            "${r.totalApoios}",
                            MaterialTheme.colorScheme.primary,
                            Modifier.weight(1f)
                        )
                    }
                }
                item {
                    val taxa = if (r.total == 0) 0f else r.resolvidas.toFloat() / r.total
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            Modifier
                                .padding(16.dp)
                                .semantics(mergeDescendants = true) {
                                    contentDescription =
                                        "Taxa de resolução ${(taxa * 100).toInt()} por cento. " +
                                            "Tempo médio de solução ${formatarDuracao(r.tempoMedioResolucaoMs)}"
                                }
                        ) {
                            Text("Taxa de resolução: ${(taxa * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { taxa },
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tempo médio para resolver: ${formatarDuracao(r.tempoMedioResolucaoMs)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (state.pendentesPorCategoria.isNotEmpty()) {
                item { TituloSecao("Problemas pendentes por categoria") }
                val maximo = state.pendentesPorCategoria.maxOf { it.quantidade }
                items(state.pendentesPorCategoria, key = { it.categoria.name }) { c ->
                    BarraProporcional(c.categoria.rotulo, c.quantidade, maximo)
                }
            }

            if (state.maisApoiadas.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TituloSecao("Mais apoiadas pela comunidade", Modifier.weight(1f))
                        TextButton(onClick = onVerTodas) { Text("Ver todas") }
                    }
                }
                items(state.maisApoiadas, key = { it.id }) { ocorrencia ->
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

@Composable
private fun CartaoChamada(onRegistrar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Viu um problema na sua rua?",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Poste apagado, buraco, calçada quebrada ou alagamento: registre em menos de um minuto e acompanhe até a solução.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRegistrar, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.AddCircle, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Registrar problema")
            }
        }
    }
}
