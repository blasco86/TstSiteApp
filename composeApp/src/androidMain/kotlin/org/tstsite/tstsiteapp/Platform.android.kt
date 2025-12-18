package org.tstsite.tstsiteapp

import android.os.Build

/**
 * 🤖 Implementación Android de la interfaz [Platform].
 *
 * Esta clase proporciona el nombre específico de la plataforma Android,
 * incluyendo la versión del SDK.
 */
class AndroidPlatform : Platform {
    /**
     * 🏷️ El nombre de la plataforma Android, incluyendo la versión del SDK.
     * Por ejemplo: "Android 33".
     */
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/**
 * 🏭 Función `actual` para obtener la implementación de [Platform] en Android.
 *
 * Esta función es la implementación concreta de `getPlatform()` definida en `commonMain`.
 * Devuelve una instancia de [AndroidPlatform].
 *
 * @return Una instancia de [AndroidPlatform].
 */
actual fun getPlatform(): Platform = AndroidPlatform()