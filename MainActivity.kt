package br.com.fiap.zelabairro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.fiap.zelabairro.ui.navigation.ZelaBairroNavegacao
import br.com.fiap.zelabairro.ui.theme.ZelaBairroTheme

/** Única Activity do app (Single-Activity): todas as telas são Composables. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZelaBairroTheme {
                ZelaBairroNavegacao()
            }
        }
    }
}
