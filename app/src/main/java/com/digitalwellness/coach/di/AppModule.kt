package com.digitalwellness.coach.di

import android.content.Context
import androidx.room.Room
import com.digitalwellness.coach.data.local.WellnessDatabase
import com.digitalwellness.coach.data.local.dao.*
import com.digitalwellness.coach.data.repository.*
import com.digitalwellness.coach.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WellnessDatabase =
        Room.databaseBuilder(context, WellnessDatabase::class.java, WellnessDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAppUsageDao(db: WellnessDatabase): AppUsageDao = db.appUsageDao()
    @Provides fun provideDailyUsageDao(db: WellnessDatabase): DailyUsageDao = db.dailyUsageDao()
    @Provides fun provideGoalDao(db: WellnessDatabase): GoalDao = db.goalDao()
    @Provides fun provideFocusSessionDao(db: WellnessDatabase): FocusSessionDao = db.focusSessionDao()
    @Provides fun provideAchievementDao(db: WellnessDatabase): AchievementDao = db.achievementDao()
    @Provides fun provideNotificationDao(db: WellnessDatabase): NotificationDao = db.notificationDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindUsageRepository(impl: UsageRepositoryImpl): UsageRepository

    @Binds @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds @Singleton
    abstract fun bindFocusRepository(impl: FocusRepositoryImpl): FocusRepository

    @Binds @Singleton
    abstract fun bindAchievementRepository(impl: AchievementRepositoryImpl): AchievementRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}
