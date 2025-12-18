package org.tstsite.tstsiteapp

/**
 * 🌐 Implementación JavaScript de la interfaz [Platform].
 *
 * Esta clase proporciona el nombre específico de la plataforma web,
 * indicando que se está ejecutando con Kotlin/JS.
 */
class JsPlatform: Platform {
    /**
     * 🏷️ El nombre de la plataforma JavaScript.
     * Por ejemplo: "Web with Kotlin/JS".
     */
    override val name: String = "Web with Kotlin/JS"
}

/**
 * 🏭 Función `actual` para obtener la implementación de [Platform] en JavaScript.
 *
 * Esta función es la implementación concreta de `getPlatform()` definida en `commonMain`.
 * Devuelve una instancia de [JsPlatform].
 *
 * @return Una instancia de [JsPlatform].
 */
actual fun getPlatform(): Platform = JsPlatform()