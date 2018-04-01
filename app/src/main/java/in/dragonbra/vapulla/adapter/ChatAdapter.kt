package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.extension.longClick
import android.arch.paging.PagedListAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.list_chat_sent.view.*
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(val context: Context, val paperPlane: PaperPlane, val clipboard: ClipboardManager)
    : PagedListAdapter<ChatMessage, ChatAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
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

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        if (message == null || !message.fromLocal) {
            return VIEW_TYPE_RECEIVED
        } else {
            return VIEW_TYPE_SENT
        }
    }

    inner class ViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(message: ChatMessage) {
            paperPlane.load(v.message, message.message, true)

            // TODO the spannable breaks events, the selector of the parent is still broken
            v.message.longClick {
                (it.parent as View).performLongClick()
            }

            v.chatLayout.longClick {
                val clip = ClipData.newPlainText("steam message", v.message.text)
                clipboard.primaryClip = clip

                Toast.makeText(context, R.string.toastClipboard, Toast.LENGTH_SHORT).show()
                true
            }

            v.time.text = TIME_FORMAT.format(Date(message.timestamp))
        }

        fun clear() {
            paperPlane.clear(v.message)
        }
    }
}