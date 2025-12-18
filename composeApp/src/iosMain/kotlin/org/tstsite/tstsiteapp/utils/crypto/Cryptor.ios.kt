package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.*
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 🍎 Implementación iOS de [Cryptor] para el cifrado Fernet.
 *
 * Esta clase proporciona la lógica específica de iOS para desencriptar tokens Fernet,
 * utilizando las APIs criptográficas de `CoreCrypto` de Apple.
 *
 * @property fernetKey La clave Fernet de 32 bytes (16 para firma, 16 para cifrado) codificada en Base64URL.
 */
@OptIn(ExperimentalEncodingApi::class, ExperimentalForeignApi::class)
actual class Cryptor actual constructor(fernetKey: String) : BaseCryptor(fernetKey) {

    private val signingKey: ByteArray
    private val encryptionKey: ByteArray

    init {
        val keyBytes = base64UrlDecode(fernetKey)
        if (keyBytes.size != 32) {
            throw IllegalArgumentException("Fernet key debe ser de 32 bytes")
        }
        signingKey = keyBytes.copyOfRange(0, 16) // Los primeros 16 bytes para la firma
        encryptionKey = keyBytes.copyOfRange(16, 32) // Los siguientes 16 bytes para el cifrado
    }

    /**
     * 🔓 Desencripta un token Fernet en la plataforma iOS.
     *
     * Este método sigue el estándar Fernet y utiliza `CoreCrypto` para las operaciones:
     * 1.  **Decodificación:** El `encryptedValue` (token Fernet) se decodifica de Base64URL.
     * 2.  **Validación de Longitud y Versión:** Se verifica que el token tenga una longitud mínima
     *     y que la versión Fernet sea la soportada (0x80).
     * 3.  **Verificación HMAC:**
     *     *   Se extrae el HMAC proporcionado al final del token.
     *     *   Se calcula el HMAC de los datos restantes (versión, timestamp, IV, ciphertext)
     *         utilizando `CCHmac` con `kCCHmacAlgSHA256` y la clave de firma (`signingKey`).
     *     *   Se compara el HMAC calculado con el proporcionado para detectar manipulaciones.
     * 4.  **Extracción de IV y Ciphertext:** Se obtienen el Vector de Inicialización (IV) y el texto cifrado.
     * 5.  **Desencriptación AES:**
     *     *   Se utiliza `CCCrypt` con `kCCDecrypt`, `kCCAlgorithmAES` y `kCCOptionPKCS7Padding`.
     *     *   La clave de cifrado (`encryptionKey`) y el IV se usan para inicializar el algoritmo.
     *     *   El texto cifrado se desencripta para obtener el texto plano original.
     *
     * @param encryptedValue El token Fernet completo (sin el prefijo `ENC()`) codificado en Base64URL.
     * @return El valor original desencriptado como String.
     * @throws IllegalArgumentException Si el token es inválido (longitud, versión) o la clave Fernet es incorrecta.
     * @throws IllegalStateException Si el HMAC no coincide (token corrupto o manipulado) o si `CCCrypt` falla.
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

        // Calcular HMAC con CoreCrypto
        val hmacCalculated = memScoped {
            val result = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)
            CCHmac(
                kCCHmacAlgSHA256,
                signingKey.refTo(0),
                signingKey.size.toULong(),
                dataToVerify.refTo(0),
                dataToVerify.size.toULong(),
                result
            )
            ByteArray(CC_SHA256_DIGEST_LENGTH) { result[it].toByte() }
        }

        if (!hmacProvided.contentEquals(hmacCalculated)) {
            throw IllegalStateException("HMAC inválido - token corrupto o manipulado")
        }

        // Extraer IV y ciphertext
        val iv = tokenBytes.copyOfRange(9, 25) // IV es de 16 bytes, después de la versión y el timestamp
        val ciphertext = tokenBytes.copyOfRange(25, tokenBytes.size - 32) // Ciphertext está entre IV y HMAC

        // Desencriptar con CoreCrypto
        return memScoped {
            val bufferSize = ciphertext.size + kCCBlockSizeAES128.toInt() // Tamaño suficiente para el texto plano + padding
            val decrypted = allocArray<UByteVar>(bufferSize)
            val numBytesDecrypted = alloc<ULongVar>()

            val status = CCCrypt(
                kCCDecrypt,
                kCCAlgorithmAES,
                kCCOptionPKCS7Padding, // Opciones de padding
                encryptionKey.refTo(0),
                kCCKeySizeAES128.toULong(), // Tamaño de la clave AES (128 bits = 16 bytes)
                iv.refTo(0),
                ciphertext.refTo(0),
                ciphertext.size.toULong(),
                decrypted,
                bufferSize.toULong(),
                numBytesDecrypted.ptr
            )

            if (status != kCCSuccess) {
                throw IllegalStateException("Error desencriptando con CCCrypt: $status")
            }

            ByteArray(numBytesDecrypted.value.toInt()) { decrypted[it].toByte() }
                .decodeToString()
        }
    }
}