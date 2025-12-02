package org.tstsite.tstsiteapp.config

import org.tstsite.tstsiteapp.utils.crypto.Cryptor

object AppConfig {
    private const val FERNET_KEY = "vlKu87oIFmDRvkvPvNlAL7qne6MQzxYvIjWm646hR1Y="
    private const val ENCRYPTED_API_KEY = "ENC(gAAAAABpCz8eJsevF_iqegTWQ_ccZgVOeDI0ulC5PaABqM8kSH51COd2FUvTRYKO0dstbq4mgmf3MXK1jxVt-Xdxhzmw1cVWZi6xrqwJfNcJWKFzBJcBrU0vNZOObV43qRWsMcBwCti7)"
    private val cryptor = Cryptor(FERNET_KEY)

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

    private const val ROUTES_AUTH = "auth"
    private const val AUTH_LOGIN = "login"

    /**
     * Devuelve la URL de login según:
     *  - Entorno (DEV/TST)
     *  - Plataforma (Android o no)
     */
    fun getApiLoginUrl(isAndroid: Boolean = false): String {
        val base = if (IS_DEV) {
            if (isAndroid) BASE_URL_DEV_ANDROID else BASE_URL_DEV
        } else {
            "$BASE_URL_TST/$SERVICE"
        }

        return "$base/$ROUTES_AUTH/$AUTH_LOGIN"
    }
}
