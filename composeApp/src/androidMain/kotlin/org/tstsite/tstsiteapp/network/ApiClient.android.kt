package org.tstsite.tstsiteapp.network

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.tstsite.tstsiteapp.config.AppConfig
import org.tstsite.tstsiteapp.model.*

actual class ApiClient actual constructor() {
    private val client = ApiClientHelper.createHttpClient()

    // ==================== AUTH ====================
    actual suspend fun login(pLogin: SesionRequest): SesionResponse {
        return try {
            val url = AppConfig.getApiLoginUrl(true) // isAndroid = true
            val apiKey = AppConfig.getApiKey()

            println("🚀 [Android] Login a: $url")

            // 1. Encriptar request si está habilitado
            val body = ApiClientHelper.encryptRequestIfEnabled(
                pLogin,
                SesionRequest.serializer()
            )

            // 2. Enviar request
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", apiKey)
                setBody(body)
            }

            // 3. Desencriptar response si es necesario
            val responseText = response.bodyAsText()
            val wrapper = ApiClientHelper.decryptResponseIfNeeded(
                responseText,
                LoginResponseWrapper.serializer()
            )

            ApiClientHelper.buildSesionResponse(wrapper)
        } catch (e: Exception) {
            throw Exception("Error en login: ${e.message}")
        }
    }

    actual suspend fun validate(token: String): ValidateResponse {
        return try {
            val response: HttpResponse = client.post(AppConfig.getApiValidateUrl(true)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
            }

            ApiClientHelper.decryptResponseIfNeeded(
                response.bodyAsText(),
                ValidateResponse.serializer()
            )
        } catch (e: Exception) {
            throw Exception("Error al validar token: ${e.message}")
        }
    }

    actual suspend fun profile(token: String): ProfileResponse {
        return try {
            val response: HttpResponse = client.get(AppConfig.getApiProfileUrl(true)) {
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
            }

            ApiClientHelper.decryptResponseIfNeeded(
                response.bodyAsText(),
                ProfileResponse.serializer()
            )
        } catch (e: Exception) {
            throw Exception("Error al obtener perfil: ${e.message}")
        }
    }

    actual suspend fun logout(token: String): LogoutResponse {
        return try {
            val response: HttpResponse = client.post(AppConfig.getApiLogoutUrl(true)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
            }

            ApiClientHelper.decryptResponseIfNeeded(
                response.bodyAsText(),
                LogoutResponse.serializer()
            )
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
            val body = ApiClientHelper.encryptRequestIfEnabled(
                EmptyRequest(),
                EmptyRequest.serializer()
            )

            val response: HttpResponse = client.post(AppConfig.getApiUsersUrl("list", true)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }

            ApiClientHelper.decryptResponseIfNeeded(
                response.bodyAsText(),
                UsersListResponse.serializer()
            )
        } catch (e: Exception) {
            throw Exception("Error al listar usuarios: ${e.message}")
        }
    }

    actual suspend fun searchUsers(token: String, searchParams: UserSearchParams): UsersListResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(
                searchParams,
                UserSearchParams.serializer()
            )

            val response: HttpResponse = client.post(AppConfig.getApiUsersUrl("search", true)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }

            ApiClientHelper.decryptResponseIfNeeded(
                response.bodyAsText(),
                UsersListResponse.serializer()
            )
        } catch (e: Exception) {
            throw Exception("Error al buscar usuarios: ${e.message}")
        }
    }

    private suspend fun userAction(action: String, token: String, userData: UserData): UserResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(
                userData,
                UserData.serializer()
            )

            val response: HttpResponse = client.post(AppConfig.getApiUsersUrl(action, true)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }

            ApiClientHelper.decryptResponseIfNeeded(
                response.bodyAsText(),
                UserResponse.serializer()
            )
        } catch (e: Exception) {
            throw Exception("Error en $action usuario: ${e.message}")
        }
    }

    // ==================== CATALOG ====================
    actual suspend fun getCatalog(token: String): CatalogResponse {
        return try {
            val response: HttpResponse = client.post(AppConfig.getApiCatalogUrl(true)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
            }

            ApiClientHelper.decryptResponseIfNeeded(
                response.bodyAsText(),
                CatalogResponse.serializer()
            )
        } catch (e: Exception) {
            throw Exception("Error al obtener catálogo: ${e.message}")
        }
    }
}