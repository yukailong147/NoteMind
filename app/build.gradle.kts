import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

fun configValue(name: String, fallback: String = ""): String {
    val projectOverride = project.findProperty(name) as String?
    return (projectOverride ?: localProperties.getProperty(name, fallback))
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
        versionCode = 10
        versionName = "1.0.0"

        buildConfigField(
            "String",
            "AI_ENDPOINT",
            "\"${configValue("AI_ENDPOINT", "https://api.deepseek.com/chat/completions")}\""
        )
        buildConfigField(
            "String",
            "AI_IMAGE_MODEL",
            "\"${configValue("AI_IMAGE_MODEL", "deepseek-flash")}\""
        )
        buildConfigField("String", "AI_API_KEY", "\"${configValue("AI_API_KEY")}\"")
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
    testImplementation("junit:junit:4.13.2")
}

