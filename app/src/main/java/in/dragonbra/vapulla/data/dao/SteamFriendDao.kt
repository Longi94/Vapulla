package `in`.dragonbra.vapulla.data.dao

import `in`.dragonbra.vapulla.adapter.FriendListItem
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

    @Query("SELECT " +
            "  sf.*, " +
            "  cm.message as last_message, " +
            "  cm.timestamp as last_message_time, " +
            "  ifnull(max(cm.id), 0) as lm " +
            "FROM steam_friend sf " +
            "LEFT JOIN chat_message cm " +
            "ON sf.id = cm.friend_id " +
            "WHERE sf.relation = 2 " +
            "   OR sf.relation = 3 " +
            "GROUP BY sf.id " +
            "ORDER BY relation ASC, " +
            "         lm DESC, " +
            "         last_log_on DESC")
    fun getLive(): LiveData<List<FriendListItem>>
}