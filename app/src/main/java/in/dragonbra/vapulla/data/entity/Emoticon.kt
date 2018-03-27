package `in`.dragonbra.vapulla.data.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "emoticon")
data class Emoticon(@PrimaryKey val name: String)