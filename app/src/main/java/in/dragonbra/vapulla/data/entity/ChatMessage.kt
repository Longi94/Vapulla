package `in`.dragonbra.vapulla.data.entity

import android.arch.persistence.room.*

@Entity(tableName = "chat_message", indices = [Index("friend_id")])
data class ChatMessage(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Long,
        @ColumnInfo(name = "message") var message: String,
        @ColumnInfo(name = "timestamp") var timestamp: Long,
        @ColumnInfo(name = "friend_id") var friendId: Long,
        @ColumnInfo(name = "from_local") var fromLocal: Boolean,
        @ColumnInfo(name = "unread") var unread: Boolean,
        @ColumnInfo(name = "timestamp_confirmed") var timestampConfirmed: Boolean
) {
    @Ignore
    constructor(message: String, timestamp: Long, friendId: Long, fromLocal: Boolean, unread: Boolean, timestampConfirmed: Boolean)
            : this(0L, message, timestamp, friendId, fromLocal, unread, timestampConfirmed)
}