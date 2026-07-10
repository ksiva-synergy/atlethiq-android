package com.example.atlethiq.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.atlethiq.theme.Base
import com.example.atlethiq.theme.Muted
import com.example.atlethiq.theme.SignalLime
import com.example.atlethiq.theme.SpaceGrotesk
import com.example.atlethiq.theme.Text as ColorText
import com.example.atlethiq.theme.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScreen(
    title: String,
    isToday: Boolean = false,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (isToday) {
                        Text(
                            text = "THU 10 JUL",
                            style = Typography.labelLarge,
                            color = Muted
                        )
                    } else {
                        Text(
                            text = title,
                            style = Typography.headlineMedium.copy(fontSize = 20.sp),
                            color = ColorText
                        )
                    }
                },
                navigationIcon = {
                    Text(
                        text = "A",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        color = SignalLime,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                },
                actions = {
                    IconButton(onClick = {}) {
                        Text(
                            text = "🧪", // flask beaker icon
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Base,
                    titleContentColor = ColorText
                )
            )
        },
        containerColor = Base
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun TodayScreen() {
    TabScreen(title = "Today", isToday = true) {
        Text("Today Tab Placeholder", style = Typography.bodyLarge, color = ColorText)
    }
}

@Composable
fun TrendsScreen() {
    TabScreen(title = "Trends") {
        Text("Trends Tab Placeholder", style = Typography.bodyLarge, color = ColorText)
    }
}

@Composable
fun LogScreen() {
    TabScreen(title = "Log") {
        Text("Log Tab Placeholder", style = Typography.bodyLarge, color = ColorText)
    }
}

@Composable
fun SourcesScreen() {
    TabScreen(title = "Sources") {
        Text("Sources Tab Placeholder", style = Typography.bodyLarge, color = ColorText)
    }
}
