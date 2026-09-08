package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AuthTextField
import com.example.ui.components.PasswordStrengthIndicator
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.Slate400
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val registerState by viewModel.registerState.collectAsStateWithLifecycle()
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
                            .testTag("btn_register_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title Section
                Text(
                    text = "Crear Cuenta 🚀",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = Color.White
                )

                Text(
                    text = "Completá tus datos para registrarte y comenzar a medir en AR.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400,
                    modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
                )

                // Form Fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AuthTextField(
                        value = registerState.name,
                        onValueChange = { viewModel.onRegisterNameChanged(it) },
                        label = "Nombre Completo",
                        placeholder = "Ej. Lucio Rostagno",
                        leadingIcon = Icons.Default.Person,
                        error = registerState.nameError,
                        testTag = "input_register_name"
                    )

                    AuthTextField(
                        value = registerState.email,
                        onValueChange = { viewModel.onRegisterEmailChanged(it) },
                        label = "Correo Electrónico",
                        placeholder = "nombre@ejemplo.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        error = registerState.emailError,
                        testTag = "input_register_email"
                    )

                    Column {
                        AuthTextField(
                            value = registerState.password,
                            onValueChange = { viewModel.onRegisterPasswordChanged(it) },
                            label = "Contraseña",
                            placeholder = "Mínimo 8 caracteres",
                            leadingIcon = Icons.Default.Lock,
                            isPassword = true,
                            error = registerState.passwordError,
                            testTag = "input_register_password"
                        )

                        PasswordStrengthIndicator(
                            strength = registerState.passwordStrength,
                            passwordLength = registerState.password.length
                        )
                    }

                    AuthTextField(
                        value = registerState.confirmPassword,
                        onValueChange = { viewModel.onRegisterConfirmPasswordChanged(it) },
                        label = "Confirmar Contraseña",
                        placeholder = "Repetí tu contraseña",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        error = registerState.confirmPasswordError,
                        testTag = "input_register_confirm_password"
                    )
                }

                // Terms & Conditions Checkbox
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onTermsToggled(!registerState.termsAccepted) }
                    ) {
                        Checkbox(
                            checked = registerState.termsAccepted,
                            onCheckedChange = { viewModel.onTermsToggled(it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryAmber,
                                checkmarkColor = Color.Black,
                                uncheckedColor = Slate400
                            ),
                            modifier = Modifier.testTag("checkbox_terms")
                        )
                        Text(
                            text = "Acepto los Términos de Servicio y la Política de Privacidad",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }

                    AnimatedVisibility(
                        visible = registerState.termsError != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (registerState.termsError != null) {
                            Text(
                                text = registerState.termsError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(start = 12.dp, top = 2.dp)
                                    .testTag("checkbox_terms_error")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        viewModel.validateAndRegister(onSuccess = onRegisterSuccess)
                    },
                    enabled = !registerState.isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAmber,
                        disabledContainerColor = PrimaryAmber.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_submit_register")
                ) {
                    if (registerState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Crear Mi Cuenta",
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
                        .padding(vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = DarkBorder
                    )
                    Text(
                        text = " o registrarse con ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = DarkBorder
                    )
                }

                // Social Register Buttons (Google & Apple)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.registerWithGoogle(onSuccess = onRegisterSuccess)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_register_social_google"),
                        border = BorderStroke(1.dp, DarkBorder)
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
                            viewModel.registerWithApple(onSuccess = onRegisterSuccess)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_register_social_apple"),
                        border = BorderStroke(1.dp, DarkBorder)
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

            // Footer Link to Login Screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Ya tenés una cuenta? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )
                Text(
                    text = "Iniciar Sesión",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryAmber,
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .testTag("link_go_to_login")
                )
            }
        }
    }
}
