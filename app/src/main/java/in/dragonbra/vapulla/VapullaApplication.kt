package `in`.dragonbra.vapulla

import `in`.dragonbra.javasteam.util.log.LogManager
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Created by lngtr on 2018-03-02.
 */
class VapullaApplication : Application() {

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()

        LogManager.addListener { clazz, message, throwable ->
            Log.d(clazz.simpleName, message, throwable)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("vapulla-service",
                    "Vapulla",
                    NotificationManager.IMPORTANCE_DEFAULT)
            channel.vibrationPattern = longArrayOf(-1L)
            channel.importance = NotificationManager.IMPORTANCE_LOW
            channel.lightColor = 0
            notificationManager.createNotificationChannel(channel)
        }
    }
}