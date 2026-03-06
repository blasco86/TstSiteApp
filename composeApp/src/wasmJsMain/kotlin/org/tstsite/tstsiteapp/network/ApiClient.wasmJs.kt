package org.tstsite.tstsiteapp.network

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.tstsite.tstsiteapp.config.AppConfig
import org.tstsite.tstsiteapp.model.*

actual class ApiClient actual constructor() {
    private val client = ApiClientHelper.createHttpClient()

    /**
     * 🔑 Realiza el proceso de login en la API.
     * @param pLogin Las credenciales de inicio de sesión.
     * @return Una `SesionResponse` con el token y los datos del usuario.
     * @throws Exception si ocurre un error durante el login.
     */
    actual suspend fun login(pLogin: SesionRequest): SesionResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(pLogin, SesionRequest.serializer())
            val response: HttpResponse = client.post(AppConfig.getApiLoginUrl(false)) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            ApiClientHelper.buildSesionResponse(
                ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), LoginResponseWrapper.serializer())
            )
        } catch (e: Exception) {
            throw Exception("El servicio no está disponible: ${e.message}")
        }
    }

    /**
     * 🛡️ Valida un token de sesión en la API.
     * @param token El token a validar.
     * @return Una `ValidateResponse` con el resultado de la validación.
     * @throws Exception si ocurre un error durante la validación.
     */
    actual suspend fun validate(token: String): ValidateResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(EmptyRequest(), EmptyRequest.serializer())
            val response: HttpResponse = client.post(AppConfig.getApiValidateUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), ValidateResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error al validar token: ${e.message}")
        }
    }

    /**
     * 👤 Obtiene el perfil del usuario desde la API.
     * @param token El token de sesión del usuario.
     * @return Una `ProfileResponse` con los datos del perfil.
     * @throws Exception si ocurre un error al obtener el perfil.
     */
    actual suspend fun profile(token: String): ProfileResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(EmptyRequest(), EmptyRequest.serializer())
            val response: HttpResponse = client.post(AppConfig.getApiProfileUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), ProfileResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error al obtener perfil: ${e.message}")
        }
    }

    /**
     * 🚪 Cierra la sesión del usuario en la API.
     * @param token El token de sesión del usuario.
     * @return Una `LogoutResponse` con el resultado del cierre de sesión.
     * @throws Exception si ocurre un error al cerrar la sesión.
     */
    actual suspend fun logout(token: String): LogoutResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(EmptyRequest(), EmptyRequest.serializer())
            val response: HttpResponse = client.post(AppConfig.getApiLogoutUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), LogoutResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error al cerrar sesión: ${e.message}")
        }
    }

    /**
     * ⚙️ Realiza una acción genérica sobre un usuario en la API.
     * @param action La acción a realizar (ej. "insert", "select", "update", "delete").
     * @param token El token de sesión del usuario.
     * @param userData Los datos del usuario para la acción.
     * @return Una `UserResponse` con el resultado de la acción.
     * @throws Exception si ocurre un error durante la acción.
     */
    private suspend fun userAction(action: String, token: String, userData: UserData): UserResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(userData, UserData.serializer())
            val response: HttpResponse = client.post(AppConfig.getApiUsersUrl(action, false)) {
                contentType(ContentType.Application.Json)
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), UserResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error en $action usuario: ${e.message}")
        }
    }

    actual suspend fun insertUser(token: String, userData: UserData) = userAction("insert", token, userData)
    actual suspend fun selectUser(token: String, username: String) = userAction("select", token, UserData(usuario = username))
    actual suspend fun updateUser(token: String, username: String, userData: UserData) = userAction("update", token, userData.copy(usuario = username))
    actual suspend fun deleteUser(token: String, username: String) = userAction("delete", token, UserData(usuario = username))

    /**
     * 📋 Obtiene la lista de todos los usuarios de la API.
     * @param token El token de sesión del usuario.
     * @return Una `UsersListResponse` con la lista de usuarios.
     * @throws Exception si ocurre un error al listar los usuarios.
     */
    actual suspend fun listUsers(token: String): UsersListResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(EmptyRequest(), EmptyRequest.serializer())
            val response: HttpResponse = client.post(AppConfig.getApiUsersUrl("list", false)) {
                contentType(ContentType.Application.Json)
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), UsersListResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error al listar usuarios: ${e.message}")
        }
    }

    /**
     * 🔎 Busca usuarios en la API según los parámetros de búsqueda.
     * @param token El token de sesión del usuario.
     * @param searchParams Los parámetros de búsqueda.
     * @return Una `UsersListResponse` con los usuarios encontrados.
     * @throws Exception si ocurre un error durante la búsqueda.
     */
    actual suspend fun searchUsers(token: String, searchParams: UserSearchParams): UsersListResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(searchParams, UserSearchParams.serializer())
            val response: HttpResponse = client.post(AppConfig.getApiUsersUrl("search", false)) {
                contentType(ContentType.Application.Json)
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), UsersListResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error al buscar usuarios: ${e.message}")
        }
    }

    /**
     * 📚 Obtiene el catálogo de la API.
     * @param token El token de sesión del usuario.
     * @return Una `CatalogResponse` con los datos del catálogo.
     * @throws Exception si ocurre un error al obtener el catálogo.
     */
    actual suspend fun getCatalog(token: String): CatalogResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(EmptyRequest(), EmptyRequest.serializer())
            val response: HttpResponse = client.post(AppConfig.getApiCatalogUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), CatalogResponse.serializer())
        } catch (e: Exception) {
            throw Exception("El servicio no está disponible: ${e.message}")
        }
    }
}