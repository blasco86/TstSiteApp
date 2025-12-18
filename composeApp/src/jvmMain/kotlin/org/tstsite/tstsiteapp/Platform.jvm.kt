package org.tstsite.tstsiteapp

/**
 * 💻 Implementación JVM de la interfaz [Platform].
 *
 * Esta clase proporciona el nombre específico de la plataforma Java Virtual Machine (JVM),
 * incluyendo la versión de Java.
 */
class JVMPlatform: Platform {
    /**
     * 🏷️ El nombre de la plataforma JVM, incluyendo la versión de Java.
     * Por ejemplo: "Java 17.0.8".
     */
    override val name: String = "Java ${System.getProperty("java.version")}"
}

/**
 * 🏭 Función `actual` para obtener la implementación de [Platform] en JVM.
 *
 * Esta función es la implementación concreta de `getPlatform()` definida en `commonMain`.
 * Devuelve una instancia de [JVMPlatform].
 *
 * @return Una instancia de [JVMPlatform].
 */
actual fun getPlatform(): Platform = JVMPlatform()