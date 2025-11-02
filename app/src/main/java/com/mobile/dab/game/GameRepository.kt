package com.mobile.dab.game

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
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
