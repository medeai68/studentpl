package com.example.studentpl.repository

import android.util.Log
import com.example.studentpl.BuildConfig
import com.example.studentpl.network.DeepSeekApiService
import com.example.studentpl.network.model.ChatMessage
import com.example.studentpl.network.model.ChatRequest
import retrofit2.HttpException

class AiRepository(
    private val deepSeekApiService: DeepSeekApiService
) {
    private val TAG = "AiRepository"

    suspend fun getChatCompletion(messages: List<ChatMessage>): Result<ChatMessage> {
        if (BuildConfig.DEEPSEEK_API_KEY.isBlank()) {
            return Result.failure(Exception("DeepSeek API Key is missing. Please add it to local.properties and rebuild."))
        }

        return try {
            val request = ChatRequest(
                model = "deepseek-chat",
                messages = messages
            )
            val response = deepSeekApiService.getChatCompletion(
                apiKey = "Bearer ${BuildConfig.DEEPSEEK_API_KEY}",
                request = request
            )
            val aiMessage = response.choices.firstOrNull()?.message
            if (aiMessage != null) {
                Result.success(aiMessage)
            } else {
                Result.failure(Exception("Empty response from DeepSeek"))
            }
        } catch (e: HttpException) {
            val msg = when(e.code()) {
                401 -> "DeepSeek 401: Invalid API Key."
                402 -> "DeepSeek 402: Insufficient Balance."
                429 -> "DeepSeek 429: Rate limit exceeded."
                else -> "DeepSeek Error ${e.code()}: ${e.message()}"
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            Log.e(TAG, "DeepSeek call failed", e)
            Result.failure(e)
        }
    }

    fun getSystemPrompt(context: String): ChatMessage {
        return ChatMessage(
            role = "system",
            content = """
                You are StudentPL AI, a helpful academic assistant. 
                You help students manage their studies, generate study plans, and suggest tasks.
                
                Current Student Context:
                $context
                
                Be concise and professional.
            """.trimIndent()
        )
    }
}
