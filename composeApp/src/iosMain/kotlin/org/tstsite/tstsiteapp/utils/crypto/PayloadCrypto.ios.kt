package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.serialization.KSerializer

actual object PayloadCrypto {
    actual suspend fun <T> encrypt(serializer: KSerializer<T>, data: T, secretKey: String): String {
        TODO("Not yet implemented")
    }

    actual suspend fun <T> decrypt(deserializer: KSerializer<T>, encryptedData: String, secretKey: String): T {
        TODO("Not yet implemented")
    }

    actual fun randomBytes(length: Int): ByteArray {
        TODO("Not yet implemented")
    }

    actual suspend fun deriveKey(password: ByteArray, salt: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }
}
