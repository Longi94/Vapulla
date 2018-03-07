package `in`.dragonbra.vapulla.data

import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.GameSchemaDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.GameSchema
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = [SteamFriend::class, ChatMessage::class, GameSchema::class], version = 1, exportSchema = true)
abstract class VapullaDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "vapulla.db"
    }

    abstract fun steamFriendDao(): SteamFriendDao

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun gameSchemaDao(): GameSchemaDao
}