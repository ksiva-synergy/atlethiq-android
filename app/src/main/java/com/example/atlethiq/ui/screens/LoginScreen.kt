package com.example.atlethiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.atlethiq.theme.Base
import com.example.atlethiq.theme.Line
import com.example.atlethiq.theme.Muted
import com.example.atlethiq.theme.Shapes
import com.example.atlethiq.theme.SignalLime
import com.example.atlethiq.theme.SpaceGrotesk
import com.example.atlethiq.theme.Text as ColorText
import com.example.atlethiq.theme.Typography
import com.example.atlethiq.ui.viewmodel.AuthState
import com.example.atlethiq.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Base)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Monogram/Logo
            Text(
                text = "ATLETHIQ",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = SignalLime
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Performance isn't always textbook.",
                style = Typography.bodyLarge,
                color = Muted
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Email input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Muted) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Line, Shapes.small),
                shape = Shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SignalLime,
                    unfocusedBorderColor = Line,
                    focusedTextColor = ColorText,
                    unfocusedTextColor = ColorText,
                    focusedContainerColor = Base,
                    unfocusedContainerColor = Base
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Muted) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Line, Shapes.small),
                shape = Shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SignalLime,
                    unfocusedBorderColor = Line,
                    focusedTextColor = ColorText,
                    unfocusedTextColor = ColorText,
                    focusedContainerColor = Base,
                    unfocusedContainerColor = Base
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error display
            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = com.example.atlethiq.theme.FlagOrange,
                    style = Typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Login Button
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(color = SignalLime)
            } else {
                Button(
                    onClick = { authViewModel.login(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = Shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SignalLime,
                        contentColor = Base
                    )
                ) {
                    Text(
                        text = "Login",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
