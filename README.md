# 📱 TstSiteApp

> **Aplicación multiplataforma** construida con Kotlin Multiplatform + Compose Multiplatform — un único codebase que compila a Web (WasmJS), Android, iOS y Desktop (JVM).

---

## 📌 Descripción

**TstSiteApp** es el frontend de la plataforma TstSite. Demuestra cómo un único proyecto Kotlin puede generar aplicaciones nativas para cinco targets simultáneamente, compartiendo toda la lógica de negocio, red, seguridad y UI desde `commonMain`, mientras cada plataforma aporta únicamente su implementación específica donde es necesario (criptografía nativa, cliente HTTP, punto de entrada).

La app consume la REST API de **TstSiteApi** con autenticación JWT y cifrado de payload end-to-end.

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología | Versión |
|------|-----------|---------|
| **Lenguaje** | Kotlin Multiplatform | 2.3.0 |
| **UI** | Compose Multiplatform | 1.9.3 |
| **Red** | Ktor Client | 3.3.3 |
| **Serialización** | Kotlinx Serialization (JSON) | — |
| **Concurrencia** | Kotlinx Coroutines | 1.10.2 |
| **Lifecycle / ViewModel** | AndroidX Lifecycle Compose | 2.9.6 |
| **Build** | Gradle (Kotlin DSL) + Version Catalog | — |
| **JDK** | Temurin 21 | — |
| **Android compileSdk** | 36 | — |
| **Android minSdk** | 24 | — |

---

## 🎯 Targets de compilación

```
TstSiteApp
├── 🌐 wasmJs          →  Web (WebAssembly + Kotlin/Wasm)  — desplegado en Alwaysdata/Apache
├── 🌐 js              →  Web (Kotlin/JS)
├── 🤖 androidTarget   →  APK / AAB  (minSdk 24, targetSdk 36)
├── 🍎 iosArm64        →  iPhone / iPad (dispositivo físico)
├── 🍎 iosSimulatorArm64 →  Simulador iOS (Apple Silicon)
└── 💻 jvm             →  Desktop (Windows .msi, macOS .dmg, Linux .deb)
```

Cada target comparte el **100% del código de negocio** desde `commonMain`. Solo difieren los puntos de entrada y las implementaciones `actual` de criptografía y cliente HTTP.

---

## 🏗️ Arquitectura — Kotlin Multiplatform (expect/actual)

El patrón `expect/actual` permite definir contratos en `commonMain` e implementarlos de forma nativa en cada plataforma:

```
commonMain
├── App.kt                     ← UI Compose compartida (Material 3)
├── config/AppConfig.kt        ← Configuración centralizada + descifrado Fernet
├── network/
│   ├── ApiClient.kt           ← expect class ApiClient (contrato común)
│   └── ApiClientHelper.kt     ← Lógica compartida (cifrado, deserialización)
├── model/                     ← Data classes + Kotlinx Serialization
└── utils/crypto/
    ├── Cryptor.kt             ← expect class Cryptor (Fernet)
    └── PayloadCrypto.kt       ← expect object PayloadCrypto (AES-256-GCM)

androidMain   → ApiClient.android.kt  |  Cryptor.android.kt  (AES/JCE)  |  OkHttp
iosMain       → ApiClient.ios.kt      |  Cryptor.ios.kt      (CoreCrypto, AES-CBC) |  Darwin
jvmMain       → ApiClient.jvm.kt      |  Cryptor.jvm.kt      (AES/JCE)  |  CIO
jsMain        → ApiClient.js.kt       |  Cryptor.js.kt       (Web Crypto API)
wasmJsMain    → ApiClient.wasmJs.kt   |  Cryptor.wasmJs.kt   (Web Crypto API via JS interop)
```

### Clientes HTTP por plataforma

| Target | Motor HTTP |
|--------|-----------|
| Android | Ktor + OkHttp |
| iOS | Ktor + Darwin (NSURLSession) |
| JVM / Desktop | Ktor + CIO |
| JS | Ktor + Fetch API |
| WasmJS | Ktor + Fetch API (JS interop) |

---

## 🔐 Seguridad cliente

### 1. Sin API Key en el cliente
El acceso a la API se realiza exclusivamente mediante JWT obtenido tras el login. La API Key fue eliminada de todos los clientes para reducir la superficie de ataque.

### 2. Fernet para secretos embebidos
La única clave que permanece en el cliente es la `SECRET_KEY` de cifrado de payload, guardada cifrada en formato **Fernet** (`ENC(...)`). Se descifra en memoria en tiempo de ejecución usando `Cryptor`:

```
FERNET_KEY (embebida) ──► Cryptor.decrypt("ENC(...)") ──► SECRET_KEY en memoria
```

