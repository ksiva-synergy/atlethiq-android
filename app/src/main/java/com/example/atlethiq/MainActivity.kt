package com.example.atlethiq

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.atlethiq.data.AppStateStore
import com.example.atlethiq.notifications.CallNotifier
import com.example.atlethiq.theme.AtlethiqTheme
import com.example.atlethiq.theme.Base
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var appStateStore: AppStateStore

  private val requestNotificationPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op; shade shows once granted */ }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    captureNotificationDeepLink(intent)
    maybeRequestNotificationPermission()

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

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    // Persist the tapped day; the Today ViewModel consumes it on the next resume.
    captureNotificationDeepLink(intent)
  }

  private fun captureNotificationDeepLink(intent: Intent?) {
    val day = intent?.getIntExtra(CallNotifier.EXTRA_NOTIF_DAY, 0) ?: 0
    if (day in 1..45) {
      appStateStore.pendingNotifDay = day
    }
  }

  private fun maybeRequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        this, Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }
}
