package com.pharmalink.core.location

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FacilityLocationModule {

    @Binds
    @Singleton
    abstract fun bindFacilityLocationService(
        impl: AndroidFacilityLocationService,
    ): FacilityLocationService
}

