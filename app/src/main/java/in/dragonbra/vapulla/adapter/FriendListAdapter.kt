package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.util.CircleTransform
import `in`.dragonbra.vapulla.util.Utils
import android.content.Context
import android.graphics.drawable.GradientDrawable
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


class FriendListAdapter(val context: Context) : RecyclerView.Adapter<FriendListAdapter.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_FRIEND = 0
        const val VIEW_TYPE_FRIEND_REQUEST = 1
    }

    var friendList: List<FriendListItem>? = null

    var requestCount = 0

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

    override fun getItemCount(): Int = if (friendList == null) 0 else friendList?.size!!

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        friendList?.get(position)?.let {
            holder.bind(it, requestCount > 0 && position == 0, requestCount > 0 && position == requestCount - 1)
        }
    }

    override fun getItemViewType(position: Int) = when (friendList?.get(position)?.relation) {
        EFriendRelationship.RequestRecipient.code() -> VIEW_TYPE_FRIEND_REQUEST
        else -> VIEW_TYPE_FRIEND
    }

    fun swap(list: List<FriendListItem>?) {
        friendList = list
        val filter = list?.filter { it.relation == EFriendRelationship.RequestRecipient.code() }
        requestCount = if (filter != null) filter.size else 0
        notifyDataSetChanged()
    }

    inner class ViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(friend: FriendListItem, header: Boolean, footer: Boolean) {

            when (friend.relation) {
                EFriendRelationship.RequestRecipient.code() -> {
                    v.header.visibility = if (header) View.VISIBLE else View.GONE
                    v.footer.visibility = if (footer) View.VISIBLE else View.GONE
                }
                else -> {
                    val state = friend.state?.let { EPersonaState.from(it) }
                    v.status.text = Utils.getStatusText(context, state, friend.gameName)
                    v.lastMessage.text = friend.lastMessage

                    (v.statusIndicator.drawable as GradientDrawable).setColor(Utils.getStatusColor(context, state, friend.gameName))

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