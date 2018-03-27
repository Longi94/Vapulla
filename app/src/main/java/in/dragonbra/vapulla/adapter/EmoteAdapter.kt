package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.data.entity.Emoticon
import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.android.synthetic.main.list_emote.view.*

class EmoteAdapter(val context: Context) : RecyclerView.Adapter<EmoteAdapter.ViewHolder>() {

    var emoteList: List<Emoticon> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_emote, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = emoteList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(emoteList[position])
    }

    fun swap(list: List<Emoticon>) {
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    list[newItemPosition] == emoteList[oldItemPosition]

            override fun getOldListSize() = emoteList.size

            override fun getNewListSize() = list.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    list[newItemPosition] == emoteList[oldItemPosition]
        })
        emoteList = list
        result.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(emote: Emoticon) {
            Glide.with(context)
                    .load("https://steamcommunity-a.akamaihd.net/economy/emoticon/:${emote.name}:")
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(v.emote)
        }
    }
}