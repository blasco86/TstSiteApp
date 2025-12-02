package org.tstsite.tstsiteapp.utils.crypto

import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.ExperimentalEncodingApi

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

    actual override suspend fun decrypt(encryptedValue: String): String {
        val tokenBytes = base64UrlDecode(encryptedValue)

        // Estructura Fernet: version(1) + timestamp(8) + iv(16) + ciphertext + hmac(32)
        if (tokenBytes.size < 57) {
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
        val iv = tokenBytes.copyOfRange(9, 25)
        val ciphertext = tokenBytes.copyOfRange(25, tokenBytes.size - 32)

        // Desencriptar
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), IvParameterSpec(iv))
        val decrypted = cipher.doFinal(ciphertext)

        return decrypted.decodeToString()
    }
}