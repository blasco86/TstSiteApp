package org.tstsite.tstsiteapp

/**
 * 👋 Clase de ejemplo para generar un saludo específico de la plataforma.
 *
 * Utiliza la interfaz [Platform] para obtener el nombre del entorno de ejecución
 * y construir un mensaje de saludo personalizado.
 */
class Greeting {
    // 🌍 Obtiene la implementación de la plataforma actual.
    private val platform = getPlatform()

    /**
     * 💬 Genera un mensaje de saludo.
     *
     * El mensaje incluye el nombre de la plataforma en la que se está ejecutando la aplicación.
     *
     * @return Una cadena de texto con el saludo, por ejemplo: "Hello, Android!".
     */
    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}