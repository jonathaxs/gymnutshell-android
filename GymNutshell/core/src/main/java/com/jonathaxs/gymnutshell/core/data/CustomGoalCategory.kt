package com.jonathaxs.gymnutshell.core.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Categoria de meta criada pelo usuário — porte de CustomGoalCategory (iOS, era Codable em UserDefaults).
 * `supportsRestDay` liga o botão ON/OFF de "dia de descanso" em todas as metas que pertencerem a ela.
 * A ordem de exibição vive na ordem unificada de categorias (GoalConfigRepository), não nesta tabela.
 * `symbolName`: chave do ícone (SF Symbol no iOS, default "square.grid.2x2.fill").
 */
@Serializable
@Entity(tableName = "custom_goal_category")
data class CustomGoalCategory(
    @PrimaryKey val id: String,
    val name: String,
    val supportsRestDay: Boolean = false,
    @Ignore val symbolName: String = DEFAULT_SYMBOL_NAME,
) {
    // Construtor usado pelo Room (sem symbolName pra não quebrar schema sem migração configurada)
    constructor(id: String, name: String, supportsRestDay: Boolean) : this(
        id = id,
        name = name,
        supportsRestDay = supportsRestDay,
        symbolName = DEFAULT_SYMBOL_NAME,
    )

    // Construtor de conveniência espelhando o iOS
    @Ignore
    constructor(name: String, supportsRestDay: Boolean = false, symbolName: String = DEFAULT_SYMBOL_NAME) : this(
        id = java.util.UUID.randomUUID().toString(),
        name = name,
        supportsRestDay = supportsRestDay,
        symbolName = symbolName,
    )

    companion object {
        const val DEFAULT_SYMBOL_NAME = "square.grid.2x2.fill"
    }
}

