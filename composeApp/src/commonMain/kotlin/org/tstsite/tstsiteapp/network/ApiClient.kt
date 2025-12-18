package org.tstsite.tstsiteapp.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tstsite.tstsiteapp.model.*
import org.tstsite.tstsiteapp.utils.crypto.PayloadCrypto
import org.tstsite.tstsiteapp.utils.crypto.CryptoHelper

/**
 * 🌐 Cliente API multiplataforma para interactuar con el backend.
 *
 * Se encarga de todas las comunicaciones de red, incluyendo la encriptación
 * y desencriptación automática de los datos si está habilitada.
 */
expect class ApiClient() {
    /**
     * 🔑 Inicia sesión en la aplicación.
     * @param pLogin Objeto con las credenciales del usuario.
     * @return Una [SesionResponse] con el token y los datos del usuario.
     */
    suspend fun login(pLogin: SesionRequest): SesionResponse

    /**
     * 🛡️ Valida un token de sesión existente.
     * @param token El token JWT a validar.
     * @return Un [ValidateResponse] indicando si el token es válido.
     */
    suspend fun validate(token: String): ValidateResponse

    /**
     * 👤 Obtiene el perfil del usuario actual.
     * @param token El token JWT del usuario.
     * @return Un [ProfileResponse] con los detalles del perfil.
     */
    suspend fun profile(token: String): ProfileResponse

    /**
     * 🚪 Cierra la sesión del usuario.
     * @param token El token JWT a invalidar.
     * @return Un [LogoutResponse] confirmando el cierre de sesión.
     */
    suspend fun logout(token: String): LogoutResponse

    /**
     * ➕ Inserta un nuevo usuario en el sistema.
     * @param token El token JWT de un administrador.
     * @param userData Los datos del nuevo usuario a crear.
     * @return Un [UserResponse] con el resultado de la operación.
     */
    suspend fun insertUser(token: String, userData: UserData): UserResponse

    /**
     * 🧑‍💻 Selecciona (obtiene) un usuario por su nombre de usuario.
     * @param token El token JWT de un administrador.
     * @param username El nombre del usuario a buscar.
     * @return Un [UserResponse] con los datos del usuario encontrado.
     */
    suspend fun selectUser(token: String, username: String): UserResponse

    /**
     * 🔄 Actualiza los datos de un usuario existente.
     * @param token El token JWT de un administrador.
     * @param username El nombre del usuario a modificar.
     * @param userData Los nuevos datos para el usuario.
     * @return Un [UserResponse] con el resultado de la operación.
     */
    suspend fun updateUser(token: String, username: String, userData: UserData): UserResponse

    /**
     * ❌ Elimina un usuario del sistema.
     * @param token El token JWT de un administrador.
     * @param username El nombre del usuario a eliminar.
     * @return Un [UserResponse] con el resultado de la operación.
     */
    suspend fun deleteUser(token: String, username: String): UserResponse

    /**
     * 📜 Lista todos los usuarios del sistema.
     * @param token El token JWT de un administrador.
     * @return Un [UsersListResponse] con la lista de todos los usuarios.
     */
    suspend fun listUsers(token: String): UsersListResponse

    /**
     * 🔍 Busca usuarios que coincidan con ciertos criterios.
     * @param token El token JWT de un administrador.
     * @param searchParams Los parámetros de búsqueda.
     * @return Un [UsersListResponse] con los usuarios encontrados.
     */
    suspend fun searchUsers(token: String, searchParams: UserSearchParams): UsersListResponse

    /**
     * 📚 Obtiene el catálogo completo de datos.
     * @param token El token JWT del usuario.
     * @return Un [CatalogResponse] con los datos del catálogo.
     */
    suspend fun getCatalog(token: String): CatalogResponse
}

/**
 * 📦 Request encriptado que se envía al servidor.
 * @property encryptedPayload El contenido de la petición cifrado en Base64.
 */
@Serializable
data class EncryptedRequest(val encryptedPayload: String)

/**
 * 🎁 Response encriptada recibida del servidor.
 * @property encryptedPayload El contenido de la respuesta cifrado en Base64.
 */
@Serializable
data class EncryptedResponse(val encryptedPayload: String)

/**
 * 🚨 Estructura para respuestas de error genéricas del servidor.
 * @property resultado Indica el estado de la operación (ej: "error").
 * @property mensaje Mensaje descriptivo del error.
 * @property detalle Información adicional sobre el error (opcional).
 */
@Serializable
data class GenericErrorResponse(
    val resultado: String,
    val mensaje: String,
    val detalle: String? = null
)

