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
import kotlin.js.unsafeCast

// External declarations for TextEncoder/Decoder
external class TextEncoder {
    fun encode(input: String): Uint8Array
}

external class TextDecoder {
    fun decode(input: JsAny): String
}

/**
 * Constantes de encriptación
 */
private const val IV_LENGTH = 12
private const val SALT_LENGTH = 32
private const val KEY_LENGTH = 32
private const val PBKDF2_ITERATIONS = 100000
private const val AUTH_TAG_LENGTH_BITS = 128 // 16 bytes * 8 bits/byte

// =============================
// Helpers JS top-level para algoritmos
// =============================

private val jsPbkdf2ImportParams: JsAny = js("({ name: 'PBKDF2' })")
private val jsAesGcmImportParams: JsAny = js("({ name: 'AES-GCM' })")
private val jsDeriveBitsUsage: JsArray<JsString> = js("(['deriveBits'])")
private val jsEncryptDecryptUsage: JsArray<JsString> = js("(['encrypt', 'decrypt'])")
private val jsEncryptUsage: JsArray<JsString> = js("(['encrypt'])")
private val jsDecryptUsage: JsArray<JsString> = js("(['decrypt'])")

private fun createPbkdf2Params(salt: Uint8Array): JsAny =
    js("({ name: 'PBKDF2', salt: salt, iterations: 100000, hash: 'SHA-256' })")

private fun createAesGcmParams(iv: Uint8Array): JsAny =
    js("({ name: 'AES-GCM', iv: iv, tagLength: 128 })")


@OptIn(ExperimentalEncodingApi::class)
@Suppress("unused", "EXPERIMENTAL_EXPECT_ACTUAL_CLASSES")
actual object PayloadCrypto {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    actual suspend fun <T> encrypt(serializer: KSerializer<T>, data: T, secretKey: String): String {
        val jsonString = json.encodeToString(serializer, data)
        val plaintext = TextEncoder().encode(jsonString).buffer.toByteArray()

        val salt = randomBytes(SALT_LENGTH)
        val iv = randomBytes(IV_LENGTH)

        val key = deriveKey(secretKey.encodeToByteArray(), salt)

        val ciphertext = encryptAESGCM(plaintext, key, iv)

        val combined = salt + iv + ciphertext

        return Base64.encode(combined)
    }

    actual suspend fun <T> decrypt(deserializer: KSerializer<T>, encryptedData: String, secretKey: String): T {
        val buffer = Base64.decode(encryptedData)

        val salt = buffer.sliceArray(0 until SALT_LENGTH)
        val iv = buffer.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val ciphertext = buffer.sliceArray(SALT_LENGTH + IV_LENGTH until buffer.size)

        val key = deriveKey(secretKey.encodeToByteArray(), salt)

        val plaintext = decryptAESGCM(ciphertext, key, iv)
        val jsonString = TextDecoder().decode(plaintext.toUint8Array())

        return json.decodeFromString(deserializer, jsonString)
    }

    actual fun randomBytes(length: Int): ByteArray {
        val array = Uint8Array(ArrayBuffer(length))
        crypto.getRandomValues(array)
        return array.buffer.toByteArray()
    }

    actual suspend fun deriveKey(password: ByteArray, salt: ByteArray): ByteArray {
        val subtle = crypto.subtle

        val passwordKey: CryptoKey = subtle.importKey(
            "raw",
            password.toUint8Array(),
            jsPbkdf2ImportParams,
            false,
            jsDeriveBitsUsage
        ).await()

        val derivedBits: ArrayBuffer = subtle.deriveBits(
            createPbkdf2Params(salt.toUint8Array()),
            passwordKey,
            KEY_LENGTH * 8 // length in bits
        ).await()

        return derivedBits.toByteArray()
    }

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
