/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.command.relay

import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.plugin.PluginManage
import net.rwhps.server.net.netconnectprotocol.internal.relay.fromRelayJumpsToAnotherServer
import net.rwhps.server.net.netconnectprotocol.realize.GameVersionRelay
import net.rwhps.server.util.IsUtil
import net.rwhps.server.util.Time
import net.rwhps.server.util.game.CommandHandler

internal class RelayClientCommands(handler: CommandHandler) {
    private val localeUtil = Data.i18NBundle

    private fun isAdmin(con: GameVersionRelay, sendMsg: Boolean = true): Boolean {
        if (con.relay?.admin === con) {
            return true
        }
        if (sendMsg) {
            sendMsg(con,localeUtil.getinput("err.noAdmin"))
        }
        return false
    }

    init {
        handler.register("help", "clientCommands.help") { _: Array<String>?, con: GameVersionRelay ->
            val str = StringBuilder(16)
            for (command in handler.commandList) {
                if (command.description.startsWith("#")) {
                    str.append("   ").append(command.text).append(if (command.paramText.isEmpty()) "" else " ")
                        .append(command.paramText).append(" - ").append(command.description.substring(1))
                        .append(Data.LINE_SEPARATOR)
                } else {
                    if ("HIDE" == command.description) {
                        continue
                    }
                    str.append("   ").append(command.text).append(if (command.paramText.isEmpty()) "" else " ")
                        .append(command.paramText).append(" - ").append(localeUtil.getinput(command.description))
                        .append(Data.LINE_SEPARATOR)
                }
            }
            sendMsg(con,str.toString())
        }

        handler.register("jump","<ip/id>", "#jump Server") { args: Array<String>, con: GameVersionRelay ->
            if (!isAdmin(con,false)) {
                con.sendCustomPacket(fromRelayJumpsToAnotherServer(args[0]))
            } else {
                sendMsg(con,"You Is ADMIN !")
            }
        }

        handler.register("kickx","<Name/Site>" ,"#Remove List") { args: Array<String>, con: GameVersionRelay ->
            if (isAdmin(con)) {
                val conTg: GameVersionRelay? = findPlayer(con,args[0])
                conTg?.let {
                    con.relayKickData.put("KICK"+it.playerRelay!!.uuid, Time.concurrentSecond()+60)
                    it.kick("you got kicked out of the server")
                    sendMsg(con,"Kick : ${args[0]} OK")
                }
            }
        }

        handler.register("ban","<Name/Site>" ,"#Remove List") { args: Array<String>, con: GameVersionRelay ->
            if (isAdmin(con)) {
                if (con.relay!!.isStartGame && con.relay!!.startGameTime < Time.concurrentSecond()) {
                    sendMsg(con,"It's been five minutes, no more kicks")
                    return@register
                }
                val conTg: GameVersionRelay? = findPlayer(con,args[0])
                conTg?.let {
                    con.relayKickData.put("KICK"+it.playerRelay!!.uuid, Int.MAX_VALUE)
                    con.relayKickData.put("BAN"+it.ip, Int.MAX_VALUE)
                    it.kick("you are banned by the server")
                    sendMsg(con,"BAN : ${args[0]} OK")
                }
            }
        }

        handler.register("mute","<Name/Site>" ,"#Remove List") { args: Array<String>, con: GameVersionRelay ->
            if (isAdmin(con)) {
                val conTg: GameVersionRelay? = findPlayer(con,args[0])

                conTg?.let {
                    it.playerRelay!!.mute = true
                    sendMsg(con,"Mute : ${args[0]} OK")
                }
            }
        }

        handler.register("unmute","<Name/Site>" ,"#Remove List") { args: Array<String>, con: GameVersionRelay ->
            if (isAdmin(con)) {
                val conTg: GameVersionRelay? = findPlayer(con,args[0])

                conTg?.let {
                    con.relayKickData.remove("MUTE"+it.playerRelay!!.uuid)
                    it.playerRelay!!.mute = false
                    sendMsg(con,"UnMute : ${args[0]} OK")
                }

            }
        }

        handler.register("allmute","#Remove List") { _: Array<String>, con: GameVersionRelay ->
            if (isAdmin(con)) {
                con.relay!!.allmute = !con.relay!!.allmute
                sendMsg(con,"全局禁言状态是 :  ${if (con.relay!!.allmute) "开启" else "关闭"}")
            }
        }

        PluginManage.runRegisterRelayClientCommands(handler)
    }

    private fun sendMsg(con: GameVersionRelay, msg: String) {
        con.sendCustomPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(msg,"RELAY-CN",5))
    }

    private fun findPlayer(con: GameVersionRelay, findIn: String): GameVersionRelay? {
        var conTg: GameVersionRelay? = null

        var findNameIn: String? = null
        var findSiteIn: Int? = null

        if (IsUtil.isNumeric(findIn)) {
            findSiteIn = findIn.toInt()-1
        } else {
            findNameIn = findIn
        }

        findNameIn?.let { findName ->
            var count = 0
            con.relay!!.abstractNetConnectIntMap.values.forEach {
                if (it.playerRelay!!.name.contains(findName,ignoreCase = true)) {
                    conTg = it
                    count++
                }
            }
            if (count > 1) {
                sendMsg(con,"目标不止一个, 请不要输入太短的玩家名")
                return@let
            }
            if (conTg == null) {
                sendMsg(con,"找不到玩家")
                return@let
            }
        }

        findSiteIn?.let {findSite ->
            con.relay!!.abstractNetConnectIntMap.values.forEach {
                if (it.playerRelay!!.site == findSite) {
                    conTg = it
                }
            }
            if (conTg == null) {
                sendMsg(con,"找不到玩家")
                return@let
            }
        }

        return conTg
    }
}