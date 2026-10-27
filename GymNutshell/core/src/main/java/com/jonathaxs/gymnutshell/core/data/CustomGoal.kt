package com.jonathaxs.gymnutshell.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jonathaxs.gymnutshell.core.domain.CategoryKind
import com.jonathaxs.gymnutshell.core.domain.GoalCategory

/**
 * Meta de rastreio criada pelo usuário — porte de CustomTrackingGoal (iOS, era Codable em UserDefaults).
 * Aqui vira @Entity Room. `categoryRaw` = rawValue de uma GoalCategory fixa, ou null (sem categoria).
 * `customCategoryId` = id de uma CustomGoalCategory criada pelo usuário (preenchido quando a meta pertence a uma).
 * `position` ordena as metas dentro da mesma categoria (reordenação pelo usuário).
 */
@Entity(tableName = "custom_goal")
data class CustomGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emoji: String,
    val name: String,
    val unit: String,
    val target: Int,
    val increment: Int,
    val categoryRaw: String? = null,
    val customCategoryId: String? = null,
    val position: Int = 0,
) {
    /** Chave usada nos intakes/Today (ex.: "custom:3"). */
    val intakeKey: String get() = "custom:$id"

    /**
     * Tipo herdado da categoria — porte de CustomTrackingGoal.kind(in:) (iOS): personalizada (procurada em
     * [categories]) ou fixa (`GoalCategory.kind`). Sem categoria, ou personalizada apagada, cai no padrão.
     */
    fun kind(categories: List<CustomGoalCategory>): CategoryKind {
        if (customCategoryId != null) {
            return categories.firstOrNull { it.id == customCategoryId }?.kind ?: CategoryKind.Standard
        }
        return GoalCategory.fromRaw(categoryRaw)?.kind ?: CategoryKind.Standard
    }
}
