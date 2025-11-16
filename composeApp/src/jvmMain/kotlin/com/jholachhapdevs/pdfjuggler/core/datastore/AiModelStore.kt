package com.jholachhapdevs.pdfjuggler.core.datastore

import com.jholachhapdevs.pdfjuggler.core.util.Resources

/**
 * Manages persistence of the selected AI model
 */
object AiModelStore {
    private const val AI_MODEL_KEY = "ai_selected_model"

    suspend fun getSelectedModel(): String {
        return PrefsManager.getString(AI_MODEL_KEY) ?: Resources.DEFAULT_AI_MODEL
    }

    suspend fun saveSelectedModel(model: String) {
        PrefsManager.saveString(AI_MODEL_KEY, model)
    }

    suspend fun clearSelectedModel() {
        PrefsManager.delete(AI_MODEL_KEY)
    }
}

