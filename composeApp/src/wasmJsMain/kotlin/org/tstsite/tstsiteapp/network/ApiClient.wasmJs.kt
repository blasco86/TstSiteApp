package org.tstsite.tstsiteapp.network

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.tstsite.tstsiteapp.config.AppConfig
import org.tstsite.tstsiteapp.model.*

/**
 * 🕸️ Implementación WasmJs del [ApiClient] multiplataforma.
 *
 * Esta clase proporciona la funcionalidad de cliente API para la plataforma WebAssembly (WasmJs),
 * utilizando Ktor para realizar las peticiones HTTP. Se encarga de la encriptación
 * y desencriptación de payloads según la configuración de [AppConfig].
 */
actual class ApiClient actual constructor() {
    private val client = ApiClientHelper.createHttpClient()

    // ==================== AUTH ====================
    /**
     * 🔑 Inicia sesión en la aplicación.
     *
     * @param pLogin Objeto [SesionRequest] con las credenciales del usuario.
     * @return Una [SesionResponse] con el token y los datos del usuario.
     * @throws Exception Si ocurre un error durante el proceso de login.
     */
    actual suspend fun login(pLogin: SesionRequest): SesionResponse {
        return try {
            val url = AppConfig.getApiLoginUrl(false)
            val apiKey = AppConfig.getApiKey()

            val body = ApiClientHelper.encryptRequestIfEnabled(
                pLogin,
                SesionRequest.serializer()
            )

            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", apiKey)
                setBody(body)
            }

            val responseText = response.bodyAsText()
            val wrapper = ApiClientHelper.decryptResponseIfNeeded(
                responseText,
                LoginResponseWrapper.serializer()
            )

            ApiClientHelper.buildSesionResponse(wrapper)
        } catch (e: Exception) {
            throw Exception("El servicio no está disponible: ${e.message}")
        }
    }

    /**
     * 🛡️ Valida un token de sesión existente.
     *
     * Envía un [EmptyRequest] cifrado si la encriptación está habilitada.
     *
     * @param token El token JWT a validar.
     * @return Un [ValidateResponse] indicando si el token es válido.
     * @throws Exception Si ocurre un error durante la validación del token.
     */
    actual suspend fun validate(token: String): ValidateResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(
                EmptyRequest(),
                EmptyRequest.serializer()
            )
            val response: HttpResponse = client.post(AppConfig.getApiValidateUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), ValidateResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error al validar token: ${e.message}")
        }
    }

    /**
     * 👤 Obtiene el perfil del usuario actual.
     *
     * Envía un [EmptyRequest] cifrado si la encriptación está habilitada.
     *
     * @param token El token JWT del usuario.
     * @return Un [ProfileResponse] con los detalles del perfil.
     * @throws Exception Si ocurre un error al obtener el perfil.
     */
    actual suspend fun profile(token: String): ProfileResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(
                EmptyRequest(),
                EmptyRequest.serializer()
            )
            val response: HttpResponse = client.post(AppConfig.getApiProfileUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), ProfileResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error al obtener perfil: ${e.message}")
        }
    }

    /**
     * 🚪 Cierra la sesión del usuario.
     *
     * Envía un [EmptyRequest] cifrado si la encriptación está habilitada.
     *
     * @param token El token JWT a invalidar.
     * @return Un [LogoutResponse] confirmando el cierre de sesión.
     * @throws Exception Si ocurre un error al cerrar la sesión.
     */
    actual suspend fun logout(token: String): LogoutResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(
                EmptyRequest(),
                EmptyRequest.serializer()
            )
            val response: HttpResponse = client.post(AppConfig.getApiLogoutUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), LogoutResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error al cerrar sesión: ${e.message}")
        }
    }

    // ==================== USERS ====================
    /**
     * ➕ Inserta un nuevo usuario en el sistema.
     *
     * @param token El token JWT de un administrador.
     * @param userData Los datos del nuevo usuario a crear.
     * @return Un [UserResponse] con el resultado de la operación.
     * @throws Exception Si ocurre un error al insertar el usuario.
     */
    actual suspend fun insertUser(token: String, userData: UserData): UserResponse {
        return userAction("insert", token, userData)
    }

    /**
     * 🧑‍💻 Selecciona (obtiene) un usuario por su nombre de usuario.
     *
     * @param token El token JWT de un administrador.
     * @param username El nombre del usuario a buscar.
     * @return Un [UserResponse] con los datos del usuario encontrado.
     * @throws Exception Si ocurre un error al seleccionar el usuario.
     */
    actual suspend fun selectUser(token: String, username: String): UserResponse {
        return userAction("select", token, UserData(usuario = username))
    }

    /**
     * 🔄 Actualiza los datos de un usuario existente.
     *
     * @param token El token JWT de un administrador.
     * @param username El nombre del usuario a modificar.
     * @param userData Los nuevos datos para el usuario.
     * @return Un [UserResponse] con el resultado de la operación.
     * @throws Exception Si ocurre un error al actualizar el usuario.
     */
    actual suspend fun updateUser(token: String, username: String, userData: UserData): UserResponse {
        return userAction("update", token, userData.copy(usuario = username))
    }

    /**
     * ❌ Elimina un usuario del sistema.
     *
     * @param token El token JWT de un administrador.
     * @param username El nombre del usuario a eliminar.
     * @return Un [UserResponse] con el resultado de la operación.
     * @throws Exception Si ocurre un error al eliminar el usuario.
     */
    actual suspend fun deleteUser(token: String, username: String): UserResponse {
        return userAction("delete", token, UserData(usuario = username))
    }

    /**
     * 📜 Lista todos los usuarios del sistema.
     *
     * Envía un [EmptyRequest] cifrado si la encriptación está habilitada.
     *
     * @param token El token JWT de un administrador.
     * @return Un [UsersListResponse] con la lista de todos los usuarios.
     * @throws Exception Si ocurre un error al listar los usuarios.
     */
    actual suspend fun listUsers(token: String): UsersListResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(
                EmptyRequest(),
                EmptyRequest.serializer()
            )
            val response: HttpResponse = client.post(AppConfig.getApiUsersUrl("list", false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), UsersListResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error al listar usuarios: ${e.message}")
        }
    }

    /**
     * 🔍 Busca usuarios que coincidan con ciertos criterios.
     *
     * @param token El token JWT de un administrador.
     * @param searchParams Los parámetros de búsqueda [UserSearchParams].
     * @return Un [UsersListResponse] con los usuarios encontrados.
     * @throws Exception Si ocurre un error al buscar usuarios.
     */
    actual suspend fun searchUsers(token: String, searchParams: UserSearchParams): UsersListResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(
                searchParams,
                UserSearchParams.serializer()
            )
            val response: HttpResponse = client.post(AppConfig.getApiUsersUrl("search", false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), UsersListResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error al buscar usuarios: ${e.message}")
        }
    }

    /**
     * 🛠️ Función interna para realizar acciones CRUD sobre usuarios.
     *
     * @param action La acción a realizar ("insert", "select", "update", "delete").
     * @param token El token JWT del usuario.
     * @param userData Los datos del usuario para la acción.
     * @return Un [UserResponse] con el resultado de la acción.
     * @throws Exception Si ocurre un error durante la acción.
     */
    private suspend fun userAction(action: String, token: String, userData: UserData): UserResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(
                userData,
                UserData.serializer()
            )
            val response: HttpResponse = client.post(AppConfig.getApiUsersUrl(action, false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), UserResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error en $action usuario: ${e.message}")
        }
    }

    // ==================== CATALOG ====================

    /**
     * 📚 Obtiene el catálogo completo de datos.
     *
     * Envía un [EmptyRequest] cifrado si la encriptación está habilitada.
     *
     * @param token El token JWT del usuario.
     * @return Un [CatalogResponse] con los datos del catálogo.
     * @throws Exception Si ocurre un error al obtener el catálogo.
     */
    actual suspend fun getCatalog(token: String): CatalogResponse {
        return try {
            val body = ApiClientHelper.encryptRequestIfEnabled(
                EmptyRequest(),
                EmptyRequest.serializer()
            )
            val response: HttpResponse = client.post(AppConfig.getApiCatalogUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", AppConfig.getApiKey())
                headers.append("Authorization", "Bearer $token")
                setBody(body)
            }
            ApiClientHelper.decryptResponseIfNeeded(response.bodyAsText(), CatalogResponse.serializer())
        } catch (e: Exception) {
            throw Exception("Error al obtener catálogo: ${e.message}")
        }
    }
}