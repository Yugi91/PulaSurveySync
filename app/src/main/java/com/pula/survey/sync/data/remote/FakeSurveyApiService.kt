package com.pula.survey.sync.data.remote

import com.pula.survey.sync.domain.model.SurveyResponse
import kotlinx.coroutines.delay

/**
 * Configurable mock API for testing sync scenarios.
 * Supports per-call failure sequences, fixed delay, and call tracking.
 */
class FakeSurveyApiService : SurveyApiService {

    private var delayMs: Long = 100L
    private var failureSequence: List<Exception?>? = null
    private var fixedError: Exception? = null

    private val _callHistory = mutableListOf<String>()
    val callHistory: List<String> get() = _callHistory.toList()
    val callCount: Int get() = _callHistory.size

    fun reset() {
        delayMs = 100L
        failureSequence = null
        fixedError = null
        _callHistory.clear()
    }

    fun delayMs(ms: Long): FakeSurveyApiService = apply { this.delayMs = ms }

    /** Fine-grained per-call control: null = success, non-null = throw that exception. */
    fun failWithSequence(sequence: List<Exception?>): FakeSurveyApiService = apply {
        this.failureSequence = sequence
        this.fixedError = null
    }

    fun alwaysFail(error: Exception): FakeSurveyApiService = apply {
        this.fixedError = error
        this.failureSequence = null
    }

    fun alwaysSucceed(): FakeSurveyApiService = apply {
        this.fixedError = null
        this.failureSequence = null
    }

    fun failOnNthCall(n: Int, error: Exception): FakeSurveyApiService = apply {
        this.failureSequence = null
        this.fixedError = null
        val seq = MutableList<Exception?>(n - 1) { null }
        seq.add(error)
        this.failureSequence = seq
    }

    fun failAfterNSuccesses(n: Int, error: Exception): FakeSurveyApiService = apply {
        val seq = MutableList<Exception?>(n) { null }
        // After n successes, all remaining calls fail
        repeat(50) { seq.add(error) }
        this.failureSequence = seq
        this.fixedError = null
    }

    override suspend fun uploadResponse(response: SurveyResponse): ApiResult {
        delay(delayMs)
        val index = _callHistory.size
        _callHistory.add(response.id.toString())

        val error = when {
            failureSequence != null -> failureSequence!!.getOrNull(index)
            fixedError != null -> fixedError
            else -> null
        }

        if (error != null) throw error

        return ApiResult.Success(response.id.toString())
    }
}
