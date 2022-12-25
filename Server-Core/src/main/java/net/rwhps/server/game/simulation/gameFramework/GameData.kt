/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.game.simulation.gameFramework

import com.corrodinggames.rts.game.n
import com.corrodinggames.rts.game.units.am
import com.corrodinggames.rts.gameFramework.j.al
import com.corrodinggames.rts.gameFramework.l
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.util.PacketType
import net.rwhps.server.util.alone.annotations.GameSimulationLayer
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.log.Log
import com.corrodinggames.rts.gameFramework.j.`as` as GameNetOutStream

object GameData {
    internal fun cleanCache() {
        FileUtil.getFolder(Data.Plugin_Cache_Path).delete()
    }

    @JvmStatic
    fun checkHess(name: String): Boolean {
        return (name == Data.headlessName && Data.game.playerManage.playerGroup.size > 1)
    }

    @GameSimulationLayer.GameSimulationLayer_KeyWords("gameSave")
    @JvmStatic
    fun getGameData(): Packet {
        val gameEngine: l = GameEngine.gameEngine
        val arVar = GameNetOutStream()
        arVar.c(0)
        arVar.a(gameEngine.bx)
        arVar.a(gameEngine.by)
        arVar.a(Data.game.income)
        arVar.a(1.0f)
        arVar.a(false)
        arVar.a(false)
        arVar.e("gameSave")
        gameEngine.ca.a(arVar)
        arVar.a("gameSave")
        return Packet(35,arVar.b(35).c)
    }

    @GameSimulationLayer.GameSimulationLayer_KeyWords("30")
    @JvmStatic
    fun getGameCheck(): Packet {
        val netEngine = GameEngine.netEngine
        val arVar = GameNetOutStream()
        arVar.a(netEngine.ah)
        arVar.a(netEngine.am.a)
        arVar.a(netEngine.am.b.size)
        val it: Iterator<*> = netEngine.am.b.iterator()
        while (it.hasNext()) {
            arVar.a((it.next() as al).b)
        }
        return Packet(PacketType.SYNC_CHECK,arVar.b(PacketType.SYNC_CHECK.typeInt).c)
    }

    @GameSimulationLayer.GameSimulationLayer_KeyWords("is victorious!")
    @JvmStatic
    fun getWin(team: Int): Boolean {
        val teamData: n = n.k(team) ?: return false

        return !teamData.b() && !teamData.G && !teamData.F && !teamData.E
    }

    @GameSimulationLayer.GameSimulationLayer_KeyWords("aiDifficulty is locked")
    @JvmStatic
    fun getPlayerBirthPointXY() {
        for (player in Data.game.playerManage.playerGroup) {
            n.k(player.site).let {
                var flagA = false
                var flagB = false
                var x: Float? = null
                var y: Float? = null
                var x2: Float? = null
                var y2: Float? = null

                for (amVar in am.bF()) {
                    if ((amVar is am) && !amVar.bV && amVar.bX == it) {
                        if (amVar.bO && !flagA) {
                            flagA = true;
                            x = amVar.eo
                            y = amVar.ep
                        }
                        if (amVar.bP && !flagB) {
                            flagB = true;
                            x2 = amVar.eo
                            y2 = amVar.ep
                        }
                    }
                }

                if (x == null) {
                    x = x2
                    y = y2
                }
                Log.clog("Site ${player.site} , $x $y")
            }
        }//: Seq<Array<Float>>
    }


    @GameSimulationLayer.GameSimulationLayer_KeyWords("exited!")
    @JvmStatic
    fun clean() {
        if (NetStaticData.ServerNetType != IRwHps.NetType.ServerProtocol) {
            return
        }
        val gameEngine: l = GameEngine.gameEngine
        gameEngine.bX.b("exited");
        InterruptedException().printStackTrace()
        Thread.sleep(100)
    }
}
