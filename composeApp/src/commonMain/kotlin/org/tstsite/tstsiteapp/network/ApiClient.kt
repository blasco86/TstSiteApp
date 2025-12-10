package org.tstsite.tstsiteapp.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tstsite.tstsiteapp.config.AppConfig
import org.tstsite.tstsiteapp.model.*
import org.tstsite.tstsiteapp.utils.crypto.PayloadCrypto
import org.tstsite.tstsiteapp.utils.crypto.CryptoHelper

/**
 * Cliente API con soporte para encriptación automática de payloads
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

/**
 * Request encriptado enviado al servidor
 */
@Serializable
data class EncryptedRequest(val encryptedPayload: String)

/**
 * Response encriptada recibida del servidor
 */
@Serializable
data class EncryptedResponse(val encryptedPayload: String)

/**
 * Helper común para todas las plataformas
 */
object ApiClientHelper {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    /**
     * Crea un HttpClient configurado
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
     * Encripta un request si ENCRYPTION_ENABLED = true
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
     * Desencripta una response si viene encriptada
     */
    suspend fun <T> decryptResponseIfNeeded(responseBody: String, deserializer: KSerializer<T>): T {
        return if (CryptoHelper.isEncryptionEnabled()) {
            try {
                // Intentar parsear como EncryptedResponse
                val encryptedResp = json.decodeFromString(
                    EncryptedResponse.serializer(),
                    responseBody
                )
                val secretKey = CryptoHelper.getSecretKey()
                PayloadCrypto.decrypt(deserializer, encryptedResp.encryptedPayload, secretKey)
            } catch (e: Exception) {
                // Si falla, parsear directo (servidor permite sin encriptar)
                println("⚠️ Parseando response sin encriptar: ${e.message}")
                json.decodeFromString(deserializer, responseBody)
            }
        } else {
            json.decodeFromString(deserializer, responseBody)
        }
    }

    /**
     * Helper para construir SesionResponse desde LoginResponseWrapper
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