/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.game

import net.rwhps.server.core.Call
import net.rwhps.server.core.NetServer
import net.rwhps.server.core.thread.CallTimeTask
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.player.Player
import net.rwhps.server.game.event.EventType
import net.rwhps.server.game.simulation.gameFramework.GameData
import net.rwhps.server.net.Administration.PlayerInfo
import net.rwhps.server.plugin.event.AbstractEvent
import net.rwhps.server.util.Time
import net.rwhps.server.util.Time.millis
import net.rwhps.server.util.game.Events
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.Log.error
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * @author RW-HPS/Dr
 */
class Event : AbstractEvent {
    override fun registerPlayerJoinEvent(player: Player) {
        if (player.name.isBlank() || player.name.length > 20) {
            player.kickPlayer(player.getinput("kick.name.failed"))
            return
        }
        if (GameData.checkHess(player.name)) {
            player.kickPlayer("Forbidden name")
            return
        }

        if (Data.core.admin.bannedUUIDs.contains(player.uuid)) {
            try {
                player.kickPlayer(player.i18NBundle.getinput("kick.ban"))
            } catch (ioException: IOException) {
                error("[Player] Send Kick Player Error", ioException)
            }
            return
        }

        if (Data.core.admin.playerDataCache.containsKey(player.uuid)) {
            val info = Data.core.admin.playerDataCache[player.uuid]
            if (info.timesKicked > millis()) {
                try {
                    player.kickPlayer(player.i18NBundle.getinput("kick.you.time"))
                } catch (ioException: IOException) {
                    error("[Player] Send Kick Player Error", ioException)
                }
                return
            } else {
                player.muteTime = info.timeMute
            }
        }

        Call.sendSystemMessage(Data.i18NBundle.getinput("player.ent", player.name))


        if (Data.config.AutoStartMinPlayerSize != -1 &&
            Data.game.playerManage.playerGroup.size >= Data.config.AutoStartMinPlayerSize &&
            !Threads.containsTimeTask(CallTimeTask.AutoStartTask)) {
            var flagCount = 0
            Threads.newTimedTask(CallTimeTask.AutoStartTask,0,1,TimeUnit.SECONDS){
                if (Data.game.isStartGame) {
                    Threads.closeTimeTask(CallTimeTask.AutoStartTask)
                    return@newTimedTask
                }

                flagCount++

                if (flagCount < 60) {
                    if ((flagCount - 55) > 0) {
                        Call.sendSystemMessage(Data.i18NBundle.getinput("auto.start",(60 - flagCount)))
                    }
                    return@newTimedTask
                }

                Threads.closeTimeTask(CallTimeTask.AutoStartTask)
                Threads.closeTimeTask(CallTimeTask.PlayerAfkTask)

                if (Data.game.maps.mapData != null) {
                    Data.game.maps.mapData!!.readMap()
                }

                val enc = NetStaticData.RwHps.abstractNetPacket.getTeamDataPacket()

                Data.game.playerManage.playerGroup.eachAll { e: Player ->
                    try {
                        e.con!!.sendTeamData(enc)
                        e.con!!.sendStartGame()
                        e.lastMoveTime = Time.concurrentSecond()
                    } catch (err: IOException) {
                        error("Start Error", err)
                    }
                }

                Data.game.isStartGame = true
                if (Data.game.sharedControl) {
                    Data.game.playerManage.playerGroup.eachAll { it.sharedControl = true }
                }

                Data.game.playerManage.updateControlIdentifier()
                Call.testPreparationPlayer()
                Events.fire(EventType.GameStartEvent())
            }
        }
        // ConnectServer("127.0.0.1",5124,player.con)
    }

    override fun registerPlayerLeaveEvent(player: Player) {
        if (Data.config.OneAdmin && player.isAdmin && Data.game.playerManage.playerGroup.size > 1) {
            try {
                var p = Data.game.playerManage.playerGroup[0]
                if (p.name == Data.headlessName) {
                    p = Data.game.playerManage.playerGroup[1]
                }
                p.isAdmin = true
                Call.upDataGameData()
                player.isAdmin = false
                Call.sendSystemMessage("give.ok", p.name)
            } catch (ignored: IndexOutOfBoundsException) {
            }
        }

        Data.core.admin.playerDataCache.put(player.uuid, PlayerInfo(player.uuid, player.kickTime, player.muteTime))

        if (Data.game.isStartGame) {
            player.sharedControl = true
            Call.sendSystemMessage("player.dis", player.name)
            Call.sendTeamData()

        } else {
            Call.sendSystemMessage("player.disNoStart", player.name)
        }

        if (Data.config.AutoStartMinPlayerSize != -1 &&
            Data.game.playerManage.playerGroup.size <= Data.config.AutoStartMinPlayerSize &&
            !Threads.containsTimeTask(CallTimeTask.AutoStartTask)) {
            Threads.closeTimeTask(CallTimeTask.AutoStartTask)
        }
    }

    override fun registerGameStartEvent() {
        Data.core.admin.playerDataCache.clear()
        Log.clog("[Start New Game]")
    }

    override fun registerGameOverEvent() {
        if (Data.game.maps.mapData != null) {
            Data.game.maps.mapData!!.clean()
        }
        NetServer.reLoadServer()
        System.gc()
    }

    override fun registerPlayerBanEvent(player: Player) {
        Data.core.admin.bannedUUIDs.add(player.uuid)
        Data.core.admin.bannedIPs.add(player.con!!.ip)
        try {
            player.kickPlayer(player.i18NBundle.getinput("kick.ban"))
        } catch (ioException: IOException) {
            error("[Player] Send Kick Player Error", ioException)
        }
        Call.sendSystemMessage("ban.yes", player.name)
    }

    override fun registerPlayerIpBanEvent(player: Player) {
        Data.core.admin.bannedIPs.add(player.con!!.ip)
        try {
            player.kickPlayer("kick.ban")
        } catch (ioException: IOException) {
            error("[Player] Send Kick Player Error", ioException)
        }
        Call.sendSystemMessage("ban.yes", player.name)
    }
}
