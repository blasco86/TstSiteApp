package org.tstsite.tstsiteapp

/**
 * 🌍 Interfaz que representa la plataforma actual de ejecución.
 *
 * Cada plataforma (Android, iOS, JVM, JS, etc.) debe implementar esta interfaz
 * para proporcionar información específica de su entorno.
 */
interface Platform {
    /**
     * 🏷️ El nombre de la plataforma.
     * Por ejemplo: "Android", "iOS", "JVM", "Browser".
     */
    val name: String
}

/**
 * 🏭 Función `expect` para obtener la implementación de [Platform] para el entorno actual.
 *
 * Esta función es implementada por cada módulo específico de plataforma (`actual`).
 * Permite que el código común acceda a información de la plataforma de forma abstracta.
 *
 * @return Una instancia de [Platform] que representa el entorno de ejecución.
 */
expect fun getPlatform(): Platform