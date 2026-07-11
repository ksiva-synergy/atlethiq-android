package com.example.atlethiq.di

import android.content.Context
import androidx.room.Room
import com.example.atlethiq.data.db.MetricSampleDao
import com.example.atlethiq.data.db.DailySnapshotDao
import com.example.atlethiq.data.db.CallFeedbackDao
import com.example.atlethiq.data.db.AtlethiqDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AtlethiqDatabase {
        return Room.databaseBuilder(
            context,
            AtlethiqDatabase::class.java,
            "atlethiq_database"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    fun provideMetricSampleDao(database: AtlethiqDatabase): MetricSampleDao {
        return database.metricSampleDao()
    }

    @Provides
    fun provideDailySnapshotDao(database: AtlethiqDatabase): DailySnapshotDao {
        return database.dailySnapshotDao()
    }

    @Provides
    fun provideCallFeedbackDao(database: AtlethiqDatabase): CallFeedbackDao {
        return database.callFeedbackDao()
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(@ApplicationContext context: Context): SupabaseClient {
        val urlResId = context.resources.getIdentifier("SUPABASE_URL", "string", context.packageName)
        val keyResId = context.resources.getIdentifier("SUPABASE_ANON_KEY", "string", context.packageName)
        val supabaseUrl = if (urlResId != 0) context.getString(urlResId) else "https://placeholder-url.supabase.co"
        val supabaseKey = if (keyResId != 0) context.getString(keyResId) else "placeholder-key"
        
        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Postgrest)
            install(Auth)
            install(Functions)
        }
    }

    @Provides
    @Singleton
    fun provideDataRepository(
        dailySnapshotDao: DailySnapshotDao,
        metricSampleDao: MetricSampleDao,
        supabaseClient: SupabaseClient
    ): com.example.atlethiq.data.DataRepository {
        return com.example.atlethiq.data.DefaultDataRepository(
            dailySnapshotDao,
            metricSampleDao,
            supabaseClient
        )
    }
}
