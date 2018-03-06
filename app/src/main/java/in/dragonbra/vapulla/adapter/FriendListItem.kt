package `in`.dragonbra.vapulla.adapter

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.PrimaryKey

data class FriendListItem(
        @PrimaryKey var id: Long,
        @ColumnInfo(name = "name") var name: String?,
        @ColumnInfo(name = "avatar") var avatar: String?,
        @ColumnInfo(name = "relation") var relation: Int,
        @ColumnInfo(name = "state") var state: Int?,
        @ColumnInfo(name = "game_app_id") var gameAppId: Int,
        @ColumnInfo(name = "game_name") var gameName: String?,
        @ColumnInfo(name = "last_log_on") var lastLogOn: Long,
        @ColumnInfo(name = "last_log_off") var lastLogOff: Long,
        @ColumnInfo(name = "state_flags") var stateFlags: Int,
        @ColumnInfo(name = "last_message") var lastMessage: String?,
        @ColumnInfo(name = "last_message_time") var lastMessageTime: Long?
)