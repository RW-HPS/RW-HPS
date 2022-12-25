/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.game.simulation.gameFramework

import com.corrodinggames.rts.gameFramework.j.ad
import com.corrodinggames.rts.gameFramework.j.c
import net.rwhps.server.data.global.Data
import net.rwhps.server.util.alone.annotations.GameSimulationLayer
import net.rwhps.server.util.log.Log
import java.io.IOException

object GameNet {
    @GameSimulationLayer.GameSimulationLayer_KeyWords("lastNetworkPlayerName")
    fun newConnect() {
        try {
            val settingsEngine = GameEngine.settingsEngine
            val netEngine = GameEngine.netEngine

            settingsEngine.lastNetworkPlayerName = Data.headlessName

            val playerName = settingsEngine.lastNetworkPlayerName

            netEngine.y = playerName
            val kVar2 = ad.b("127.0.0.1:${Data.config.Port}",false)
            netEngine.a(kVar2)
            val it: Iterator<*> = netEngine.aM.iterator()
            while (it.hasNext()) {
                (it.next() as c).i = true
            }
        } catch (e2: IOException) {
            Log.error("[GameCore] NewConnect Error",e2)
        }
    }
}