/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.netconnectprotocol.realize

import net.rwhps.server.data.global.Data
import net.rwhps.server.data.player.Player
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.util.Time
import java.io.IOException

/**
 * Provides support for most common packages for the server
 *
 * @author RW-HPS/Dr
 */
class GameVersionPacketOld : GameVersionPacket() {
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
        }
    }
}