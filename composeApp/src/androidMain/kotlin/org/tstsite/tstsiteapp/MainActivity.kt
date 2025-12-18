package org.tstsite.tstsiteapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * 🚀 Actividad principal de la aplicación Android.
 *
 * Esta es la actividad de entrada para la aplicación en la plataforma Android.
 * Se encarga de configurar la vista y cargar el contenido de Compose.
 */
class MainActivity : ComponentActivity() {
    /**
     * 🎨 Se llama cuando la actividad es creada por primera vez.
     *
     * Configura el modo de pantalla completa (`enableEdgeToEdge`) y establece
     * el contenido de la interfaz de usuario utilizando la función `App()` de Compose.
     *
     * @param savedInstanceState Si la actividad se está recreando después de que
     *     su estado anterior fuera guardado, este es el estado.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge() // Habilita el modo de pantalla completa (edge-to-edge)
        super.onCreate(savedInstanceState)

        setContent {
            App() // Carga el componente principal de Compose
        }
    }
}

/**
 * 🖼️ Vista previa de la aplicación Android en Compose.
 *
 * Esta función Composable permite visualizar el componente `App()` en el editor
 * de Android Studio sin necesidad de ejecutar la aplicación en un dispositivo.
 */
@Preview
@Composable
fun AppAndroidPreview() {
    App()
}