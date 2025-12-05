@file:OptIn(ExperimentalWasmJsInterop::class)

package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.coroutines.await
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsString
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.unsafeCast

// =============================
// Interop JS / Web Crypto (Centralized Declarations)
// =============================

external class ArrayBuffer(byteLength: Int) : JsAny {
    val byteLength: Int
}

external class Uint8Array(buffer: ArrayBuffer) : JsAny {
    val length: Int
    val buffer: ArrayBuffer // Propiedad añadida
    operator fun get(index: Int): Byte
    operator fun set(index: Int, value: Byte)
}

external val crypto: Crypto

external interface Crypto : JsAny {
    val subtle: SubtleCrypto
    fun getRandomValues(array: Uint8Array)
}

external interface SubtleCrypto : JsAny {
    fun importKey(
        format: String,
        keyData: JsAny, // Uint8Array or ArrayBuffer
        algorithm: JsAny,
        extractable: Boolean,
        keyUsages: JsArray<JsString>
    ): Promise<CryptoKey>

    fun sign(
        algorithm: JsAny,
        key: CryptoKey,
        data: Uint8Array
    ): Promise<ArrayBuffer>

    fun decrypt(
        algorithm: JsAny,
        key: CryptoKey,
        data: Uint8Array
    ): Promise<ArrayBuffer>

    fun deriveBits(
        algorithm: JsAny,
        baseKey: CryptoKey,
        length: Int
    ): Promise<ArrayBuffer>

    fun encrypt(
        algorithm: JsAny,
        key: CryptoKey,
        data: JsAny // BufferSource
    ): Promise<ArrayBuffer>
}

external interface CryptoKey : JsAny

// =============================
// Helpers de conversión
// =============================

internal fun ArrayBuffer.toByteArray(): ByteArray {
    val arr = Uint8Array(this)
    val byteArray = ByteArray(arr.length)
    for (i in 0 until arr.length) byteArray[i] = arr[i]
    return byteArray
}

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

@OptIn(ExperimentalEncodingApi::class)
actual class Cryptor actual constructor(fernetKey: String) : BaseCryptor(fernetKey) {

    private val signingKey: ByteArray
    private val encryptionKey: ByteArray

    init {
        val keyBytes = base64UrlDecode(fernetKey)
        require(keyBytes.size == 32) { "Fernet key debe ser de 32 bytes" }
        signingKey = keyBytes.copyOfRange(0, 16)
        encryptionKey = keyBytes.copyOfRange(16, 32)
    }

    actual override suspend fun decrypt(encryptedValue: String): String {
        val token = base64UrlDecode(encryptedValue)
        require(token.size >= 57) { "Token Fernet inválido" }
        require(token[0] == 0x80.toByte()) { "Versión Fernet no soportada" }

        val hmacProvided = token.copyOfRange(token.size - 32, token.size)
        val dataToVerify = token.copyOfRange(0, token.size - 32)
        val hmacCalculated = computeHmacSha256(dataToVerify, signingKey)
        require(hmacProvided.contentEquals(hmacCalculated)) { "HMAC inválido - token corrupto o manipulado" }

        val iv = token.copyOfRange(9, 25)
        val ciphertext = token.copyOfRange(25, token.size - 32)

        return decryptAesCbc(ciphertext, encryptionKey, iv)
    }

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
