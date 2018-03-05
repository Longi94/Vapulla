package `in`.dragonbra.vapulla.data.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index

@Entity(tableName = "chat_message", indices = [Index("friend_id")], primaryKeys = ["message", "timestamp", "friend_id", "from_local"])
data class ChatMessage(
        @ColumnInfo(name = "message") var message: String,
        @ColumnInfo(name = "timestamp") var timestamp: Long,
        @ColumnInfo(name = "friend_id") var friendId: Long,
        @ColumnInfo(name = "from_local") var fromLocal: Boolean,
        @ColumnInfo(name = "delivered") var delivered: Boolean
)