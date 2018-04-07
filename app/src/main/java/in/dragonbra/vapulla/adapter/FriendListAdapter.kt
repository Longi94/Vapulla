package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.extension.*
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.util.OfflineStatusUpdater
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.recyclerview.TextHeader
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.brandongogetap.stickyheaders.exposed.StickyHeaderHandler
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.android.synthetic.main.list_friend.view.*
import kotlinx.android.synthetic.main.list_friend_request.view.*
import kotlinx.android.synthetic.main.list_header.view.*
import org.jetbrains.anko.find
import org.jetbrains.anko.textColor
import java.util.*


class FriendListAdapter(val context: Context, val schemaManager: GameSchemaManager,
                        val paperPlane: PaperPlane, val offlineStatusUpdater: OfflineStatusUpdater) :
        RecyclerView.Adapter<FriendListAdapter.ViewHolder>(), StickyHeaderHandler {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_FRIEND_REQUEST = 1
        const val VIEW_TYPE_FRIEND_OFFLINE = 2
        const val VIEW_TYPE_FRIEND_ONLINE = 3
        const val VIEW_TYPE_FRIEND_IN_GAME = 4
        const val VIEW_TYPE_FRIEND_RECENT = 5
    }

    var friendList: MutableList<Any> = LinkedList()

    var listener: OnItemSelectedListener? = null

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private var updateTime = 0L

    private var recentsTimeout = prefs.getString("pref_friends_list_recents", "604800000").toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = when (viewType) {
            VIEW_TYPE_FRIEND_REQUEST -> R.layout.list_friend_request
            VIEW_TYPE_HEADER -> R.layout.list_header
            else -> R.layout.list_friend
        }
        val v = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = friendList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        friendList[position].let {
            holder.bind(it)
        }
    }

    override fun getItemViewType(position: Int) = getItemViewType(friendList[position])

    private fun getItemViewType(item: Any): Int {
        if (item is FriendListItem) {
            return if (item.relation == EFriendRelationship.RequestRecipient.code()) {
                VIEW_TYPE_FRIEND_REQUEST
            } else if (recentsTimeout == 0L || (recentsTimeout > 0L && item.lastMessageTime?.let { it >= updateTime - recentsTimeout } == true)) {
                VIEW_TYPE_FRIEND_RECENT
            } else if (item.isInGame()) {
                VIEW_TYPE_FRIEND_IN_GAME
            } else if (item.isOnline()) {
                VIEW_TYPE_FRIEND_ONLINE
            } else VIEW_TYPE_FRIEND_OFFLINE
        }
        return VIEW_TYPE_HEADER
    }

    override fun getAdapterData(): MutableList<*> = friendList

    fun swap(list: List<FriendListItem>, updateTime: Long) {
        this.updateTime = updateTime
        recentsTimeout = prefs.getString("pref_friends_list_recents", "604800000").toLong()

        var currentViewType = -1
        val newList: MutableList<Any> = LinkedList(list)

        if (!newList.isEmpty()) {
            for (i in (list.size - 1) downTo 0) {
                val type = getItemViewType(newList[i])
                if (currentViewType == -1) {
                    currentViewType = type
                } else if (type != currentViewType) {
                    newList.add(i + 1, TextHeader(getHeader(currentViewType)))
                    currentViewType = type
                }
            }
            newList.add(0, TextHeader(getHeader(currentViewType)))
        }

        val result = DiffUtil.calculateDiff(FriendDiffUtil(newList))
        friendList = newList
        result.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(item: Any) {

            (item as? TextHeader)?.let {
                v.header.text = it.title
            }

            (item as? FriendListItem)?.let { friend ->
                when (friend.relation) {
                    EFriendRelationship.RequestRecipient.code() -> {
                        v.moreButton.click {

                            val popup = PopupMenu(context, it)
                            popup.menuInflater.inflate(R.menu.menu_friend_request, popup.menu)
                            popup.setOnMenuItemClickListener { item ->
                                when (item.itemId) {
                                    R.id.accept -> {
                                        listener?.onRequestAccept(friend)
                                        true
                                    }
                                    R.id.ignore -> {
                                        listener?.onRequestIgnore(friend)
                                        true
                                    }
                                    R.id.block -> {
                                        listener?.onRequestBlock(friend)
                                        true
                                    }
                                    else -> false
                                }
                            }
                            popup.show()
                        }
                    }
                    else -> {
                        if (friend.gameAppId > 0) {
                            runOnBackgroundThread {
                                schemaManager.touch(friend.gameAppId)
                            }
                        }

                        if (Strings.isNullOrEmpty(friend.nickname)) {
                            v.nickname.hide()
                            v.nickname.text = null
                        } else {
                            v.nickname.show()
                            v.nickname.text = context.getString(R.string.nicknameFormat, friend.nickname)
                        }

                        val state = friend.state?.let { EPersonaState.from(it) }

                        if ((friend.lastMessageTime == null || friend.typingTs > friend.lastMessageTime!!)
                                && friend.typingTs > System.currentTimeMillis() - 20000L) {
                            offlineStatusUpdater.clear(v.status)
                            v.status.text = context.getString(R.string.statusTyping)
                            v.status.textColor = ContextCompat.getColor(context, R.color.colorAccent)
                            v.status.bold()
                        } else {
                            offlineStatusUpdater.schedule(v.status, friend)
                            v.status.text = Utils.getStatusText(context, state, friend.gameAppId, friend.gameName, friend.lastLogOff)
                            v.status.textColor = ContextCompat.getColor(context, android.R.color.secondary_text_dark)
                            v.status.normal()
                        }

                        paperPlane.load(v.lastMessage, friend.lastMessage ?: "", false)
                        if (friend.lastMessageUnread == true) {
                            v.lastMessage.textColor = ContextCompat.getColor(context, R.color.colorAccent)
                            v.lastMessage.bold()
                        } else {
                            v.lastMessage.textColor = ContextCompat.getColor(context, android.R.color.secondary_text_dark)
                            v.lastMessage.normal()
                        }

                        (v.statusIndicator.drawable as GradientDrawable).setColor(Utils.getStatusColor(context, state, friend.gameAppId, friend.gameName))

                        v.mobileIndicator.hide()
                        v.webIndicator.hide()
                        val flags = EPersonaStateFlag.from(friend.stateFlags)
                        if (flags.contains(EPersonaStateFlag.ClientTypeMobile)) {
                            v.mobileIndicator.show()
                        } else if (flags.contains(EPersonaStateFlag.ClientTypeWeb)) {
                            v.webIndicator.show()
                        }

                        v.click {
                            listener?.onItemSelected(friend)
                        }
                    }
                }

                v.find<TextView>(R.id.username).text = friend.name

                Glide.with(context)
                        .load(Utils.getAvatarUrl(friend.avatar))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .apply(Utils.avatarOptions)
                        .into(v.find(R.id.avatar))

            }
        }
    }

    private fun getHeader(viewType: Int): String {
        return context.getString(when (viewType) {
            VIEW_TYPE_FRIEND_REQUEST -> R.string.headerFriendRequest
            VIEW_TYPE_FRIEND_OFFLINE -> R.string.headerFriendOffline
            VIEW_TYPE_FRIEND_ONLINE -> R.string.headerFriendOnline
            VIEW_TYPE_FRIEND_IN_GAME -> R.string.headerFriendInGame
            VIEW_TYPE_FRIEND_RECENT -> R.string.headerFriendRecent
            else -> R.string.headerFriendOffline
        })
    }

    inner class FriendDiffUtil(val list: MutableList<Any>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

            val newItem = list[newItemPosition]
            val oldItem = friendList[oldItemPosition]

            return newItem is FriendListItem && oldItem is FriendListItem && newItem.id == oldItem.id ||
                    newItem is TextHeader && oldItem is TextHeader && newItem.title == oldItem.title
        }

        override fun getOldListSize() = friendList.size

        override fun getNewListSize() = list.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                list[newItemPosition] == friendList[oldItemPosition]
    }

    interface OnItemSelectedListener {
        fun onItemSelected(friend: FriendListItem)
        fun onRequestAccept(friend: FriendListItem)
        fun onRequestIgnore(friend: FriendListItem)
        fun onRequestBlock(friend: FriendListItem)
    }
}