package `in`.dragonbra.vapulla.module

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
}