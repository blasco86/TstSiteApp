package org.tstsite.tstsiteapp.utils.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Implementación de Fernet para Kotlin Multiplatform
 * Compatible con la encriptación usada en Node.js (fernet npm package)
 */
@OptIn(ExperimentalEncodingApi::class)
expect class Cryptor(fernetKey: String) {
    /**
     * Desencripta un valor Fernet
     * @param encryptedValue Token Fernet completo (sin prefijo ENC())
     * @return Valor desencriptado o el original si no es válido
     */
    suspend fun decrypt(encryptedValue: String): String

    /**
     * Verifica si un valor está encriptado
     * @param value Valor a verificar (puede incluir ENC())
     */
    fun isEncrypted(value: String): Boolean

    /**
     * Desencripta un valor que puede tener el formato ENC(token)
     * @param value Valor en formato ENC(token) o texto plano
     */
    suspend fun decryptValue(value: String): String
}

/**
 * Implementación común de utilidades
 */
@OptIn(ExperimentalEncodingApi::class)
abstract class BaseCryptor(protected val fernetKey: String) {

    fun isEncrypted(value: String): Boolean {
        return value.startsWith("ENC(") && value.endsWith(")")
    }

    suspend fun decryptValue(value: String): String {
        if (!isEncrypted(value)) return value

        return try {
            val token = value.substring(4, value.length - 1)
            decrypt(token)
        } catch (e: Exception) {
            value // Retorna el valor original si falla
        }
    }

    protected fun base64UrlDecode(input: String): ByteArray {
        val base64 = input.replace('-', '+').replace('_', '/')
        return Base64.decode(base64)
    }

    protected fun base64UrlEncode(input: ByteArray): String {
        return Base64.encode(input)
            .replace('+', '-')
            .replace('/', '_')
            .trimEnd('=')
    }

    abstract suspend fun decrypt(encryptedValue: String): String
}