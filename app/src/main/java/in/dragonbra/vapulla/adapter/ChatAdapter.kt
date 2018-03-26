package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import android.arch.paging.PagedListAdapter
import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.list_chat_received.view.*

/**
 * Created by lngtr on 2018-03-05.
 */
class ChatAdapter(val context: Context, val paperPlane: PaperPlane) : PagedListAdapter<ChatMessage, ChatAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        const val VIEW_TYPE_RECEIVED = 0
        const val VIEW_TYPE_SENT = 1
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
        val layoutRes = when (viewType) {
            VIEW_TYPE_RECEIVED -> R.layout.list_chat_received
            VIEW_TYPE_SENT -> R.layout.list_chat_sent
            else -> R.layout.list_chat_received
        }
        val v = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(v, paperPlane)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getItem(position)
        if (message != null) {
            holder.bind(message)
        } else {
            holder.clear()
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        if (message == null || !message.fromLocal) {
            return VIEW_TYPE_RECEIVED
        } else {
            return VIEW_TYPE_SENT
        }
    }

    class ViewHolder(val v: View, val paperPlane: PaperPlane) : RecyclerView.ViewHolder(v) {
        fun bind(message: ChatMessage) {
            paperPlane.load(v.message, message.message, true)
        }

        fun clear() {
            paperPlane.clear(v.message)
        }
    }
}