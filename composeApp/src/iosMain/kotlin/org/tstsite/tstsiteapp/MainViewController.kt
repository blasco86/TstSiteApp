package org.tstsite.tstsiteapp

import androidx.compose.ui.window.ComposeUIViewController

/**
 * 🚀 Punto de entrada principal para la aplicación iOS.
 *
 * Esta función crea un `UIViewController` de Compose que aloja la interfaz
 * de usuario definida por el componente [App]. Es el punto de inicio de la
 * aplicación en la plataforma iOS.
 *
 * @return Un `UIViewController` configurado para mostrar el contenido de Compose.
 */
fun MainViewController() = ComposeUIViewController { App() }