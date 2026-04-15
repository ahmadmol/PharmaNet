package com.pharmalink.core.di

import com.pharmalink.core.common.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        val supabaseUrl = BuildConfig.SUPABASE_URL.trim()
        val supabaseKey = BuildConfig.SUPABASE_KEY.trim()

        check(supabaseUrl.isNotBlank()) {
            "SUPABASE_URL is blank. Define SUPABASE_URL before starting the app."
        }
        check(supabaseKey.isNotBlank()) {
            "SUPABASE_KEY is blank. Define SUPABASE_KEY before starting the app."
        }

        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey,
        ) {
            httpEngine = OkHttp.create()
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
