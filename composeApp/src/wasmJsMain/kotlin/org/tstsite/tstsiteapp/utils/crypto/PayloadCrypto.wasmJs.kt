@file:OptIn(ExperimentalWasmJsInterop::class)
package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.coroutines.await
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsString

// --- Declaraciones Externas para TextEncoder/Decoder ---
/**
 * 📝 Interfaz externa para el objeto `TextEncoder` de JavaScript.
 * Permite codificar cadenas de texto en `Uint8Array`.
 */
external class TextEncoder {
    /**
     * ➡️ Codifica una cadena de texto en un `Uint8Array`.
     * @param input La cadena de texto a codificar.
     * @return Un `Uint8Array` que representa la cadena codificada.
     */
    fun encode(input: String): Uint8Array
}

/**
 * 📖 Interfaz externa para el objeto `TextDecoder` de JavaScript.
 * Permite decodificar `Uint8Array` en cadenas de texto.
 */
external class TextDecoder {
    /**
     * ⬅️ Decodifica un `Uint8Array` en una cadena de texto.
     * @param input El `Uint8Array` a decodificar.
     * @return La cadena de texto decodificada.
     */
    fun decode(input: JsAny): String
}

/**
 * 🔒 Constantes de configuración para el cifrado AES-256-GCM.
 */
private const val IV_LENGTH = 12
private const val SALT_LENGTH = 32
private const val AUTH_TAG_LENGTH_BITS = 128 // 16 bytes * 8 bits/byte

// =============================
// Helpers JS top-level para algoritmos
// =============================

private val jsAesGcmImportParams: JsAny = js("({ name: 'AES-GCM' })")
private val jsEncryptUsage: JsArray<JsString> = js("(['encrypt'])")
private val jsDecryptUsage: JsArray<JsString> = js("(['decrypt'])")

// Nuevas constantes para HMAC
private val jsHmacImportParams: JsAny = js("({ name: 'HMAC', hash: 'SHA-256' })")
private val jsHmacSignUsage: JsArray<JsString> = js("(['sign'])")
private val jsHmacSignAlgorithm: JsAny = js("({ name: 'HMAC' })") // CORREGIDO: Añadida constante


private fun createAesGcmParams(iv: Uint8Array): JsAny =
    js("({ name: 'AES-GCM', iv: iv, tagLength: 128 })")


/**
 * 🕸️ Implementación WasmJs de [PayloadCrypto] para el cifrado de payloads.
 *
 * Este objeto proporciona la lógica específica de WasmJs para cifrar y descifrar
 * datos utilizando AES-256-GCM y HMAC-SHA256 para la derivación de claves,
 * apoyándose en la [Web Crypto API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Crypto_API)
 * del navegador, a través de la interoperabilidad con JavaScript.
 */
@OptIn(ExperimentalEncodingApi::class)
@Suppress("unused", "EXPERIMENTAL_EXPECT_ACTUAL_CLASSES")
actual object PayloadCrypto {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 🔒 Cifra datos serializables en la plataforma WasmJs.
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
        val jsonString = json.encodeToString(serializer, data)
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
    }

    /**
     * 🔓 Descifra un payload cifrado en la plataforma WasmJs.
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

        return json.decodeFromString(deserializer, jsonString)
    }

    /**
     * 🎲 Genera un array de bytes aleatorios criptográficamente seguros en WasmJs.
     *
     * Utiliza `crypto.getRandomValues` de la Web Crypto API para asegurar la aleatoriedad
     * necesaria para salts e IVs.
     *
     * @param length El número de bytes aleatorios a generar.
     * @return Un [ByteArray] con los bytes generados.
     */
    actual fun randomBytes(length: Int): ByteArray {
        val array = Uint8Array(ArrayBuffer(length))
        crypto.getRandomValues(array)
        return array.buffer.toByteArray()
    }

    /**
     * 🔑 Deriva una clave a partir de una contraseña y un salt usando HMAC-SHA256 en WasmJs.
     *
     * Utiliza `SubtleCrypto.importKey` y `SubtleCrypto.sign` de la Web Crypto API.
     *
     * @param password La contraseña o secreto original como [String].
     * @param salt Un conjunto de bytes aleatorios.
     * @return La clave derivada como [ByteArray].
     * @throws Exception Si ocurre un error durante la derivación de la clave.
     */
    actual suspend fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val subtle = crypto.subtle
        val passwordBytes = password.encodeToByteArray()

        val hmacKey: CryptoKey = subtle.importKey(
            "raw",
            passwordBytes.toUint8Array(),
            jsHmacImportParams,
            false, // not extractable
            jsHmacSignUsage // only need to sign (HMAC)
        ).await()

        val signature: ArrayBuffer = subtle.sign(
            jsHmacSignAlgorithm, // CORREGIDO: Usar la constante
            hmacKey,
            salt.toUint8Array()
        ).await()

        return signature.toByteArray()
    }

    /**
     * 🔒 Función auxiliar para cifrar datos utilizando AES-GCM en WasmJs.
     *
     * @param plaintext Los datos en texto plano a cifrar.
     * @param key La clave AES para el cifrado.
     * @param iv El Vector de Inicialización (IV).
     * @return Los datos cifrados como [ByteArray] (ciphertext + authTag).
     * @throws Exception Si ocurre un error durante el cifrado.
     */
    private suspend fun encryptAESGCM(
        plaintext: ByteArray,
        key: ByteArray,
        iv: ByteArray
    ): ByteArray {
        val subtle = crypto.subtle

        val cryptoKey: CryptoKey = subtle.importKey(
            "raw",
            key.toUint8Array(),
            jsAesGcmImportParams,
            false,
            jsEncryptUsage
        ).await()

        val encryptedBuffer: ArrayBuffer = subtle.encrypt(
            createAesGcmParams(iv.toUint8Array()),
            cryptoKey,
            plaintext.toUint8Array()
        ).await()

        return encryptedBuffer.toByteArray()
    }

    /**
     * 🔓 Función auxiliar para descifrar datos utilizando AES-GCM en WasmJs.
     *
     * @param ciphertext Los datos cifrados (ciphertext + authTag).
     * @param key La clave AES para la desencriptación.
     * @param iv El Vector de Inicialización (IV).
     * @return Los datos desencriptados en texto plano como [ByteArray].
     * @throws Exception Si ocurre un error durante el descifrado (ej. clave incorrecta, datos corruptos).
     */
    private suspend fun decryptAESGCM(
        ciphertext: ByteArray,
        key: ByteArray,
        iv: ByteArray
    ): ByteArray {
        val subtle = crypto.subtle

        val cryptoKey: CryptoKey = subtle.importKey(
            "raw",
            key.toUint8Array(),
            jsAesGcmImportParams,
            false,
            jsDecryptUsage
        ).await()

        val decryptedBuffer: ArrayBuffer = subtle.decrypt(
            createAesGcmParams(iv.toUint8Array()),
            cryptoKey,
            ciphertext.toUint8Array()
        ).await()

        return decryptedBuffer.toByteArray()
    }
}