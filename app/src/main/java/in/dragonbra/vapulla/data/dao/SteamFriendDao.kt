package `in`.dragonbra.vapulla.data.dao

import `in`.dragonbra.vapulla.data.entity.SteamFriend
import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*

@Dao
interface SteamFriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg steamFriends: SteamFriend)

    @Query("SELECT * FROM steam_friend WHERE id = :id")
    fun find(id: Long): SteamFriend?

    @Update
    fun update(vararg steamFriends: SteamFriend)

    @Query("SELECT * FROM steam_friend")
    fun getAllObservable(): LiveData<List<SteamFriend>>
}