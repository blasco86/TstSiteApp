package org.tstsite.tstsiteapp.utils.crypto

import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 🤖 Implementación Android de [Cryptor] para el cifrado Fernet.
 *
 * Esta clase proporciona la lógica específica de Android para desencriptar tokens Fernet,
 * utilizando las APIs criptográficas de Java (Javax.crypto).
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
        signingKey = keyBytes.copyOfRange(0, 16)
        encryptionKey = keyBytes.copyOfRange(16, 32)
    }

    /**
     * 🔓 Desencripta un token Fernet en la plataforma Android.
     *
     * Este método sigue el estándar Fernet:
     * 1. Decodifica el token de Base64URL.
     * 2. Verifica la versión del token.
     * 3. Extrae el HMAC y los datos a verificar.
     * 4. Calcula el HMAC con la clave de firma y compara.
     * 5. Extrae el IV y el texto cifrado.
     * 6. Desencripta el texto cifrado usando AES/CBC/PKCS5Padding con la clave de cifrado y el IV.
     *
     * @param encryptedValue El token Fernet completo (sin el prefijo `ENC()`) codificado en Base64URL.
     * @return El valor original desencriptado como String.
     * @throws IllegalArgumentException Si el token es inválido o la clave Fernet es incorrecta.
     * @throws IllegalStateException Si el HMAC no coincide, indicando manipulación del token.
     * @throws Exception Si ocurre cualquier otro error durante la desencriptación.
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

        // Extraer componentes
        val hmacProvided = tokenBytes.copyOfRange(tokenBytes.size - 32, tokenBytes.size)
        val dataToVerify = tokenBytes.copyOfRange(0, tokenBytes.size - 32)

        // Verificar HMAC
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(signingKey, "HmacSHA256"))
        val hmacCalculated = mac.doFinal(dataToVerify)

        if (!hmacProvided.contentEquals(hmacCalculated)) {
            throw IllegalStateException("HMAC inválido - token corrupto o manipulado")
        }

        // Extraer IV y ciphertext
        val iv = tokenBytes.copyOfRange(9, 25) // IV es de 16 bytes, después de la versión y el timestamp
        val ciphertext = tokenBytes.copyOfRange(25, tokenBytes.size - 32) // Ciphertext está entre IV y HMAC

        // Desencriptar
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), IvParameterSpec(iv))
        val decrypted = cipher.doFinal(ciphertext)

        return decrypted.decodeToString()
    }
}