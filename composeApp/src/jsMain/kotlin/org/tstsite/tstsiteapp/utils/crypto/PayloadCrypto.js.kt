package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val IV_LENGTH = 12
private const val SALT_LENGTH = 32
private const val KEY_LENGTH = 32
private const val PBKDF2_ITERATIONS = 100000
private const val AUTH_TAG_LENGTH_BITS = 128

@OptIn(ExperimentalEncodingApi::class)
@Suppress("unused")
actual object PayloadCrypto {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    actual suspend fun <T> encrypt(serializer: KSerializer<T>, data: T, secretKey: String): String {
        try {
            val jsonString = json.encodeToString(serializer, data)
            console.log("🔐 Encriptando JSON:", jsonString)

            val plaintext = js("new TextEncoder()").encode(jsonString).toByteArray()
            val salt = randomBytes(SALT_LENGTH)
            val iv = randomBytes(IV_LENGTH)
            val key = deriveKey(secretKey.encodeToByteArray(), salt)
            val ciphertext = encryptAESGCM(plaintext, key, iv)
            val combined = salt + iv + ciphertext

            return Base64.encode(combined)
        } catch (e: Exception) {
            console.error("❌ Error en encrypt:", e.message)
            throw e
        }
    }

    actual suspend fun <T> decrypt(deserializer: KSerializer<T>, encryptedData: String, secretKey: String): T {
        try {
            val buffer = Base64.decode(encryptedData)
            val salt = buffer.sliceArray(0 until SALT_LENGTH)
            val iv = buffer.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
            val ciphertext = buffer.sliceArray(SALT_LENGTH + IV_LENGTH until buffer.size)

            val key = deriveKey(secretKey.encodeToByteArray(), salt)
            val plaintext = decryptAESGCM(ciphertext, key, iv)
            val jsonString = js("new TextDecoder()").decode(plaintext.toUint8Array()) as String

            console.log("🔓 JSON desencriptado:", jsonString)
            return json.decodeFromString(deserializer, jsonString)
        } catch (e: Exception) {
            console.error("❌ Error en decrypt:", e.message)
            throw e
        }
    }

    actual fun randomBytes(length: Int): ByteArray {
        val array = Uint8Array(length)
        js("crypto").getRandomValues(array)
        return array.toByteArray()
    }

    actual suspend fun deriveKey(password: ByteArray, salt: ByteArray): ByteArray {
        try {
            val subtle = js("crypto.subtle")

            val passwordKey = subtle.importKey(
                "raw",
                password.toUint8Array(),
                js("({ name: 'PBKDF2' })"),
                false,
                js("(['deriveBits'])")
            ).await()

            val saltArray = salt.toUint8Array()
            val derivedBits = subtle.deriveBits(
                js("({ name: 'PBKDF2', salt: saltArray, iterations: $PBKDF2_ITERATIONS, hash: 'SHA-256' })"),
                passwordKey,
                KEY_LENGTH * 8
            ).await()

            return (derivedBits as ArrayBuffer).toByteArray()
        } catch (e: Exception) {
            console.error("❌ Error en deriveKey:", e.message)
            throw e
        }
    }

    private suspend fun encryptAESGCM(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        try {
            val subtle = js("crypto.subtle")

            val cryptoKey = subtle.importKey(
                "raw",
                key.toUint8Array(),
                js("({ name: 'AES-GCM' })"),
                false,
                js("(['encrypt'])")
            ).await()

            val ivArray = iv.toUint8Array()
            val encryptedBuffer = subtle.encrypt(
                js("({ name: 'AES-GCM', iv: ivArray, tagLength: $AUTH_TAG_LENGTH_BITS })"),
                cryptoKey,
                plaintext.toUint8Array()
            ).await()

            return (encryptedBuffer as ArrayBuffer).toByteArray()
        } catch (e: Exception) {
            console.error("❌ Error en encryptAESGCM:", e.message)
            throw e
        }
    }

    private suspend fun decryptAESGCM(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        try {
            val subtle = js("crypto.subtle")

            val cryptoKey = subtle.importKey(
                "raw",
                key.toUint8Array(),
                js("({ name: 'AES-GCM' })"),
                false,
                js("(['decrypt'])")
            ).await()

            val ivArray = iv.toUint8Array()
            val decryptedBuffer = subtle.decrypt(
                js("({ name: 'AES-GCM', iv: ivArray, tagLength: $AUTH_TAG_LENGTH_BITS })"),
                cryptoKey,
                ciphertext.toUint8Array()
            ).await()

            return (decryptedBuffer as ArrayBuffer).toByteArray()
        } catch (e: Exception) {
            console.error("❌ Error en decryptAESGCM:", e.message)
            throw e
        }
    }

    // Helpers de conversión
    private fun ByteArray.toUint8Array(): Uint8Array {
        val uint8Array = Uint8Array(this.size)
        for (i in this.indices) {
            uint8Array.asDynamic()[i] = (this[i].toInt() and 0xFF)
        }
        return uint8Array
    }

    private fun Uint8Array.toByteArray(): ByteArray {
        val byteArray = ByteArray(this.length)
        for (i in 0 until this.length) {
            byteArray[i] = this.asDynamic()[i].unsafeCast<Int>().toByte()
        }
        return byteArray
    }

    private fun ArrayBuffer.toByteArray(): ByteArray {
        return Uint8Array(this).toByteArray()
    }
}