package org.tstsite.tstsiteapp

/**
 * 🕸️ Implementación WasmJs de la interfaz [Platform].
 *
 * Esta clase proporciona el nombre específico de la plataforma WebAssembly,
 * indicando que se está ejecutando con Kotlin/Wasm.
 */
class WasmPlatform: Platform {
    /**
     * 🏷️ El nombre de la plataforma WebAssembly.
     * Por ejemplo: "Web with Kotlin/Wasm".
     */
    override val name: String = "Web with Kotlin/Wasm"
}

/**
 * 🏭 Función `actual` para obtener la implementación de [Platform] en WasmJs.
 *
 * Esta función es la implementación concreta de `getPlatform()` definida en `commonMain`.
 * Devuelve una instancia de [WasmPlatform].
 *
 * @return Una instancia de [WasmPlatform].
 */
actual fun getPlatform(): Platform = WasmPlatform()