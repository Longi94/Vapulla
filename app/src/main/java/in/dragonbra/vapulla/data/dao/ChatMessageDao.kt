package `in`.dragonbra.vapulla.data.dao

import `in`.dragonbra.vapulla.data.entity.ChatMessage
import android.arch.paging.DataSource
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_message WHERE message = :message AND timestamp = :timestamp AND friend_id = :friendId AND from_local = :fromLocal")
    fun find(message: String, timestamp: Long, friendId: Long, fromLocal: Boolean): ChatMessage?

    @Insert
    fun insert(vararg messages: ChatMessage)

    @Query("SELECT * FROM chat_message WHERE friend_id = :friendId ORDER BY timestamp DESC")
    fun findLivePaged(friendId: Long): DataSource.Factory<Int, ChatMessage>
}