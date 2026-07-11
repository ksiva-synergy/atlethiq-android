package com.example.atlethiq.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.atlethiq.theme.Base
import com.example.atlethiq.theme.Line
import com.example.atlethiq.theme.Muted
import com.example.atlethiq.theme.SignalLime
import com.example.atlethiq.theme.ElectricCyan
import com.example.atlethiq.theme.FlagOrange
import com.example.atlethiq.theme.Text as ColorText
import com.example.atlethiq.theme.Typography
import com.example.atlethiq.ui.screens.LogScreen
import com.example.atlethiq.ui.screens.SourcesScreen
import com.example.atlethiq.ui.screens.TodayScreen
import com.example.atlethiq.ui.screens.TrendsScreen
import com.example.atlethiq.ui.viewmodel.TodayViewModel

enum class BottomTab {
    TODAY, TRENDS, LOG, SOURCES
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    todayViewModel: TodayViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(BottomTab.TODAY) }
    val snapshot by todayViewModel.currentSnapshot.collectAsState()
    
    val indicatorColor = remember(snapshot) {
        when (snapshot?.call) {
            "go" -> SignalLime
            "hold" -> ElectricCyan
            "back_off" -> FlagOrange
            else -> SignalLime
        }
    }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Base)
            ) {
                // Divider line above bottom navigation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Line)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        val label = when (tab) {
                            BottomTab.TODAY -> "Today"
                            BottomTab.TRENDS -> "Trends"
                            BottomTab.LOG -> "Log"
                            BottomTab.SOURCES -> "Sources"
                        }
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = label,
                                style = Typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                ),
                                color = if (isSelected) ColorText else Muted
                            )
                            
                            // Selected indicator: highlight stroke (today's ink line under the label)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .fillMaxWidth(0.3f)
                                        .height(3.dp)
                                        .background(indicatorColor)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .fillMaxWidth(0.3f)
                                        .height(3.dp)
                                        .background(Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Base,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                BottomTab.TODAY -> TodayScreen(todayViewModel = todayViewModel)
                BottomTab.TRENDS -> TrendsScreen()
                BottomTab.LOG -> LogScreen()
                BottomTab.SOURCES -> SourcesScreen()
            }
        }
    }
}
