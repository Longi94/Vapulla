package `in`.dragonbra.vapulla.data.dao

import `in`.dragonbra.vapulla.data.entity.GameSchema
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

@Dao
interface GameSchemaDao {
    @Query("SELECT * FROM game_schema WHERE id = :id")
    fun find(id: Int): GameSchema?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg gameSchema: GameSchema)
}