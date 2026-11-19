package br.com.fiap.zelabairro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Banco de dados local (SQLite via Room).
 * Implementado como Singleton: uma única instância durante toda a vida do app,
 * evitando abrir várias conexões e desperdiçar memória.
 */
@Database(
    entities = [OcorrenciaEntity::class, HistoricoStatusEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ZelaBairroDatabase : RoomDatabase() {

    abstract fun ocorrenciaDao(): OcorrenciaDao

    companion object {
        private const val NOME_BANCO = "zelabairro.db"

        @Volatile
        private var instancia: ZelaBairroDatabase? = null

        fun getInstance(context: Context): ZelaBairroDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    // applicationContext evita vazamento de memória de uma Activity
                    context.applicationContext,
                    ZelaBairroDatabase::class.java,
                    NOME_BANCO
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Popula exemplos apenas na primeira criação do banco
                            DadosIniciais.popular(db)
                        }
                    })
                    .build()
                    .also { instancia = it }
            }
    }
}
