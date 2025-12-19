package org.tstsite.tstsiteapp.config

import org.tstsite.tstsiteapp.utils.crypto.Cryptor

/**
 * ⚙️ Objeto de configuración central de la aplicación.
 *
 * Contiene todas las constantes, claves y URLs necesarias para
 * que la aplicación funcione correctamente en diferentes entornos.
 */
object AppConfig {
    // --- Claves de Cifrado ---
    private const val FERNET_KEY = "vlKu87oIFmDRvkvPvNlAL7qne6MQzxYvIjWm646hR1Y="
    private const val SECRET_KEY = "ENC(gAAAAABpOrq7l4K9Mc7MWlhsUJSfgNJWRag781be3t2ojKn7ij6TMPp_lT5DVDZE_fGx9UmA0yLkHaxPAQafaytb6Z4nlQqPXWtHTXQsAmTwVmOBklrDTcRqexpUji2Stw1W16mLN32i)"
    private const val ENCRYPTED_API_KEY = "ENC(gAAAAABpQz0yB2TqCSTbgPgQ4iVnaxKijRZOcpwthBvmLhnccrDJufNkxzZAzIsNCOUvQHzzt5J5gJEGHEhWIKO7AXU3d8Tvh2Iv1ARyLpkEuY5Go9C8rbaJLEkzRg0F0MCfeiX59kXu)"
    private val cryptor = Cryptor(FERNET_KEY)

    /**
     * 🔐 **FLAG GLOBAL DE ENCRIPTACIÓN**
     * - `true`: Usa encriptación de payloads (debe coincidir con la configuración de la API).
     * - `false`: Envía datos sin encriptar (recomendado para desarrollo y depuración).
     */
    const val ENCRYPTION_ENABLED = true

    /**
     * 🔑 Obtiene la API Key desencriptada.
     * @return La API Key lista para usar en las cabeceras.
     */
    suspend fun getApiKey(): String {
        return cryptor.decryptValue(ENCRYPTED_API_KEY)
    }

    /**
     * 🤫 Obtiene la clave secreta para el cifrado de payloads.
     * @return La clave secreta desencriptada.
     */
    suspend fun getKey(): String {
        return cryptor.decryptValue(SECRET_KEY)
    }

    // --- Gestión de Entornos ---
    // true = usar entorno de Desarrollo (DEV), false = usar entorno de Testing (TST)
    private const val IS_DEV = false

    private const val SERVICE = "api"

    // URLs base para los diferentes entornos
    private const val BASE_URL_DEV = "http://localhost:3000"
    private const val BASE_URL_DEV_ANDROID = "http://10.0.2.2:3000" // IP especial para emulador Android
    private const val BASE_URL_TST = "https://tstsite.alwaysdata.net"

    // --- Endpoints de la API ---
    private const val ROUTES_AUTH = "auth"
    private const val AUTH_LOGIN = "login"
    private const val AUTH_VALIDATE = "validate"
    private const val AUTH_PROFILE = "profile"
    private const val AUTH_LOGOUT = "logout"

    private const val ROUTES_USERS = "users"
    private const val ROUTES_CATALOG = "catalog"

    /**
     * 🌍 Devuelve la URL base correcta según el entorno y la plataforma.
     * @param isAndroid Indica si la ejecución es en un dispositivo/emulador Android.
     * @return La URL base para las peticiones a la API.
     */
    fun getBaseUrl(isAndroid: Boolean = false): String {
        return if (IS_DEV) {
            if (isAndroid) BASE_URL_DEV_ANDROID else BASE_URL_DEV
        } else {
            "$BASE_URL_TST/$SERVICE"
        }
    }

    /**
     * 🚪 Construye la URL para el endpoint de login.
     * @param isAndroid Indica si la ejecución es en Android.
     * @return La URL completa para iniciar sesión.
     */
    fun getApiLoginUrl(isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_LOGIN"
    }

    /**
     * 🛡️ Construye la URL para el endpoint de validación de token.
     * @param isAndroid Indica si la ejecución es en Android.
     * @return La URL completa para validar un token.
     */
    fun getApiValidateUrl(isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_VALIDATE"
    }

    /**
     * 👤 Construye la URL para el endpoint de perfil de usuario.
     * @param isAndroid Indica si la ejecución es en Android.
     * @return La URL completa para obtener el perfil.
     */
    fun getApiProfileUrl(isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_PROFILE"
    }

    /**
     * 🚪 Construye la URL para el endpoint de cierre de sesión.
     * @param isAndroid Indica si la ejecución es en Android.
     * @return La URL completa para cerrar sesión.
     */
    fun getApiLogoutUrl(isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_LOGOUT"
    }

    /**
     * 👥 Construye la URL para los endpoints de gestión de usuarios.
     * @param accion La acción específica a realizar (ej: "insert", "list").
     * @param isAndroid Indica si la ejecución es en Android.
     * @return La URL completa para la acción de usuario.
     */
    fun getApiUsersUrl(accion: String, isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_USERS/$accion"
    }

    /**
     * 📚 Construye la URL para el endpoint del catálogo.
     * @param isAndroid Indica si la ejecución es en Android.
     * @return La URL completa para obtener el catálogo.
     */
    fun getApiCatalogUrl(isAndroid: Boolean = false): String {
        return "${getBaseUrl(isAndroid)}/$ROUTES_CATALOG"
    }
}