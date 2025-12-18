package org.tstsite.tstsiteapp.utils.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 🔐 Gestor de Cifrado Fernet para Kotlin Multiplatform.
 *
 * Esta clase proporciona una implementación compatible con la librería `fernet` de Node.js,
 * permitiendo desencriptar valores que han sido cifrados en el backend.
 *
 * Se utiliza principalmente para proteger secretos dentro de la propia aplicación, como la API Key.
 *
 * @property fernetKey La clave Fernet utilizada para la desencriptación.
 */
@OptIn(ExperimentalEncodingApi::class)
expect class Cryptor(fernetKey: String) {
    /**
     * 🔓 Desencripta un token Fernet.
     *
     * @param encryptedValue El token Fernet completo (sin el prefijo `ENC()`).
     * @return El valor original desencriptado. Si la desencriptación falla, puede lanzar una excepción.
     */
    suspend fun decrypt(encryptedValue: String): String

    /**
     * 🤔 Verifica si un valor tiene el formato de encriptación `ENC(...)`.
     *
     * @param value El valor a comprobar.
     * @return `true` si el valor está envuelto en `ENC()`, `false` en caso contrario.
     */
    fun isEncrypted(value: String): Boolean

    /**
     * 🔓 Desencripta de forma segura un valor que puede estar o no encriptado.
     *
     * Si el valor tiene el formato `ENC(token)`, lo desencripta.
     * Si no, o si la desencriptación falla, devuelve el valor original.
     *
     * @param value El valor a desencriptar (ej: "texto_plano" o "ENC(token_cifrado)").
     * @return El valor desencriptado o el original si no procede.
     */
    suspend fun decryptValue(value: String): String
}

/**
 * 뼈 Clase base abstracta para [Cryptor] que contiene la lógica común.
 *
 * Implementa las funciones de utilidad `isEncrypted` y `decryptValue`,
 * así como los métodos para codificar y decodificar en Base64URL.
 *
 * @property fernetKey La clave Fernet.
 */
@OptIn(ExperimentalEncodingApi::class)
abstract class BaseCryptor(protected val fernetKey: String) {

    /**
     * 🤔 Verifica si un valor tiene el formato de encriptación `ENC(...)`.
     */
    fun isEncrypted(value: String): Boolean {
        return value.startsWith("ENC(") && value.endsWith(")")
    }

    /**
     * 🔓 Desencripta de forma segura un valor que puede estar o no encriptado.
     */
    suspend fun decryptValue(value: String): String {
        if (!isEncrypted(value)) return value

        return try {
            val token = value.substring(4, value.length - 1)
            decrypt(token)
        } catch (e: Exception) {
            println("⚠️ Error al desencriptar valor: ${e.message}")
            value // Retorna el valor original si falla para evitar crashes
        }
    }

    /**
     * ➡️ Decodifica una cadena en formato Base64URL a un array de bytes.
     */
    protected fun base64UrlDecode(input: String): ByteArray {
        return Base64.UrlSafe.decode(input)
    }

    /**
     * ⬅️ Codifica un array de bytes a una cadena en formato Base64URL.
     */
    protected fun base64UrlEncode(input: ByteArray): String {
        return Base64.UrlSafe.encode(input)
    }

    /**
     * 🔓 Función abstracta que debe ser implementada por cada plataforma.
     * Contiene la lógica específica de desencriptación Fernet.
     */
    abstract suspend fun decrypt(encryptedValue: String): String
}