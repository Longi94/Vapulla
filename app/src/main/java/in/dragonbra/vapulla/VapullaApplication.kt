package `in`.dragonbra.vapulla

import `in`.dragonbra.javasteam.util.log.LogManager
import `in`.dragonbra.vapulla.component.DaggerVapullaComponent
import `in`.dragonbra.vapulla.component.VapullaComponent
import `in`.dragonbra.vapulla.module.AppModule
import `in`.dragonbra.vapulla.module.PresenterModule
import `in`.dragonbra.vapulla.module.StorageModule
import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import com.google.firebase.crash.FirebaseCrash

class VapullaApplication : Application() {

    lateinit var graph: VapullaComponent

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()

        FirebaseCrash.setCrashCollectionEnabled(!BuildConfig.DEBUG)

        LogManager.addListener { clazz, message, throwable ->
            Log.d(clazz.simpleName, message, throwable)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val serviceChannel = NotificationChannel("vapulla-service",
                    "Vapulla service",
                    NotificationManager.IMPORTANCE_LOW)
            serviceChannel.enableVibration(false)
            serviceChannel.importance = NotificationManager.IMPORTANCE_LOW
            serviceChannel.enableLights(false)
            notificationManager.createNotificationChannel(serviceChannel)

            val friendRequestChannel = NotificationChannel("vapulla-friend-request",
                    "Friend request",
                    NotificationManager.IMPORTANCE_DEFAULT)
            friendRequestChannel.importance = NotificationManager.IMPORTANCE_DEFAULT
            friendRequestChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            friendRequestChannel.lightColor = 0xffffffff.toInt()
            notificationManager.createNotificationChannel(friendRequestChannel)


            val messageChannel = NotificationChannel("vapulla-message", "New messages",
                    NotificationManager.IMPORTANCE_HIGH)

            messageChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            messageChannel.lightColor = 0xffffffff.toInt()

            notificationManager.createNotificationChannel(messageChannel)
        }

        graph = DaggerVapullaComponent.builder()
                .appModule(AppModule(this))
                .storageModule(StorageModule())
                .presenterModule(PresenterModule())
                .build()

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false)
    }
}