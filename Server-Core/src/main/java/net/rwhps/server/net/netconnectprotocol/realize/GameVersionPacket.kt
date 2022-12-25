/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.netconnectprotocol.realize

import net.rwhps.server.data.global.Cache
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.player.Player
import net.rwhps.server.game.GameMaps
import net.rwhps.server.game.GameUnitType
import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.io.output.CompressOutputStream
import net.rwhps.server.io.packet.GameCommandPacket
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.core.AbstractNetPacket
import net.rwhps.server.net.netconnectprotocol.internal.server.*
import net.rwhps.server.net.netconnectprotocol.internal.server.chatMessagePacketInternal
import net.rwhps.server.net.netconnectprotocol.internal.server.gameTickCommandPacketInternal
import net.rwhps.server.net.netconnectprotocol.internal.server.gameTickCommandsPacketInternal
import net.rwhps.server.net.netconnectprotocol.internal.server.gameTickPacketInternal
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.IsUtil
import net.rwhps.server.util.PacketType
import net.rwhps.server.util.Time
import net.rwhps.server.util.alone.annotations.MainProtocolImplementation
import net.rwhps.server.util.encryption.Game
import net.rwhps.server.util.encryption.digest.DigestUtil
import net.rwhps.server.util.log.Log.error
import java.io.IOException
import java.math.BigInteger

/**
 * Provides support for most common packages for the server
 *
 * @author RW-HPS/Dr
 */
@MainProtocolImplementation
open class GameVersionPacket : AbstractNetPacket {
    @Throws(IOException::class)
    override fun getSystemMessagePacket(msg: String): Packet =
        chatMessagePacketInternal(msg, "SERVER", 5)

    @Throws(IOException::class)
    override fun getChatMessagePacket(msg: String, sendBy: String, team: Int): Packet =
        chatMessagePacketInternal(msg, sendBy, team)

    @Throws(IOException::class)
    override fun getPingPacket(player: Player): Packet {
        player.timeTemp = System.currentTimeMillis()
        val o = GameOutputStream()
        o.writeLong(0L)
        o.writeByte(0)
        return o.createPacket(PacketType.HEART_BEAT)
    }

    @Throws(IOException::class)
    override fun getTickPacket(tick: Int): Packet =
        gameTickPacketInternal(tick)

    @Throws(IOException::class)
    override fun getGameTickCommandPacket(tick: Int, cmd: GameCommandPacket): Packet =
        gameTickCommandPacketInternal(tick,cmd)

    @Throws(IOException::class)
    override fun getGameTickCommandsPacket(tick: Int, cmd: Seq<GameCommandPacket>): Packet =
        gameTickCommandsPacketInternal(tick,cmd)

    @Throws(IOException::class)
    override fun getTeamDataPacket(startGame: Boolean): CompressOutputStream {
        Data.game.playerManage.updateControlIdentifier()

        val enc = CompressOutputStream.getGzipOutputStream("teams", true)
        Data.game.playerManage.runPlayerArrayDataRunnable { player: Player? ->
            try {
                if (player == null) {
                    enc.writeBoolean(false)
                } else {
                    enc.writeBoolean(true)
                    enc.writeInt(0)
                    writePlayer(player, enc, startGame)
                }
            } catch (e: Exception) {
                error("[ALL/Player] Get Server Team Info", e)
            }
        }
        return enc
    }

    // 0->本地 1->自定义 2->保存的游戏
    @Throws(IOException::class)
    override fun getStartGamePacket(): Packet {
        val o = GameOutputStream()
        o.writeByte(0)
        // 0->本地 1->自定义 2->保存的游戏
        o.writeInt(Data.game.maps.mapType.ordinal)
        if (Data.game.maps.mapType == GameMaps.MapType.defaultMap) {
            o.writeString("maps/skirmish/" + Data.game.maps.mapPlayer + Data.game.maps.mapName + ".tmx")
        } else {
            o.flushMapData(Data.game.maps.mapData!!.mapSize, Data.game.maps.mapData!!.bytesMap!!)
            o.writeString("SAVE:" + Data.game.maps.mapName + ".tmx")
        }
        o.writeBoolean(false)
        return o.createPacket(PacketType.START_GAME)
    }

    @Throws(IOException::class)
    override fun getPacketMapName(bytes: ByteArray): String {
        GameInputStream(bytes).use { stream ->
            stream.readString()
            stream.readInt()
            stream.readInt()
            return stream.readString()
        }
    }

