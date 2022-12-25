/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.core.server

import net.rwhps.server.data.player.Player
import net.rwhps.server.io.output.CompressOutputStream
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.core.DataPermissionStatus
import net.rwhps.server.util.log.Log
import org.intellij.lang.annotations.JdkConstants
import org.jetbrains.annotations.Nls
import java.io.IOException

/**
 * Only provide interface but not implement
 * As the interface of game CoreNet, it provides various version support for GameServer
 * @author RW-HPS/Dr
 * @date 2021/7/31/ 14:14
 */
interface AbstractNetConnectServer {
    val permissionStatus: DataPermissionStatus.ServerStatus

    /**
     * Acquire players
     * @return Player
     */
    val player: Player

    /**
     * Send the message package named by the system
     * SERVER: ...
     * @param msg The message
     */
    fun sendSystemMessage(@Nls msg: String)

    /**
     * Send a message named by username
     * @param msg String
     * @param sendBy String
     * @param team Int
     */
    fun sendChatMessage(@Nls msg: String, sendBy: String, team: Int)

    /**
     * Send server info
     * @param utilData 是否发送UnitData
     * @throws IOException Error
     */
    @Throws(IOException::class)
    fun sendServerInfo(utilData: Boolean)

    /**
     * Send team pack
     * @param gzip GzipPacket
     */
    fun sendTeamData(gzip: CompressOutputStream)

    /**
     * send Surrender
     */
    fun sendSurrender()

    /**
     * Kick the player
     * @param reason Reason
     * @throws IOException Error
     */
    @Throws(IOException::class)
    fun sendKick(@Nls reason: String)

    /**
     * Ping
     */
    fun sendPing()

    /**
     * Send game start package
     * @throws IOException Error
     */
    @Throws(IOException::class)
    fun sendStartGame()

    /**
     * wrong password
     * @throws IOException err
     */
    @Throws(IOException::class)
    fun sendErrorPasswd()

    /**
     * Send reconnect packet
     * @param packet ByteBuf
     */
    fun sendGameSave(packet: Packet)

    /**
     * Accept language pack
     * @param p Packet
     * @throws IOException Error
     */
    @Throws(IOException::class)
    fun receiveChat(p: Packet)

    /**
     * Accept displacement package
     * @param p Packet
     * @throws IOException Error
     */
    @Throws(IOException::class)
    fun receiveCommand(p: Packet)

    /**
     * Check player data correctness
     * @param p Packet
     * @return Pass
     * @throws IOException Error
     */
    @Throws(IOException::class)
    fun receiveCheckPacket(packet: Packet)

    /**
     * Extract the GameSave package
     * @param packet Packet
     * @return Packet
     * @throws IOException Error
     */
    @Throws(IOException::class)
    fun getGameSaveData(packet: Packet): ByteArray

    /**
     * Get player information and register
     * @param p Packet
     * @return Registration status
     * @throws IOException err
     */
    @JdkConstants.BoxLayoutAxis
    @Throws(IOException::class)
    fun getPlayerInfo(p: Packet): Boolean

    /**
     * Register connection
     * @param p Packet
     * @throws IOException err
     */
    @Throws(IOException::class)
    fun registerConnection(p: Packet)

    fun gameSummon(unit: String, x: Float, y: Float)


    fun reConnect() {
        try {
            sendKick("不支持重连 || Does not support reconnection")
        } catch (e: IOException) {
            Log.error("(", e)
        }
    }

    fun sync()

    /**
     * Server type
     * @param msg Message
     * @param run Callback
     */
    fun sendRelayServerType(msg: String, run: ((String) -> Unit)? = null)

    /**
     * Type Reply
     */
    fun sendRelayServerTypeReply(packet: Packet)
}