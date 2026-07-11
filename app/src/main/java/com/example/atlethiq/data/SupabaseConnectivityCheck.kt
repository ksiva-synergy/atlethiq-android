package com.example.atlethiq.data

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseConnectivityCheck {
    private const val TAG = "SupabaseConnCheck"

    suspend fun verifyTables(supabaseClient: SupabaseClient) {
        val tables = listOf("metric_samples", "daily_snapshots", "call_feedback", "engine_config")
        Log.i(TAG, "=== STARTING PROGRAMMATIC SUPABASE CONNECTIVITY CHECK ===")
        for (table in tables) {
            try {
                val response = withContext(Dispatchers.IO) {
                    supabaseClient.postgrest.from(table).select {
                        limit(1)
                    }
                }
                Log.i(TAG, "Table '$table' verification: SUCCESS (data: ${response.data})")
            } catch (e: Exception) {
                Log.e(TAG, "Table '$table' verification: FAILED", e)
            }
        }
        Log.i(TAG, "=== SUPABASE CONNECTIVITY CHECK COMPLETED ===")
    }
}
