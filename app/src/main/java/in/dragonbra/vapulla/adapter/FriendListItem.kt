package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.javasteam.util.Strings
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.PrimaryKey

data class FriendListItem(
        @PrimaryKey var id: Long,
        @ColumnInfo(name = "name") var name: String?,
        @ColumnInfo(name = "avatar") var avatar: String?,
        @ColumnInfo(name = "relation") var relation: Int,
        @ColumnInfo(name = "state") var state: Int?,
        @ColumnInfo(name = "game_app_id") var gameAppId: Int,
        @ColumnInfo(name = "playing_game_name") var gameName: String?,
        @ColumnInfo(name = "last_log_on") var lastLogOn: Long,
        @ColumnInfo(name = "last_log_off") var lastLogOff: Long,
        @ColumnInfo(name = "state_flags") var stateFlags: Int,
        @ColumnInfo(name = "typing_timestamp") var typingTs: Long,
        @ColumnInfo(name = "last_message") var lastMessage: String?,
        @ColumnInfo(name = "last_message_time") var lastMessageTime: Long?,
        @ColumnInfo(name = "last_message_unread") var lastMessageUnread: Boolean?,
        @ColumnInfo(name = "nickname") var nickname: String?
) {

    fun isInGame() = gameAppId > 0 || !Strings.isNullOrEmpty(gameName)

    fun isOnline() = state?.let { it > 0 } ?: false
}