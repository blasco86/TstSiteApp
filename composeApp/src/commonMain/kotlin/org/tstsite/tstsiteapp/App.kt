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

@Composable
@Preview
fun App() {
    MaterialTheme {
        var token by remember { mutableStateOf<String?>(null) }
        var resultado by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(false) }
        val apiClient = remember { ApiClient() }
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "TstSite API Demo",
                style = MaterialTheme.typography.headlineMedium
            )

            // ==================== AUTH ====================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Autenticación", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

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
                        enabled = !loading
                    ) {
                        Text("1. Login")
                    }

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
                        enabled = !loading && token != null
                    ) {
                        Text("2. Validar Token")
                    }

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

                    Button(
                        onClick = {
                            token?.let {
                                loading = true
                                coroutineScope.launch {
                                    try {
                                        val response = apiClient.logout(it)
                                        resultado = "✅ ${response.message}"
                                        token = null
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
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("4. Logout")
                    }
                }
            }

            // ==================== USERS ====================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gestión de Usuarios", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

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

            // ==================== CATALOG ====================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Catálogo", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

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

            // ==================== RESULTADO ====================
            if (loading) {
                CircularProgressIndicator()
            }

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

            Text(
                "Token: ${if (token != null) "✅ Guardado" else "❌ No disponible"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
/*
package org.tstsite.tstsiteapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import tstsiteapp.composeapp.generated.resources.Res
import tstsiteapp.composeapp.generated.resources.compose_multiplatform
import org.tstsite.tstsiteapp.model.SesionRequest
import org.tstsite.tstsiteapp.model.SesionResponse
import org.tstsite.tstsiteapp.network.ApiClient

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        var resultMsj by remember { mutableStateOf("") }
        val apiClient = remember { ApiClient() }
        var responseSesion by remember { mutableStateOf<SesionResponse?>(null) }
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = {
                showContent = !showContent

                coroutineScope.launch {
                    try {
                        val sesionRequest = SesionRequest("adminTstSite", "adminBlasco86")
                        responseSesion = apiClient.login(sesionRequest)
                        resultMsj = "Sesión iniciada"

                    } catch (e: Exception) {
                        //e.printStackTrace()
                        //resultMsj = "Error al desencriptar: ${e.message}"
                        resultMsj = "Error al iniciar sesión: ${e.message}"
                    }
                }
            }) {
                Text("Click me!")
            }

            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")


                    responseSesion?.user?.let { user ->
                        Text("Perfil: ${user.perfil}")
                        Text("Usuario: ${user.usuario}")
                    }
                    Text(resultMsj)
                }
            }
        }
    }
}
*/