    override fun getDeceiveGameSave(): Packet {
        val cPacket: Packet? = Cache.packetCache["getDeceiveGameSave"]
        if (IsUtil.notIsBlank(cPacket)) {
            return cPacket!!
        }

        val o = GameOutputStream()
        o.writeByte(0)
        o.writeInt(0)
        o.writeInt(0)
        o.writeFloat(0)
        o.writeFloat(0)
        o.writeBoolean(true)
        o.writeBoolean(false)
        val gzipEncoder = CompressOutputStream.getGzipOutputStream("gameSave", false)
        gzipEncoder.writeString("This is RW-HPS [Deceive Get GameSave]!")
        o.flushEncodeData(gzipEncoder)

        val cachePacket = o.createPacket(PacketType.SYNC)
        Cache.packetCache.put("getDeceiveGameSave",cachePacket)

        return cachePacket
    }

    override fun gameSummonPacket(index: Int, unit: String, x: Float, y: Float, size: Int): GameCommandPacket {
        Data.game.playerManage.updateControlIdentifier()

        val outStream = GameOutputStream()
        outStream.writeByte(index)
        outStream.writeBoolean(true)
        // 建造
        outStream.writeInt(GameUnitType.GameActions.BUILD.ordinal)


        var unitID = -2
        if (IsUtil.notIsNumeric(unit)) {
            GameUnitType.GameUnits.values().forEach {
                if (it.name.equals(unit,ignoreCase = true)) {
                    unitID = it.ordinal
                    return@forEach
                }
            }
        } else {
            unitID = unit.toInt()
        }

        outStream.writeInt(unitID)

        if (unitID == -2) {
            outStream.writeString(unit)
        }


        // X
        outStream.writeFloat(x)
        // Y
        outStream.writeFloat(y)
        // Tager
        outStream.writeLong(-1L)
        // 如果生成圆 那么这就是半径
        outStream.writeByte(size)
        outStream.writeFloat(1)
        outStream.writeFloat(1)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)
        outStream.writeBoolean(false)

        //
        outStream.writeInt(-1)
        outStream.writeInt(-1)

        outStream.writeBoolean(false)
        outStream.writeBoolean(false)

        outStream.writeInt(0)

        outStream.writeBoolean(false)
        outStream.writeBoolean(false)


        outStream.writeLong(-1)
        outStream.writeString("-1")
        outStream.writeBoolean(false)
        outStream.writeShort(Data.game.playerManage.sharedControlPlayer.toShort())
        // System action
        outStream.writeBoolean(true)
        outStream.writeByte(0)
        outStream.writeFloat(0f)
        outStream.writeFloat(0f)
        //action type
        outStream.writeInt(5)
        outStream.writeInt(0)
        outStream.writeBoolean(false)
        return GameCommandPacket(index, outStream.getPacketBytes())
    }

    @Throws(IOException::class)
    override fun getExitPacket(): Packet {
        val cPacket: Packet? = Cache.packetCache["getExitPacket"]
        if (IsUtil.notIsBlank(cPacket)) {
            return cPacket!!
        }

        val cachePacket = playerExitPacketInternal()
        Cache.packetCache.put("getExitPacket",cachePacket)

        return cachePacket
    }

    @Throws(IOException::class)
    override fun writePlayer(player: Player, stream: GameOutputStream, startGame: Boolean) {
        with (stream) {
            if (startGame) {
                writeByte(player.site)
                writeInt(player.ping)
                // 玩家是否可控
                writeBoolean(player.controlThePlayer)
                writeBoolean(player.sharedControl)
                return
            }
            writeByte(player.site)
            writeInt(Data.game.credits)
            writeInt(player.team)
            writeIsString(player.name)
            writeBoolean(false)

            /* -1 N/A ; -2 -  ; -99 HOST */
            writeInt(if (player.con != null) player.ping else if (Time.concurrentSecond()-player.lastMoveTime > 120) -1 else -2)
            writeLong(System.currentTimeMillis())
            /* MS */
            writeBoolean(false)
            writeInt(0)

            writeInt(player.site)
            writeByte(0)

            /* 共享控制 */
            writeBoolean(Data.game.sharedControl)
            /* 是否掉线 */
            writeBoolean(player.sharedControl)
            /* 是否投降 */
            writeBoolean(false)
            writeBoolean(false)
            writeInt(-9999)

            writeBoolean(false)
            // 延迟后显示 （HOST)
            writeInt(if (player.isAdmin) 1 else 0)

            // Ai Difficulty Override
            writeIsInt(1)
            // Player Start Unit
            writeIsInt(player.startUnit)
            // ?
            writeIsInt(0)
            // Player Color
            writeIsInt(player.color)
            // Game Player Color
            writeInt(if (player.color > 0) player.color else -1)
        }
    }
}