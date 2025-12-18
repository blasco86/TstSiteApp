package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.coroutines.await
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

private const val IV_LENGTH = 12
private const val SALT_LENGTH = 32
private const val AUTH_TAG_LENGTH_BITS = 128 // 128 bits = 16 bytes

/**
 * 🌐 Implementación JavaScript de [PayloadCrypto] para el cifrado de payloads.
 *
 * Este objeto proporciona la lógica específica de JavaScript para cifrar y descifrar
 * datos utilizando AES-256-GCM y HMAC-SHA256 para la derivación de claves,
 * apoyándose en la [Web Crypto API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Crypto_API)
 * del navegador.
 */
@OptIn(ExperimentalEncodingApi::class)
@Suppress("unused")
actual object PayloadCrypto {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 🔒 Cifra datos serializables en la plataforma JavaScript.
     *
     * Utiliza AES-256-GCM para el cifrado, HMAC-SHA256 para derivar la clave
     * y genera un salt y un IV aleatorios para cada operación.
     *
     * @param serializer El serializador de Kotlinx para el tipo de objeto [T].
     * @param data El objeto de datos a cifrar.
     * @param secretKey La clave secreta principal para la derivación de la clave de cifrado.
     * @return Una cadena en formato Base64 que representa el payload cifrado (salt + IV + ciphertext + authTag).
     * @throws Exception Si ocurre un error durante el proceso de cifrado.
     */
    actual suspend fun <T> encrypt(serializer: KSerializer<T>, data: T, secretKey: String): String {
        try {
            val jsonString = json.encodeToString(serializer, data)
            console.log("🔐 Encriptando JSON:", jsonString)

            val plaintext = jsonString.encodeToByteArray() // CORREGIDO
            val salt = randomBytes(SALT_LENGTH)
            val iv = randomBytes(IV_LENGTH)
            val key = deriveKey(secretKey, salt) // Usar la nueva deriveKey con String

            val ciphertextWithTag = encryptAESGCM(plaintext, key, iv) // Esto devuelve ciphertext + tag combinados

            // Separar el ciphertext real y el authentication tag
            val actualCiphertext = ciphertextWithTag.copyOfRange(0, ciphertextWithTag.size - (AUTH_TAG_LENGTH_BITS / 8))
            val authTag = ciphertextWithTag.copyOfRange(ciphertextWithTag.size - (AUTH_TAG_LENGTH_BITS / 8), ciphertextWithTag.size)

            val combined = salt + iv + actualCiphertext + authTag

            return Base64.encode(combined)
        } catch (e: Exception) {
            console.error("❌ Error en encrypt:", e.message)
            throw e
        }
    }

    /**
     * 🔓 Descifra un payload cifrado en la plataforma JavaScript.
     *
     * Espera un payload en formato Base64 que contiene el salt, IV, texto cifrado y authTag.
     *
     * @param deserializer El deserializador de Kotlinx para el tipo de objeto de destino [T].
     * @param encryptedData La cadena en formato Base64 que contiene los datos cifrados.
     * @param secretKey La clave secreta principal para la derivación de la clave de descifrado.
     * @return El objeto de datos [T] original.
     * @throws Exception Si ocurre un error durante el descifrado (ej. clave incorrecta, datos corruptos).
     */
    actual suspend fun <T> decrypt(deserializer: KSerializer<T>, encryptedData: String, secretKey: String): T {
        try {
            val buffer = Base64.decode(encryptedData)
            val salt = buffer.sliceArray(0 until SALT_LENGTH)
            val iv = buffer.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
            val actualCiphertext = buffer.sliceArray(SALT_LENGTH + IV_LENGTH until buffer.size - (AUTH_TAG_LENGTH_BITS / 8))
            val authTag = buffer.sliceArray(buffer.size - (AUTH_TAG_LENGTH_BITS / 8) until buffer.size)

            val key = deriveKey(secretKey, salt) // Usar la nueva deriveKey con String

            // Recombinar ciphertext y authTag para decryptAESGCM
            val ciphertextWithTag = actualCiphertext + authTag

            val plaintext = decryptAESGCM(ciphertextWithTag, key, iv)
            val jsonString = plaintext.decodeToString() // CORREGIDO

            console.log("🔓 JSON desencriptado:", jsonString)
            return json.decodeFromString(deserializer, jsonString)
        } catch (e: Exception) {
            console.error("❌ Error en decrypt:", e.message)
            throw e
        }
    }

    /**
     * 🎲 Genera un array de bytes aleatorios criptográficamente seguros en JavaScript.
     *
     * Utiliza `crypto.getRandomValues` de la Web Crypto API para asegurar la aleatoriedad
     * necesaria para salts e IVs.
     *
     * @param length El número de bytes aleatorios a generar.
     * @return Un [ByteArray] con los bytes generados.
     */
    actual fun randomBytes(length: Int): ByteArray {
        val array = Uint8Array(length)
        js("crypto").getRandomValues(array)
        return array.toByteArray()
    }

    /**
     * 🔑 Deriva una clave a partir de una contraseña y un salt usando HMAC-SHA256 en JavaScript.
     *
     * Utiliza `SubtleCrypto.importKey` y `SubtleCrypto.sign` de la Web Crypto API.
     *
     * @param password La contraseña o secreto original como [String].
     * @param salt Un conjunto de bytes aleatorios.
     * @return La clave derivada como [ByteArray].
     * @throws Exception Si ocurre un error durante la derivación de la clave.
     */
    actual suspend fun deriveKey(password: String, salt: ByteArray): ByteArray {
        try {
            val subtle = js("crypto.subtle")
            val passwordBytes = password.encodeToByteArray()

            val hmacKey = (subtle.importKey(
                "raw",
                passwordBytes.toUint8Array(),
                js("({ name: 'HMAC', hash: 'SHA-256' })"),
                false, // not extractable
                js("(['sign'])") // only need to sign (HMAC)
            ) as Promise<*>).await()

            val signature = (subtle.sign(
                js("({ name: 'HMAC' })"), // CORREGIDO: Solo el nombre del algoritmo
                hmacKey,
                salt.toUint8Array()
            ) as Promise<*>).await()

            return (signature as ArrayBuffer).toByteArray()
        } catch (e: Exception) {
            console.error("❌ Error en deriveKey (HMAC):", e.message)
            throw e
        }
    }

    /**
     * 🔒 Función auxiliar para cifrar datos utilizando AES-GCM en JavaScript.
     *
     * @param plaintext Los datos en texto plano a cifrar.
     * @param key La clave AES para el cifrado.
     * @param iv El Vector de Inicialización (IV).
     * @return Los datos cifrados como [ByteArray] (ciphertext + authTag).
     * @throws Exception Si ocurre un error durante el cifrado.
     */
    private suspend fun encryptAESGCM(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        try {
            val subtle = js("crypto.subtle")

            val cryptoKey = (subtle.importKey(
                "raw",
                key.toUint8Array(),
                js("({ name: 'AES-GCM' })"),
                false,
                js("(['encrypt'])")
            ) as Promise<*>).await()

            val ivArray = iv.toUint8Array()
            val encryptedBuffer = (subtle.encrypt(
                js("({ name: 'AES-GCM', iv: ivArray, tagLength: $AUTH_TAG_LENGTH_BITS })"),
                cryptoKey,
                plaintext.toUint8Array()
            ) as Promise<*>).await()

            return (encryptedBuffer as ArrayBuffer).toByteArray()
        } catch (e: Exception) {
            console.error("❌ Error en encryptAESGCM:", e.message)
            throw e
        }
    }

    /**
     * 🔓 Función auxiliar para descifrar datos utilizando AES-GCM en JavaScript.
     *
     * @param ciphertext Los datos cifrados (ciphertext + authTag).
     * @param key La clave AES para la desencriptación.
     * @param iv El Vector de Inicialización (IV).
     * @return Los datos desencriptados en texto plano como [ByteArray].
     * @throws Exception Si ocurre un error durante el descifrado (ej. clave incorrecta, datos corruptos).
     */
    private suspend fun decryptAESGCM(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        try {
            val subtle = js("crypto.subtle")

            val cryptoKey = (subtle.importKey(
                "raw",
                key.toUint8Array(),
                js("({ name: 'AES-GCM' })"),
                false,
                js("(['decrypt'])")
            ) as Promise<*>).await()

            val ivArray = iv.toUint8Array()
            val decryptedBuffer = (subtle.decrypt(
                js("({ name: 'AES-GCM', iv: ivArray, tagLength: $AUTH_TAG_LENGTH_BITS })"),
                cryptoKey,
                ciphertext.toUint8Array()
            ) as Promise<*>).await()

            return (decryptedBuffer as ArrayBuffer).toByteArray()
        } catch (e: Exception) {
            console.error("❌ Error en decryptAESGCM:", e.message)
            throw e
        }
    }

    // --- Funciones de Extensión para Conversión de Tipos ---
    /**
     * ➡️ Convierte un `ByteArray` de Kotlin a un `Uint8Array` de JavaScript.
     */
    private fun ByteArray.toUint8Array(): Uint8Array {
        val uint8Array = Uint8Array(this.size)
        for (i in this.indices) {
            uint8Array.asDynamic()[i] = (this[i].toInt() and 0xFF)
        }
        return uint8Array
    }

    /**
     * ⬅️ Convierte un `Uint8Array` de JavaScript a un `ByteArray` de Kotlin.
     */
    private fun Uint8Array.toByteArray(): ByteArray {
        val byteArray = ByteArray(this.length)
        for (i in 0 until this.length) {
            byteArray[i] = this.asDynamic()[i].unsafeCast<Int>().toByte()
        }
        return byteArray
    }

    /**
     * ⬅️ Convierte un `ArrayBuffer` de JavaScript a un `ByteArray` de Kotlin.
     */
    private fun ArrayBuffer.toByteArray(): ByteArray {
        return Uint8Array(this).toByteArray()
    }
}