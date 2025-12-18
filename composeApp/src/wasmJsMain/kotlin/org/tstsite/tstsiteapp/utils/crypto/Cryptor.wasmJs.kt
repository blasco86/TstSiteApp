@file:OptIn(ExperimentalWasmJsInterop::class)

package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.coroutines.await
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsString
import kotlin.js.ExperimentalWasmJsInterop

// =============================
// Interop JS / Web Crypto (Declaraciones Centralizadas)
// =============================

/**
 * 📦 Interfaz externa para el objeto `ArrayBuffer` de JavaScript.
 * Representa un búfer de datos binarios de longitud fija.
 */
external class ArrayBuffer(byteLength: Int) : JsAny {
    val byteLength: Int
}

/**
 * 🔢 Interfaz externa para el objeto `Uint8Array` de JavaScript.
 * Representa un array de enteros sin signo de 8 bits.
 */
external class Uint8Array(buffer: ArrayBuffer) : JsAny {
    val length: Int
    val buffer: ArrayBuffer // Propiedad añadida para acceder al ArrayBuffer subyacente
    operator fun get(index: Int): Byte
    operator fun set(index: Int, value: Byte)
}

/**
 * 🌐 Interfaz externa para acceder a la API de Criptografía Web del navegador.
 */
external val crypto: Crypto

/**
 * 🔑 Interfaz externa para el objeto `Crypto` de la Web Crypto API.
 * Proporciona acceso a operaciones criptográficas.
 */
external interface Crypto : JsAny {
    /**
     * 🛠️ Acceso a las operaciones criptográficas sutiles (SubtleCrypto).
     */
    val subtle: SubtleCrypto
    /**
     * 🎲 Rellena un `Uint8Array` con valores aleatorios criptográficamente seguros.
     */
    fun getRandomValues(array: Uint8Array)
}

/**
 * ⚙️ Interfaz externa para el objeto `SubtleCrypto` de la Web Crypto API.
 * Contiene métodos para operaciones criptográficas de bajo nivel.
 */
external interface SubtleCrypto : JsAny {
    /**
     * 📥 Importa una clave criptográfica.
     * @param format Formato de la clave (ej: "raw").
     * @param keyData Datos de la clave (Uint8Array o ArrayBuffer).
     * @param algorithm Algoritmo de la clave (ej: AES-CBC, HMAC).
     * @param extractable Indica si la clave puede ser exportada.
     * @param keyUsages Usos permitidos para la clave (ej: "sign", "decrypt").
     * @return Una Promesa que resuelve a un objeto `CryptoKey`.
     */
    fun importKey(
        format: String,
        keyData: JsAny, // Uint8Array or ArrayBuffer
        algorithm: JsAny,
        extractable: Boolean,
        keyUsages: JsArray<JsString>
    ): Promise<CryptoKey>

    /**
     * ✍️ Firma datos con una clave HMAC.
     * @param algorithm Algoritmo de firma (ej: { name: 'HMAC' }).
     * @param key La clave `CryptoKey` a usar para firmar.
     * @param data Los datos a firmar.
     * @return Una Promesa que resuelve a un `ArrayBuffer` con la firma.
     */
    fun sign(
        algorithm: JsAny,
        key: CryptoKey,
        data: Uint8Array
    ): Promise<ArrayBuffer>

    /**
     * 🔓 Desencripta datos con una clave AES-CBC.
     * @param algorithm Algoritmo de desencriptación (ej: { name: 'AES-CBC', iv: ivArray }).
     * @param key La clave `CryptoKey` a usar para desencriptar.
     * @param data Los datos cifrados.
     * @return Una Promesa que resuelve a un `ArrayBuffer` con los datos desencriptados.
     */
    fun decrypt(
        algorithm: JsAny,
        key: CryptoKey,
        data: Uint8Array
    ): Promise<ArrayBuffer>

    /**
     * 🔑 Deriva bits de clave a partir de una clave base y un algoritmo.
     * @param algorithm Algoritmo de derivación (ej: PBKDF2).
     * @param baseKey La clave base para la derivación.
     * @param length La longitud de los bits derivados deseada.
     * @return Una Promesa que resuelve a un `ArrayBuffer` con los bits derivados.
     */
    fun deriveBits(
        algorithm: JsAny,
        baseKey: CryptoKey,
        length: Int
    ): Promise<ArrayBuffer>

    /**
     * 🔒 Cifra datos con una clave AES-GCM.
     * @param algorithm Algoritmo de cifrado (ej: { name: 'AES-GCM', iv: ivArray }).
     * @param key La clave `CryptoKey` a usar para cifrar.
     * @param data Los datos a cifrar.
     * @return Una Promesa que resuelve a un `ArrayBuffer` con los datos cifrados.
     */
    fun encrypt(
        algorithm: JsAny,
        key: CryptoKey,
        data: JsAny // BufferSource
    ): Promise<ArrayBuffer>
}

