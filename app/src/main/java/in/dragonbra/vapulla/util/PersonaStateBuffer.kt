package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.spongycastle.util.encoders.Hex
import java.util.*

class PersonaStateBuffer(val steamFriendDao: SteamFriendDao) : AnkoLogger {
    private val map: MutableMap<SteamID, PersonaState> = hashMapOf()

    private val mapLock: Any = Any()

    @Volatile
    private var isRunning = false

    private val thread: Runnable = Runnable {
        info("starting persona state buffer thread")
        isRunning = true

        while (isRunning) {
            Thread.sleep(1000L)
            process()
        }
        info("stopping persona state buffer thread")
    }

    fun push(state: PersonaState) {
        synchronized(mapLock) {
            if (map.contains(state.friendID)) {
                val old = map[state.friendID]

                if (state.state != EPersonaState.Offline || state.lastLogOff > old?.lastLogOff) {
                    map[state.friendID] = state
                }
            } else {
                map[state.friendID] = state
            }
        }
    }

    private fun process() {
        val friendsToUpdate: MutableList<SteamFriend> = LinkedList()

        synchronized(mapLock) {
            if (map.isEmpty()) {
                return
            }

            map.entries.forEach {
                val id = it.key
                val state = it.value

                val friend = steamFriendDao.find(id.convertToUInt64())

                if (friend != null && (state.state != EPersonaState.Offline || state.lastLogOff.time > friend.lastLogOff)) {
                    val avatarHash = Hex.toHexString(state.avatarHash)

                    friend.name = state.name
                    friend.avatar = avatarHash
                    friend.state = state.state.code()
                    friend.gameName = state.gameName
                    friend.gameAppId = state.gameAppID
                    friend.lastLogOn = state.lastLogOn.time
                    friend.lastLogOff = state.lastLogOff.time
                    friend.stateFlags = EPersonaStateFlag.code(state.stateFlags)

                    friendsToUpdate.add(friend)
                }
            }

            map.clear()
        }

        steamFriendDao.insert(*friendsToUpdate.toTypedArray())
    }

    fun start() {
        Thread(thread, "PersonaStateBufferThread").start()
    }

    fun stop() {
        isRunning = false
    }
}