package com.example.atlethiq.domain.dedup

/**
 * PLACEHOLDER dedup priority config for the Sources attribution table.
 *
 * The real config-driven resolver (AGENTS.md "DEDUP" rule; execution-plan-v2 §B M6) does not
 * exist yet — that milestone builds `domain/dedup/` as a unit-tested resolver. This object
 * mirrors the priority order the `compute-call` Edge Function currently hardcodes
 * (supabase/functions/compute-call/engine.ts: mock_whoop > mock_oneplus > other), so the
 * Sources screen shows a table that matches what the engine actually does today, but it is
 * NOT the M6 resolver and should be replaced when that milestone lands.
 */
object PlaceholderDedupConfig {
    /** Highest-priority source listed first, per metric type. */
    val priorityByMetricType: Map<String, List<String>> = mapOf(
        "hrv_rmssd" to listOf("mock_whoop", "mock_oneplus"),
        "resting_hr" to listOf("mock_whoop", "mock_oneplus"),
        "sleep_session" to listOf("mock_whoop", "mock_oneplus"),
        "steps" to listOf("mock_whoop"),
        "exercise_session" to listOf("mock_whoop"),
        "rpe" to listOf("manual"),
        "subjective_feel" to listOf("manual"),
    )

    val metricDisplayNames: Map<String, String> = mapOf(
        "hrv_rmssd" to "HRV",
        "resting_hr" to "Resting HR",
        "sleep_session" to "Sleep",
        "steps" to "Steps",
        "exercise_session" to "Training load",
        "rpe" to "RPE",
        "subjective_feel" to "Subjective feel",
    )

    fun sourceDisplayName(sourceApp: String): String = when (sourceApp) {
        "mock_whoop" -> "mock_whoop"
        "mock_oneplus" -> "mock_oneplus"
        "manual" -> "manual"
        else -> sourceApp
    }
}
