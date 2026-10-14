package com.ajtecnology.smartvest.ui

enum class LoginFailureKind {
    WRONG_CREDENTIALS,
    RATE_LIMITED,
    LAB_ERROR,
    NO_CONNECTION
}

fun classifyLoginFailure(raw: String?): LoginFailureKind {
    val message = raw.orEmpty()
    return when {
        message == "UNAUTHORIZED" || message.contains("incorrectos", ignoreCase = true) ->
            LoginFailureKind.WRONG_CREDENTIALS
        message.contains("429") -> LoginFailureKind.RATE_LIMITED
        message.startsWith("Error HTTP") -> LoginFailureKind.LAB_ERROR
        else -> LoginFailureKind.NO_CONNECTION
    }
}
