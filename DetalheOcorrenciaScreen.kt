package br.com.fiap.zelabairro.ui.screens.detalhe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.zelabairro.data.local.HistoricoStatusEntity
import br.com.fiap.zelabairro.data.local.OcorrenciaEntity
import br.com.fiap.zelabairro.model.StatusOcorrencia
import br.com.fiap.zelabairro.ui.AppViewModelProvider
import br.com.fiap.zelabairro.ui.components.EstadoVazio
import br.com.fiap.zelabairro.ui.components.SeloPrioridade
import br.com.fiap.zelabairro.ui.components.SeloStatus
import br.com.fiap.zelabairro.ui.components.TituloSecao
import br.com.fiap.zelabairro.ui.components.cor
import br.com.fiap.zelabairro.ui.components.formatarData

/** Detalhe da ocorrência: dados, apoio comunitário, linha do tempo e confirmação. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalheOcorrenciaScreen(
    onVoltar: () -> Unit,
    viewModel: DetalheOcorrenciaViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mensagem by viewModel.mensagem.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var mostrarReabrir by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mensagem) {
        mensagem?.let {
            snackbar.showSnackbar(it)
            viewModel.mensagemExibida()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.ocorrencia?.protocolo ?: "Ocorrência") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val ocorrencia = state.ocorrencia
        when {
            state.carregando -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            ocorrencia == null -> EstadoVazio(
                "Ocorrência não encontrada",
                "Ela pode ter sido removida.",
                Modifier.padding(padding)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { CartaoDados(ocorrencia) }
                item {
                    CartaoApoio(ocorrencia, onApoiar = viewModel::alternarApoio)
                }
                if (ocorrencia.status == StatusOcorrencia.RESOLVIDA) {
                    item { CartaoConfirmacao(onNaoResolvido = { mostrarReabrir = true }) }
                }
                item { TituloSecao("Linha do tempo do atendimento") }
                itemsIndexed(state.historico, key = { _, h -> h.id }) { indice, h ->
                    ItemLinhaDoTempo(h, ultimo = indice == state.historico.lastIndex)
                }
                ocorrencia.status.proximo()?.let { proximo ->
                    item { CartaoDemonstracao(proximo, onAvancar = viewModel::avancarStatus) }
                }
            }
        }
    }

    if (mostrarReabrir) {
        DialogoReabrir(
            onConfirmar = {
                viewModel.reabrir(it)
                mostrarReabrir = false
            },
            onCancelar = { mostrarReabrir = false }
        )
    }
}

@Composable
private fun CartaoDados(o: OcorrenciaEntity) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SeloStatus(o.status)
                if (o.status.pendente) SeloPrioridade(o.prioridade)
            }
            Text(o.titulo, style = MaterialTheme.typography.titleLarge)
            Text(o.categoria.rotulo, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            Linha("Endereço", "${o.endereco} – ${o.bairro}")
            if (o.pontoReferencia.isNotBlank()) Linha("Referência", o.pontoReferencia)
            Linha("Órgão responsável", o.categoria.orgaoResponsavel)
            Linha("Aberta em", formatarData(o.criadaEm))
            o.resolvidaEm?.let { Linha("Resolvida em", formatarData(it)) }
            if (o.afetaAcessibilidade) Linha("Acessibilidade", "Afeta a mobilidade de pessoas com deficiência ou idosos")
            HorizontalDivider()
            Text(o.descricao, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Linha(rotulo: String, valor: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(rotulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Funcionalidade 3 - Apoio comunitário. */
@Composable
private fun CartaoApoio(o: OcorrenciaEntity, onApoiar: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "${o.qtdApoios} ${if (o.qtdApoios == 1) "vizinho é afetado" else "vizinhos são afetados"} por este problema",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Cada apoio aumenta a prioridade e mostra ao órgão quantas pessoas sofrem com o problema.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            if (!o.status.pendente) {
                Text("Ocorrência resolvida: apoios encerrados.", style = MaterialTheme.typography.bodyMedium)
            } else if (o.apoiadaPorMim) {
                OutlinedButton(onClick = onApoiar, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Remover meu apoio")
                }
            } else {
                Button(onClick = onApoiar, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.ThumbUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Também sou afetado")
                }
            }
        }
    }
}

@Composable
private fun CartaoConfirmacao(onNaoResolvido: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("O problema foi mesmo resolvido?", style = MaterialTheme.typography.titleMedium)
            Text(
                "Se o problema continua no local, avise. A ocorrência volta para análise.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onNaoResolvido, modifier = Modifier.fillMaxWidth()) {
                Text("Não, o problema continua")
            }
        }
    }
}

@Composable
private fun ItemLinhaDoTempo(h: HistoricoStatusEntity, ultimo: Boolean) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
            Box(
                Modifier
                    .size(16.dp)
                    .background(h.status.cor(), CircleShape)
            )
            if (!ultimo) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(h.status.rotulo, style = MaterialTheme.typography.titleMedium, color = h.status.cor())
            Text(formatarData(h.dataHora), style = MaterialTheme.typography.labelMedium)
            Text(h.observacao, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Simula a resposta do órgão público para a apresentação. Em produção essa
 * atualização viria do back-end (procedure PKG_ZELABAIRRO.atualizar_status no Oracle).
 */
@Composable
private fun CartaoDemonstracao(proximo: StatusOcorrencia, onAvancar: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text("Modo demonstração (órgão público)", style = MaterialTheme.typography.labelLarge)
            Text(
                "Simula a atualização feita pela prefeitura.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAvancar, modifier = Modifier.fillMaxWidth()) {
                Text("Avançar para: ${proximo.rotulo}")
            }
        }
    }
}

@Composable
private fun DialogoReabrir(onConfirmar: (String) -> Unit, onCancelar: () -> Unit) {
    var motivo by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Reabrir ocorrência") },
        text = {
            OutlinedTextField(
                value = motivo,
                onValueChange = { motivo = it.take(200) },
                label = { Text("O que ainda está errado?") },
                minLines = 3
            )
        },
        confirmButton = { TextButton(onClick = { onConfirmar(motivo) }) { Text("Reabrir") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
