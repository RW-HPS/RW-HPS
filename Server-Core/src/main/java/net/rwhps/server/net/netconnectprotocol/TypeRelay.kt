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
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.core.ConnectionAgreement
import net.rwhps.server.net.core.DataPermissionStatus.RelayStatus
import net.rwhps.server.net.core.DataPermissionStatus.RelayStatus.*
import net.rwhps.server.net.core.TypeConnect
import net.rwhps.server.net.core.server.AbstractNetConnect
import net.rwhps.server.net.netconnectprotocol.realize.GameVersionRelay
import net.rwhps.server.util.PacketType.*
import net.rwhps.server.util.ReflectionUtils
import net.rwhps.server.util.game.CommandHandler

/**
 * Parse the [net.rwhps.server.net.core.IRwHps.NetType.RelayProtocol] protocol
 * @property con                GameVersionRelay
 * @property conClass           Initialize
 * @property abstractNetConnect AbstractNetConnect
 * @property version            Parser version
 * @author RW-HPS/Dr
 */
open class TypeRelay : TypeConnect {
    val con: GameVersionRelay
    var conClass: Class<out GameVersionRelay>? = null

    override val abstractNetConnect: AbstractNetConnect
        get() = con

    constructor(con: GameVersionRelay) {
        this.con = con
    }
    constructor(con: Class<out GameVersionRelay>) {
        // will not be used ; just override the initial value to avoid refusing to compile
        this.con = ReflectionUtils.accessibleConstructor(con, ConnectionAgreement::class.java).newInstance(ConnectionAgreement())

        // use for instantiation
        conClass = con
    }

    override fun getTypeConnect(connectionAgreement: ConnectionAgreement): TypeConnect {
         return TypeRelay(ReflectionUtils.accessibleConstructor(conClass!!, ConnectionAgreement::class.java).newInstance(connectionAgreement))
    }

    @Throws(Exception::class)
    override fun typeConnect(packet: Packet) {
        if (relayCheck(packet)) {
            return
        }

        val permissionStatus = con.permissionStatus

        // CPU branch prediction
        if (permissionStatus == RelayStatus.HostPermission) {
            when (packet.type) {
                PACKET_FORWARD_CLIENT_TO  -> {
                    con.addRelaySend(packet)
                    return
                }
                CHAT -> {
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
                            }
                        }
                    }
                    return
                }
                else -> {}
            }
        }

        when (packet.type) {
            // Thanks Discord vlad75724#3575
            // Fix Server Not use 50M Maps
            START_GAME,
            SERVER_INFO,
            CHAT,
            TEAM_LIST -> {
            }
            SYNCCHECKSUM_STATUS,
            HEART_BEAT_RESPONSE,
            GAMECOMMAND_RECEIVE -> {
                con.sendResultPing(packet)
            }
            HEART_BEAT -> {
                con.addGroup(packet)
                con.getPingData(packet)
            }
            PACKET_FORWARD_CLIENT_TO_REPEATED -> {
            }
            ACCEPT_START_GAME -> {
                con.relay!!.isStartGame = true
                con.sendResultPing(packet)
            }
            DISCONNECT -> con.disconnect()
            SERVER_DEBUG_RECEIVE -> con.debug(packet)
            else -> con.sendResultPing(packet)

        }
    }

    protected fun relayCheck(packet: Packet): Boolean {
        con.lastReceivedTime()

        val permissionStatus = con.permissionStatus

        if (permissionStatus.ordinal < RelayStatus.PlayerPermission.ordinal) {
            when (permissionStatus) {
                // Initial Connection
                InitialConnection -> {
                    if (packet.type == PREREGISTER_INFO_RECEIVE) {
                        con.permissionStatus = GetPlayerInfo
                        con.setCachePacket(packet)
                        val registerServer = GameOutputStream()
                        registerServer.writeString(Data.SERVER_ID_RELAY_GET)
                        registerServer.writeInt(1)
                        registerServer.writeInt(0)
                        registerServer.writeInt(0)
                        registerServer.writeString("com.corrodinggames.rts.server")
                        registerServer.writeString(Data.SERVER_RELAY_UUID)
                        registerServer.writeInt("Dr @ 2022".hashCode())
                        con.sendPacket(registerServer.createPacket(PREREGISTER_INFO))
                    } else if (packet.type == SERVER_DEBUG_RECEIVE) {
                        con.debug(packet)
                    }
                    return true
                }
                GetPlayerInfo -> {
                    if (packet.type == REGISTER_PLAYER) {
                        con.relayRegisterConnection(packet)
                        // Wait Certified
                        con.permissionStatus = WaitCertified
                        con.sendRelayServerInfo()
                        con.sendVerifyClientValidity()
                    }
                    return true
                }
                WaitCertified -> {
                    if (packet.type == RELAY_POW_RECEIVE) {
                        if (con.receiveVerifyClientValidity(packet)) {
                            // Certified End
                            con.permissionStatus = CertifiedEnd
                            if (!Data.config.SingleUserRelay) {
                                con.relayDirectInspection()
                            } else {
                                NetStaticData.relay.setAddSize()
                                // No HOST
                                if (NetStaticData.relay.admin == null) {
                                    // Set This is HOST
                                    con.sendRelayServerId()
                                } else {
                                    // Join RELAY
                                    con.addRelayConnect()
                                }
                            }
                        } else {
                            con.sendVerifyClientValidity()
                        }
                    } else {
                        con.disconnect()
                    }
                    return true
                }
                CertifiedEnd -> {
                    if (packet.type == RELAY_118_117_RETURN) {
                        con.sendRelayServerTypeReply(packet)
                    }
                    return true
                }
                // 你肯定没验证
                else -> {
                    con.disconnect()
                }
            }
        }
        return false
    }

    override val version: String
        get() = "RELAY 1.1.0"
}