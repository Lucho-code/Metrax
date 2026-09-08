package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AuthTextField
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.Slate400
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 500.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .testTag("btn_login_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Title Section
                Text(
                    text = "¡Hola de nuevo! 👋",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = Color.White
                )

                Text(
                    text = "Ingresá tus credenciales para acceder a tus mediciones guardadas en la nube.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400,
                    modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
                )

                // Form Fields
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    AuthTextField(
                        value = loginState.email,
                        onValueChange = { viewModel.onLoginEmailChanged(it) },
                        label = "Correo Electrónico",
                        placeholder = "nombre@ejemplo.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        error = loginState.emailError,
                        testTag = "input_login_email"
                    )

                    AuthTextField(
                        value = loginState.password,
                        onValueChange = { viewModel.onLoginPasswordChanged(it) },
                        label = "Contraseña",
                        placeholder = "••••••••",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        error = loginState.passwordError,
                        testTag = "input_login_password"
                    )
                }

                // Options Row: Remember Me & Forgot Password
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.onRememberMeToggled(!loginState.rememberMe) }
                    ) {
                        Checkbox(
                            checked = loginState.rememberMe,
                            onCheckedChange = { viewModel.onRememberMeToggled(it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryAmber,
                                checkmarkColor = Color.Black,
                                uncheckedColor = Slate400
                            ),
                            modifier = Modifier.testTag("checkbox_remember_me")
                        )
                        Text(
                            text = "Recordarme",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }

                    TextButton(
                        onClick = {
                            Toast.makeText(context, "Se envió un enlace de recuperación a tu email", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.testTag("btn_forgot_password")
                    ) {
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            color = PrimaryAmber,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        viewModel.validateAndLogin(onSuccess = onLoginSuccess)
                    },
                    enabled = !loginState.isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAmber,
                        disabledContainerColor = PrimaryAmber.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_submit_login")
                ) {
                    if (loginState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Iniciar Sesión",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // Divider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = DarkBorder
                    )
                    Text(
                        text = " o continuar con ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = DarkBorder
                    )
                }

                // Social Login Placeholders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.onLoginEmailChanged("google.user@example.com")
                            viewModel.onLoginPasswordChanged("Secret123")
                            viewModel.validateAndLogin(onLoginSuccess)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_social_google"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.onLoginEmailChanged("apple.user@example.com")
                            viewModel.onLoginPasswordChanged("Secret123")
                            viewModel.validateAndLogin(onLoginSuccess)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_social_apple"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_apple_logo),
                            contentDescription = "Apple",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apple", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Footer Link to Register Screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No tenés una cuenta? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )
                Text(
                    text = "Registrate",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryAmber,
                    modifier = Modifier
                        .clickable { onNavigateToRegister() }
                        .testTag("link_go_to_register")
                )
            }
        }
    }
}
