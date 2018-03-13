package `in`.dragonbra.vapulla.overlay

import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.paging.PagedList
import android.content.Context
import android.widget.ImageView
import io.mattcarroll.hover.HoverMenu
import java.util.*

class ChatBubbleMenu(val context: Context, val lifecycleOwner: LifecycleOwner) : HoverMenu() {

    companion object {
        const val ID = "chat_bubble_menu"
    }

    private val sections: MutableList<Section> = LinkedList()

    override fun getSections() = sections

    override fun getId() = ID

    override fun getSection(index: Int) = sections[index]

    override fun getSection(sectionId: SectionId) = sections.find { it.id == sectionId }

    override fun getSectionCount() = sections.size

    fun addFriend(id: Long, friend: LiveData<FriendListItem>, chatData: LiveData<PagedList<ChatMessage>>) {
        val sectionId = SectionId(id.toString())
        if (getSection(sectionId) != null) {
            return
        }

        val avatar = ImageView(context)
        val content = ChatBubbleContent(context, avatar, friend, chatData, lifecycleOwner)

        val section = Section(sectionId, avatar, content)

        sections.add(section)
        notifyMenuChanged()
    }

    fun clear() {
        sections.clear()
        notifyMenuChanged()
    }
}