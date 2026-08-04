package com.subkan.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.subkan.core.time.AppClock
import com.subkan.core.time.SystemAppClock
import com.subkan.data.local.MIGRATION_1_2
import com.subkan.data.local.SubKanDatabase
import com.subkan.data.local.dao.PaymentCardDao
import com.subkan.data.local.dao.SubscriptionDao
import com.subkan.data.reminder.AlarmReminderScheduler
import com.subkan.data.reminder.ReminderScheduler
import com.subkan.data.repository.OfflinePaymentCardRepository
import com.subkan.data.repository.OfflineSubscriptionRepository
import com.subkan.data.repository.PaymentCardRepository
import com.subkan.data.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SubKanDatabase =
        Room.databaseBuilder(context, SubKanDatabase::class.java, SubKanDatabase.NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun providePaymentCardDao(database: SubKanDatabase): PaymentCardDao = database.paymentCardDao()

    @Provides
    fun provideSubscriptionDao(database: SubKanDatabase): SubscriptionDao =
        database.subscriptionDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { File(context.filesDir, "datastore/subkan.preferences_pb") },
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPaymentCardRepository(
        impl: OfflinePaymentCardRepository,
    ): PaymentCardRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        impl: OfflineSubscriptionRepository,
    ): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindAppClock(impl: SystemAppClock): AppClock

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: AlarmReminderScheduler): ReminderScheduler
}
