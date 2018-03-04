package `in`.dragonbra.vapulla.data.entity

import `in`.dragonbra.javasteam.types.SteamID
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

    fun getAvatarUrl() = if (avatar == null || avatar == "0000000000000000000000000000000000000000") {
        "http://cdn.akamai.steamstatic.com/steamcommunity/public/images/avatars/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"
    } else {
        "http://cdn.akamai.steamstatic.com/steamcommunity/public/images/avatars/${avatar?.substring(0, 2)}/${avatar}_full.jpg"
    }

    fun getSteamId() = SteamID(id)
}