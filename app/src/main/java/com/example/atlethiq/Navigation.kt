package com.example.atlethiq

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.atlethiq.ui.main.MainScreen
import com.example.atlethiq.ui.screens.LoginScreen
import com.example.atlethiq.ui.viewmodel.AuthState
import com.example.atlethiq.ui.viewmodel.AuthViewModel

@Composable
fun MainNavigation(
  authViewModel: AuthViewModel = viewModel()
) {
  val backStack = rememberNavBackStack(Main)
  val authState by authViewModel.authState.collectAsState()

  if (authState is AuthState.Authenticated) {
    NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider =
        entryProvider {
          entry<Main> {
            MainScreen()
          }
        },
    )
  } else {
    LoginScreen(authViewModel = authViewModel)
  }
}
