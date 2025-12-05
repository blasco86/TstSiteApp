package org.tstsite.tstsiteapp.config

import org.tstsite.tstsiteapp.utils.crypto.Cryptor

object AppConfig {
    private const val FERNET_KEY = "vlKu87oIFmDRvkvPvNlAL7qne6MQzxYvIjWm646hR1Y="
    private const val ENCRYPTED_API_KEY = "ENC(gAAAAABpCz8eJsevF_iqegTWQ_ccZgVOeDI0ulC5PaABqM8kSH51COd2FUvTRYKO0dstbq4mgmf3MXK1jxVt-Xdxhzmw1cVWZi6xrqwJfNcJWKFzBJcBrU0vNZOObV43qRWsMcBwCti7)"
    private val cryptor = Cryptor(FERNET_KEY)

    // 🔐 FLAG GLOBAL DE ENCRIPTACIÓN
    // true = usar encriptación de payloads (debe coincidir con la API)
    // false = enviar datos sin encriptar (recomendado para desarrollo)
    const val ENCRYPTION_ENABLED = false

    suspend fun getApiKey(): String {
        return cryptor.decryptValue(ENCRYPTED_API_KEY)
    }

    // ---------- ENTORNOS ----------
    // true = usar DEV, false = usar TST
    private const val IS_DEV = true

    private const val SERVICE = "api"

    // Ojo: para DEV definimos dos variantes: general y Android
    private const val BASE_URL_DEV = "http://localhost:3000"
    private const val BASE_URL_DEV_ANDROID = "http://10.0.2.2:3000"

    private const val BASE_URL_TST = "https://tstsite.alwaysdata.net"

    // ---------- ENDPOINTS ----------
    private const val ROUTES_AUTH = "auth"
    private const val AUTH_LOGIN = "login"
    private const val AUTH_VALIDATE = "validate"
    private const val AUTH_PROFILE = "profile"
    private const val AUTH_LOGOUT = "logout"

    private const val ROUTES_USERS = "users"
    private const val ROUTES_CATALOG = "catalog"

    /**
     * Devuelve la URL base según entorno y plataforma
     */
    fun getBaseUrl(isAndroid: Boolean = false): String {
        return if (IS_DEV) {
            if (isAndroid) BASE_URL_DEV_ANDROID else BASE_URL_DEV
        } else {
            "$BASE_URL_TST/$SERVICE"
        }
    }

    /**
     * Devuelve la URL de login
     */
    fun getApiLoginUrl(isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_LOGIN"
    }

    /**
     * Devuelve la URL de validación de token
     */
    fun getApiValidateUrl(isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_VALIDATE"
    }

    /**
     * Devuelve la URL de perfil
     */
    fun getApiProfileUrl(isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_PROFILE"
    }

    /**
     * Devuelve la URL de logout
     */
    fun getApiLogoutUrl(isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_LOGOUT"
    }

    /**
     * Devuelve la URL de usuarios con acción
     */
    fun getApiUsersUrl(accion: String, isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_USERS/$accion"
    }

    /**
     * Devuelve la URL de catálogo
     */
    fun getApiCatalogUrl(isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_CATALOG"
    }
}