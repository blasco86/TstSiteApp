package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.cinterop.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import platform.CoreCrypto.*
import platform.posix.arc4random_buf
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val IV_LENGTH = 16 // AES-CBC usa un IV de 16 bytes
private const val SALT_LENGTH = 32
private const val KEY_LENGTH = 32 // Para HMAC-SHA256, la salida es de 32 bytes

/**
 * 🍎 Implementación iOS de [PayloadCrypto] para el cifrado de payloads.
 *
 * ⚠️ **ADVERTENCIA:** Esta implementación utiliza AES-CBC debido a limitaciones en las APIs de CoreCrypto
 * disponibles en el target de iOS del proyecto. El resto de plataformas (Android, JVM) usan AES-GCM.
 * Para una compatibilidad completa, el `iosDeploymentTarget` del proyecto debería ser 10.3 o superior
 * para poder usar las APIs de AES-GCM.
 */
@OptIn(ExperimentalEncodingApi::class, ExperimentalForeignApi::class)
actual object PayloadCrypto {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 🔒 Cifra datos serializables en la plataforma iOS usando AES-CBC.
     */
    actual suspend fun <T> encrypt(serializer: KSerializer<T>, data: T, secretKey: String): String {
        val plaintext = json.encodeToString(serializer, data).encodeToByteArray()
        val salt = randomBytes(SALT_LENGTH)
        val iv = randomBytes(IV_LENGTH)
        val key = deriveKey(secretKey, salt)
        val ciphertext = aesCrypt(plaintext, key, iv, kCCEncrypt)
        // El formato para CBC será diferente al de GCM, pero mantenemos la estructura.
        return Base64.encode(salt + iv + ciphertext)
    }

    /**
     * 🔓 Descifra un payload cifrado en la plataforma iOS usando AES-CBC.
     */
    actual suspend fun <T> decrypt(deserializer: KSerializer<T>, encryptedData: String, secretKey: String): T {
        val buffer = Base64.decode(encryptedData)
        val salt = buffer.sliceArray(0 until SALT_LENGTH)
        val iv = buffer.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val ciphertext = buffer.sliceArray(SALT_LENGTH + IV_LENGTH until buffer.size)
        val key = deriveKey(secretKey, salt)
        val plaintext = aesCrypt(ciphertext, key, iv, kCCDecrypt)
        return json.decodeFromString(deserializer, plaintext.decodeToString())
    }

    /**
     * 🎲 Genera un array de bytes aleatorios criptográficamente seguros en iOS.
     */
    actual fun randomBytes(length: Int): ByteArray {
        val byteArray = ByteArray(length)
        if (length > 0) {
            byteArray.usePinned { pinned ->
                arc4random_buf(pinned.addressOf(0).reinterpret<UByteVar>(), length.toULong())
            }
        }
        return byteArray
    }

    /**
     * 🔑 Deriva una clave a partir de una contraseña y un salt usando HMAC-SHA256 en iOS.
     *
     * @param password La contraseña o secreto original como [String].
     * @param salt Un conjunto de bytes aleatorios.
     * @return La clave derivada como [ByteArray].
     */
    actual suspend fun deriveKey(password: String, salt: ByteArray): ByteArray = memScoped {
        val hmacAlgorithm = kCCHmacAlgSHA256
        val derivedKey = ByteArray(KEY_LENGTH)
        val passwordBytes = password.encodeToByteArray()

        passwordBytes.usePinned { pinnedPassword ->
            salt.usePinned { pinnedSalt ->
                derivedKey.usePinned { pinnedDerivedKey ->
                    CCHmac(
                        hmacAlgorithm,
                        pinnedPassword.addressOf(0),
                        passwordBytes.size.toULong(),
                        pinnedSalt.addressOf(0),
                        salt.size.toULong(),
                        pinnedDerivedKey.addressOf(0)
                    )
                }
            }
        }
        return derivedKey
    }

    /**
     * 🔐 Función auxiliar para realizar operaciones AES-CBC (cifrado/descifrado) en iOS.
     */
    private fun aesCrypt(data: ByteArray, key: ByteArray, iv: ByteArray, operation: CCOperation): ByteArray = memScoped {
        val numBytes = alloc<ULongVar>()
        // El buffer debe ser suficiente para el texto + padding
        val bufferSize = data.size + kCCBlockSizeAES128.toInt()
        val buffer = allocArray<UByteVar>(bufferSize)

        val status = key.usePinned { pinnedKey ->
            iv.usePinned { pinnedIv ->
                data.usePinned { pinnedData ->
                    CCCrypt(
                        operation,
                        kCCAlgorithmAES,
                        kCCOptionPKCS7Padding, // Usando PKCS7Padding para CBC
                        pinnedKey.addressOf(0),
                        key.size.toULong(),
                        pinnedIv.addressOf(0),
                        pinnedData.addressOf(0),
                        data.size.toULong(),
                        buffer,
                        bufferSize.toULong(),
                        numBytes.ptr
                    )
                }
            }
        }

        if (status != kCCSuccess) throw IllegalStateException("AES-CBC operation failed: $status")
        return buffer.readBytes(numBytes.value.toInt())
    }
}