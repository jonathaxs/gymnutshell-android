package com.jonathaxs.gymnutshell.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Acesso ao histórico de dias (Room).
 * Espelha o que no iOS era feito direto no ModelContext do SwiftData.
 */
class DailyRecordRepository(private val context: Context) {

    private val db = DatabaseProvider.get(context)
    private val recordDao = db.dailyRecordDao()

    private object Keys {
        val fiveLevelsMigration = booleanPreferencesKey("migration.achievement.fiveLevels")
    }

    /** Histórico observável (a UI reage a mudanças). */
    val records: Flow<List<DailyRecord>> = recordDao.observeAll()

    suspend fun findByDate(epochDay: Long): DailyRecord? = recordDao.findByDate(epochDay)
    suspend fun upsert(record: DailyRecord) = recordDao.upsert(record)
    suspend fun deleteByDate(date: Long) = recordDao.deleteByDate(date)
    suspend fun allRecords(): List<DailyRecord> = recordDao.getAll()

    /** Substitui todo o histórico (restauração de backup). */
    suspend fun replaceAll(records: List<DailyRecord>) = recordDao.replaceAll(records)

    /**
     * Roda uma vez no launch: reclassifica os dias salvos com as faixas de 5 níveis (2.0).
     */
    suspend fun migrateToFiveLevelsIfNeeded() {
        val alreadyMigrated = context.appPreferences.data.first()[Keys.fiveLevelsMigration] ?: false
        if (alreadyMigrated) return
        val currentRecords = recordDao.getAll()
        val normalized = currentRecords.map { it.normalizeAchievement() }
        recordDao.replaceAll(normalized)
        context.appPreferences.edit { prefs ->
            prefs[Keys.fiveLevelsMigration] = true
        }
    }
}
