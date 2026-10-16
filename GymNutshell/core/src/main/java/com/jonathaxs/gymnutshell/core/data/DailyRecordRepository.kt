package com.jonathaxs.gymnutshell.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Acesso ao histórico de dias e aos bônus de sequência (Room).
 * Espelha o que no iOS era feito direto no ModelContext do SwiftData.
 */
class DailyRecordRepository(private val context: Context) {

    private val db = DatabaseProvider.get(context)
    private val recordDao = db.dailyRecordDao()
    private val streakDao = db.streakBonusDao()

    private object Keys {
        val fiveLevelsMigration = booleanPreferencesKey("migration.achievement.fiveLevels")
    }

    /** Histórico observável (a UI reage a mudanças). */
    val records: Flow<List<DailyRecord>> = recordDao.observeAll()

    /** Bônus de sequência observáveis. */
    val bonuses: Flow<List<StreakBonus>> = streakDao.observeAll()

    suspend fun findByDate(epochDay: Long): DailyRecord? = recordDao.findByDate(epochDay)
    suspend fun upsert(record: DailyRecord) = recordDao.upsert(record)
    suspend fun allRecords(): List<DailyRecord> = recordDao.getAll()

    /** Substitui todo o histórico (restauração de backup). */
    suspend fun replaceAll(records: List<DailyRecord>) = recordDao.replaceAll(records)

    suspend fun awardedAnchors(): Set<Long> = streakDao.awardedAnchorDays().toSet()
    suspend fun insertBonus(bonus: StreakBonus) = streakDao.insert(bonus)
    suspend fun allBonuses(): List<StreakBonus> = streakDao.getAll()

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
