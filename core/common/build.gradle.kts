import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.pharmalink.core.common"
    compileSdk {
        version = release(36)
    }
    defaultConfig {
        minSdk = 25
        consumerProguardFiles("consumer-rules.pro")

        val localProps = rootProject.file("local.properties")
        val props = Properties()
        if (localProps.exists()) {
            localProps.inputStream().use { stream -> props.load(stream) }
        }
        val supabaseUrl = props.getProperty("SUPABASE_URL", "")
        val supabaseKey = props.getProperty("SUPABASE_KEY", "")
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl.replace("\"", "\\\"")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${supabaseKey.replace("\"", "\\\"")}\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.okhttp)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
