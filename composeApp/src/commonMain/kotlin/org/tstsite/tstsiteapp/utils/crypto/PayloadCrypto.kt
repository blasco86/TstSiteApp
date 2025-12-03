package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 🔐 Sistema de encriptación bidireccional para TstSite
 * Compatible con API Node.js Express
 */

@Serializable
data class EncryptedPayload(
    val encryptedPayload: String
)

@OptIn(ExperimentalTime::class)
@Serializable
data class PayloadWrapper<T>(
    val data: T,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Interfaz común para operaciones criptográficas
 */
expect object PayloadCrypto {
    inline fun <reified T> encrypt(data: T, secretKey: String): String
    inline fun <reified T> decrypt(encryptedData: String, secretKey: String): T
    fun randomBytes(length: Int): ByteArray
    fun deriveKey(password: ByteArray, salt: ByteArray): ByteArray
}

/**
 * 🌐 Cliente API con encriptación automática
 */
class SecureApiClient(
    val baseUrl: String,
    val apiKey: String,
    val secretKey: String,
    val encryptionEnabled: Boolean = false
) {
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * POST con encriptación opcional
     */
    suspend inline fun <reified T, reified R> post(
        endpoint: String,
        data: T?,
        token: String? = null,
        forceEncrypt: Boolean = false
    ): R {
        val headers = buildMap {
            put("Content-Type", "application/json")
            put("x-api-key", apiKey)
            token?.let { put("Authorization", "Bearer $it") }
        }

        // Determinar si encriptar
        val shouldEncrypt = (encryptionEnabled || forceEncrypt) && data != null

        val body = if (shouldEncrypt && data != null) {
            // Envolver con timestamp y encriptar
            val wrapper = PayloadWrapper(data)
            val encrypted = PayloadCrypto.encrypt(wrapper, secretKey)
            json.encodeToString(EncryptedPayload(encrypted))
        } else if (data != null) {
            // Enviar sin encriptar
            json.encodeToString(data)
        } else {
            null
        }

        val response: String = makeHttpRequest(
            url = "$baseUrl$endpoint",
            method = "POST",
            headers = headers,
            body = body
        )

        // Intentar desencriptar respuesta si viene encriptada
        return try {
            val encryptedResponse = json.decodeFromString<EncryptedPayload>(response)
            PayloadCrypto.decrypt(encryptedResponse.encryptedPayload, secretKey)
        } catch (e: Exception) {
            // Si no está encriptada, parsear directamente
            json.decodeFromString(response)
        }
    }

    /**
     * GET sin encriptación
     */
    suspend inline fun <reified R> get(
        endpoint: String,
        token: String? = null
    ): R {
        val headers = buildMap {
            put("x-api-key", apiKey)
            token?.let { put("Authorization", "Bearer $it") }
        }

        val response: String = makeHttpRequest(
            url = "$baseUrl$endpoint",
            method = "GET",
            headers = headers
        )

        return json.decodeFromString(response)
    }
}

/**
 * HTTP request - implementación específica por plataforma
 */
expect suspend fun makeHttpRequest(
    url: String,
    method: String,
    headers: Map<String, String>,
    body: String? = null
): String

/**
 * 🏭 Factory para crear clientes API
 */
object ApiClientFactory {
    fun create(
        environment: Environment = Environment.DEV,
        encryptionEnabled: Boolean = false
    ): SecureApiClient {
        return SecureApiClient(
            baseUrl = environment.baseUrl,
            apiKey = environment.apiKey,
            secretKey = environment.secretKey,
            encryptionEnabled = encryptionEnabled
        )
    }
}

/**
 * 🌍 Configuración por entornos
 */
enum class Environment(
    val baseUrl: String,
    val apiKey: String,
    val secretKey: String
) {
    DEV(
        baseUrl = "http://localhost:3000",
        apiKey = "qr27IUNYhrJcvZXzPwOOM4yjrfFZE9IKp-JmRuFxUOM",
        secretKey = "qr27IUNYhrJcvZXzPwOOM4yjrfFZE9IKp-JmRuFxUOM"
    ),
    TST(
        baseUrl = "https://tstsite.alwaysdata.net",
        apiKey = "qr27IUNYhrJcvZXzPwOOM4yjrfFZE9IKp-JmRuFxUOM",
        secretKey = "qr27IUNYhrJcvZXzPwOOM4yjrfFZE9IKp-JmRuFxUOM"
    )
}