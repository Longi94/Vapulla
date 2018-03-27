package `in`.dragonbra.vapulla.module

import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class StorageModule {

    @Provides
    @Singleton
    fun provideAccountManage(context: Context) = AccountManager(context)

    @Provides
    @Singleton
    fun provideVapullaDatabase(context: Context) =
            Room.databaseBuilder(context, VapullaDatabase::class.java, VapullaDatabase.DATABASE_NAME).build()

    @Provides
    @Singleton
    fun provideSteamFriendDao(db: VapullaDatabase) = db.steamFriendDao()

    @Provides
    @Singleton
    fun provideChatMessageDao(db: VapullaDatabase) = db.chatMessageDao()

    @Provides
    @Singleton
    fun provideGameSchemaDao(db: VapullaDatabase) = db.gameSchemaDao()

    @Provides
    @Singleton
    fun provideEmoticonDao(db: VapullaDatabase) = db.emoticonDao()
}