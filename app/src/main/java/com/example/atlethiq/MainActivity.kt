package com.example.atlethiq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.atlethiq.theme.AtlethiqTheme
import com.example.atlethiq.theme.Base
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      AtlethiqTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Base
        ) {
          MainNavigation()
        }
      }
    }
  }
}
