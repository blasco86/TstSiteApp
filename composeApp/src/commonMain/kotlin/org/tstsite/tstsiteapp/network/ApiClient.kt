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
 * La autenticación se realiza exclusivamente mediante JWT.
 * La API Key ha sido eliminada del cliente — el acceso se controla
 * en el servidor mediante CORS + rate limiting para /login,
 * y JWT para el resto de endpoints.
 */
expect class ApiClient() {
    suspend fun login(pLogin: SesionRequest): SesionResponse
    suspend fun validate(token: String): ValidateResponse
    suspend fun profile(token: String): ProfileResponse
    suspend fun logout(token: String): LogoutResponse
    suspend fun insertUser(token: String, userData: UserData): UserResponse
    suspend fun selectUser(token: String, username: String): UserResponse
    suspend fun updateUser(token: String, username: String, userData: UserData): UserResponse
    suspend fun deleteUser(token: String, username: String): UserResponse
    suspend fun listUsers(token: String): UsersListResponse
    suspend fun searchUsers(token: String, searchParams: UserSearchParams): UsersListResponse
    suspend fun getCatalog(token: String): CatalogResponse
}

@Serializable
data class EncryptedRequest(val encryptedPayload: String)

@Serializable
data class EncryptedResponse(val encryptedPayload: String)

@Serializable
data class GenericErrorResponse(
    val resultado: String,
    val mensaje: String,
    val detalle: String? = null
)

object ApiClientHelper {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    /**
     * 🛠️ Crea y configura una instancia del cliente HTTP de Ktor.
     * @return Una instancia de `HttpClient` con serialización JSON y logging configurados.
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
     * 🔒 Cifra el payload de una solicitud si el cifrado está habilitado.
     * @param data El objeto de datos a cifrar.
     * @param serializer El serializador para el tipo de datos `T`.
     * @return Un `EncryptedRequest` si el cifrado está habilitado, o el objeto original si no lo está.
     */
    suspend fun <T> encryptRequestIfEnabled(data: T, serializer: KSerializer<T>): Any? {
        return if (CryptoHelper.isEncryptionEnabled()) {
            val secretKey = CryptoHelper.getSecretKey()
            val encrypted = PayloadCrypto.encrypt(serializer, data, secretKey)
            EncryptedRequest(encrypted)
        } else {
            data
        }
    }

    /**
     * 🔓 Desencripta el cuerpo de una respuesta si es necesario.
     * @param responseBody El cuerpo de la respuesta como una cadena de texto.
     * @param deserializer El deserializador para el tipo de datos `T`.
     * @return El objeto de datos deserializado, ya sea del payload desencriptado o del cuerpo de la respuesta original.
     * @throws Exception si la respuesta es un error genérico.
     */
    suspend fun <T> decryptResponseIfNeeded(responseBody: String, deserializer: KSerializer<T>): T {
        fun checkForGenericError(body: String): Nothing? {
            try {
                val errorResponse = json.decodeFromString(GenericErrorResponse.serializer(), body)
                if (errorResponse.resultado == "error") {
                    throw Exception(errorResponse.mensaje + (errorResponse.detalle?.let { ": $it" } ?: ""))
                }
            } catch (e: Exception) {
                println("DEBUG: Not a generic error response: ${e.message}")
            }
            return null
        }

        if (CryptoHelper.isEncryptionEnabled()) {
            try {
                val encryptedResp = json.decodeFromString(EncryptedResponse.serializer(), responseBody)
                val secretKey = CryptoHelper.getSecretKey()
                return PayloadCrypto.decrypt(deserializer, encryptedResp.encryptedPayload, secretKey)
            } catch (e: Exception) {
                println("⚠️ Fallo al desencriptar: ${e.message}")
                checkForGenericError(responseBody)
                return json.decodeFromString(deserializer, responseBody)
            }
        } else {
            checkForGenericError(responseBody)
            return json.decodeFromString(deserializer, responseBody)
        }
    }

    /**
     * 🔄 Construye un objeto `SesionResponse` a partir de un `LoginResponseWrapper`.
     * @param wrapper El `LoginResponseWrapper` recibido de la API.
     * @return Un objeto `SesionResponse` con los datos de la sesión del usuario.
     * @throws Exception si el wrapper indica un error o si faltan campos esenciales.
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