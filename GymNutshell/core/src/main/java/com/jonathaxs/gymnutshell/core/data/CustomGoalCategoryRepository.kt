package com.jonathaxs.gymnutshell.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jonathaxs.gymnutshell.core.domain.CategoryKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** Acesso às categorias personalizadas (Room) — porte do CustomGoalCategoriesStore (iOS). */
class CustomGoalCategoryRepository(private val context: Context) {

    private val dao = DatabaseProvider.get(context).customGoalCategoryDao()

    private fun symbolKey(id: String) = stringPreferencesKey("customCategory.symbol.$id")
    private fun kindKey(id: String) = stringPreferencesKey("customCategory.kind.$id")

    /** Junta ícone e tipo (DataStore) à linha do Room. Sem tipo salvo (criada antes da 2.0), sai do supportsRestDay. */
    private fun merge(cat: CustomGoalCategory, prefs: androidx.datastore.preferences.core.Preferences): CustomGoalCategory {
        val symbol = prefs[symbolKey(cat.id)] ?: CustomGoalCategory.DEFAULT_SYMBOL_NAME
        val kind = prefs[kindKey(cat.id)]?.let(CategoryKind::fromRaw)
            ?: if (cat.supportsRestDay) CategoryKind.RestDay else CategoryKind.Standard
        return cat.copy(symbolName = symbol).withKind(kind)
    }

    val categories: Flow<List<CustomGoalCategory>> = dao.observeAll()
        .combine(context.appPreferences.data) { list, prefs -> list.map { merge(it, prefs) } }

    suspend fun all(): List<CustomGoalCategory> {
        val list = dao.getAll()
        val prefs = context.appPreferences.data.first()
        return list.map { merge(it, prefs) }
    }

    suspend fun upsert(category: CustomGoalCategory) {
        dao.upsert(category)
        context.appPreferences.edit { prefs ->
            prefs[symbolKey(category.id)] = category.symbolName
            prefs[kindKey(category.id)] = category.kind.rawValue
        }
    }

    suspend fun delete(category: CustomGoalCategory) {
        dao.delete(category)
        context.appPreferences.edit { prefs ->
            prefs.remove(symbolKey(category.id))
            prefs.remove(kindKey(category.id))
        }
    }

    /** Substitui todas as categorias personalizadas (restauração de backup). */
    suspend fun replaceAll(categories: List<CustomGoalCategory>) {
        dao.replaceAll(categories)
        context.appPreferences.edit { prefs ->
            categories.forEach { cat ->
                prefs[symbolKey(cat.id)] = cat.symbolName
                prefs[kindKey(cat.id)] = cat.kind.rawValue
            }
        }
    }
}

