package `in`.dragonbra.vapulla

import `in`.dragonbra.javasteam.util.log.LogManager
import android.app.Application
import android.util.Log

/**
 * Created by lngtr on 2018-03-02.
 */
class VapullaApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        LogManager.addListener { clazz, message, throwable ->
            Log.d(clazz.simpleName, message, throwable)
        }
    }
}