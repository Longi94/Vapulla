package `in`.dragonbra.vapulla

import `in`.dragonbra.javasteam.util.log.LogManager
import `in`.dragonbra.vapulla.component.DaggerVapullaComponent
import `in`.dragonbra.vapulla.component.VapullaComponent
import `in`.dragonbra.vapulla.module.AppModule
import `in`.dragonbra.vapulla.module.PresenterModule
import `in`.dragonbra.vapulla.module.StorageModule
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

class VapullaApplication : Application() {

    lateinit var graph: VapullaComponent

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

        graph = DaggerVapullaComponent.builder()
                .appModule(AppModule(this))
                .storageModule(StorageModule())
                .presenterModule(PresenterModule())
                .build()
    }
}