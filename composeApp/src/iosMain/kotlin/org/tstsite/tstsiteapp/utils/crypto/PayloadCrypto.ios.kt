package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.cinterop.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import platform.CoreCrypto.*
import platform.posix.arc4random_buf
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val IV_LENGTH = 12
private const val SALT_LENGTH = 32
private const val KEY_LENGTH = 32
private const val PBKDF2_ITERATIONS = 100_000

@OptIn(ExperimentalEncodingApi::class, ExperimentalForeignApi::class)
actual object PayloadCrypto {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    actual suspend fun <T> encrypt(serializer: KSerializer<T>, data: T, secretKey: String): String {
        val plaintext = json.encodeToString(serializer, data).encodeToByteArray()
        val salt = randomBytes(SALT_LENGTH)
        val iv = randomBytes(IV_LENGTH)
        val key = deriveKey(secretKey.encodeToByteArray(), salt)
        val ciphertext = aesCrypt(plaintext, key, iv, kCCEncrypt)
        return Base64.encode(salt + iv + ciphertext)
    }

    actual suspend fun <T> decrypt(deserializer: KSerializer<T>, encryptedData: String, secretKey: String): T {
        val buffer = Base64.decode(encryptedData)
        val salt = buffer.sliceArray(0 until SALT_LENGTH)
        val iv = buffer.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val ciphertext = buffer.sliceArray(SALT_LENGTH + IV_LENGTH until buffer.size)
        val key = deriveKey(secretKey.encodeToByteArray(), salt)
        val plaintext = aesCrypt(ciphertext, key, iv, kCCDecrypt)
        return json.decodeFromString(deserializer, plaintext.decodeToString())
    }

    actual fun randomBytes(length: Int): ByteArray {
        val byteArray = ByteArray(length)
        if (length > 0) {
            byteArray.usePinned { pinned ->
                arc4random_buf(pinned.addressOf(0).reinterpret<UByteVar>(), length.toULong())
            }
        }
        return byteArray
    }

    actual suspend fun deriveKey(password: ByteArray, salt: ByteArray): ByteArray {
        val derivedKey = ByteArray(KEY_LENGTH)
        // No need for password.usePinned here, as we're passing a String
        salt.usePinned { pinnedSalt ->
            derivedKey.usePinned { pinnedDerivedKey ->
                val status = CCKeyDerivationPBKDF(
                    kCCPBKDF2,
                    password.decodeToString(), // Pass String directly
                    password.size.toULong(),
                    pinnedSalt.addressOf(0).reinterpret<UByteVar>(),
                    salt.size.toULong(),
                    kCCPRFHmacAlgSHA256,
                    PBKDF2_ITERATIONS.toUInt(),
                    pinnedDerivedKey.addressOf(0).reinterpret<UByteVar>(),
                    KEY_LENGTH.toULong()
                )
                if (status != kCCSuccess) throw IllegalStateException("Key derivation failed: $status")
            }
        }
        return derivedKey
    }

    private fun aesCrypt(data: ByteArray, key: ByteArray, iv: ByteArray, operation: CCOperation): ByteArray = memScoped {
        val numBytes = alloc<ULongVar>()
        val bufferSize = data.size + kCCBlockSizeAES128.toInt()
        val buffer = allocArray<UByteVar>(bufferSize)

        val status = key.usePinned { pinnedKey ->
            iv.usePinned { pinnedIv ->
                data.usePinned { pinnedData ->
                    CCCrypt(
                        operation,
                        kCCAlgorithmAES,
                        kCCOptionPKCS7Padding,
                        pinnedKey.addressOf(0).reinterpret<UByteVar>(),
                        key.size.toULong(),
                        pinnedIv.addressOf(0).reinterpret<UByteVar>(),
                        pinnedData.addressOf(0).reinterpret<UByteVar>(),
                        data.size.toULong(),
                        buffer,
                        bufferSize.toULong(),
                        numBytes.ptr
                    )
                }
            }
        }

        if (status != kCCSuccess) throw IllegalStateException("AES operation failed: $status")
        buffer.readBytes(numBytes.value.toInt())
    }
}
