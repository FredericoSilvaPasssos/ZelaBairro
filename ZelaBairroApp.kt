package br.com.fiap.zelabairro

import android.app.Application
import android.content.Context
import br.com.fiap.zelabairro.data.local.ZelaBairroDatabase
import br.com.fiap.zelabairro.data.repository.OcorrenciaRepository

/**
 * Classe Application: criada uma única vez quando o app abre.
 * Guarda o [AppContainer], uma injeção de dependências manual e simples.
 */
class ZelaBairroApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Container de dependências. O "by lazy" adia a criação do banco até o primeiro uso,
 * deixando a abertura do app mais rápida.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val repository: OcorrenciaRepository by lazy {
        OcorrenciaRepository(ZelaBairroDatabase.getInstance(appContext).ocorrenciaDao())
    }
}
