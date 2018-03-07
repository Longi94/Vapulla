package `in`.dragonbra.vapulla.module

import `in`.dragonbra.vapulla.data.dao.GameSchemaDao
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.retrofit.ISteamUserStats
import android.content.Context
import android.support.v4.app.NotificationManagerCompat
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(val context: Context) {

    @Provides
    @Singleton
    fun provideContext() = context

    @Provides
    @Singleton
    fun provideNotificationManager(context: Context) = NotificationManagerCompat.from(context)

    @Provides
    @Singleton
    fun provideGameSchemaManager(gameSchemaDao: GameSchemaDao, steamUserStats: ISteamUserStats)
            = GameSchemaManager(gameSchemaDao, steamUserStats)
}