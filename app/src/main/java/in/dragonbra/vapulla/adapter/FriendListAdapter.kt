package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.extension.click
import `in`.dragonbra.vapulla.util.CircleTransform
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.list_friend.view.*


class FriendListAdapter(val context: Context) : RecyclerView.Adapter<FriendListAdapter.ViewHolder>() {

    var friendList: List<SteamFriend>? = null

    val glideOptions = RequestOptions()
            .transform(CircleTransform())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_friend, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = if (friendList == null) 0 else friendList?.size!!

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        friendList?.get(position)?.let { holder.bind(it) }
    }

    fun swap(list: List<SteamFriend>?) {
        friendList = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(friend: SteamFriend) {
            v.username.text = friend.name
            v.status.text = friend.state?.let { EPersonaState.from(it).toString() }

            Glide.with(context)
                    .load(friend.getAvatarUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(glideOptions)
                    .into(v.avatar)

            v.click { }
        }
    }
}