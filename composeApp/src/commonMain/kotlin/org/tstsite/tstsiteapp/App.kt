package org.tstsite.tstsiteapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.tstsite.tstsiteapp.model.*
import org.tstsite.tstsiteapp.network.ApiClient

/**
 * 📱 Componente principal de la aplicación.
 *
 * Esta función Composable define la interfaz de usuario de demostración
 * para interactuar con la API de TstSite. Permite probar las funcionalidades
 * de autenticación, gestión de usuarios y consulta de catálogo.
 */
@Composable
@Preview
fun App() {
    MaterialTheme {
        // 🔑 Estado del token de autenticación. Nulo si no hay sesión iniciada.
        var token by remember { mutableStateOf<String?>(null) }
        // 📝 Mensaje de resultado de las operaciones de la API.
        var resultado by remember { mutableStateOf("") }
        // 🔄 Indicador de carga para las operaciones asíncronas.
        var loading by remember { mutableStateOf(false) }

        // 🌐 Instancia del cliente API para realizar las llamadas.
        val apiClient = remember { ApiClient() }
        // 🚀 Scope para lanzar corrutinas en el contexto de la UI.
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Permite scroll si el contenido es largo
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre elementos
        ) {
            Text(
                "TstSite API Demo",
                style = MaterialTheme.typography.headlineMedium
            )

            // ==================== 🔑 AUTENTICACIÓN 🔑 ====================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Autenticación", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    /**
                     * ➡️ Botón para iniciar sesión.
                     * Realiza una llamada a `apiClient.login` con credenciales de prueba.
                     */
                    Button(
                        onClick = {
                            loading = true
                            coroutineScope.launch {
                                try {
                                    val response = apiClient.login(
                                        SesionRequest("adminTstSite", "adminBlasco86")
                                    )
                                    token = response.token
                                    resultado = """
                                        ✅ Login exitoso
                                        Usuario: ${response.user?.usuario}
                                        Perfil: ${response.user?.perfil}
                                        Token guardado
                                    """.trimIndent()
                                } catch (e: Exception) {
                                    resultado = "❌ Error login: ${e.message}"
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading // Deshabilita el botón durante la carga
                    ) {
                        Text("1. Login")
                    }

                    /**
                     * 🛡️ Botón para validar el token actual.
                     * Llama a `apiClient.validate` usando el token obtenido en el login.
                     */
                    Button(
                        onClick = {
                            token?.let {
                                loading = true
                                coroutineScope.launch {
                                    try {
                                        val response = apiClient.validate(it)
                                        resultado = """
                                            ✅ Token válido
                                            Usuario: ${response.user?.username}
                                            Rol: ${response.user?.role}
                                        """.trimIndent()
                                    } catch (e: Exception) {
                                        resultado = "❌ Error validate: ${e.message}"
                                    } finally {
                                        loading = false
                                    }
                                }
                            } ?: run { resultado = "⚠️ Primero haz login" }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && token != null // Requiere token y no estar cargando
                    ) {
                        Text("2. Validar Token")
                    }

                    /**
                     * 👤 Botón para ver el perfil del usuario.
                     * Llama a `apiClient.profile` con el token actual.
                     */
                    Button(
                        onClick = {
                            token?.let {
                                loading = true
                                coroutineScope.launch {
                                    try {
                                        val response = apiClient.profile(it)
                                        resultado = """
                                            ✅ Perfil obtenido
                                            Usuario: ${response.user.username}
                                            Rol: ${response.user.role}
                                        """.trimIndent()
                                    } catch (e: Exception) {
                                        resultado = "❌ Error profile: ${e.message}"
                                    } finally {
                                        loading = false
                                    }
                                }
                            } ?: run { resultado = "⚠️ Primero haz login" }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && token != null
                    ) {
                        Text("3. Ver Perfil")
                    }

                    /**
                     * 🚪 Botón para cerrar la sesión.
                     * Llama a `apiClient.logout` y limpia el token local.
                     */
                    Button(
                        onClick = {
                            token?.let {
                                loading = true
                                coroutineScope.launch {
                                    try {
                                        val response = apiClient.logout(it)
                                        resultado = "✅ ${response.message}"
                                        token = null // Limpia el token al cerrar sesión
                                    } catch (e: Exception) {
                                        resultado = "❌ Error logout: ${e.message}"
                                    } finally {
                                        loading = false
                                    }
                                }
                            } ?: run { resultado = "⚠️ Primero haz login" }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && token != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error // Color rojo para logout
                        )
                    ) {
                        Text("4. Logout")
                    }
                }
            }

            // ==================== 👥 GESTIÓN DE USUARIOS 👥 ====================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gestión de Usuarios", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    /**
                     * 📜 Botón para listar todos los usuarios.
                     * Llama a `apiClient.listUsers`.
                     */
                    Button(
                        onClick = {
                            token?.let {
                                loading = true
                                coroutineScope.launch {
                                    try {
                                        val response = apiClient.listUsers(it)
                                        resultado = """
                                            ✅ Usuarios: ${response.usuarios.size}
                                            ${response.usuarios.take(3).joinToString("\n") { u ->
                                                "- ${u.usuario} (${u.perfil})"
                                            }}
                                        """.trimIndent()
                                    } catch (e: Exception) {
                                        resultado = "❌ Error listar: ${e.message}"
                                    } finally {
                                        loading = false
                                    }
                                }
                            } ?: run { resultado = "⚠️ Primero haz login" }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && token != null
                    ) {
                        Text("5. Listar Usuarios")
                    }

                    /**
                     * 🔍 Botón para buscar usuarios con perfil "Admin".
                     * Llama a `apiClient.searchUsers` con un filtro.
                     */
                    Button(
                        onClick = {
                            token?.let {
                                loading = true
                                coroutineScope.launch {
                                    try {
                                        val response = apiClient.searchUsers(
                                            it,
                                            UserSearchParams(perfil = "Admin")
                                        )
                                        resultado = """
                                            ✅ Admins encontrados: ${response.usuarios.size}
                                            ${response.usuarios.joinToString("\n") { u ->
                                                "- ${u.usuario}"
                                            }}
                                        """.trimIndent()
                                    } catch (e: Exception) {
                                        resultado = "❌ Error buscar: ${e.message}"
                                    } finally {
                                        loading = false
                                    }
                                }
                            } ?: run { resultado = "⚠️ Primero haz login" }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && token != null
                    ) {
                        Text("6. Buscar Admins")
                    }
                }
            }

            // ==================== 📚 CATÁLOGO 📚 ====================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Catálogo", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    /**
                     * 📦 Botón para obtener el catálogo de productos.
                     * Llama a `apiClient.getCatalog`.
                     */
                    Button(
                        onClick = {
                            token?.let {
                                loading = true
                                coroutineScope.launch {
                                    try {
                                        val response = apiClient.getCatalog(it)
                                        resultado = """
                                            ✅ Catálogo obtenido
                                            Categorías: ${response.total_categorias}
                                            ${response.catalogo.take(3).joinToString("\n") { cat ->
                                                "- ${cat.nombre} (${cat.productos.size} productos)"
                                            }}
                                        """.trimIndent()
                                    } catch (e: Exception) {
                                        resultado = "❌ Error catálogo: ${e.message}"
                                    } finally {
                                        loading = false
                                    }
                                }
                            } ?: run { resultado = "⚠️ Primero haz login" }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && token != null
                    ) {
                        Text("7. Ver Catálogo")
                    }
                }
            }

            // ==================== 📊 RESULTADO Y ESTADO ====================
            // Indicador de carga visible cuando `loading` es true.
            if (loading) {
                CircularProgressIndicator()
            }

            // Muestra el mensaje de `resultado` si no está vacío.
            AnimatedVisibility(resultado.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        resultado,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Muestra el estado actual del token.
            Text(
                "Token: ${if (token != null) "✅ Guardado" else "❌ No disponible"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
