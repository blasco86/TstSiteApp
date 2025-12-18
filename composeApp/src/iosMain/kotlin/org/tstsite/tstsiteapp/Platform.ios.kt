package org.tstsite.tstsiteapp

import platform.UIKit.UIDevice

/**
 * 🍎 Implementación iOS de la interfaz [Platform].
 *
 * Esta clase proporciona el nombre específico de la plataforma iOS,
 * incluyendo el nombre del sistema y su versión.
 */
class IOSPlatform: Platform {
    /**
     * 🏷️ El nombre de la plataforma iOS, incluyendo el nombre del sistema y su versión.
     * Por ejemplo: "iOS 17.0".
     */
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

/**
 * 🏭 Función `actual` para obtener la implementación de [Platform] en iOS.
 *
 * Esta función es la implementación concreta de `getPlatform()` definida en `commonMain`.
 * Devuelve una instancia de [IOSPlatform].
 *
 * @return Una instancia de [IOSPlatform].
 */
actual fun getPlatform(): Platform = IOSPlatform()