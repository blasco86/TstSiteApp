package org.tstsite.tstsiteapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== AUTH ====================

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

@Serializable
data class SesionResponse(
    val token: String?,
    val expiresIn: Int?,
    val user: UsuarioLogin?
)

@Serializable
data class UsuarioLogin(
    val idUsuario: Int,
    val usuario: String,
    val perfil: String,
    val estado: String,
    val permisos: List<String> = emptyList(),
    val detalles: DetalleUsuarioLogin? = null
)

@Serializable
data class DetalleUsuarioLogin(
    val nombre: String? = null,
    val apellidos: String? = null,
    val telefono: String? = null,
    val mail: String? = null,
    val direccion: String? = null,
    val fechaNacimiento: String? = null
)

@Serializable
data class SesionRequest(
    @SerialName("username")
    val username: String,
    val password: String
)

@Serializable
data class ValidateResponse(
    val valid: Boolean,
    val user: TokenPayload? = null,
    val resultado: String? = null,
    val mensaje: String? = null
)

@Serializable
data class TokenPayload(
    val sub: Int,
    val username: String,
    val role: String,
    val iat: Long,
    val exp: Long,
    val jti: String,
    val iss: String,
    val aud: String
)

@Serializable
data class ProfileResponse(
    val resultado: String,
    val message: String,
    val user: TokenPayload
)

@Serializable
data class LogoutResponse(
    val resultado: String,
    val message: String
)

// ==================== USERS ====================

@Serializable
data class UserData(
    val usuario: String? = null,
    val password: String? = null,
    val id_perfil: Int? = null,
    val id_estado: Int? = null,
    val detalles: DetalleUsuario? = null
)

@Serializable
data class DetalleUsuario(
    val nombre: String? = null,
    val apellidos: String? = null,
    val telefono: String? = null,
    val mail: String? = null,
    val direccion: String? = null,
    val fecha_nacimiento: String? = null
)

@Serializable
data class UserResponse(
    val resultado: String,
    val mensaje: String? = null,
    val id_usuario: Int? = null,
    val usuario: UserInfo? = null
)

@Serializable
data class UserInfo(
    val id: Int,
    val usuario: String,
    val estado: String,
    val perfil: String,
    val intentos_fallidos: Int? = null,
    val detalles: DetalleUsuario? = null
)

@Serializable
data class UsersListResponse(
    val resultado: String,
    val usuarios: List<UserInfo>
)

@Serializable
data class UserSearchParams(
    val usuario: String? = null,
    val estado: String? = null,
    val perfil: String? = null
)

// ==================== CATALOG ====================

@Serializable
data class CatalogResponse(
    val resultado: String,
    val total_categorias: Int,
    val catalogo: List<TipoProducto>
)

@Serializable
data class TipoProducto(
    val id: Int,
    val nombre: String,
    val slug: String? = null,
    val orden: Int,
    val productos: List<Producto> = emptyList(),
    val subtipos: List<TipoProducto>? = null
)

@Serializable
data class Producto(
    val id: Int,
    val nombre: String,
    val slug: String? = null,
    val atributos: Map<String, String> = emptyMap()
)

// ==================== COMMON ====================

@Serializable
data class ApiError(
    val resultado: String,
    val mensaje: String,
    val detalle: String? = null
)