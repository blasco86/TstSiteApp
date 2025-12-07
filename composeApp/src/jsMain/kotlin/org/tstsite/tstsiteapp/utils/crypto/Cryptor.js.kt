package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

// Declaraciones externas para Web Crypto API
external val crypto: Crypto

external interface Crypto {
    val subtle: SubtleCrypto
}

external interface SubtleCrypto {
    fun importKey(
        format: String,
        keyData: Uint8Array,
        algorithm: dynamic,
        extractable: Boolean,
        keyUsages: Array<String>
    ): Promise<CryptoKey>

    fun sign(
        algorithm: dynamic,
        key: CryptoKey,
        data: Uint8Array
    ): Promise<ArrayBuffer>

    fun decrypt(
        algorithm: dynamic,
        key: CryptoKey,
        data: Uint8Array
    ): Promise<ArrayBuffer>
}

external interface CryptoKey

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

        if (tokenBytes.size < 57) {
            throw IllegalArgumentException("Token Fernet inválido")
        }

        val version = tokenBytes[0]
        if (version != 0x80.toByte()) {
            throw IllegalArgumentException("Versión Fernet no soportada")
        }

        val hmacProvided = tokenBytes.copyOfRange(tokenBytes.size - 32, tokenBytes.size)
        val dataToVerify = tokenBytes.copyOfRange(0, tokenBytes.size - 32)

        val hmacCalculated = computeHmacSha256(dataToVerify, signingKey)

        if (!hmacProvided.contentEquals(hmacCalculated)) {
            throw IllegalStateException("HMAC inválido - token corrupto o manipulado")
        }

        val iv = tokenBytes.copyOfRange(9, 25)
        val ciphertext = tokenBytes.copyOfRange(25, tokenBytes.size - 32)

        return decryptAesCbc(ciphertext, encryptionKey, iv)
    }

    private suspend fun computeHmacSha256(data: ByteArray, key: ByteArray): ByteArray {
        val subtle = crypto.subtle

        val keyResult = subtle.importKey(
            "raw",
            key.toUint8Array(),
            js("({ name: 'HMAC', hash: 'SHA-256' })"),
            false,
            arrayOf("sign")
        ).await()

        val signatureBuffer = subtle.sign(
            js("({ name: 'HMAC' })"),
            keyResult,
            data.toUint8Array()
        ).await()

        return signatureBuffer.toByteArray()
    }

    private suspend fun decryptAesCbc(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): String {
        val subtle = crypto.subtle

        val keyResult = subtle.importKey(
            "raw",
            key.toUint8Array(),
            js("({ name: 'AES-CBC' })"),
            false,
            arrayOf("decrypt")
        ).await()

        val ivArray = iv.toUint8Array()
        val decryptedBuffer = subtle.decrypt(
            js("({ name: 'AES-CBC', iv: ivArray })"),
            keyResult,
            ciphertext.toUint8Array()
        ).await()

        return decryptedBuffer.toByteArray().decodeToString()
    }

    // Helpers de conversión
    private fun ByteArray.toUint8Array(): Uint8Array {
        val uint8Array = Uint8Array(this.size)
        for (i in this.indices) {
            uint8Array.asDynamic()[i] = (this[i].toInt() and 0xFF)
        }
        return uint8Array
    }

    private fun ArrayBuffer.toByteArray(): ByteArray {
        val uint8Array = Uint8Array(this)
        val byteArray = ByteArray(uint8Array.length)
        for (i in 0 until uint8Array.length) {
            byteArray[i] = uint8Array.asDynamic()[i].unsafeCast<Int>().toByte()
        }
        return byteArray
    }
}