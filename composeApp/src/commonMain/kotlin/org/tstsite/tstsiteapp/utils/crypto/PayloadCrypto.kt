package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import org.tstsite.tstsiteapp.config.AppConfig

/**
 * 🔐 Sistema de encriptación de payloads para comunicación segura con la API
 *
 * Características:
 * - AES-256-GCM para encriptación
 * - PBKDF2 para derivación de claves
 * - Compatible con la implementación Node.js de la API
 * - Activación/desactivación mediante AppConfig.ENCRYPTION_ENABLED
 */

/**
 * Wrapper para payloads encriptados
 */
@Serializable
data class EncryptedPayload(
    val encryptedPayload: String
)

/**
 * Interfaz común para operaciones de encriptación
 * Implementación específica por plataforma
 */
expect object PayloadCrypto {
    /**
     * Encripta datos serializables
     * @param serializer Serializador para el tipo T
     * @param data Datos a encriptar
     * @param secretKey Clave secreta para encriptación
     * @return String Base64 con datos encriptados
     */
    suspend fun <T> encrypt(serializer: KSerializer<T>, data: T, secretKey: String): String

    /**
     * Desencripta datos
     * @param deserializer Deserializador para el tipo T
     * @param encryptedData String Base64 con datos encriptados
     * @param secretKey Clave secreta para desencriptación
     * @return Datos originales deserializados
     */
    suspend fun <T> decrypt(deserializer: KSerializer<T>, encryptedData: String, secretKey: String): T

    /**
     * Genera bytes aleatorios criptográficamente seguros
     */
    fun randomBytes(length: Int): ByteArray

    /**
     * Deriva una clave a partir de una contraseña usando PBKDF2
     */
    suspend fun deriveKey(password: ByteArray, salt: ByteArray): ByteArray
}

/**
 * 🔧 Helper para determinar si usar encriptación
 */
object CryptoHelper {
    /**
     * Verifica si la encriptación está habilitada
     */
    fun isEncryptionEnabled(): Boolean = AppConfig.ENCRYPTION_ENABLED

    /**
     * Obtiene la clave secreta desde AppConfig
     */
    suspend fun getSecretKey(): String = AppConfig.getApiKey()
}