/**
 * 🔑 Interfaz externa para una clave criptográfica manejada por la Web Crypto API.
 */
external interface CryptoKey : JsAny

// =============================
// Helpers de conversión
// =============================

/**
 * ⬅️ Convierte un `ArrayBuffer` de JavaScript a un `ByteArray` de Kotlin.
 */
internal fun ArrayBuffer.toByteArray(): ByteArray {
    val arr = Uint8Array(this)
    val byteArray = ByteArray(arr.length)
    for (i in 0 until arr.length) byteArray[i] = arr[i]
    return byteArray
}

/**
 * ➡️ Convierte un `ByteArray` de Kotlin a un `Uint8Array` de JavaScript.
 */
internal fun ByteArray.toUint8Array(): Uint8Array {
    val buffer = ArrayBuffer(size)
    val arr = Uint8Array(buffer)
    for (i in indices) arr[i] = this[i]
    return arr
}

// =============================
// Helpers JS top-level
// =============================

private val jsHmacAlgorithm: JsAny = js("({ name: 'HMAC', hash: 'SHA-256' })")
private val jsHmacSign: JsArray<JsString> = js("(['sign'])")
private val jsHmacSignAlgorithm: JsAny = js("({ name: 'HMAC' })")
private val jsAesKeyAlgorithm: JsAny = js("({ name: 'AES-CBC' })")
private val jsAesDecryptUsage: JsArray<JsString> = js("(['decrypt'])")

@Suppress("UNUSED_PARAMETER")
private fun createAesCbcAlgorithm(iv: Uint8Array): JsAny =
    js("({ name: 'AES-CBC', iv: iv })")

private fun jsAesDecryptAlgorithm(iv: ByteArray): JsAny {
    val uint8Iv = iv.toUint8Array()
    return createAesCbcAlgorithm(uint8Iv)
}
// =============================
// Cryptor
// =============================

/**
 * 🕸️ Implementación WasmJs de [Cryptor] para el cifrado Fernet.
 *
 * Esta clase proporciona la lógica específica de WasmJs para desencriptar tokens Fernet,
 * utilizando la [Web Crypto API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Crypto_API)
 * del navegador, a través de la interoperabilidad con JavaScript.
 *
 * @property fernetKey La clave Fernet de 32 bytes (16 para firma, 16 para cifrado) codificada en Base64URL.
 */
