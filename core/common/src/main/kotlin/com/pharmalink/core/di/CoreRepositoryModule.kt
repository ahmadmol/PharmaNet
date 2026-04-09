package com.pharmalink.core.di

import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.core.repository.DeliveryRepository
import com.pharmalink.core.repository.NotificationRepository
import com.pharmalink.core.repository.OrderRepository
import com.pharmalink.core.repository.PharmaBackedDeliveryRepository
import com.pharmalink.core.repository.PharmaBackedNotificationRepository
import com.pharmalink.core.repository.PharmaBackedOrderRepository
import com.pharmalink.core.repository.PharmaBackedRequestRepository
import com.pharmalink.core.repository.PharmaBackedWarehouseRepository
import com.pharmalink.core.repository.RequestRepository
import com.pharmalink.core.repository.SupabaseAuthRepository
import com.pharmalink.core.repository.WarehouseRepository
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.data.repository.SupabasePharmaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings: [PharmaRepository] is the app data source; feature repositories delegate to it.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPharmaRepository(
        impl: SupabasePharmaRepository,
    ): PharmaRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: SupabaseAuthRepository,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        impl: PharmaBackedOrderRepository,
    ): OrderRepository

    @Binds
    @Singleton
    abstract fun bindWarehouseRepository(
        impl: PharmaBackedWarehouseRepository,
    ): WarehouseRepository

    @Binds
    @Singleton
    abstract fun bindRequestRepository(
        impl: PharmaBackedRequestRepository,
    ): RequestRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: PharmaBackedNotificationRepository,
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindDeliveryRepository(
        impl: PharmaBackedDeliveryRepository,
    ): DeliveryRepository
}