Implementación de Fernet por plataforma:
- **Android / JVM:** `AES/CBC/PKCS5Padding` + `HmacSHA256` via JCE
- **iOS:** `CommonCrypto` (CoreCrypto framework) — AES-CBC + HMAC-SHA256
- **JS / WasmJS:** `Web Crypto API` (`SubtleCrypto.decrypt` + `SubtleCrypto.sign`)

### 3. Cifrado de payload end-to-end
Todos los cuerpos de petición y respuesta viajan cifrados con **AES-256-GCM**, independientemente de HTTPS:

- `salt` de 32 bytes aleatorios (único por mensaje)
- `IV` de 12 bytes aleatorios criptográficamente seguros
- `authTag` de 16 bytes — detecta manipulaciones en tránsito
- Formato: `Base64( salt[32] + iv[12] + ciphertext + authTag[16] )`
- Compatible con el backend Node.js (TstSiteApi)

> iOS usa AES-CBC en lugar de AES-GCM por limitaciones de las APIs de `CoreCrypto` disponibles en el target mínimo del proyecto.

### 4. Control de entornos
`AppConfig` centraliza las URLs y permite alternar entre `DEV` (localhost) y `TST` (cloud) con una sola constante `IS_DEV`. En Android, detecta el emulador y apunta a `10.0.2.2:3000` automáticamente.

---

## 📁 Estructura del proyecto

```
TstSiteApp/
├── composeApp/
│   ├── build.gradle.kts                   # Configuración Kotlin Multiplatform
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── App.kt                     # UI principal (Compose Material 3)
│       │   ├── Platform.kt                # expect interface Platform
│       │   ├── config/AppConfig.kt        # URLs, entornos, descifrado de clave
│       │   ├── model/                     # Data classes serializables
│       │   ├── network/
│       │   │   ├── ApiClient.kt           # expect class ApiClient
│       │   │   └── ApiClientHelper.kt     # Shared: cifrado, descifrado, HttpClient
│       │   └── utils/crypto/
│       │       ├── Cryptor.kt             # expect class Cryptor (Fernet)
│       │       └── PayloadCrypto.kt       # expect object PayloadCrypto (AES-GCM)
│       ├── androidMain/                   # actual Android: OkHttp, JCE, MainActivity
│       ├── iosMain/                       # actual iOS: Darwin, CoreCrypto, UIViewController
│       ├── jvmMain/                       # actual JVM: CIO, JCE, Window Desktop
│       ├── jsMain/                        # actual JS: Fetch API, Web Crypto
│       └── wasmJsMain/                    # actual WasmJS: JS interop, Web Crypto
├── gradle/
│   └── libs.versions.toml                 # Version catalog centralizado
└── gradlew
```

---

## 🌐 Funcionalidades de la UI

La app incluye una interfaz de demostración completa construida con **Compose Material 3** que cubre todos los flujos de la API:

| Módulo | Funciones |
|--------|-----------|
| **Autenticación** | Login (→ obtiene JWT), Validar token, Ver perfil, Logout (revoca token) |
| **Usuarios** | Listar todos, Búsqueda parametrizada (por perfil u otros filtros) |
| **Catálogo** | Obtener catálogo completo con categorías y productos |

La UI usa `rememberCoroutineScope` + `coroutineScope.launch` para las llamadas asíncronas, con indicador `CircularProgressIndicator` durante la carga y `AnimatedVisibility` para mostrar resultados.

---

## 🔄 Build y despliegue

### Compilación WasmJS (producción)
```bash
./gradlew clean
./gradlew :composeApp:wasmJsBrowserDistribution --no-daemon
# Output: composeApp/build/dist/wasmJs/productionExecutable/
```

### Distribuciones Desktop
```bash
./gradlew packageMsi      # Windows
./gradlew packageDmg      # macOS
./gradlew packageDeb      # Linux
```

### Android
```bash
./gradlew assembleRelease
```

> El despliegue del build WasmJS a producción es automático mediante el pipeline de **GitHub Actions** del repositorio central. Ver `TstSiteApi` para los detalles del CI/CD.

---

## ⚙️ Configuración de entornos

```kotlin
// composeApp/src/commonMain/.../config/AppConfig.kt

private const val IS_DEV = false          // true → localhost, false → cloud
private const val BASE_URL_TST = "https://tstsite.alwaysdata.net"

// En Android con emulador, apunta a 10.0.2.2 en lugar de localhost
fun getBaseUrl(isAndroid: Boolean = false): String
```

---

## 🔗 Repositorios relacionados

| Repositorio | Descripción |
|-------------|-------------|
| `TstSiteApi` | Backend REST API (Node.js + Express + PostgreSQL) |
| `TstSiteDB` | Scripts SQL y migraciones PostgreSQL |
| `TstSiteApp` | **Este repositorio** — Frontend Kotlin Multiplatform |

---

## 📄 Licencia

MIT
