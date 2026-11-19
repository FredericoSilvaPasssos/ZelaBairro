package br.com.fiap.zelabairro.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.fiap.zelabairro.ui.screens.detalhe.DetalheOcorrenciaScreen
import br.com.fiap.zelabairro.ui.screens.inicio.InicioScreen
import br.com.fiap.zelabairro.ui.screens.nova.NovaOcorrenciaScreen
import br.com.fiap.zelabairro.ui.screens.ocorrencias.OcorrenciasScreen

/** Rotas de navegação do app. */
object Rotas {
    const val INICIO = "inicio"
    const val OCORRENCIAS = "ocorrencias"
    const val NOVA = "nova"
    const val ARG_ID = "id"
    const val DETALHE = "detalhe/{$ARG_ID}"
    fun detalhe(id: Long) = "detalhe/$id"
}

private data class ItemMenu(val rota: String, val rotulo: String, val icone: ImageVector)

private val itensMenu = listOf(
    ItemMenu(Rotas.INICIO, "Início", Icons.Filled.Home),
    ItemMenu(Rotas.NOVA, "Registrar", Icons.Filled.AddCircle),
    ItemMenu(Rotas.OCORRENCIAS, "Ocorrências", Icons.AutoMirrored.Filled.List)
)

/** Estrutura raiz: barra inferior + grafo de navegação. */
@Composable
fun ZelaBairroNavegacao(navController: NavHostController = rememberNavController()) {
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route
    val mostrarMenu = itensMenu.any { it.rota == rotaAtual }

    Scaffold(
        bottomBar = {
            if (mostrarMenu) {
                NavigationBar {
                    itensMenu.forEach { item ->
                        NavigationBarItem(
                            selected = rotaAtual == item.rota,
                            onClick = { navController.navegarAba(item.rota) },
                            icon = { Icon(item.icone, contentDescription = null) },
                            label = { Text(item.rotulo) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Rotas.INICIO,
            modifier = Modifier.padding(padding)
        ) {
            composable(Rotas.INICIO) {
                InicioScreen(
                    onAbrirOcorrencia = { navController.navigate(Rotas.detalhe(it)) },
                    onRegistrar = { navController.navegarAba(Rotas.NOVA) },
                    onVerTodas = { navController.navegarAba(Rotas.OCORRENCIAS) }
                )
            }
            composable(Rotas.OCORRENCIAS) {
                OcorrenciasScreen(
                    onAbrirOcorrencia = { navController.navigate(Rotas.detalhe(it)) }
                )
            }
            composable(Rotas.NOVA) {
                NovaOcorrenciaScreen(
                    onConcluido = { navController.navegarAba(Rotas.OCORRENCIAS) }
                )
            }
            composable(
                route = Rotas.DETALHE,
                arguments = listOf(navArgument(Rotas.ARG_ID) { type = NavType.LongType })
            ) {
                DetalheOcorrenciaScreen(onVoltar = { navController.popBackStack() })
            }
        }
    }
}

/** Navega entre abas sem empilhar telas repetidas e preservando o estado de cada aba. */
private fun NavHostController.navegarAba(rota: String) {
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
