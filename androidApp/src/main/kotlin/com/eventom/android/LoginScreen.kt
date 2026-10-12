package com.eventom.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eventom.presentation.AuthUiState

/** Field/button corner radius shared by the login UI. */
private val LoginFieldShape = RoundedCornerShape(14.dp)

/**
 * Mock login (M2), Eventim look: bright background, ticket-blue accents,
 * rounded fields, one prominent CTA. Credentials go straight into the
 * shared AuthViewModel; navigation is not this composable's business —
 * the root composition reacts to `AuthUiState.Success`.
 */
@Composable
fun LoginScreen(
    authState: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val isLoading = authState is AuthUiState.Loading
    val errorMessage = (authState as? AuthUiState.Error)?.message

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(contentPadding)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Back")
            }
        }

        Spacer(Modifier.height(24.dp))

        // EVENTIM-style wordmark — shared component (splash + login identical).
        EvenTomWordmark()
        Spacer(Modifier.height(6.dp))
        Text(
            "Sign in to see your events",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            singleLine = true,
            enabled = !isLoading,
            shape = LoginFieldShape,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            enabled = !isLoading,
            shape = LoginFieldShape,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                        onSignIn(email, password)
                    }
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        errorMessage?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onSignIn(email, password) },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            shape = LoginFieldShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = TicketBlue,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Text("Sign in", fontWeight = FontWeight.SemiBold)
            }
        }

        // Demo-credential hint so the mock backend is easy to try.
        Spacer(Modifier.height(14.dp))
        Text(
            "Demo account: demo@eventom.dev · demo1234",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}