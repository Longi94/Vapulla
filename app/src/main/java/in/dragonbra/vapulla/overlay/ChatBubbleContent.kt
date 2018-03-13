package `in`.dragonbra.vapulla.overlay

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.ChatAdapter
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.recyclerview.ChatAdapterDataObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.paging.PagedList
import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import io.mattcarroll.hover.Content
import kotlinx.android.synthetic.main.bubble_chat.view.*
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.textColor

class ChatBubbleContent(
        val context: Context,
        val avatar: ImageView,
        val friend: LiveData<FriendListItem>,
        val chatData: LiveData<PagedList<ChatMessage>>,
        val lifecycleOwner: LifecycleOwner
) : Content, TextWatcher {

    private val view: View
    private val chatAdapter: ChatAdapter

    private val chatObserver = Observer<PagedList<ChatMessage>> { list ->
        list?.let { setChatData(it) }
    }

    private val friendObserver = Observer<FriendListItem> { friend ->
        friend?.let { setFriendData(it) }
    }

    private val avatarObserver = Observer<FriendListItem> { friend ->
        friend?.let { setAvatar(it) }
    }

    init {
        view = context.layoutInflater.inflate(R.layout.bubble_chat, null)
        chatAdapter = ChatAdapter()

        val layoutManager = LinearLayoutManager(context)
        layoutManager.reverseLayout = true
        view.chatList.layoutManager = layoutManager
        view.chatList.adapter = chatAdapter

        chatAdapter.registerAdapterDataObserver(ChatAdapterDataObserver(
                chatAdapter,
                layoutManager,
                view.chatList
        ))

        view.messageBox.addTextChangedListener(this)

        friend.value?.let(this::setAvatar)
        friend.observe(lifecycleOwner, avatarObserver)
    }

    override fun onShown() {
        friend.value?.let(this::setFriendData)
        friend.observe(lifecycleOwner, friendObserver)

        chatData.value?.let(this::setChatData)
        chatData.observe(lifecycleOwner, chatObserver)
    }

    override fun getView() = view

    override fun onHidden() {
        friend.removeObserver(friendObserver)
        chatData.removeObserver(chatObserver)
    }

    override fun isFullscreen() = true

    override fun afterTextChanged(s: Editable?) {
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    private fun setFriendData(friend: FriendListItem) {
        view.friendUsername.text = friend.name

        if ((friend.lastMessageTime == null || friend.typingTs > friend.lastMessageTime!!)
                && friend.typingTs > System.currentTimeMillis() - 20000L) {
            view.friendStatus.text = context.getString(R.string.statusTyping)
            view.friendStatus.textColor = ContextCompat.getColor(context, R.color.colorAccent)
            view.friendStatus.setTypeface(view.friendStatus.typeface, Typeface.BOLD)
        } else {
            view.friendStatus.text = Utils.getStatusText(context, EPersonaState.from(friend.state ?: 0), friend.gameAppId, friend.gameName, friend.lastLogOff)
            view.friendStatus.textColor = ContextCompat.getColor(context, android.R.color.secondary_text_dark)
            view.friendStatus.setTypeface(Typeface.create(view.friendStatus.typeface, Typeface.NORMAL), Typeface.NORMAL)
        }
    }

    private fun setAvatar(friend: FriendListItem) {
        Glide.with(context)
                .load(Utils.getAvatarUrl(friend.avatar))
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(Utils.avatarOptions)
                .into(avatar)
    }

    private fun setChatData(list: PagedList<ChatMessage>) = chatAdapter.submitList(list)
}