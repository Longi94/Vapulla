package `in`.dragonbra.vapulla.overlay

import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.paging.PagedList
import android.content.Context
import android.view.WindowManager
import io.mattcarroll.hover.HoverView
import io.mattcarroll.hover.OnExitListener
import io.mattcarroll.hover.window.WindowViewController

class BubbleManager(val context: Context, val windowManager: WindowManager) : OnExitListener {

    private var chatBubbleMenu: ChatBubbleMenu? = null

    private var hoverView: HoverView? = null

    override fun onExit() {
    }

    fun create(id: Long, friend: LiveData<FriendListItem>, chatData: LiveData<PagedList<ChatMessage>>, lifecycleOwner: LifecycleOwner) {
        chatBubbleMenu = ChatBubbleMenu(context, lifecycleOwner)
        chatBubbleMenu?.addFriend(id, friend, chatData)
        hoverView = HoverView.createForWindow(context, WindowViewController(windowManager))
        hoverView?.addToWindow()
        hoverView?.setOnExitListener(this)
        hoverView?.setMenu(chatBubbleMenu)
        hoverView?.collapse()
    }

    fun close() {
        chatBubbleMenu?.clear()
        chatBubbleMenu = null
        hoverView?.removeFromWindow()
    }
}