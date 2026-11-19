package br.com.fiap.zelabairro.ui.screens.nova

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.zelabairro.model.Categoria
import br.com.fiap.zelabairro.ui.AppViewModelProvider
import br.com.fiap.zelabairro.ui.components.SeloPrioridade
import br.com.fiap.zelabairro.ui.components.TituloSecao

/** Funcionalidade 1 - Formulário de registro de problema em espaço público. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NovaOcorrenciaScreen(
    onConcluido: () -> Unit,
    viewModel: NovaOcorrenciaViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(form.erro) {
        form.erro?.let {
            snackbar.showSnackbar(it)
            viewModel.erroVisto()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar problema") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Campos com * são obrigatórios. Quanto mais detalhes, mais rápido o órgão encontra o local.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TituloSecao("1. Qual é o problema? *")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Categoria.entries.forEach { categoria ->
                    FilterChip(
                        selected = form.categoria == categoria,
                        onClick = { viewModel.onCategoria(categoria) },
                        label = { Text(categoria.rotulo) }
                    )
                }
            }
            if (form.tentouEnviar && !form.categoriaValida) {
                Text("Escolha uma categoria.", color = MaterialTheme.colorScheme.error)
            }
            form.categoria?.let {
                Text(
                    "Será encaminhado para: ${it.orgaoResponsavel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            CampoTexto(
                valor = form.titulo,
                onChange = viewModel::onTitulo,
                rotulo = "Título curto *",
                erro = form.tentouEnviar && !form.tituloValido,
                mensagemErro = "Use pelo menos 5 caracteres."
            )

            TituloSecao("2. Onde fica? *")
            CampoTexto(
                valor = form.bairro,
                onChange = viewModel::onBairro,
                rotulo = "Bairro *",
                erro = form.tentouEnviar && !form.bairroValido,
                mensagemErro = "Informe o bairro."
            )
            CampoTexto(
                valor = form.endereco,
                onChange = viewModel::onEndereco,
                rotulo = "Rua e número aproximado *",
                erro = form.tentouEnviar && !form.enderecoValido,
                mensagemErro = "Informe o endereço."
            )
            CampoTexto(
                valor = form.pontoReferencia,
                onChange = viewModel::onReferencia,
                rotulo = "Ponto de referência (opcional)"
            )

            TituloSecao("3. Conte o que está acontecendo *")
            OutlinedTextField(
                value = form.descricao,
                onValueChange = viewModel::onDescricao,
                label = { Text("Descrição *") },
                isError = form.tentouEnviar && !form.descricaoValida,
                supportingText = {
                    Text(
                        if (form.tentouEnviar && !form.descricaoValida) "Descreva com pelo menos 10 caracteres."
                        else "${form.descricao.length}/500"
                    )
                },
                minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            // Linha inteira clicável (área de toque grande) e anunciada como interruptor
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = form.afetaAcessibilidade,
                            onValueChange = viewModel::onAcessibilidade,
                            role = Role.Switch
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Afeta a acessibilidade?", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Atrapalha cadeirantes, pessoas com deficiência visual, idosos ou carrinhos de bebê.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(checked = form.afetaAcessibilidade, onCheckedChange = null)
                }
            }

            form.prioridadeEstimada?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Prioridade inicial: ", style = MaterialTheme.typography.bodyMedium)
                    SeloPrioridade(it)
                }
                Text(
                    "A prioridade sobe conforme outros moradores apoiam a ocorrência.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = viewModel::enviar,
                enabled = !form.salvando,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (form.salvando) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Enviar ocorrência")
                }
            }
        }
    }

    form.protocoloGerado?.let { protocolo ->
        AlertDialog(
            onDismissRequest = { viewModel.protocoloVisto() },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
            title = { Text("Ocorrência registrada!") },
            text = {
                Text(
                    "Anote seu protocolo: $protocolo\n\nVocê pode acompanhar cada etapa do atendimento na aba Ocorrências."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.protocoloVisto()
                    onConcluido()
                }) { Text("Acompanhar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.protocoloVisto() }) { Text("Registrar outro") }
            }
        )
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    onChange: (String) -> Unit,
    rotulo: String,
    erro: Boolean = false,
    mensagemErro: String = ""
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(rotulo) },
        isError = erro,
        supportingText = { if (erro) Text(mensagemErro) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth()
    )
}
