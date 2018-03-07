package `in`.dragonbra.vapulla.manager

import `in`.dragonbra.vapulla.data.dao.GameSchemaDao
import `in`.dragonbra.vapulla.data.entity.GameSchema
import `in`.dragonbra.vapulla.retrofit.ISteamUserStats

class GameSchemaManager(val gameSchemaDao: GameSchemaDao, private val steamUserStats: ISteamUserStats) {

    companion object {
        const val UPDATE_INTERVAL = 604800000L
    }

    private val fetchingIds: MutableSet<Int> = mutableSetOf()

    fun touch(id: Int) {
        if (fetchingIds.contains(id)) {
            return
        }

        val schema = gameSchemaDao.find(id)

        if (schema == null || schema.modifyDate < System.currentTimeMillis() - UPDATE_INTERVAL) {
            fetchingIds.add(id)

            val resp = steamUserStats.getSchemaForGame(id).execute()

            if (resp.isSuccessful) {
                val name = resp.body()?.game?.gameName

                if (name != null) {
                    gameSchemaDao.insert(GameSchema(id, name, System.currentTimeMillis()))
                }
            }
            fetchingIds.remove(id)
        }
    }
}