package com.jonathaxs.gymnutshell.core.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.jonathaxs.gymnutshell.core.domain.CategoryKind
import com.jonathaxs.gymnutshell.core.domain.CategoryKindSerializer
import kotlinx.serialization.Serializable

/**
 * Categoria de meta criada pelo usuário — porte de CustomGoalCategory (iOS, era Codable em UserDefaults).
 * `kind` define o controle das metas na Hoje (padrão, com descanso ou check-in) e pode ser trocado depois.
 * `supportsRestDay` é a coluna antiga do Room e o campo da 1.0 no backup; fica sempre igual a `kind == RestDay`
 * (use [withKind] pra trocar o tipo). `kind` não vai pro Room: fica no DataStore, igual ao `symbolName`.
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
    // Sem `kind` no JSON (backup da 1.0 ou do Android antigo), o tipo sai do supportsRestDay.
    @Ignore @Serializable(with = CategoryKindSerializer::class)
    val kind: CategoryKind = if (supportsRestDay) CategoryKind.RestDay else CategoryKind.Standard,
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

    /** Troca o tipo mantendo `supportsRestDay` coerente (Room e backup antigo leem só ele). */
    fun withKind(newKind: CategoryKind): CustomGoalCategory =
        copy(kind = newKind, supportsRestDay = newKind == CategoryKind.RestDay)

    companion object {
        const val DEFAULT_SYMBOL_NAME = "square.grid.2x2.fill"
    }
}

