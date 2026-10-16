package com.jonathaxs.gymnutshell.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** Acesso às categorias personalizadas (Room) — porte do CustomGoalCategoriesStore (iOS). */
class CustomGoalCategoryRepository(private val context: Context) {

    private val dao = DatabaseProvider.get(context).customGoalCategoryDao()

    private fun symbolKey(id: String) = stringPreferencesKey("customCategory.symbol.$id")

    val categories: Flow<List<CustomGoalCategory>> = dao.observeAll()
        .combine(context.appPreferences.data) { list, prefs ->
            list.map { cat ->
                val symbol = prefs[symbolKey(cat.id)] ?: CustomGoalCategory.DEFAULT_SYMBOL_NAME
                cat.copy(symbolName = symbol)
            }
        }

    suspend fun all(): List<CustomGoalCategory> {
        val list = dao.getAll()
        val prefs = context.appPreferences.data.first()
        return list.map { cat ->
            val symbol = prefs[symbolKey(cat.id)] ?: CustomGoalCategory.DEFAULT_SYMBOL_NAME
            cat.copy(symbolName = symbol)
        }
    }

    suspend fun upsert(category: CustomGoalCategory) {
        dao.upsert(category)
        context.appPreferences.edit { prefs ->
            prefs[symbolKey(category.id)] = category.symbolName
        }
    }

    suspend fun delete(category: CustomGoalCategory) {
        dao.delete(category)
        context.appPreferences.edit { prefs ->
            prefs.remove(symbolKey(category.id))
        }
    }

    /** Substitui todas as categorias personalizadas (restauração de backup). */
    suspend fun replaceAll(categories: List<CustomGoalCategory>) {
        dao.replaceAll(categories)
        context.appPreferences.edit { prefs ->
            categories.forEach { cat ->
                prefs[symbolKey(cat.id)] = cat.symbolName
            }
        }
    }
}

