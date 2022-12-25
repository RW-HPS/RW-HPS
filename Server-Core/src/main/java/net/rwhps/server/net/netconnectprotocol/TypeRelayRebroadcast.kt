/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.netconnectprotocol

import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.core.ConnectionAgreement
import net.rwhps.server.net.core.DataPermissionStatus.RelayStatus
import net.rwhps.server.net.core.TypeConnect
import net.rwhps.server.net.netconnectprotocol.realize.GameVersionRelay
import net.rwhps.server.net.netconnectprotocol.realize.GameVersionRelayRebroadcast
import net.rwhps.server.util.PacketType
import net.rwhps.server.util.ReflectionUtils
import net.rwhps.server.util.game.CommandHandler

/**
 * Parse the [net.rwhps.server.net.core.IRwHps.NetType.RelayMulticastProtocol] protocol
 * @property con                GameVersionRelay
 * @property conClass           Initialize
 * @property abstractNetConnect AbstractNetConnect
 * @property version            Parser version
 * @author RW-HPS/Dr
 */
class TypeRelayRebroadcast : TypeRelay {
    constructor(con: GameVersionRelay) : super(con)

    constructor(con: Class<out GameVersionRelayRebroadcast>) : super(con)

    override fun getTypeConnect(connectionAgreement: ConnectionAgreement): TypeConnect {
        return TypeRelayRebroadcast(ReflectionUtils.accessibleConstructor(conClass!!, ConnectionAgreement::class.java).newInstance(connectionAgreement))
    }

    @Throws(Exception::class)
    override fun typeConnect(packet: Packet) {
        if (relayCheck(packet)) {
            return
        }

        val permissionStatus = con.permissionStatus

        if (permissionStatus == RelayStatus.HostPermission) {
            when (packet.type) {
                PacketType.PACKET_FORWARD_CLIENT_TO  -> {
                    con.addRelaySend(packet)
                    return
                }
                PacketType.PACKET_FORWARD_CLIENT_TO_REPEATED  -> {
                    con.multicastAnalysis(packet)
                    return
                }
                else -> {
                    con.setlastSentPacket(packet)
                    // Command?
                    if (packet.type == PacketType.CHAT) {
                        GameInputStream(packet).use {
                            val message = it.readString()
                            it.skip(1)
                            if (it.readIsString() == con.name) {
                                if (message.startsWith(".")) {
                                    val response = Data.RELAY_COMMAND.handleMessage(message, con)
                                    if (response == null || response.type == CommandHandler.ResponseType.noCommand) {
                                    } else if (response.type != CommandHandler.ResponseType.valid) {
                                        val text: String = when (response.type) {
                                            CommandHandler.ResponseType.manyArguments -> "Too many arguments. Usage: " + response.command.text + " " + response.command.paramText
                                            CommandHandler.ResponseType.fewArguments -> "Too few arguments. Usage: " + response.command.text + " " + response.command.paramText
                                            else -> return@use
                                        }
                                        con.sendPacket(NetStaticData.RwHps.abstractNetPacket.getSystemMessagePacket(text))
                                    }
                                    return
                                }
                            }
                        }
                    }
                }
            }
        }


        when (packet.type) {
            // 快速抛弃
            PacketType.START_GAME,
            PacketType.CHAT,
            PacketType.SERVER_INFO,
            PacketType.TEAM_LIST -> {
                //Log.clog("抛弃 {0}",packet.type.typeInt)
            }

            PacketType.HEART_BEAT -> {
                con.getPingData(packet)
                // 直接组播 Ping 包, 避免被 [PacketType.PACKET_FORWARD_CLIENT_TO_REPEATED] 转发 丢失性能?
                //con.addGroup(packet)
            }

            PacketType.SYNCCHECKSUM_STATUS,
            PacketType.HEART_BEAT_RESPONSE,
            PacketType.GAMECOMMAND_RECEIVE -> {
                con.sendResultPing(packet)
            }

            else -> {
                when (packet.type) {
                    PacketType.CHAT_RECEIVE -> con.receiveChat(packet)
                    PacketType.REGISTER_PLAYER -> con.relayRegisterConnection(packet)
                    PacketType.ACCEPT_START_GAME -> {
                        con.relay!!.isStartGame = true
                        con.sendResultPing(packet)
                    }
                    PacketType.DISCONNECT -> con.disconnect()
                    PacketType.SERVER_DEBUG_RECEIVE -> con.debug(packet)
                    PacketType.GET_SERVER_INFO_RECEIVE -> con.exCommand(packet)
                    else -> con.sendResultPing(packet)
                }
            }
        }

    }

    override val version: String
        get() = "${Data.SERVER_CORE_VERSION}: 2.2.0"
}