package com.mobile.dab.data

import android.content.Context
import com.mobile.dab.data.database.AppDatabase
import com.mobile.dab.data.entity.GameResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(context: Context) {
    private val db = AppDatabase.Companion.getInstance(context)
    private val dao = db.gameResultDao()

    suspend fun saveResult(result: GameResult) {
        withContext(Dispatchers.IO) {
            dao.insert(result)
        }
    }

    suspend fun getHistory(): List<GameResult> = withContext(Dispatchers.IO) {
        dao.getAll()
    }
}