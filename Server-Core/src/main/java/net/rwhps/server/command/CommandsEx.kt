/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.command

import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.global.Relay
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.net.StartNet
import net.rwhps.server.net.core.IRwHps.NetType.*
import net.rwhps.server.net.core.server.AbstractNetConnect
import net.rwhps.server.util.PacketType
import net.rwhps.server.util.game.CommandHandler
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author RW-HPS/Dr
 */
class CommandsEx(handler: CommandHandler) {
    private fun registerCore(handler: CommandHandler) {
        handler.register("hi", "") { _: Array<String>?, con: AbstractNetConnect ->
            val out = GameOutputStream()

            if (NetStaticData.ServerNetType == NullProtocol) {
                out.writeString("SERVER CLOSE")
            } else {
                out.writeString("ServerNetType")
                out.writeString(NetStaticData.ServerNetType.name)
            }

            when (NetStaticData.ServerNetType) {
                ServerProtocol, ServerProtocolOld, ServerTestProtocol -> {
                    out.writeString("PlayerSize")
                    out.writeInt(Data.game.playerManage.playerGroup.size)
                    out.writeString("MaxPlayer")
                    out.writeInt(Data.config.MaxPlayer)

                    out.writeString("MapName")
                    out.writeString(Data.game.maps.mapName)
                    out.writeString("Income")
                    out.writeString("Credits")
                    out.writeInt(when (Data.game.credits) {
                        1 -> 0
                        2 -> 1000
                        3 -> 2000
                        4 -> 5000
                        5 -> 10000
                        6 -> 50000
                        7 -> 100000
                        8 -> 200000
                        0 -> 4000
                        else -> 0
                    })
                    out.writeString("NoNukes")
                    out.writeBoolean(Data.game.noNukes)
                    out.writeString("InitUnit")
                    out.writeInt(Data.game.initUnit)
                    out.writeString("Mist")
                    out.writeInt(Data.game.mist)
                    out.writeString("SharedControl")
                    out.writeBoolean(Data.game.sharedControl)
                }
                RelayProtocol,RelayMulticastProtocol -> {
                    out.writeString("PlayerSize")
                    val size = AtomicInteger()
                    NetStaticData.startNet.eachAll { e: StartNet -> size.addAndGet(e.getConnectSize()) }
                    out.writeInt(size.get())

                    out.writeString("RoomAllSize")
                    out.writeInt(Relay.roomAllSize)
                    out.writeString("RoomNoStartSize")
                    out.writeInt(Relay.roomNoStartSize)
                }
                NullProtocol,DedicatedToTheBackend -> {}
            }
            con.sendExCommand(out.createPacket(PacketType.GET_SERVER_INFO))
        }
    }

    companion object {
        private val localeUtil = Data.i18NBundle
    }

    init {
        registerCore(handler)
    }
}