package org.tstsite.tstsiteapp.utils.crypto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 🔒 Constantes de configuración para el cifrado AES-256-GCM.
 */
private const val ALGORITHM = "AES/GCM/NoPadding"
private const val IV_LENGTH = 12 // Longitud del Vector de Inicialización (IV) en bytes
private const val AUTH_TAG_LENGTH = 16 // Longitud del Tag de Autenticación en bytes (128 bits)
private const val SALT_LENGTH = 32 // Longitud del Salt en bytes para la derivación de clave

/**
 * 💻 Implementación JVM de [PayloadCrypto] para el cifrado de payloads.
 *
 * Este objeto proporciona la lógica específica de JVM para cifrar y descifrar
 * datos utilizando AES-256-GCM y PBKDF2 para la derivación de claves.
 */
@OptIn(ExperimentalEncodingApi::class)
@Suppress("unused") // Suprime la advertencia de que PayloadCrypto no se usa
actual object PayloadCrypto {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val secureRandom = SecureRandom()

    /**
     * 🔒 Cifra datos serializables en la plataforma JVM.
     *
     * Utiliza AES-256-GCM para el cifrado, PBKDF2 para derivar la clave
     * y genera un salt y un IV aleatorios para cada operación.
     *
     * @param serializer El serializador de Kotlinx para el tipo de objeto [T].
     * @param data El objeto de datos a cifrar.
     * @param secretKey La clave secreta principal para la derivación de la clave de cifrado.
     * @return Una cadena en formato Base64 que representa el payload cifrado (salt + IV + ciphertext + authTag).
     * @throws Exception Si ocurre un error durante el proceso de cifrado.
     */
    actual suspend fun <T> encrypt(serializer: KSerializer<T>, data: T, secretKey: String): String {
        // Serializar a JSON
        val jsonString = json.encodeToString(serializer, data)
        val plaintext = jsonString.toByteArray(Charsets.UTF_8)

        // Generar salt e IV aleatorios
        val salt = randomBytes(SALT_LENGTH)
        val iv = randomBytes(IV_LENGTH)

        // Derivar clave usando HMAC-SHA256
        val key = deriveKey(secretKey, salt)

        // Encriptar
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(AUTH_TAG_LENGTH * 8, iv) // AUTH_TAG_LENGTH en bits
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertextWithTag = cipher.doFinal(plaintext)

        // Separar ciphertext y authentication tag
        val actualCiphertext = ciphertextWithTag.copyOfRange(0, ciphertextWithTag.size - AUTH_TAG_LENGTH)
        val authTag = ciphertextWithTag.copyOfRange(ciphertextWithTag.size - AUTH_TAG_LENGTH, ciphertextWithTag.size)

        // Combinar: salt + iv + actualCiphertext + authTag
        val combined = salt + iv + actualCiphertext + authTag

        return Base64.encode(combined)
    }

    /**
     * 🔓 Descifra un payload cifrado en la plataforma JVM.
     *
     * Espera un payload en formato Base64 que contiene el salt, IV, texto cifrado y authTag.
     *
     * @param deserializer El deserializador de Kotlinx para el tipo de objeto de destino [T].
     * @param encryptedData La cadena en formato Base64 que contiene los datos cifrados.
     * @param secretKey La clave secreta principal para la derivación de la clave de descifrado.
     * @return El objeto de datos [T] original.
     * @throws Exception Si ocurre un error durante el proceso de descifrado (ej. clave incorrecta, datos corruptos).
     */
    actual suspend fun <T> decrypt(deserializer: KSerializer<T>, encryptedData: String, secretKey: String): T {
        // Decodificar Base64
        val buffer = Base64.decode(encryptedData)

        // Extraer componentes (salt, IV, ciphertext, authTag) en el orden esperado
        val salt = buffer.sliceArray(0 until SALT_LENGTH)
        val iv = buffer.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val actualCiphertext = buffer.sliceArray(SALT_LENGTH + IV_LENGTH until buffer.size - AUTH_TAG_LENGTH)
        val authTag = buffer.sliceArray(buffer.size - AUTH_TAG_LENGTH until buffer.size)

        // Derivar clave usando HMAC-SHA256
        val key = deriveKey(secretKey, salt)

        // Desencriptar
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        // En GCM, el tag se pasa como parte de GCMParameterSpec en modo DECRYPT
        val gcmSpec = GCMParameterSpec(AUTH_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        // Concatenar el ciphertext real y el tag para pasarlo a doFinal en modo DECRYPT
        val ciphertextWithTag = actualCiphertext + authTag
        val plaintext = cipher.doFinal(ciphertextWithTag)
        val jsonString = String(plaintext, Charsets.UTF_8)

        // Deserializar a objeto
        return json.decodeFromString(deserializer, jsonString)
    }

    /**
     * 🎲 Genera un array de bytes aleatorios criptográficamente seguros en JVM.
     *
     * Utiliza `java.security.SecureRandom` para asegurar la aleatoriedad necesaria
     * para salts e IVs.
     *
     * @param length El número de bytes aleatorios a generar.
     * @return Un [ByteArray] con los bytes generados.
     */
    actual fun randomBytes(length: Int): ByteArray {
        return ByteArray(length).apply {
            secureRandom.nextBytes(this)
        }
    }

    /**
     * 🔑 Deriva una clave a partir de una contraseña y un salt usando HMAC-SHA256 en JVM.
     *
     * @param password La contraseña o secreto original como [String].
     * @param salt Un conjunto de bytes aleatorios.
     * @return La clave derivada como [ByteArray].
     * @throws Exception Si ocurre un error durante la derivación de la clave.
     */
    actual suspend fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val hmacAlgorithm = "HmacSHA256"
        val mac = Mac.getInstance(hmacAlgorithm)
        // La 'password' (nuestra SECRET_KEY) es la clave para el HMAC.
        val keySpec = SecretKeySpec(password.toByteArray(Charsets.UTF_8), hmacAlgorithm)
        mac.init(keySpec)
        // El 'salt' es el dato que se procesa con el HMAC.
        val derivedKey = mac.doFinal(salt)
        return derivedKey
    }
}