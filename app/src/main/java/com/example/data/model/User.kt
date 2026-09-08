package com.example.data.model

data class User(
    val name: String,
    val email: String,
    val token: String = "token_${System.currentTimeMillis()}"
)

enum class AuthMode {
    WELCOME,
    LOGIN,
    REGISTER,
    AUTHENTICATED
}

enum class PasswordStrength(val label: String, val score: Int) {
    WEAK("Débil", 1),
    MEDIUM("Aceptable", 2),
    STRONG("Fuerte", 3)
}
