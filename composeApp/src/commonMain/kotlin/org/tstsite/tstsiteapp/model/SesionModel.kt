package org.tstsite.tstsiteapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
