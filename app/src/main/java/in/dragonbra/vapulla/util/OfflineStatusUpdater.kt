package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListItem
import android.content.Context
import android.text.format.DateUtils
import android.widget.TextView

class OfflineStatusUpdater(val context: Context) {
    private val views: MutableMap<TextView, Long> = HashMap()

    fun schedule(view: TextView, friend: FriendListItem) {
        if (friend.state == null || friend.state == EPersonaState.Offline.code()) {
            views[view] = friend.lastLogOff
        } else {
            views.remove(view)
        }
    }

    fun updateAll() {
        views.entries.forEach {
            it.key.text = context.getString(R.string.statusOffline,
                    DateUtils.getRelativeTimeSpanString(it.value, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS))
        }
    }

    fun clear() {
        views.clear()
    }

    fun clear(view: TextView) {
        views.remove(view)
    }
}