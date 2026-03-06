package org.tstsite.tstsiteapp.config

import org.tstsite.tstsiteapp.utils.crypto.Cryptor

/**
 * ⚙️ Objeto de configuración central de la aplicación.
 *
 * La API Key ha sido eliminada del cliente. El acceso a la API se realiza
 * exclusivamente mediante JWT obtenido tras el login del usuario.
 *
 * El único secreto que permanece en el cliente es la SECRET_KEY para
 * el cifrado de payloads (ENCRYPTION_ENABLED), que protege el contenido
 * del payload en tránsito, no el acceso a la API.
 */
object AppConfig {
    // --- Claves de Cifrado de Payload ---
    private const val FERNET_KEY = "vlKu87oIFmDRvkvPvNlAL7qne6MQzxYvIjWm646hR1Y="
    private const val SECRET_KEY = "ENC(gAAAAABpOrq7l4K9Mc7MWlhsUJSfgNJWRag781be3t2ojKn7ij6TMPp_lT5DVDZE_fGx9UmA0yLkHaxPAQafaytb6Z4nlQqPXWtHTXQsAmTwVmOBklrDTcRqexpUji2Stw1W16mLN32i)"
    private val cryptor = Cryptor(FERNET_KEY)

    /**
     * 🔐 **FLAG GLOBAL DE ENCRIPTACIÓN DE PAYLOAD**
     * - `true`: Cifra el cuerpo de cada petición (protege datos en tránsito).
     * - `false`: Envía datos sin cifrar (solo para desarrollo/depuración).
     */
    const val ENCRYPTION_ENABLED = true

    /**
     * 🤫 Obtiene la clave secreta para el cifrado de payloads.
     * @return La clave secreta desencriptada.
     */
    suspend fun getKey(): String {
        return cryptor.decryptValue(SECRET_KEY)
    }

    // --- Gestión de Entornos ---
    private const val IS_DEV = false
    private const val SERVICE = "api"

    private const val BASE_URL_DEV = "http://localhost:3000"
    private const val BASE_URL_DEV_ANDROID = "http://10.0.2.2:3000"
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
     * 🌐 Obtiene la URL base de la API según el entorno de desarrollo y la plataforma.
     * @param isAndroid `true` si la llamada se realiza desde un emulador Android, `false` en caso contrario.
     * @return La URL base de la API.
     */
    fun getBaseUrl(isAndroid: Boolean = false): String {
        return if (IS_DEV) {
            if (isAndroid) BASE_URL_DEV_ANDROID else BASE_URL_DEV
        } else {
            "$BASE_URL_TST/$SERVICE"
        }
    }

    /**
     * 🔑 Obtiene la URL para el endpoint de login de la API.
     * @param isAndroid `true` si la llamada se realiza desde un emulador Android, `false` en caso contrario.
     * @return La URL completa para el login.
     */
    fun getApiLoginUrl(isAndroid: Boolean = false) =
        "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_LOGIN"

    /**
     * 🛡️ Obtiene la URL para el endpoint de validación de token de la API.
     * @param isAndroid `true` si la llamada se realiza desde un emulador Android, `false` en caso contrario.
     * @return La URL completa para la validación de token.
     */
    fun getApiValidateUrl(isAndroid: Boolean = false) =
        "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_VALIDATE"

    /**
     * 👤 Obtiene la URL para el endpoint de perfil de usuario de la API.
     * @param isAndroid `true` si la llamada se realiza desde un emulador Android, `false` en caso contrario.
     * @return La URL completa para el perfil de usuario.
     */
    fun getApiProfileUrl(isAndroid: Boolean = false) =
        "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_PROFILE"

    /**
     * 🚪 Obtiene la URL para el endpoint de logout de la API.
     * @param isAndroid `true` si la llamada se realiza desde un emulador Android, `false` en caso contrario.
     * @return La URL completa para el logout.
     */
    fun getApiLogoutUrl(isAndroid: Boolean = false) =
        "${getBaseUrl(isAndroid)}/$ROUTES_AUTH/$AUTH_LOGOUT"

    /**
     * 👥 Obtiene la URL para los endpoints de usuarios de la API.
     * @param action La acción específica dentro del recurso de usuarios (ej. "create", "delete").
     * @param isAndroid `true` si la llamada se realiza desde un emulador Android, `false` en caso contrario.
     * @return La URL completa para la acción de usuarios.
     */
    fun getApiUsersUrl(action: String, isAndroid: Boolean = false) =
        "${getBaseUrl(isAndroid)}/$ROUTES_USERS/$action"

    /**
     * 📚 Obtiene la URL para el endpoint de catálogo de la API.
     * @param isAndroid `true` si la llamada se realiza desde un emulador Android, `false` en caso contrario.
     * @return La URL completa para el catálogo.
     */
    fun getApiCatalogUrl(isAndroid: Boolean = false) =
        "${getBaseUrl(isAndroid)}/$ROUTES_CATALOG"
}