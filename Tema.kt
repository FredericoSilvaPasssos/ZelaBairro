package br.com.fiap.zelabairro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta com contraste mínimo 4.5:1 entre texto e fundo (WCAG AA)
val VerdeCidade = Color(0xFF00695C)
val VerdeEscuro = Color(0xFF00594F)
val VerdeClaro = Color(0xFFB2DFDB)
val Ambar = Color(0xFFFFB300)
val VermelhoAlerta = Color(0xFFB3261E)
val AzulInfo = Color(0xFF1565C0)
val VerdeSucesso = Color(0xFF2E7D32)
val CinzaTexto = Color(0xFF424242)

private val EsquemaClaro = lightColorScheme(
    primary = VerdeCidade,
    onPrimary = Color.White,
    primaryContainer = VerdeClaro,
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Ambar,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFECB3),
    onSecondaryContainer = Color(0xFF2A1800),
    error = VermelhoAlerta,
    background = Color(0xFFF7FAF9),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1C),
    onSurfaceVariant = CinzaTexto
)

private val EsquemaEscuro = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF003731),
    primaryContainer = VerdeEscuro,
    onPrimaryContainer = VerdeClaro,
    secondary = Color(0xFFFFD54F),
    onSecondary = Color.Black,
    error = Color(0xFFF2B8B5)
)

/** Textos um pouco maiores que o padrão para facilitar a leitura por idosos. */
private val TipografiaAcessivel = Typography().run {
    copy(
        bodyLarge = bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
        bodyMedium = bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun ZelaBairroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaEscuro else EsquemaClaro,
        typography = TipografiaAcessivel,
        content = content
    )
}
