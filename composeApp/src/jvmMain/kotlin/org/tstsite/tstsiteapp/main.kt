package org.tstsite.tstsiteapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/**
 * 🚀 Punto de entrada principal para la aplicación de escritorio JVM.
 *
 * Esta función inicia la aplicación Compose para escritorio, creando una ventana
 * y cargando el componente principal de la aplicación [App].
 */
fun main() = application {
    /**
     * 🖼️ Define la ventana principal de la aplicación.
     *
     * @param onCloseRequest Acción a realizar cuando se solicita cerrar la ventana (salir de la aplicación).
     * @param title El título que se mostrará en la barra de título de la ventana.
     */
    Window(
        onCloseRequest = ::exitApplication, // Cierra la aplicación al cerrar la ventana
        title = "TstSiteApp", // Título de la ventana
    ) {
        App() // Carga el componente principal de Compose dentro de la ventana
    }
}