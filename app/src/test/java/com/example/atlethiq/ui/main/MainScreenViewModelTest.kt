package com.example.atlethiq.ui.main

import com.example.atlethiq.data.AttributionRow
import com.example.atlethiq.data.DataRepository
import com.example.atlethiq.data.LogHistoryRow
import com.example.atlethiq.data.SourceSummary
import com.example.atlethiq.data.TrendsChartData
import com.example.atlethiq.data.models.DailySnapshot
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyLoading() = runTest {
    val viewModel = MainScreenViewModel(FakeMyModelRepository())
    assertEquals(viewModel.uiState.first(), MainScreenUiState.Loading)
  }

  @Test
  fun uiState_onItemSaved_isDisplayed() = runTest {
    val viewModel = MainScreenViewModel(FakeMyModelRepository())
    assertEquals(viewModel.uiState.first(), MainScreenUiState.Loading)
  }
}

private class FakeMyModelRepository : DataRepository {
  override val data: Flow<List<String>> = flow { emit(listOf("Sample")) }
  override fun getDailySnapshot(day: String): Flow<DailySnapshot?> = flow { emit(null) }
  override suspend fun getDailySnapshotSync(day: String): DailySnapshot? = null
  override suspend fun getHrvSamplesForSparkline(endDateStr: String): List<Double> = emptyList()
  override suspend fun loadSeededDataFromSupabase(userId: String) {}
  override suspend fun getAllDailySnapshots(): List<DailySnapshot> = emptyList()
  override suspend fun getLatestDayWithData(): String? = null
  override suspend fun getTrendsChartData(): TrendsChartData =
    TrendsChartData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 7.5, emptyList(), emptyList())
  override suspend fun getSourceSummaries(selectedDateStr: String): List<SourceSummary> = emptyList()
  override fun getAttributionRows(): List<AttributionRow> = emptyList()
  override suspend fun getLogHistory(): List<LogHistoryRow> = emptyList()
  override suspend fun logManualEntry(userId: String, date: String, rpe: Int?, feel: String?, note: String?) {}
}
