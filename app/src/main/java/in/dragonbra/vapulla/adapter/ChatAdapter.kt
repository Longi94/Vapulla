package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.list_chat.view.*

/**
 * Created by lngtr on 2018-03-05.
 */
class ChatAdapter : PagedListAdapter<ChatMessage, ChatAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage?, newItem: ChatMessage?): Boolean {
                return oldItem?.message == newItem?.message &&
                        oldItem?.timestamp == newItem?.timestamp &&
                        oldItem?.fromLocal == newItem?.fromLocal &&
                        oldItem?.friendId == newItem?.friendId
            }

            override fun areContentsTheSame(oldItem: ChatMessage?, newItem: ChatMessage?): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_chat, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getItem(position)
        if (message != null) {
            holder.bind(message)
        } else {
            holder.clear()
        }
    }

    class ViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(message: ChatMessage) {
            v.message.text = message.message
        }

        fun clear() {
            v.message.text = ""
        }
    }
}