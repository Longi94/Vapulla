package `in`.dragonbra.vapulla.data

import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = arrayOf(SteamFriend::class), version = 1, exportSchema = true)
abstract class VapullaDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "vapulla.db"
    }
    abstract fun steamFriendDao(): SteamFriendDao
}