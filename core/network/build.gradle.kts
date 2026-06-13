plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.pharmalink.core.network"
    compileSdk {
        version = release(36)
    }
    defaultConfig {
        minSdk = 25
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Expose Firebase types to consumers so Hilt/KSP can resolve bindings (e.g. FirebaseFirestore) from :app
    api(platform(libs.firebase.bom))
    api(libs.firebase.firestore)
    api(libs.firebase.messaging)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
}
