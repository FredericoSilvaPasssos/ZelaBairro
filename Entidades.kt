package br.com.fiap.zelabairro.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fiap.zelabairro.model.Categoria
import br.com.fiap.zelabairro.model.Prioridade
import br.com.fiap.zelabairro.model.StatusOcorrencia

/**
 * Tabela local "ocorrencias": um problema registrado em espaço público.
 * Os enums são gravados como TEXT (nome do valor) pelo conversor padrão do Room.
 * Índices em status e categoria aceleram os filtros da tela de acompanhamento.
 */
@Entity(
    tableName = "ocorrencias",
    indices = [
        Index("status"),
        Index("categoria"),
        Index(value = ["protocolo"], unique = true)
    ]
)
data class OcorrenciaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val protocolo: String,
    val titulo: String,
    val descricao: String,
    val categoria: Categoria,
    val bairro: String,
    val endereco: String,
    val pontoReferencia: String,
    val afetaAcessibilidade: Boolean,
    val status: StatusOcorrencia,
    val qtdApoios: Int,
    val apoiadaPorMim: Boolean,
    val prioridade: Prioridade,
    val criadaEm: Long,
    val atualizadaEm: Long,
    val resolvidaEm: Long?
)

/**
 * Tabela local "historico_status": cada mudança de status gera uma linha,
 * formando a linha do tempo exibida ao cidadão. Apagada em cascata com a ocorrência.
 */
@Entity(
    tableName = "historico_status",
    foreignKeys = [
        ForeignKey(
            entity = OcorrenciaEntity::class,
            parentColumns = ["id"],
            childColumns = ["ocorrenciaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ocorrenciaId")]
)
data class HistoricoStatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ocorrenciaId: Long,
    val status: StatusOcorrencia,
    val observacao: String,
    val dataHora: Long
)

/** Resultado da consulta agregada do painel (calculado pelo SQLite, não em memória). */
data class IndicadoresResumo(
    val total: Int,
    val emAberto: Int,
    val emAtendimento: Int,
    val resolvidas: Int,
    val totalApoios: Int,
    val tempoMedioResolucaoMs: Double?
)

/** Quantidade de ocorrências pendentes por categoria (gráfico de barras do painel). */
data class CategoriaContagem(
    val categoria: Categoria,
    val quantidade: Int
)
