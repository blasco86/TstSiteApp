package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

const val ALGORITHM = "AES/GCM/NoPadding"
const val IV_LENGTH = 12
const val AUTH_TAG_LENGTH = 16
const val SALT_LENGTH = 32
const val KEY_LENGTH = 32
const val PBKDF2_ITERATIONS = 100000

@OptIn(ExperimentalEncodingApi::class)
actual object PayloadCrypto {
    val json = Json { ignoreUnknownKeys = true }
    val secureRandom = SecureRandom()

    actual inline fun <reified T> encrypt(data: T, secretKey: String): String {
        val jsonString = json.encodeToString(data)
        val plaintext = jsonString.toByteArray(Charsets.UTF_8)

        val salt = randomBytes(SALT_LENGTH)
        val iv = randomBytes(IV_LENGTH)
        val key = deriveKey(secretKey.toByteArray(Charsets.UTF_8), salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(AUTH_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext)
        val combined = salt + iv + ciphertext

        return Base64.encode(combined)
    }

    actual inline fun <reified T> decrypt(encryptedData: String, secretKey: String): T {
        val buffer = Base64.decode(encryptedData)

        val salt = buffer.sliceArray(0 until SALT_LENGTH)
        val iv = buffer.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val ciphertext = buffer.sliceArray(SALT_LENGTH + IV_LENGTH until buffer.size)

        val key = deriveKey(secretKey.toByteArray(Charsets.UTF_8), salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(AUTH_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        val plaintext = cipher.doFinal(ciphertext)
        val jsonString = String(plaintext, Charsets.UTF_8)

        return json.decodeFromString(jsonString)
    }

    actual fun randomBytes(length: Int): ByteArray {
        return ByteArray(length).apply { secureRandom.nextBytes(this) }
    }

    actual fun deriveKey(password: ByteArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            String(password, Charsets.UTF_8).toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH * 8
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}

// Cliente HTTP con Ktor
private val httpClient = HttpClient(CIO) {
    expectSuccess = false
}

actual suspend fun makeHttpRequest(
    url: String,
    method: String,
    headers: Map<String, String>,
    body: String?
): String {
    val response: HttpResponse = when (method) {
        "GET" -> httpClient.get(url) {
            headers.forEach { (key, value) -> header(key, value) }
        }
        "POST" -> httpClient.post(url) {
            headers.forEach { (key, value) -> header(key, value) }
            body?.let {
                contentType(ContentType.Application.Json)
                setBody(it)
            }
        }
        "PUT" -> httpClient.put(url) {
            headers.forEach { (key, value) -> header(key, value) }
            body?.let {
                contentType(ContentType.Application.Json)
                setBody(it)
            }
        }
        "DELETE" -> httpClient.delete(url) {
            headers.forEach { (key, value) -> header(key, value) }
        }
        else -> error("Método HTTP no soportado: $method")
    }

    return response.bodyAsText()
}