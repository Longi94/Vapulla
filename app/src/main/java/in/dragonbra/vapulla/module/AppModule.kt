package `in`.dragonbra.vapulla.module

import `in`.dragonbra.vapulla.data.dao.GameSchemaDao
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.overlay.BubbleManager
import `in`.dragonbra.vapulla.retrofit.StoreFront
import android.content.Context
import android.support.v4.app.NotificationManagerCompat
import android.view.WindowManager
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
    fun provideWindowManager(context: Context) =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    @Provides
    @Singleton
    fun provideGameSchemaManager(gameSchemaDao: GameSchemaDao, storeFront: StoreFront)
            = GameSchemaManager(gameSchemaDao, storeFront)

    @Provides
    @Singleton
    fun provideBubbleManager(context: Context, windowManager: WindowManager)
            = BubbleManager(context, windowManager)
}