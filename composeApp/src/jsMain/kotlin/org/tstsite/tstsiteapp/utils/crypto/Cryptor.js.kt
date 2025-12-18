package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

// --- Declaraciones Externas para la Web Crypto API ---
/**
 * 🌐 Interfaz externa para acceder a la API de Criptografía Web del navegador.
 */
external val crypto: Crypto

/**
 * 🔑 Interfaz externa para el objeto `Crypto` de la Web Crypto API.
 * Proporciona acceso a operaciones criptográficas.
 */
external interface Crypto {
    /**
     * 🛠️ Acceso a las operaciones criptográficas sutiles (SubtleCrypto).
     */
    val subtle: SubtleCrypto
}

/**
 * ⚙️ Interfaz externa para el objeto `SubtleCrypto` de la Web Crypto API.
 * Contiene métodos para operaciones criptográficas de bajo nivel.
 */
external interface SubtleCrypto {
    /**
     * 📥 Importa una clave criptográfica.
     * @param format Formato de la clave (ej: "raw").
     * @param keyData Datos de la clave.
     * @param algorithm Algoritmo de la clave (ej: AES-CBC, HMAC).
     * @param extractable Indica si la clave puede ser exportada.
     * @param keyUsages Usos permitidos para la clave (ej: "sign", "decrypt").
     * @return Una Promesa que resuelve a un objeto `CryptoKey`.
     */
    fun importKey(
        format: String,
        keyData: Uint8Array,
        algorithm: dynamic, // Puede ser un objeto con propiedades como { name: 'HMAC', hash: 'SHA-256' }
        extractable: Boolean,
        keyUsages: Array<String>
    ): Promise<CryptoKey>

    /**
     * ✍️ Firma datos con una clave HMAC.
     * @param algorithm Algoritmo de firma (ej: { name: 'HMAC' }).
     * @param key La clave `CryptoKey` a usar para firmar.
     * @param data Los datos a firmar.
     * @return Una Promesa que resuelve a un `ArrayBuffer` con la firma.
     */
    fun sign(
        algorithm: dynamic,
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
        algorithm: dynamic,
        key: CryptoKey,
        data: Uint8Array
    ): Promise<ArrayBuffer>
}

/**
 * 🔑 Interfaz externa para una clave criptográfica manejada por la Web Crypto API.
 */
external interface CryptoKey

// --- Implementación JavaScript de Cryptor ---
/**
 * 🌐 Implementación JavaScript de [Cryptor] para el cifrado Fernet.
 *
 * Esta clase proporciona la lógica específica de JavaScript para desencriptar tokens Fernet,
 * utilizando la [Web Crypto API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Crypto_API)
 * del navegador.
 *
 * @property fernetKey La clave Fernet de 32 bytes (16 para firma, 16 para cifrado) codificada en Base64URL.
 */
@OptIn(ExperimentalEncodingApi::class)
actual class Cryptor actual constructor(fernetKey: String) : BaseCryptor(fernetKey) {

    private val signingKey: ByteArray
    private val encryptionKey: ByteArray

    init {
        val keyBytes = base64UrlDecode(fernetKey)
        if (keyBytes.size != 32) {
            throw IllegalArgumentException("Fernet key debe ser de 32 bytes")
        }
        signingKey = keyBytes.copyOfRange(0, 16) // Los primeros 16 bytes para la firma HMAC
        encryptionKey = keyBytes.copyOfRange(16, 32) // Los siguientes 16 bytes para el cifrado AES
    }

    /**
     * 🔓 Desencripta un token Fernet en la plataforma JavaScript.
     *
     * Este método sigue el estándar Fernet y utiliza la Web Crypto API:
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
        if (tokenBytes.size < 57) { // 1 (version) + 8 (timestamp) + 16 (iv) + 32 (hmac) = 57 bytes mínimos para un token vacío
            throw IllegalArgumentException("Token Fernet inválido (muy corto)")
        }

        val version = tokenBytes[0]
        if (version != 0x80.toByte()) {
            throw IllegalArgumentException("Versión Fernet no soportada: $version")
        }

        // Extraer componentes para la verificación HMAC
        val hmacProvided = tokenBytes.copyOfRange(tokenBytes.size - 32, tokenBytes.size)
        val dataToVerify = tokenBytes.copyOfRange(0, tokenBytes.size - 32)

        // Calcular HMAC usando la Web Crypto API
        val hmacCalculated = computeHmacSha256(dataToVerify, signingKey)

        if (!hmacProvided.contentEquals(hmacCalculated)) {
            throw IllegalStateException("HMAC inválido - token corrupto o manipulado")
        }

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

        val keyResult = subtle.importKey(
            "raw",
            key.toUint8Array(),
            js("({ name: 'HMAC', hash: 'SHA-256' })"), // Algoritmo HMAC con SHA-256
            false,
            arrayOf("sign") // Uso de la clave: firmar
        ).await()

        val signatureBuffer = subtle.sign(
            js("({ name: 'HMAC' })"), // CORREGIDO: Solo el nombre del algoritmo
            keyResult,
            data.toUint8Array()
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

        val keyResult = subtle.importKey(
            "raw",
            key.toUint8Array(),
            js("({ name: 'AES-CBC' })"), // Algoritmo AES-CBC
            false,
            arrayOf("decrypt") // Uso de la clave: desencriptar
        ).await()

        val ivArray = iv.toUint8Array()
        val decryptedBuffer = subtle.decrypt(
            js("({ name: 'AES-CBC', iv: ivArray })"), // Algoritmo AES-CBC con el IV
            keyResult,
            ciphertext.toUint8Array()
        ).await()

        return decryptedBuffer.toByteArray().decodeToString()
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
     * ⬅️ Convierte un `ArrayBuffer` de JavaScript a un `ByteArray` de Kotlin.
     */
    private fun ArrayBuffer.toByteArray(): ByteArray {
        val uint8Array = Uint8Array(this)
        val byteArray = ByteArray(uint8Array.length)
        for (i in 0 until uint8Array.length) {
            byteArray[i] = uint8Array.asDynamic()[i].unsafeCast<Int>().toByte()
        }
        return byteArray
    }
}