/**
 * 🛠️ Objeto de ayuda con funciones comunes para todas las plataformas.
 */
object ApiClientHelper {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    /**
     * 🌐 Crea una instancia configurada de [HttpClient].
     * @return Un cliente HTTP listo para usar.
     */
    fun createHttpClient(): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("[Ktor] $message")
                    }
                }
                level = LogLevel.ALL
            }
        }
    }

    /**
     * 🔒 Encripta un objeto de datos si la encriptación está habilitada.
     * Si no, devuelve el objeto original.
     * @param data El objeto de datos a encriptar.
     * @param serializer El serializador para el tipo de objeto [T].
     * @return Un [EncryptedRequest] si la encriptación está activa, o el objeto [data] original.
     */
    suspend fun <T> encryptRequestIfEnabled(data: T, serializer: KSerializer<T>): Any? {
        return if (CryptoHelper.isEncryptionEnabled()) {
            val secretKey = CryptoHelper.getSecretKey()
            val encrypted = PayloadCrypto.encrypt(serializer, data, secretKey)
            EncryptedRequest(encrypted)
        } else {
            data // Sin encriptar
        }
    }

    /**
     * 🔓 Desencripta el cuerpo de una respuesta si viene encriptado.
     * Si la desencriptación falla o no está habilitada, intenta leerlo como texto plano.
     * @param responseBody El cuerpo de la respuesta como String.
     * @param deserializer El deserializador para el tipo de objeto de destino [T].
     * @return El objeto de datos [T] deserializado.
     * @throws Exception Si la respuesta es un error del servidor.
     */
    suspend fun <T> decryptResponseIfNeeded(responseBody: String, deserializer: KSerializer<T>): T {
        // Helper function to check for generic error response
        fun checkForGenericError(body: String): Nothing? {
            try {
                val errorResponse = json.decodeFromString(GenericErrorResponse.serializer(), body)
                if (errorResponse.resultado == "error") {
                    throw Exception(errorResponse.mensaje + (errorResponse.detalle?.let { ": $it" } ?: ""))
                }
            } catch (e: Exception) {
                // If it's not a GenericErrorResponse, or if parsing fails,
                // we just let the next step handle it.
                println("DEBUG: Not a generic error response or failed to parse as one: ${e.message}")
            }
            return null // Not an error, or parsing failed, continue
        }

        if (CryptoHelper.isEncryptionEnabled()) {
            try {
                // 1. Try to parse as EncryptedResponse
                val encryptedResp = json.decodeFromString(EncryptedResponse.serializer(), responseBody)
                val secretKey = CryptoHelper.getSecretKey()
                return PayloadCrypto.decrypt(deserializer, encryptedResp.encryptedPayload, secretKey)
            } catch (e: Exception) {
                // 2. If encrypted parsing fails, check if it's a generic error response
                println("⚠️ Fallo al desencriptar o parsear como EncryptedResponse. Intentando como error o sin encriptar: ${e.message}")
                checkForGenericError(responseBody) // This will throw if it's a generic error
                // 3. If not an encrypted response and not a generic error, try as plain (unencrypted) successful response
                return json.decodeFromString(deserializer, responseBody)
            }
        } else {
            // If encryption is NOT enabled
            // 1. Check if it's a generic error response
            checkForGenericError(responseBody) // This will throw if it's a generic error
            // 2. If not a generic error, parse as plain (unencrypted) successful response
            return json.decodeFromString(deserializer, responseBody)
        }
    }

    /**
     * 👷‍♂️ Construye un objeto [SesionResponse] a partir de un [LoginResponseWrapper].
     * Lanza una excepción si faltan campos esenciales.
     * @param wrapper El objeto intermedio recibido del login.
     * @return Un [SesionResponse] completo y validado.
     */
    fun buildSesionResponse(wrapper: LoginResponseWrapper): SesionResponse {
        if (wrapper.resultado == "error") {
            throw Exception(wrapper.mensaje ?: "Error desconocido en login")
        }
        val user = UsuarioLogin(
            idUsuario = wrapper.idUsuario ?: throw Exception("idUsuario no encontrado"),
            usuario = wrapper.usuario ?: throw Exception("usuario no encontrado"),
            perfil = wrapper.perfil ?: throw Exception("perfil no encontrado"),
            estado = wrapper.estado ?: throw Exception("estado no encontrado"),
            permisos = wrapper.permisos,
            detalles = wrapper.detalles
        )
        return SesionResponse(
            token = wrapper.token ?: throw Exception("Token no encontrado"),
            expiresIn = wrapper.expiresIn ?: throw Exception("expiresIn no encontrado"),
            user = user
        )
    }
}