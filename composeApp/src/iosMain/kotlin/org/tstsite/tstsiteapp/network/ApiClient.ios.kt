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
import org.tstsite.tstsiteapp.model.LoginResponseWrapper
import org.tstsite.tstsiteapp.model.SesionRequest
import org.tstsite.tstsiteapp.model.SesionResponse
import org.tstsite.tstsiteapp.model.UsuarioLogin
import org.tstsite.tstsiteapp.config.AppConfig.getApiKey

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

    actual suspend fun login(pLogin: SesionRequest): SesionResponse {
        try {
            val response: LoginResponseWrapper = client.post(AppConfig.getApiLoginUrl(false)) {
                contentType(ContentType.Application.Json)
                headers.append("x-api-key", getApiKey())
                setBody(pLogin)
            }.body()

            if (response.resultado == "error") {
                throw Exception(response.mensaje ?: "Error de inicio de sesión desconocido")
            }

            val user = UsuarioLogin(
                idUsuario = response.idUsuario ?: throw Exception("idUsuario no encontrado en la respuesta"),
                usuario = response.usuario ?: throw Exception("usuario no encontrado en la respuesta"),
                perfil = response.perfil ?: throw Exception("perfil no encontrado en la respuesta"),
                estado = response.estado ?: throw Exception("estado no encontrado en la respuesta"),
                permisos = response.permisos,
                detalles = response.detalles
            )

            return SesionResponse(
                token = response.token ?: throw Exception("Token no encontrado en la respuesta"),
                expiresIn = response.expiresIn ?: throw Exception("expiresIn no encontrado en la respuesta"),
                user = user
            )
        }catch (e: Exception){
            throw Exception("El servicio no está disponible")
        }
    }
}