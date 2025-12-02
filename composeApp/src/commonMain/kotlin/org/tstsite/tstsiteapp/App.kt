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