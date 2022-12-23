/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.game.simulation

import net.rwhps.server.core.Call
import net.rwhps.server.core.thread.CallTimeTask
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.player.Player
import net.rwhps.server.game.simulation.gameFramework.GameData
import net.rwhps.server.game.simulation.gameFramework.GameNet
import net.rwhps.server.game.simulation.pivatedata.PrivateClass_Player
import net.rwhps.server.plugin.event.AbstractEvent
import net.rwhps.server.util.log.Log
import java.util.concurrent.TimeUnit

class GameHeadlessEvent : AbstractEvent {
    override fun registerGameStartEvent() {
        Threads.newTimedTask(CallTimeTask.CallCheckTask,0,2, TimeUnit.SECONDS,Call::sendCheckData)
    }

    override fun registerGameOverEvent() {
        Threads.closeTimeTask(CallTimeTask.CallCheckTask)
        Log.debug("Stop CallCheckTask")

        Call.killAllPlayer()

        GameData.clean()
        Log.clog("Stop GameHeadless")
//        GameNet.newConnect()
        Log.clog("ReRun GameHeadless")
    }

    override fun registerPlayerJoinEvent(player: Player) {
        Data.game.playerManage.containsName(Data.headlessName)?.sendTeamData()
        player.playerPrivateData = PrivateClass_Player.getPlayerData(player.site)
    }
}
