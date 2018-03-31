package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.ChatAdapter
import `in`.dragonbra.vapulla.adapter.EmoteAdapter
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.EmoticonDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.extension.*
import `in`.dragonbra.vapulla.presenter.ChatPresenter
import `in`.dragonbra.vapulla.service.ImgurAuthService
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.recyclerview.ChatAdapterDataObserver
import `in`.dragonbra.vapulla.view.ChatView
import android.arch.paging.PagedList
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
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
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.dialog_nickname.view.*
import org.jetbrains.anko.browse
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textColor
import javax.inject.Inject


class ChatActivity : VapullaBaseActivity<ChatView, ChatPresenter>(), ChatView, TextWatcher,
        PopupMenu.OnMenuItemClickListener, EmoteAdapter.EmoteListener {

    companion object {
        const val INTENT_STEAM_ID = "steam_id"

        const val REQUEST_IMAGE_GET = 100
    }

    @Inject
    lateinit var chatMessageDao: ChatMessageDao

    @Inject
    lateinit var steamFriendDao: SteamFriendDao

    @Inject
    lateinit var emoticonDao: EmoticonDao

    @Inject
    lateinit var imgurAuthService: ImgurAuthService

    lateinit var paperPlane: PaperPlane

    lateinit var chatAdapter: ChatAdapter

    lateinit var emoteAdapter: EmoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        paperPlane = PaperPlane(this, 18.0f)
        chatAdapter = ChatAdapter(this, paperPlane)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        chatList.layoutManager = layoutManager
        chatList.adapter = chatAdapter

        chatAdapter.registerAdapterDataObserver(ChatAdapterDataObserver(
                chatAdapter,
                layoutManager,
                chatList
        ))

        emoteAdapter = EmoteAdapter(this, this)

        val emoteLayoutManager = FlexboxLayoutManager(this)
        emoteLayoutManager.flexDirection = FlexDirection.ROW
        emoteLayoutManager.justifyContent = JustifyContent.CENTER

        emoteList.layoutManager = emoteLayoutManager;
        emoteList.adapter = emoteAdapter

        messageBox.addTextChangedListener(this)
        messageBox.setOnClickListener { emoteList.hide() }

        moreButton.setOnClickListener {
            val popup = PopupMenu(this@ChatActivity, it)
            popup.menuInflater.inflate(R.menu.menu_chat, popup.menu)
            popup.show()
            popup.setOnMenuItemClickListener(this@ChatActivity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        paperPlane.clearAll()
    }

    override fun createPresenter(): ChatPresenter {
        val steamId = SteamID(intent.getLongExtra(INTENT_STEAM_ID, 0L))
        return ChatPresenter(this, chatMessageDao, steamFriendDao, emoticonDao, imgurAuthService, steamId)
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
        supportFinishAfterTransition()
    }

    override fun afterTextChanged(s: Editable?) {
        presenter.typing()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.isEmpty()) {
            imageButton.show()
        } else {
            imageButton.hide()
        }
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
        R.id.viewAccount -> {
            presenter.viewAccountMenuClicked()
            true
        }
        R.id.viewAliases -> {
            presenter.viewAliasesMenuClicked()
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

    override fun browseUrl(url: String) {
        browse(url)
    }

    override fun showAliases(names: List<String>) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)

            builder.setTitle("Past aliases")
                    .setItems(names.toTypedArray(), null)
                    .setNegativeButton("Close", null)

            builder.create().show()
        }
    }

    override fun showEmotes(list: List<Emoticon>) {
        emoteAdapter.swap(list)
    }

    override fun onEmoteSelected(emoticon: Emoticon) {
        messageBox.text.insert(messageBox.selectionStart, ":${emoticon.name}:")
    }

    override fun showImgurDialog() {
        val builder = AlertDialog.Builder(this)

        builder.setMessage("Link your Imgur account to send images to your friends.")
                .setTitle("Sharing images")
                .setPositiveButton("Yes", { _, _ -> startActivity<SettingsActivity>() })
                .setNegativeButton("Cancel", null)

        builder.create().show()
    }

    override fun showPhotoSelector() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_GET)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {
            presenter.sendImage(data.data)
        }
    }

    override fun showUploadDialog() {
        runOnUiThread {
            imageButton.isClickable = false
            uploadProgressBar.show()
            uploadProgressBar.isIndeterminate = true
        }
    }

    override fun imageUploadFail() {
        runOnUiThread {
            imageButton.isClickable = true
            uploadProgressBar.hide()
            Snackbar.make(rootLayout, "Failed to upload image", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun imageUploadSuccess() {
        runOnUiThread {
            imageButton.isClickable = true
            uploadProgressBar.hide()
        }
    }

    override fun imageUploadProgress(total: Int, progress: Int) {
        uploadProgressBar.max = total
        uploadProgressBar.progress = progress
        uploadProgressBar.isIndeterminate = false
    }

    @Suppress("UNUSED_PARAMETER")
    fun navigateUp(v: View) {
        supportFinishAfterTransition()
    }

    @Suppress("UNUSED_PARAMETER")
    fun sendMessage(v: View) {
        val message = messageBox.text.toString()

        if (!Strings.isNullOrEmpty(message)) {
            messageBox.setText("")
            presenter.sendMessage(message)
        }

    }

    @Suppress("UNUSED_PARAMETER")
    fun toggleEmote(v: View) {
        emoteList.toggleVisibility()

        if (emoteList.isVisible()) {
            Utils.hideKeyboardFrom(this, messageBox)
            presenter.requestEmotes()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun sendImage(v: View) {
        presenter.imageButtonClicked()
    }
}
