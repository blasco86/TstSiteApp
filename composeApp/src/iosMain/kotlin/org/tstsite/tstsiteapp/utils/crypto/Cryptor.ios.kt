package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.*
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class, ExperimentalForeignApi::class)
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

        if (tokenBytes.size < 57) {
            throw IllegalArgumentException("Token Fernet inválido")
        }

        val version = tokenBytes[0]
        if (version != 0x80.toByte()) {
            throw IllegalArgumentException("Versión Fernet no soportada")
        }

        val hmacProvided = tokenBytes.copyOfRange(tokenBytes.size - 32, tokenBytes.size)
        val dataToVerify = tokenBytes.copyOfRange(0, tokenBytes.size - 32)

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
            throw IllegalStateException("HMAC inválido")
        }

        val iv = tokenBytes.copyOfRange(9, 25)
        val ciphertext = tokenBytes.copyOfRange(25, tokenBytes.size - 32)

        return memScoped {
            val bufferSize = ciphertext.size + kCCBlockSizeAES128.toInt()
            val decrypted = allocArray<UByteVar>(bufferSize)
            val numBytesDecrypted = alloc<ULongVar>()

            val status = CCCrypt(
                kCCDecrypt,
                kCCAlgorithmAES,
                kCCOptionPKCS7Padding,
                encryptionKey.refTo(0),
                kCCKeySizeAES128.toULong(),
                iv.refTo(0),
                ciphertext.refTo(0),
                ciphertext.size.toULong(),
                decrypted,
                bufferSize.toULong(),
                numBytesDecrypted.ptr
            )

            if (status != kCCSuccess) {
                throw IllegalStateException("Error desencriptando: $status")
            }

            ByteArray(numBytesDecrypted.value.toInt()) { decrypted[it].toByte() }
                .decodeToString()
        }
    }
}