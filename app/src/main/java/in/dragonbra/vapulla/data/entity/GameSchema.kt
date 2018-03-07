package `in`.dragonbra.vapulla.data.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "game_schema")
data class GameSchema(
        @ColumnInfo(name = "id") @PrimaryKey var id: Int,
        @ColumnInfo(name = "name") var name: String?,
        @ColumnInfo(name = "modify_date") var modifyDate: Long
)