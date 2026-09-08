package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuthMode
import com.example.data.model.PasswordStrength
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false
)

data class RegisterFormState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsAccepted: Boolean = false,
    val termsError: String? = null,
    val passwordStrength: PasswordStrength = PasswordStrength.WEAK,
    val isLoading: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("metrax_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authMode = MutableStateFlow(AuthMode.WELCOME)
    val authMode: StateFlow<AuthMode> = _authMode.asStateFlow()

    private val _loginState = MutableStateFlow(LoginFormState())
    val loginState: StateFlow<LoginFormState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterFormState())
    val registerState: StateFlow<RegisterFormState> = _registerState.asStateFlow()

    private val emailPattern = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

    init {
        // Load saved session if exists
        val savedEmail = prefs.getString("user_email", null)
        val savedName = prefs.getString("user_name", null)
        if (!savedEmail.isNullOrBlank() && !savedName.isNullOrBlank()) {
            _currentUser.value = User(name = savedName, email = savedEmail)
            _authMode.value = AuthMode.AUTHENTICATED
        }
    }

    fun setAuthMode(mode: AuthMode) {
        _authMode.value = mode
    }

    // --- LOGIN FORM METHODS ---

    fun onLoginEmailChanged(email: String) {
        _loginState.value = _loginState.value.copy(
            email = email,
            emailError = null,
            generalError = null
        )
    }

    fun onLoginPasswordChanged(password: String) {
        _loginState.value = _loginState.value.copy(
            password = password,
            passwordError = null,
            generalError = null
        )
    }

    fun onRememberMeToggled(remember: Boolean) {
        _loginState.value = _loginState.value.copy(rememberMe = remember)
    }

    fun validateAndLogin(onSuccess: () -> Unit) {
        val state = _loginState.value
        var isValid = true

        var emailErr: String? = null
        var passwordErr: String? = null

        if (state.email.isBlank()) {
            emailErr = "El correo electrónico es obligatorio."
            isValid = false
        } else if (!emailPattern.matcher(state.email.trim()).matches()) {
            emailErr = "Ingresa un formato de correo válido (ej. usuario@dominio.com)."
            isValid = false
        }

        if (state.password.isBlank()) {
            passwordErr = "La contraseña es obligatoria."
            isValid = false
        } else if (state.password.length < 6) {
            passwordErr = "La contraseña debe tener al menos 6 caracteres."
            isValid = false
        }

        if (!isValid) {
            _loginState.value = state.copy(
                emailError = emailErr,
                passwordError = passwordErr
            )
            return
        }

        // Simulate Login API Call
        viewModelScope.launch {
            _loginState.value = state.copy(isLoading = true, generalError = null)
            
            // Artificial delay to simulate real auth network call
            kotlinx.coroutines.delay(600)

            // Derive user name from email if logging in
            val inferredName = state.email.substringBefore("@")
                .replace(".", " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            val user = User(name = inferredName, email = state.email.trim())
            _currentUser.value = user
            _authMode.value = AuthMode.AUTHENTICATED

            if (state.rememberMe) {
                prefs.edit()
                    .putString("user_email", user.email)
                    .putString("user_name", user.name)
                    .apply()
            }

            _loginState.value = LoginFormState() // Reset form
            onSuccess()
        }
    }

    // --- REGISTER FORM METHODS ---

    fun onRegisterNameChanged(name: String) {
        _registerState.value = _registerState.value.copy(
            name = name,
            nameError = null
        )
    }

    fun onRegisterEmailChanged(email: String) {
        _registerState.value = _registerState.value.copy(
            email = email,
            emailError = null
        )
    }

    fun onRegisterPasswordChanged(password: String) {
        val strength = calculatePasswordStrength(password)
        _registerState.value = _registerState.value.copy(
            password = password,
            passwordError = null,
            passwordStrength = strength,
            confirmPasswordError = if (_registerState.value.confirmPassword.isNotEmpty() && password != _registerState.value.confirmPassword) {
                "Las contraseñas no coinciden."
            } else null
        )
    }

    fun onRegisterConfirmPasswordChanged(confirmPassword: String) {
        val password = _registerState.value.password
        val confirmError = if (confirmPassword != password) "Las contraseñas no coinciden." else null
        _registerState.value = _registerState.value.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = confirmError
        )
    }

    fun onTermsToggled(accepted: Boolean) {
        _registerState.value = _registerState.value.copy(
            termsAccepted = accepted,
            termsError = null
        )
    }

    fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.length < 8) return PasswordStrength.WEAK
        val hasDigits = password.any { it.isDigit() }
        val hasLetters = password.any { it.isLetter() }
        val hasUppercase = password.any { it.isUpperCase() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        return when {
            password.length >= 10 && hasDigits && hasLetters && (hasUppercase || hasSpecial) -> PasswordStrength.STRONG
            password.length >= 8 && hasDigits && hasLetters -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    fun validateAndRegister(onSuccess: () -> Unit) {
        val state = _registerState.value
        var isValid = true

        var nameErr: String? = null
        var emailErr: String? = null
        var passwordErr: String? = null
        var confirmErr: String? = null
        var termsErr: String? = null

        if (state.name.trim().length < 2) {
            nameErr = "Por favor ingresa tu nombre completo (mínimo 2 caracteres)."
            isValid = false
        }

        if (state.email.isBlank()) {
            emailErr = "El correo electrónico es obligatorio."
            isValid = false
        } else if (!emailPattern.matcher(state.email.trim()).matches()) {
            emailErr = "Ingresa un correo electrónico válido."
            isValid = false
        }

        if (state.password.length < 8) {
            passwordErr = "La contraseña debe tener al menos 8 caracteres."
            isValid = false
        } else if (!state.password.any { it.isDigit() } || !state.password.any { it.isLetter() }) {
            passwordErr = "La contraseña debe incluir letras y números."
            isValid = false
        }

        if (state.confirmPassword != state.password) {
            confirmErr = "Las contraseñas no coinciden."
            isValid = false
        }

        if (!state.termsAccepted) {
            termsErr = "Debes aceptar los términos y condiciones para continuar."
            isValid = false
        }

        if (!isValid) {
            _registerState.value = state.copy(
                nameError = nameErr,
                emailError = emailErr,
                passwordError = passwordErr,
                confirmPasswordError = confirmErr,
                termsError = termsErr
            )
            return
        }

        // Simulate Register API Call
        viewModelScope.launch {
            _registerState.value = state.copy(isLoading = true)

            kotlinx.coroutines.delay(700)

            val user = User(name = state.name.trim(), email = state.email.trim())
            _currentUser.value = user
            _authMode.value = AuthMode.AUTHENTICATED

            prefs.edit()
                .putString("user_email", user.email)
                .putString("user_name", user.name)
                .apply()

            _registerState.value = RegisterFormState() // Reset form
            onSuccess()
        }
    }

    fun logout() {
        _currentUser.value = null
        _authMode.value = AuthMode.WELCOME
        prefs.edit().clear().apply()
    }
}
