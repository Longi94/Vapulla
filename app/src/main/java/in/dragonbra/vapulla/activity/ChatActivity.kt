package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.ChatAdapter
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.extension.bold
import `in`.dragonbra.vapulla.extension.hide
import `in`.dragonbra.vapulla.extension.normal
import `in`.dragonbra.vapulla.extension.show
import `in`.dragonbra.vapulla.presenter.ChatPresenter
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.recyclerview.ChatAdapterDataObserver
import `in`.dragonbra.vapulla.view.ChatView
import android.arch.paging.PagedList
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.dialog_nickname.view.*
import org.jetbrains.anko.textColor
import javax.inject.Inject


class ChatActivity : VapullaBaseActivity<ChatView, ChatPresenter>(), ChatView, TextWatcher, PopupMenu.OnMenuItemClickListener {

    companion object {
        const val INTENT_STEAM_ID = "steam_id"
    }

    @Inject
    lateinit var chatMessageDao: ChatMessageDao

    @Inject
    lateinit var steamFriendDao: SteamFriendDao

    lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatAdapter = ChatAdapter()

        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        chatList.layoutManager = layoutManager
        chatList.adapter = chatAdapter

        chatAdapter.registerAdapterDataObserver(ChatAdapterDataObserver(
                chatAdapter,
                layoutManager,
                chatList
        ))

        messageBox.addTextChangedListener(this)

        moreButton.setOnClickListener {
            val popup = PopupMenu(this@ChatActivity, it)
            popup.menuInflater.inflate(R.menu.menu_chat, popup.menu)
            popup.show()
            popup.setOnMenuItemClickListener(this@ChatActivity)
        }
    }

    override fun createPresenter(): ChatPresenter {
        val steamId = SteamID(intent.getLongExtra(INTENT_STEAM_ID, 0L))
        return ChatPresenter(this, chatMessageDao, steamFriendDao, steamId)
    }

    override fun closeApp() {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
            finish()
        }
    }

    override fun showChat(list: PagedList<ChatMessage>?) {
        chatAdapter.submitList(list)
    }

    override fun updateFriendData(friend: FriendListItem?) {
        if (friend == null) {
            return
        }
        runOnUiThread {
            val state = EPersonaState.from(friend.state ?: 0)
            friendUsername.text = friend.name

            if (Strings.isNullOrEmpty(friend.nickname)) {
                friendNickname.hide()
            } else {
                friendNickname.show()
                friendNickname.text = "(${friend.nickname})"
            }

            if ((friend.lastMessageTime == null || friend.typingTs > friend.lastMessageTime!!)
                    && friend.typingTs > System.currentTimeMillis() - 20000L) {
                friendStatus.text = getString(R.string.statusTyping)
                friendStatus.textColor = ContextCompat.getColor(this@ChatActivity, R.color.colorAccent)
                friendStatus.bold()
            } else {
                friendStatus.text = Utils.getStatusText(this@ChatActivity, state, friend.gameAppId, friend.gameName, friend.lastLogOff)
                friendStatus.textColor = ContextCompat.getColor(this@ChatActivity, android.R.color.secondary_text_dark)
                friendStatus.normal()
            }

            Glide.with(this@ChatActivity)
                    .load(Utils.getAvatarUrl(friend.avatar))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(Utils.avatarOptions)
                    .into(friendAvatar)
        }
    }

    override fun navigateUp() {
        NavUtils.navigateUpFromSameTask(this)
    }

    override fun afterTextChanged(s: Editable?) {
        presenter.typing()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun onMenuItemClick(item: MenuItem) = when (item.itemId) {
        R.id.removeFriend -> {
            presenter.removeFriend()
            true
        }
        R.id.blockFriend -> {
            presenter.blockFriend()
            true
        }
        R.id.setNickname -> {
            presenter.nicknameMenuClicked()
            true
        }
        else -> false
    }

    override fun showRemoveFriendDialog(name: String) {
        val builder = AlertDialog.Builder(this)

        builder.setMessage("Are you sure you want to remove $name from you friends list?")
                .setTitle("Remove $name")
                .setPositiveButton("Yes", { _, _ -> presenter.confirmRemoveFriend() })
                .setNegativeButton("No", null)

        builder.create().show()
    }

    override fun showBlockFriendDialog(name: String) {
        val builder = AlertDialog.Builder(this)

        builder.setMessage("Are you sure you want to block $name? This will block all kinds of communication with your friend. You can undo this by visiting their profile and unblocking them.")
                .setTitle("Block all interactions with $name")
                .setPositiveButton("Yes", { _, _ -> presenter.confirmBlockFriend() })
                .setNegativeButton("No", null)

        builder.create().show()
    }

    override fun showNicknameDialog(nickname: String) {
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_nickname, null)
        v.nickname.setText(nickname)

        val builder = AlertDialog.Builder(this)
                .setTitle("Set nickname")
                .setView(v)
                .setPositiveButton("Set", { _, _ -> presenter.setNickname(v.nickname.text.toString()) })
                .setNegativeButton("Cancel", null)

        builder.create().show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun navigateUp(v: View) {
        NavUtils.navigateUpFromSameTask(this)
    }

    @Suppress("UNUSED_PARAMETER")
    fun sendMessage(v: View) {
        val message = messageBox.text.toString()

        if (!Strings.isNullOrEmpty(message)) {
            messageBox.setText("")
            presenter.sendMessage(message)
        }

    }
}
