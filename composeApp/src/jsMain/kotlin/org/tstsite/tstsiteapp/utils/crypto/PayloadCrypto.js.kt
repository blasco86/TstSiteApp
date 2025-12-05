package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
// Eliminadas: importaciones directas de TextDecoder y TextEncoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop
// import kotlin.js.getGlobalThis // Importada explícitamente - Reemplazada por globalThis

/**
 * Constantes de encriptación
 */
private const val IV_LENGTH = 12
private const val SALT_LENGTH = 32
private const val KEY_LENGTH = 32
private const val PBKDF2_ITERATIONS = 100000
private const val AUTH_TAG_LENGTH_BITS = 128 // 16 bytes * 8 bits/byte

@OptIn(ExperimentalEncodingApi::class)
@Suppress("unused") // Suprime la advertencia de que PayloadCrypto no se usa
actual object PayloadCrypto {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    actual suspend fun <T> encrypt(serializer: KSerializer<T>, data: T, secretKey: String): String {
        // Serializar a JSON
        val jsonString = json.encodeToString(serializer, data)
        // Acceder a TextEncoder a través del objeto global
        val plaintext = js("new TextEncoder()").encode(jsonString).toByteArray()

        // Generar salt e IV aleatorios
        val salt = randomBytes(SALT_LENGTH)
        val iv = randomBytes(IV_LENGTH)

        // Derivar clave
        val key = deriveKey(js("new TextEncoder()").encode(secretKey).toByteArray(), salt)

        // Encriptar
        val ciphertext = encryptAESGCM(plaintext, key, iv)

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
        val key = deriveKey(js("new TextEncoder()").encode(secretKey).toByteArray(), salt)

        // Desencriptar
        val plaintext = decryptAESGCM(ciphertext, key, iv)
        // Acceder a TextDecoder a través del objeto global
        val jsonString = js("new TextDecoder()").decode(plaintext.toUint8Array())

        // Deserializar
        return json.decodeFromString(deserializer, jsonString)
    }

    actual fun randomBytes(length: Int): ByteArray {
        val array = Uint8Array(length)
        // Acceder a window.crypto globalmente
        js("globalThis").asDynamic().crypto.getRandomValues(array)
        return array.toByteArray()
    }

    actual suspend fun deriveKey(password: ByteArray, salt: ByteArray): ByteArray {
        // Acceso dinámico a subtle para evitar problemas de interfaz
        val subtleDynamic = js("globalThis").asDynamic().crypto.subtle

        val passwordKey = subtleDynamic.importKey(
            "raw",
            password.toUint8Array(),
            js("({ name: 'PBKDF2' })"),
            false,
            arrayOf("deriveBits")
        ).await()

        val derivedBits = subtleDynamic.deriveBits(
            js("({ name: 'PBKDF2', salt: salt.toUint8Array(), iterations: $PBKDF2_ITERATIONS, hash: 'SHA-256' })"),
            passwordKey,
            KEY_LENGTH * 8 // length in bits
        ).await()

        return derivedBits.toByteArray()
    }

    private fun encryptAESGCM(
        plaintext: ByteArray,
        key: ByteArray,
        iv: ByteArray
    ): ByteArray {
        // Acceso dinámico a subtle para evitar problemas de interfaz
        val subtleDynamic = js("globalThis").asDynamic().crypto.subtle

        // Importar clave
        val cryptoKey = subtleDynamic.importKey(
            "raw",
            key.toUint8Array(),
            js("({ name: 'AES-GCM' })"),
            false,
            arrayOf("encrypt")
        ).await()

        // Encriptar
        val encryptedBuffer = subtleDynamic.encrypt(
            js("({ name: 'AES-GCM', iv: iv.toUint8Array(), tagLength: $AUTH_TAG_LENGTH_BITS })"),
            cryptoKey,
            plaintext.toUint8Array()
        ).await()

        return encryptedBuffer.toByteArray()
    }

    private fun decryptAESGCM(
        ciphertext: ByteArray,
        key: ByteArray,
        iv: ByteArray
    ): ByteArray {
        // Acceso dinámico a subtle para evitar problemas de interfaz
        val subtleDynamic = js("globalThis").asDynamic().crypto.subtle

        // Importar clave
        val cryptoKey = subtleDynamic.importKey(
            "raw",
            key.toUint8Array(),
            js("({ name: 'AES-GCM' })"),
            false,
            arrayOf("decrypt")
        ).await()

        // Desencriptar
        val decryptedBuffer = subtleDynamic.decrypt(
            js("({ name: 'AES-GCM', iv: iv.toUint8Array(), tagLength: $AUTH_TAG_LENGTH_BITS })"),
            cryptoKey,
            ciphertext.toUint8Array()
        ).await()

        return decryptedBuffer.toByteArray()
    }

    // Helper functions for ByteArray <-> Uint8Array conversion
    @OptIn(ExperimentalWasmJsInterop::class)
    private fun ByteArray.toUint8Array(): Uint8Array {
        val uint8Array = Uint8Array(this.size)
        for (i in 0 until this.size) {
            uint8Array.asDynamic()[i] = this[i].toInt() and 0xFF // Ensure it's a positive byte value
        }
        return uint8Array
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun Uint8Array.toByteArray(): ByteArray {
        val byteArray = ByteArray(this.length)
        for (i in 0 until this.length) {
            byteArray[i] = this.asDynamic()[i].unsafeCast<Int>().toByte()
        }
        return byteArray
    }

    // For ArrayBuffer.toByteArray()
    @OptIn(ExperimentalWasmJsInterop::class)
    private fun ArrayBuffer.toByteArray(): ByteArray {
        return Uint8Array(this).toByteArray()
    }
}
