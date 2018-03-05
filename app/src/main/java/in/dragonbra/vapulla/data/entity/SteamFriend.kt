package `in`.dragonbra.vapulla.data.entity

import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.util.Utils
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.Nullable

@Entity(tableName = "steam_friend")
data class SteamFriend(
        @PrimaryKey var id: Long,
        @ColumnInfo(name = "name") @Nullable var name: String?,
        @ColumnInfo(name = "avatar") @Nullable var avatar: String?,
        @ColumnInfo(name = "relation") @Nullable var relation: Int?,
        @ColumnInfo(name = "state") @Nullable var state: Int?,
        @ColumnInfo(name = "game_name") @Nullable var gameName: String?,
        @ColumnInfo(name = "last_log_on") @Nullable var lastLogOn: Long?,
        @ColumnInfo(name = "last_log_off") @Nullable var lastLogOff: Long?
) {
    @Ignore
    constructor(id: Long) : this(id, null, null, null, null, null, null, null)

    fun getAvatarUrl() = Utils.getAvatarUrl(avatar)

    fun getSteamId() = SteamID(id)
}