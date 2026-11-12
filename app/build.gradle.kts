import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

fun localValue(name: String): String {
    return localProperties.getProperty(name, "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

plugins {
    id("com.android.application")
}

android {
    namespace = "com.team.classroomnotes"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.team.classroomnotes"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"

        buildConfigField("String", "AI_ENDPOINT", "\"${localValue("AI_ENDPOINT")}\"")
        buildConfigField("String", "AI_MODEL", "\"${localValue("AI_MODEL")}\"")
        buildConfigField("String", "AI_API_KEY", "\"${localValue("AI_API_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.customview:customview:1.1.0")
    implementation("androidx.drawerlayout:drawerlayout:1.1.1")
}

