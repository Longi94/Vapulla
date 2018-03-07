package `in`.dragonbra.vapulla.manager

import `in`.dragonbra.vapulla.data.dao.GameSchemaDao
import `in`.dragonbra.vapulla.data.entity.GameSchema
import `in`.dragonbra.vapulla.retrofit.StoreFront

class GameSchemaManager(val gameSchemaDao: GameSchemaDao, private val storeFront: StoreFront) {

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

            val resp = storeFront.getAppDetails(id).execute()

            if (resp.isSuccessful) {
                val name = resp.body()?.get(id)?.data?.name

                if (name != null) {
                    gameSchemaDao.insert(GameSchema(id, name, System.currentTimeMillis()))
                }
            }
            fetchingIds.remove(id)
        }
    }
}