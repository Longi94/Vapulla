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
import `in`.dragonbra.vapulla.presenter.ChatPresenter
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.view.ChatView
import android.arch.paging.PagedList
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.android.synthetic.main.activity_chat.*
import org.jetbrains.anko.textColor
import javax.inject.Inject

class ChatActivity : VapullaBaseActivity<ChatView, ChatPresenter>(), ChatView, TextWatcher {

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

        messageBox.addTextChangedListener(this)
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
            val state = if (friend.state == null) EPersonaState.Offline else EPersonaState.from(friend.state!!)
            friendUsername.text = friend.name

            if ((friend.lastMessageTime == null || friend.typingTs > friend.lastMessageTime!!)
                    && friend.typingTs > System.currentTimeMillis() - 20000L) {
                friendStatus.text = getString(R.string.statusTyping)
                friendStatus.textColor = ContextCompat.getColor(this@ChatActivity, R.color.colorAccent)
                friendStatus.setTypeface(friendStatus.typeface, Typeface.BOLD)
            } else {
                friendStatus.text = Utils.getStatusText(this@ChatActivity, state, friend.gameAppId, friend.gameName, friend.lastLogOff)
                friendStatus.textColor = ContextCompat.getColor(this@ChatActivity, android.R.color.secondary_text_dark)
                friendStatus.setTypeface(Typeface.create(friendStatus.typeface, Typeface.NORMAL), Typeface.NORMAL)
            }

            Glide.with(this@ChatActivity)
                    .load(Utils.getAvatarUrl(friend.avatar))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(Utils.avatarOptions)
                    .into(friendAvatar)
        }
    }

    override fun afterTextChanged(s: Editable?) {
        presenter.typing()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
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
