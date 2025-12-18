package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import org.tstsite.tstsiteapp.config.AppConfig

/**
 * 🚀 Sistema de Cifrado de Payloads para la API.
 *
 * Este objeto se encarga de cifrar y descifrar los cuerpos (payloads) de las
 * peticiones y respuestas de la API, garantizando una comunicación segura.
 *
 * ---
 * ### ✨ Características Principales:
 * - **Algoritmo:** AES-256-GCM, un estándar robusto para el cifrado autenticado.
 * - **Derivación de Clave:** Utiliza PBKDF2 para generar una clave segura a partir de un secreto.
 * - **Compatibilidad:** Diseñado para ser 100% compatible con la implementación del backend en Node.js.
 * - **Configurable:** Se puede activar o desactivar globalmente a través de `AppConfig.ENCRYPTION_ENABLED`.
 * ---
 */

/**
 * 📦 Contenedor para un payload cifrado.
 *
 * Este es el formato que se envía y recibe de la API cuando el cifrado está activo.
 *
 * @property encryptedPayload El contenido cifrado, codificado en Base64.
 */
@Serializable
data class EncryptedPayload(
    val encryptedPayload: String
)

/**
 * 🛡️ Interfaz `expect` para las operaciones de cifrado de payloads.
 *
 * Define las funciones que cada plataforma (`android`, `jvm`, `js`, etc.) debe implementar
 * con su lógica específica para el cifrado y descifrado.
 */
expect object PayloadCrypto {
    /**
     * 🔒 Cifra un objeto de datos serializable.
     *
     * @param serializer El serializador de Kotlinx para el tipo de objeto [T].
     * @param data El objeto de datos a cifrar.
     * @param secretKey La clave secreta utilizada para la derivación de la clave de cifrado.
     * @return Una cadena en formato Base64 que representa el payload cifrado.
     */
    suspend fun <T> encrypt(serializer: KSerializer<T>, data: T, secretKey: String): String

    /**
     * 🔓 Descifra un payload y lo convierte de nuevo a un objeto de datos.
     *
     * @param deserializer El deserializador de Kotlinx para el tipo de objeto de destino [T].
     * @param encryptedData La cadena en formato Base64 que contiene los datos cifrados.
     * @param secretKey La clave secreta utilizada para la derivación de la clave de descifrado.
     * @return El objeto de datos [T] original.
     */
    suspend fun <T> decrypt(deserializer: KSerializer<T>, encryptedData: String, secretKey: String): T

    /**
     * 🎲 Genera un array de bytes aleatorios y criptográficamente seguros.
     *
     * Esencial para crear el "salt" en PBKDF2 y el "IV" (vector de inicialización) en AES.
     *
     * @param length El número de bytes aleatorios a generar.
     * @return Un [ByteArray] con los bytes generados.
     */
    fun randomBytes(length: Int): ByteArray

    /**
     * 🔑 Deriva una clave de cifrado a partir de una contraseña y un "salt" usando PBKDF2.
     *
     * Este proceso añade una capa de seguridad, haciendo que la clave final no sea directamente
     * la contraseña original.
     *
     * @param password La contraseña o secreto original.
     * @param salt Un conjunto de bytes aleatorios para asegurar que la derivación sea única.
     * @return La clave derivada, lista para ser usada en el algoritmo de cifrado.
     */
    suspend fun deriveKey(password: String, salt: ByteArray): ByteArray
}

/**
 * 🛠️ Objeto de ayuda para gestionar la configuración del cifrado.
 *
 * Centraliza el acceso a la configuración de cifrado definida en [AppConfig].
 */
object CryptoHelper {
    /**
     * 🤔 Comprueba si el cifrado de payloads está habilitado en la configuración.
     *
     * @return `true` si `AppConfig.ENCRYPTION_ENABLED` es verdadero.
     */
    fun isEncryptionEnabled(): Boolean = AppConfig.ENCRYPTION_ENABLED

    /**
     * 🤫 Obtiene la clave secreta principal desde la configuración de la app.
     *
     * @return La clave secreta que se usará para derivar las claves de cifrado.
     */
    suspend fun getSecretKey(): String = AppConfig.getKey()
}