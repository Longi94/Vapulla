package `in`.dragonbra.vapulla.steam.callback

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserverFriends.CMsgClientEmoticonList
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg

class EmoticonListCallback(msg: CMsgClientEmoticonList.Builder) : CallbackMsg() {
    val emoticons: List<Emoticon> = msg.emoticonsList.map { Emoticon(it) }
}

data class Emoticon(val name: String, val count: Int) {
    constructor(emoticon: CMsgClientEmoticonList.Emoticon) : this(emoticon.name, emoticon.count)
}