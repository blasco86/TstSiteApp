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

private const val ALGORITHM = "AES/GCM/NoPadding"
private const val IV_LENGTH = 12
private const val AUTH_TAG_LENGTH = 128 // ⚠️ En BITS, no bytes (128 bits = 16 bytes)
private const val SALT_LENGTH = 32

/**
 * 🤖 Implementación Android de [PayloadCrypto] para el cifrado de payloads.
 *
 * Este objeto proporciona la lógica específica de Android para cifrar y descifrar
 * datos utilizando AES-256-GCM, PBKDF2 para derivación de claves y HMAC-SHA256
 * para la compatibilidad con el backend.
 */
@OptIn(ExperimentalEncodingApi::class)
@Suppress("unused")
actual object PayloadCrypto {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val secureRandom = SecureRandom()

    /**
     * 🔒 Cifra datos serializables en la plataforma Android.
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
        val jsonString = json.encodeToString(serializer, data)
        val plaintext = jsonString.toByteArray(Charsets.UTF_8)
        val salt = randomBytes(SALT_LENGTH)
        val iv = randomBytes(IV_LENGTH)

        val key = deriveKey(secretKey, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(AUTH_TAG_LENGTH, iv) // 128 bits = 16 bytes

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        // ✅ En GCM, doFinal() devuelve: [ciphertext + authTag fusionados]
        val ciphertextWithTag = cipher.doFinal(plaintext)

        val combined = salt + iv + ciphertextWithTag

        return Base64.encode(combined)
    }

    /**
     * 🔓 Descifra un payload cifrado en la plataforma Android.
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
        val buffer = Base64.decode(encryptedData)

        val salt = buffer.sliceArray(0 until SALT_LENGTH)
        val iv = buffer.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val ciphertextWithTag = buffer.sliceArray(SALT_LENGTH + IV_LENGTH until buffer.size)

        val key = deriveKey(secretKey, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(AUTH_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        // ✅ GCM procesa automáticamente el authTag que está al final
        val plaintext = cipher.doFinal(ciphertextWithTag)
        val jsonString = String(plaintext, Charsets.UTF_8)

        return json.decodeFromString(deserializer, jsonString)
    }

    /**
     * 🎲 Genera un array de bytes aleatorios criptográficamente seguros en Android.
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
     * 🔑 Deriva una clave a partir de una contraseña y un salt usando HMAC-SHA256 en Android.
     *
     * Este método es compatible con la derivación de claves utilizada en el backend.
     *
     * @param password La contraseña o secreto original como [String].
     * @param salt Un conjunto de bytes aleatorios.
     * @return La clave derivada como [ByteArray].
     * @throws Exception Si ocurre un error durante la derivación de la clave.
     */
    actual suspend fun deriveKey(password: String, salt: ByteArray): ByteArray {
        // Usamos HMAC-SHA256 para derivar la clave. Es un método estándar y
        // 100% compatible entre plataformas, a diferencia de PBKDF2.
        val hmacAlgorithm = "HmacSHA256"
        val mac = Mac.getInstance(hmacAlgorithm)
        // La 'password' (nuestra SECRET_KEY) es la clave para el HMAC.
        val keySpec = SecretKeySpec(password.toByteArray(Charsets.UTF_8), hmacAlgorithm)
        mac.init(keySpec)
        // El 'salt' es el dato que se procesa con el HMAC.
        return mac.doFinal(salt)
    }
}