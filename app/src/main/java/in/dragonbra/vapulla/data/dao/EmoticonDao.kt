package `in`.dragonbra.vapulla.data.dao

import `in`.dragonbra.vapulla.data.entity.Emoticon
import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

@Dao
interface EmoticonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg emoticon: Emoticon)

    @Query("SELECT * FROM emoticon ORDER BY name ASC")
    fun getLive(): LiveData<List<Emoticon>>

    @Query("SELECT * FROM emoticon ORDER BY name ASC")
    fun find(): List<Emoticon>

    @Query("DELETE FROM emoticon")
    fun delete()
}