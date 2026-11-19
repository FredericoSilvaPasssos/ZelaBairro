package br.com.fiap.zelabairro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.fiap.zelabairro.data.local.OcorrenciaEntity
import br.com.fiap.zelabairro.model.Prioridade
import br.com.fiap.zelabairro.model.StatusOcorrencia
import br.com.fiap.zelabairro.ui.theme.AzulInfo
import br.com.fiap.zelabairro.ui.theme.VermelhoAlerta
import br.com.fiap.zelabairro.ui.theme.VerdeSucesso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Cor associada a cada status (sempre acompanhada de texto, nunca só cor). */
fun StatusOcorrencia.cor(): Color = when (this) {
    StatusOcorrencia.REGISTRADA -> Color(0xFF5D4037)
    StatusOcorrencia.EM_ANALISE -> AzulInfo
    StatusOcorrencia.EM_ATENDIMENTO -> Color(0xFF6A1B9A)
    StatusOcorrencia.RESOLVIDA -> VerdeSucesso
}

fun Prioridade.cor(): Color = when (this) {
    Prioridade.ALTA -> VermelhoAlerta
    Prioridade.MEDIA -> Color(0xFF8D6E00)
    Prioridade.BAIXA -> Color(0xFF546E7A)
}

/** Selo colorido com texto (ex.: status ou prioridade). */
@Composable
fun Selo(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = cor,
        contentColor = Color.White,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SeloStatus(status: StatusOcorrencia) = Selo(status.rotulo, status.cor())

@Composable
fun SeloPrioridade(prioridade: Prioridade) = Selo("Prioridade ${prioridade.rotulo}", prioridade.cor())

/** Título de seção marcado como "heading" para leitores de tela (TalkBack). */
@Composable
fun TituloSecao(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.semantics { heading() }
    )
}

/**
 * Cartão de ocorrência usado na lista e no painel.
 * O botão de apoio usa stateDescription para o TalkBack anunciar "apoiada"/"não apoiada".
 */
@Composable
fun OcorrenciaCard(
    ocorrencia: OcorrenciaEntity,
    onClick: () -> Unit,
    onApoiar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SeloStatus(ocorrencia.status)
                    if (ocorrencia.status.pendente) SeloPrioridade(ocorrencia.prioridade)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = ocorrencia.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = ocorrencia.categoria.rotulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${ocorrencia.bairro} • ${ocorrencia.endereco}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Protocolo ${ocorrencia.protocolo} • ${formatarData(ocorrencia.criadaEm)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalIconToggleButton(
                    checked = ocorrencia.apoiadaPorMim,
                    onCheckedChange = { onApoiar() },
                    enabled = ocorrencia.status.pendente,
                    modifier = Modifier.semantics {
                        stateDescription = if (ocorrencia.apoiadaPorMim) "apoiada por você" else "não apoiada"
                    }
                ) {
                    Icon(
                        imageVector = if (ocorrencia.apoiadaPorMim) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Também sou afetado"
                    )
                }
                Text(
                    text = "${ocorrencia.qtdApoios}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/** Cartão numérico do painel de indicadores. */
@Composable
fun IndicadorCard(titulo: String, valor: String, cor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = cor
            )
            Text(text = titulo, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Barra horizontal simples (gráfico) desenhada com Box, sem bibliotecas extras. */
@Composable
fun BarraProporcional(rotulo: String, valor: Int, maximo: Int, modifier: Modifier = Modifier) {
    val fracao = if (maximo <= 0) 0f else valor.toFloat() / maximo
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Row(Modifier.fillMaxWidth()) {
            Text(rotulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("$valor", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(5.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fracao.coerceIn(0.02f, 1f))
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp))
            )
        }
    }
}

/** Mensagem amigável para listas vazias. */
@Composable
fun EstadoVazio(titulo: String, descricao: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            descricao,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- Formatação ----------

fun formatarData(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(millis))

/** Converte milissegundos em texto amigável: "3 dias e 4 h" / "5 h". */
fun formatarDuracao(millis: Double?): String {
    if (millis == null || millis <= 0) return "—"
    val horasTotais = TimeUnit.MILLISECONDS.toHours(millis.toLong())
    val dias = horasTotais / 24
    val horas = horasTotais % 24
    return when {
        dias == 0L -> "$horas h"
        horas == 0L -> "$dias ${if (dias == 1L) "dia" else "dias"}"
        else -> "$dias ${if (dias == 1L) "dia" else "dias"} e $horas h"
    }
}
