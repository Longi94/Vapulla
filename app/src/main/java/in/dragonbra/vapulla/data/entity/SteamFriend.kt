package `in`.dragonbra.vapulla.data.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "steam_friend")
data class SteamFriend (
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
        @ColumnInfo(name = "typing_timestamp") var typingTs: Long
) {
    @Ignore
    constructor(id: Long) : this(id, null, null, 0, null, 0, null, 0, 0, 0, 0L)
}