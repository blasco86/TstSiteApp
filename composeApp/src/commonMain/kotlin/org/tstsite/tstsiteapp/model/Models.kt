package org.tstsite.tstsiteapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ==================== 🔑 AUTENTICACIÓN 🔑 ====================

/**
 * 📦 Wrapper para la respuesta cruda del endpoint de login.
 * Contiene todos los posibles campos que puede devolver la API,
 * antes de ser procesados y convertidos a un [SesionResponse] limpio.
 */
@Serializable
data class LoginResponseWrapper(
    val message: String? = null,
    val token: String? = null,
    val expiresIn: Int? = null,
    val resultado: String? = null,
    val mensaje: String? = null,
    val idUsuario: Int? = null,
    val usuario: String? = null,
    val perfil: String? = null,
    val estado: String? = null,
    val permisos: List<String> = emptyList(),
    val detalles: DetalleUsuarioLogin? = null
)

/**
 *  sesión de usuario final.
 * Contiene el token y la información esencial del usuario.
 */
@Serializable
data class SesionResponse(
    val token: String?,
    val expiresIn: Int?,
    val user: UsuarioLogin?
)

/**
 * 🧑‍💻 Información del usuario que ha iniciado sesión.
 */
@Serializable
data class UsuarioLogin(
    val idUsuario: Int,
    val usuario: String,
    val perfil: String,
    val estado: String,
    val permisos: List<String> = emptyList(),
    val detalles: DetalleUsuarioLogin? = null
)

/**
 * ℹ️ Detalles adicionales del usuario que ha iniciado sesión.
 */
@Serializable
data class DetalleUsuarioLogin(
    val nombre: String? = null,
    val apellidos: String? = null,
    val telefono: String? = null,
    val mail: String? = null,
    val direccion: String? = null,
    val fechaNacimiento: String? = null
)

/**
 * 📥 Petición para iniciar sesión.
 * @property username Nombre de usuario.
 * @property password Contraseña del usuario.
 */
@Serializable
data class SesionRequest(
    @SerialName("username")
    val username: String,
    val password: String
)

/**
 * 🛡️ Respuesta de la validación de un token.
 * @property valid `true` si el token es válido, `false` si no lo es.
 * @property user Payload del token si es válido.
 */
@Serializable
data class ValidateResponse(
    val valid: Boolean,
    val user: TokenPayload? = null,
    val resultado: String? = null,
    val mensaje: String? = null
)

/**
 * 📜 Contenido (payload) de un token JWT.
 * Contiene la información estándar de un token.
 */
@Serializable
data class TokenPayload(
    val sub: Int? = null, // Subject (ID de usuario)
    val username: String? = null,
    val role: String? = null,
    val iat: Long, // Issued At
    val exp: Long, // Expiration Time
    val jti: String, // JWT ID
    val iss: String, // Issuer
    val aud: String  // Audience
)

/**
 * 👤 Respuesta al solicitar el perfil de usuario.
 */
@Serializable
data class ProfileResponse(
    val resultado: String,
    val message: String,
    val user: TokenPayload
)

/**
 * 🚪 Respuesta al cerrar sesión.
 */
@Serializable
data class LogoutResponse(
    val resultado: String,
    val message: String
)

// ==================== 👥 USUARIOS 👥 ====================

/**
 * 📝 Datos para crear o actualizar un usuario.
 * Todos los campos son opcionales para permitir actualizaciones parciales.
 */
@Serializable
data class UserData(
    val usuario: String? = null,
    val password: String? = null,
    val id_perfil: Int? = null,
    val id_estado: Int? = null,
    val detalles: DetalleUsuario? = null
)

/**
 * ℹ️ Detalles adicionales de un usuario.
 */
@Serializable
data class DetalleUsuario(
    val nombre: String? = null,
    val apellidos: String? = null,
    val telefono: String? = null,
    val mail: String? = null,
    val direccion: String? = null,
    val fecha_nacimiento: String? = null
)

/**
 * ✅ Respuesta genérica para operaciones de usuario (crear, leer, actualizar, borrar).
 */
@Serializable
data class UserResponse(
    val resultado: String,
    val mensaje: String? = null,
    val id_usuario: Int? = null,
    val usuario: UserInfo? = null
)

/**
 * 🧑‍💻 Información detallada de un usuario del sistema.
 */
@Serializable
data class UserInfo(
    val id: Int,
    val usuario: String,
    val estado: String,
    val perfil: String,
    val intentos_fallidos: Int? = null,
    val detalles: DetalleUsuario? = null
)

/**
 * 📜 Respuesta que contiene una lista de usuarios.
 */
@Serializable
data class UsersListResponse(
    val resultado: String,
    val usuarios: List<UserInfo>
)

/**
 * 🔍 Parámetros para buscar usuarios.
 * Los campos nulos no se tienen en cuenta en la búsqueda.
 */
@Serializable
data class UserSearchParams(
    val usuario: String? = null,
    val estado: String? = null,
    val perfil: String? = null
)

// ==================== 📚 CATÁLOGO 📚 ====================

/**
 * 🗂️ Respuesta que contiene el catálogo completo de productos.
 */
@Serializable
data class CatalogResponse(
    val resultado: String,
    val total_categorias: Int,
    val catalogo: List<TipoProducto>
)

/**
 * 📂 Representa una categoría o tipo de producto.
 * Puede contener sub-tipos, creando una estructura de árbol.
 */
@Serializable
data class TipoProducto(
    val id: Int,
    val nombre: String,
    val slug: String? = null,
    val orden: Int,
    val productos: List<Producto> = emptyList(),
    val subtipos: List<TipoProducto>? = null
)

/**
 * 📦 Representa un producto individual dentro de un tipo.
 * @property atributos Un mapa flexible para cualquier tipo de dato adicional.
 */
@Serializable
data class Producto(
    val id: Int,
    val nombre: String,
    val slug: String? = null,
    val atributos: Map<String, JsonElement> = emptyMap()
)

// ==================== ⚙️ COMÚN ⚙️ ====================

/**
 * ❌ Modelo para errores de la API.
 */
@Serializable
data class ApiError(
    val resultado: String,
    val mensaje: String,
    val detalle: String? = null
)

/**
 * 텅 Petición vacía.
 * Se utiliza en endpoints POST que no requieren enviar datos en el cuerpo,
 * pero que necesitan un cuerpo para el cifrado.
 */
@Serializable
class EmptyRequest