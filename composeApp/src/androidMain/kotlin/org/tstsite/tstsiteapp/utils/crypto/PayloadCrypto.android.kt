package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Constantes de encriptación
 */
private const val ALGORITHM = "AES/GCM/NoPadding"
private const val IV_LENGTH = 12
private const val AUTH_TAG_LENGTH = 16
private const val SALT_LENGTH = 32
private const val KEY_LENGTH = 32
private const val PBKDF2_ITERATIONS = 100000

@OptIn(ExperimentalEncodingApi::class)
@Suppress("unused") // Suprime la advertencia de que PayloadCrypto no se usa
actual object PayloadCrypto {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val secureRandom = SecureRandom()

    actual suspend fun <T> encrypt(serializer: KSerializer<T>, data: T, secretKey: String): String {
        // Serializar a JSON
        val jsonString = json.encodeToString(serializer, data)
        val plaintext = jsonString.toByteArray(Charsets.UTF_8)

        // Generar salt e IV aleatorios
        val salt = randomBytes(SALT_LENGTH)
        val iv = randomBytes(IV_LENGTH)

        // Derivar clave
        val key = deriveKey(secretKey.toByteArray(Charsets.UTF_8), salt)

        // Encriptar
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(AUTH_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext)

        // Combinar: salt + iv + ciphertext
        val combined = salt + iv + ciphertext

        return Base64.encode(combined)
    }

    actual suspend fun <T> decrypt(deserializer: KSerializer<T>, encryptedData: String, secretKey: String): T {
        // Decodificar Base64
        val buffer = Base64.decode(encryptedData)

        // Extraer componentes
        val salt = buffer.sliceArray(0 until SALT_LENGTH)
        val iv = buffer.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val ciphertext = buffer.sliceArray(SALT_LENGTH + IV_LENGTH until buffer.size)

        // Derivar clave
        val key = deriveKey(secretKey.toByteArray(Charsets.UTF_8), salt)

        // Desencriptar
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(AUTH_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        val plaintext = cipher.doFinal(ciphertext)
        val jsonString = String(plaintext, Charsets.UTF_8)

        // Deserializar
        return json.decodeFromString(deserializer, jsonString)
    }

    actual fun randomBytes(length: Int): ByteArray {
        return ByteArray(length).apply {
            secureRandom.nextBytes(this)
        }
    }

    actual suspend fun deriveKey(password: ByteArray, salt: ByteArray): ByteArray {
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
