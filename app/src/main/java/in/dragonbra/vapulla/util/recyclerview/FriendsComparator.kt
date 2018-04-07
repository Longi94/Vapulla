package `in`.dragonbra.vapulla.util.recyclerview

import `in`.dragonbra.vapulla.adapter.FriendListItem
import android.content.Context
import android.preference.PreferenceManager

class FriendsComparator(context: Context, private val updateTime: Long) : Comparator<FriendListItem> {

    private val recentsTimeout: Long

    init {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        recentsTimeout = prefs.getString("pref_friends_list_recents", "604800000").toLong()
    }

    override fun compare(o1: FriendListItem, o2: FriendListItem): Int {
        if (o1.relation != o2.relation) {
            return o1.relation - o2.relation
        }

        val (isRecent1, isRecent2) = if (recentsTimeout > 0) {
            Pair(o1.lastMessageTime?.let { it >= updateTime - recentsTimeout } == true,
                    o2.lastMessageTime?.let { it >= updateTime - recentsTimeout } == true)
        } else if (recentsTimeout == 0L) {
            Pair(o1.lastMessageTime != null, o2.lastMessageTime != null)
        } else {
            Pair(false, false)
        }

        if (isRecent1 && isRecent2) {
            return (o2.lastMessageTime!! - o1.lastMessageTime!!).toInt()
        } else if (isRecent1) {
            return -1
        } else if (isRecent2) {
            return 1
        }

        val statusC = compareStatuses(o1, o2)
        return if (statusC != 0) {
            statusC
        } else {
            compareNames(o1.name, o2.name)
        }
    }

    private fun compareNames(s1: String?, s2: String?): Int =
            if (s1 != null && s2 != null) s1.compareTo(s2, true)
            else if (s1 != null) 1
            else if (s2 != null) -1
            else 0

    private fun compareStatuses(o1: FriendListItem, o2: FriendListItem): Int {
        val inGame1 = o1.isInGame()
        val inGame2 = o2.isInGame()

        if (inGame1 && inGame2) {
            return 0
        } else if (inGame1) {
            return -1
        } else if (inGame2) {
            return 1
        }

        val online1 = o1.isOnline()
        val online2 = o2.isOnline()

        return if (online1 && online2) 0
        else if (online1) -1
        else if (online2) 1
        else 0
    }
}