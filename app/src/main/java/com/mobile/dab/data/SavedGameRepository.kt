package com.mobile.dab.data

import android.content.Context
import android.util.Log
import com.mobile.dab.data.json.SavedGameState
import com.mobile.dab.ui.composable.game.state.GameUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException


private const val FILE_EXTENSION = ".dabgame"
private const val TAG = "SavedGameRepo"

/**
 * Handles the saved games in JSON files.
 */
class SavedGameRepository(context: Context) {
    private val filesDir = context.filesDir
    private val json = Json {
        allowStructuredMapKeys = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    /**
     * Save the current game state in a JSON file.
     */
    suspend fun saveGame(state: GameUiState, startTime: Long) = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "game_$timestamp$FILE_EXTENSION"
            val savedState = SavedGameState(fileName, state, timestamp, startTime)

            val file = File(filesDir, fileName)
            val jsonString = json.encodeToString(savedState)
            file.writeText(jsonString)
            Log.d(TAG, "Game saved in $fileName")
        } catch (e: IOException) {
            Log.e(TAG, "An error occurred saving the game", e)
        }
    }

    /**
     * Gets a list of all saved games.
     */
    suspend fun getSavedGames(): List<SavedGameState> = withContext(Dispatchers.IO) {
        try {
            filesDir.listFiles { _, name -> name.endsWith(FILE_EXTENSION) }
                ?.mapNotNull { file ->
                    loadGameFromFile(file)
                }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving all saved games", e)
            emptyList()
        }
    }

    /**
     * Loads a game state from an specific file.
     */
    suspend fun loadGame(fileName: String): SavedGameState? = withContext(Dispatchers.IO) {
        try {
            val file = File(filesDir, fileName)
            if (!file.exists()) return@withContext null

            loadGameFromFile(file)
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred loading game $fileName", e)
            null
        }
    }

    /**
     * Deletes a saved file game.
     */
    suspend fun deleteGame(fileName: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(filesDir, fileName)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Game deleted: $fileName")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "An error occurred deleting the game $fileName", e)
        }
    }

    private fun loadGameFromFile(file: File): SavedGameState? {
        return try {
            val jsonString = file.readText()
            json.decodeFromString<SavedGameState>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Parse error ${file.name}", e)
            null
        }
    }
}