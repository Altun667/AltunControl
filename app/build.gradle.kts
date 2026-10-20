plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
// Surum numarasi GitHub etiketinden gelir (ornek: 2.0.0.1), her yeni etiketle otomatik artar
val verName = ((project.findProperty("appVersion") as String?) ?: "2.2.0.0").trim().trimStart('v', 'V')
val verParts = verName.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 } + listOf(0, 0, 0, 0)
val verCode = verParts[0] * 1000000 + verParts[1] * 10000 + verParts[2] * 100 + verParts[3]
android {
    namespace = "com.altuncontrol.app"
    compileSdk = 34
    defaultConfig { applicationId = "com.altuncontrol.app"; minSdk = 26; targetSdk = 34; versionCode = verCode; versionName = verName }
    signingConfigs {
        create("altun") {
            // Anahtar repoda tutulmaz: GitHub gizli kasasindan (Secrets) ortam degiskeni olarak gelir
            val ks = System.getenv("KEYSTORE_FILE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("debug") {
            if (System.getenv("KEYSTORE_FILE") != null) signingConfig = signingConfigs.getByName("altun")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies { implementation("androidx.core:core:1.13.1") }
