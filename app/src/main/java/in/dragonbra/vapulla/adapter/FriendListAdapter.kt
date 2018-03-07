package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.util.CircleTransform
import `in`.dragonbra.vapulla.util.Utils
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.list_friend.view.*
import kotlinx.android.synthetic.main.list_friend_request.view.*
import org.jetbrains.anko.find


class FriendListAdapter(val context: Context, val schemaManager: GameSchemaManager) : RecyclerView.Adapter<FriendListAdapter.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_FRIEND = 0
        const val VIEW_TYPE_FRIEND_REQUEST = 1
    }

    var friendList: List<FriendListItem> = emptyList()

    private var requestCount = 0

    val glideOptions = RequestOptions()
            .transform(CircleTransform())

    var listener: OnItemSelectedListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = when (viewType) {
            VIEW_TYPE_FRIEND_REQUEST -> R.layout.list_friend_request
            else -> R.layout.list_friend
        }
        val v = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = friendList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        friendList[position].let {
            holder.bind(it, requestCount > 0 && position == 0, requestCount > 0 && position == requestCount - 1)
        }
    }

    override fun getItemViewType(position: Int) = when (friendList[position].relation) {
        EFriendRelationship.RequestRecipient.code() -> VIEW_TYPE_FRIEND_REQUEST
        else -> VIEW_TYPE_FRIEND
    }

    fun swap(list: List<FriendListItem>) {
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    list[newItemPosition].id == friendList[oldItemPosition].id

            override fun getOldListSize() = friendList.size

            override fun getNewListSize() = list.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    list[newItemPosition] == friendList[oldItemPosition]
        })
        friendList = list
        requestCount = list.count { it.relation == EFriendRelationship.RequestRecipient.code() }
        result.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(friend: FriendListItem, header: Boolean, footer: Boolean) {

            when (friend.relation) {
                EFriendRelationship.RequestRecipient.code() -> {
                    v.header.visibility = if (header) View.VISIBLE else View.GONE
                    v.footer.visibility = if (footer) View.VISIBLE else View.GONE
                }
                else -> {
                    if (friend.gameAppId > 0) {
                        runOnBackgroundThread {
                            schemaManager.touch(friend.gameAppId)
                        }
                    }

                    val state = friend.state?.let { EPersonaState.from(it) }
                    v.status.text = Utils.getStatusText(context, state, friend.gameAppId, friend.gameName, friend.lastLogOff)
                    v.lastMessage.text = friend.lastMessage

                    (v.statusIndicator.drawable as GradientDrawable).setColor(Utils.getStatusColor(context, state, friend.gameAppId, friend.gameName))

                    v.mobileIndicator.visibility = View.GONE
                    v.webIndicator.visibility = View.GONE
                    val flags = EPersonaStateFlag.from(friend.stateFlags)
                    if (flags.contains(EPersonaStateFlag.ClientTypeMobile)) {
                        v.mobileIndicator.visibility = View.VISIBLE
                    } else if (flags.contains(EPersonaStateFlag.ClientTypeWeb)) {
                        v.webIndicator.visibility = View.VISIBLE
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
                    .apply(glideOptions)
                    .into(v.find(R.id.avatar))
        }
    }

    interface OnItemSelectedListener {
        fun onItemSelected(friend: FriendListItem)
    }
}