@OptIn(ExperimentalEncodingApi::class)
actual class Cryptor actual constructor(fernetKey: String) : BaseCryptor(fernetKey) {

    private val signingKey: ByteArray
    private val encryptionKey: ByteArray

    init {
        val keyBytes = base64UrlDecode(fernetKey)
        require(keyBytes.size == 32) { "Fernet key debe ser de 32 bytes" }
        signingKey = keyBytes.copyOfRange(0, 16) // Los primeros 16 bytes para la firma HMAC
        encryptionKey = keyBytes.copyOfRange(16, 32) // Los siguientes 16 bytes para el cifrado AES
    }

    /**
     * 🔓 Desencripta un token Fernet en la plataforma WasmJs.
     *
     * Este método sigue el estándar Fernet y utiliza la Web Crypto API a través de JS interop:
     * 1.  **Decodificación:** El `encryptedValue` (token Fernet) se decodifica de Base64URL.
     * 2.  **Validación de Longitud y Versión:** Se verifica que el token tenga una longitud mínima
     *     y que la versión Fernet sea la soportada (0x80).
     * 3.  **Verificación HMAC:**
     *     *   Se extrae el HMAC proporcionado al final del token.
     *     *   Se calcula el HMAC de los datos restantes (versión, timestamp, IV, ciphertext)
     *         utilizando `computeHmacSha256` (que usa `SubtleCrypto.sign`).
     *     *   Se compara el HMAC calculado con el proporcionado para detectar manipulaciones.
     * 4.  **Extracción de IV y Ciphertext:** Se obtienen el Vector de Inicialización (IV) y el texto cifrado.
     * 5.  **Desencriptación AES:**
     *     *   Se utiliza `decryptAesCbc` (que usa `SubtleCrypto.decrypt`) para desencriptar
     *         el texto cifrado con la clave de cifrado y el IV.
     *
     * @param encryptedValue El token Fernet completo (sin el prefijo `ENC()`) codificado en Base64URL.
     * @return El valor original desencriptado como String.
     * @throws IllegalArgumentException Si el token es inválido (longitud, versión).
     * @throws IllegalStateException Si el HMAC no coincide (token corrupto o manipulado) o si la desencriptación falla.
     */
    actual override suspend fun decrypt(encryptedValue: String): String {
        val tokenBytes = base64UrlDecode(encryptedValue)

        // Estructura Fernet: version(1) + timestamp(8) + iv(16) + ciphertext + hmac(32)
        require(tokenBytes.size >= 57) { "Token Fernet inválido (muy corto)" } // 1 (version) + 8 (timestamp) + 16 (iv) + 32 (hmac) = 57 bytes mínimos para un token vacío
        require(tokenBytes[0] == 0x80.toByte()) { "Versión Fernet no soportada: ${tokenBytes[0]}" }

        // Extraer componentes para la verificación HMAC
        val hmacProvided = tokenBytes.copyOfRange(tokenBytes.size - 32, tokenBytes.size)
        val dataToVerify = tokenBytes.copyOfRange(0, tokenBytes.size - 32)

        // Calcular HMAC usando la Web Crypto API
        val hmacCalculated = computeHmacSha256(dataToVerify, signingKey)

        require(hmacProvided.contentEquals(hmacCalculated)) { "HMAC inválido - token corrupto o manipulado" }

        // Extraer IV y ciphertext
        val iv = tokenBytes.copyOfRange(9, 25) // IV es de 16 bytes, después de la versión y el timestamp
        val ciphertext = tokenBytes.copyOfRange(25, tokenBytes.size - 32) // Ciphertext está entre IV y HMAC

        // Desencriptar usando la Web Crypto API
        return decryptAesCbc(ciphertext, encryptionKey, iv)
    }

    /**
     * ✍️ Calcula el HMAC-SHA256 de unos datos con una clave dada, utilizando la Web Crypto API.
     *
     * @param data Los datos a firmar.
     * @param key La clave secreta para el HMAC.
     * @return El HMAC calculado como un `ByteArray`.
     */
    private suspend fun computeHmacSha256(data: ByteArray, key: ByteArray): ByteArray {
        val subtle = crypto.subtle

        val keyResult: CryptoKey = subtle.importKey(
            "raw", key.toUint8Array(), jsHmacAlgorithm, false, jsHmacSign
        ).await()

        val signatureBuffer: ArrayBuffer = subtle.sign(
            jsHmacSignAlgorithm, keyResult, data.toUint8Array()
        ).await()

        return signatureBuffer.toByteArray()
    }

    /**
     * 🔓 Desencripta datos utilizando AES-CBC con la Web Crypto API.
     *
     * @param ciphertext Los datos cifrados.
     * @param key La clave AES para la desencriptación.
     * @param iv El Vector de Inicialización (IV).
     * @return Los datos desencriptados como String.
     */
    private suspend fun decryptAesCbc(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): String {
        val subtle = crypto.subtle

        val keyResult: CryptoKey = subtle.importKey(
            "raw", key.toUint8Array(), jsAesKeyAlgorithm, false, jsAesDecryptUsage
        ).await()

        val decryptedBuffer: ArrayBuffer = subtle.decrypt(
            jsAesDecryptAlgorithm(iv), keyResult, ciphertext.toUint8Array()
        ).await()

        return decryptedBuffer.toByteArray().decodeToString()
    }
}