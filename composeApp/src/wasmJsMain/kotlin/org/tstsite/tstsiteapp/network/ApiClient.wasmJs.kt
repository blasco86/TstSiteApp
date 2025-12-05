package org.tstsite.tstsiteapp.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.tstsite.tstsiteapp.config.AppConfig
import org.tstsite.tstsiteapp.model.*

actual class ApiClient actual constructor() {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
        }
    }

    // ==================== AUTH ====================

    actual suspend fun login(pLogin: SesionRequest): SesionResponse {
        return try {
            val response: LoginResponseWrapper = client.post(AppConfig.getApiLoginUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                setBody(pLogin)
            }.body()

            if (response.resultado == "error") {
                throw Exception(response.mensaje ?: "Error de inicio de sesión desconocido")
            }

            val user = UsuarioLogin(
                idUsuario = response.idUsuario ?: throw Exception("idUsuario no encontrado"),
                usuario = response.usuario ?: throw Exception("usuario no encontrado"),
                perfil = response.perfil ?: throw Exception("perfil no encontrado"),
                estado = response.estado ?: throw Exception("estado no encontrado"),
                permisos = response.permisos,
                detalles = response.detalles
            )

            SesionResponse(
                token = response.token ?: throw Exception("Token no encontrado"),
                expiresIn = response.expiresIn ?: throw Exception("expiresIn no encontrado"),
                user = user
            )
        } catch (e: Exception) {
            throw Exception("El servicio no está disponible: ${e.message}")
        }
    }

    actual suspend fun validate(token: String): ValidateResponse {
        return try {
            client.post(AppConfig.getApiValidateUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
            }.body()
        } catch (e: Exception) {
            throw Exception("Error al validar token: ${e.message}")
        }
    }

    actual suspend fun profile(token: String): ProfileResponse {
        return try {
            client.get(AppConfig.getApiProfileUrl(false)) {
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
            }.body()
        } catch (e: Exception) {
            throw Exception("Error al obtener perfil: ${e.message}")
        }
    }

    actual suspend fun logout(token: String): LogoutResponse {
        return try {
            client.post(AppConfig.getApiLogoutUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
            }.body()
        } catch (e: Exception) {
            throw Exception("Error al cerrar sesión: ${e.message}")
        }
    }

    // ==================== USERS ====================

    actual suspend fun insertUser(token: String, userData: UserData): UserResponse {
        return userAction("insert", token, userData)
    }

    actual suspend fun selectUser(token: String, username: String): UserResponse {
        return userAction("select", token, UserData(usuario = username))
    }

    actual suspend fun updateUser(token: String, username: String, userData: UserData): UserResponse {
        return userAction("update", token, userData.copy(usuario = username))
    }

    actual suspend fun deleteUser(token: String, username: String): UserResponse {
        return userAction("delete", token, UserData(usuario = username))
    }

    actual suspend fun listUsers(token: String): UsersListResponse {
        return try {
            client.post(AppConfig.getApiUsersUrl("list", false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(emptyMap<String, String>())
            }.body()
        } catch (e: Exception) {
            throw Exception("Error al listar usuarios: ${e.message}")
        }
    }

    actual suspend fun searchUsers(token: String, searchParams: UserSearchParams): UsersListResponse {
        return try {
            client.post(AppConfig.getApiUsersUrl("search", false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(searchParams)
            }.body()
        } catch (e: Exception) {
            throw Exception("Error al buscar usuarios: ${e.message}")
        }
    }

    private suspend fun userAction(action: String, token: String, userData: UserData): UserResponse {
        return try {
            client.post(AppConfig.getApiUsersUrl(action, false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(userData)
            }.body()
        } catch (e: Exception) {
            throw Exception("Error en $action usuario: ${e.message}")
        }
    }

    // ==================== CATALOG ====================

    actual suspend fun getCatalog(token: String): CatalogResponse {
        return try {
            client.post(AppConfig.getApiCatalogUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
            }.body()
        } catch (e: Exception) {
            throw Exception("Error al obtener catálogo: ${e.message}")
        }
    }
}