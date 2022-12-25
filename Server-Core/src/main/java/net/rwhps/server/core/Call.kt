/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.core

import net.rwhps.server.core.thread.CallTimeTask
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.core.thread.Threads.newCountdown
import net.rwhps.server.core.thread.TimeTaskData
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.player.Player
import net.rwhps.server.game.event.EventType.GameOverEvent
import net.rwhps.server.game.simulation.gameFramework.GameData
import net.rwhps.server.io.packet.GameCommandPacket
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.Time
import net.rwhps.server.util.game.Events
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.Log.error
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
/**
 * @author RW-HPS/Dr
 */
object Call {
    /**
     * 对指定玩家发送一条统一的消息
     *
     * @param player 玩家
     * @param text 消息
     */
    @JvmStatic
    fun sendMessage(player: Player, text: String) {
        try {
            NetStaticData.groupNet.broadcast(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(text, player.name, player.team))
        } catch (e: IOException) {
            error("[ALL] Send Player Chat Error", e)
        }
    }

    /**
     * 对指定玩家发送一条本地化的消息
     *
     * @param player 玩家
     * @param text 消息
     * @param obj 参数
     */
    @JvmStatic
    fun sendMessageLocal(player: Player, text: String, vararg obj: Any) {
        Data.game.playerManage.playerGroup.eachAll { e: Player -> e.sendMessage(player, e.i18NBundle.getinput(text, *obj)) }
    }

    /**
     * 对指定队伍发送一条统一的消息
     *
     * @param team 队伍ID
     * @param player Player
     * @param text String
     */
    @JvmStatic
    fun sendTeamMessage(team: Int, player: Player, text: String) {
        Data.game.playerManage.playerGroup.eachAllFind({ e: Player -> e.team == team }) { p: Player -> p.sendMessage(player, "[TEAM] $text") }
    }

    @JvmStatic
    fun sendSystemTeamMessageLocal(team: Int, text: String, vararg obj: Any) {
        Data.game.playerManage.playerGroup.eachAllFind({ e: Player -> e.team == team }) { p: Player -> p.sendSystemMessage("[TEAM] " + p.i18NBundle.getinput(text, *obj)) }
    }

    @JvmStatic
    fun sendSystemMessage(text: String) {
        try {
            NetStaticData.groupNet.broadcast(NetStaticData.RwHps.abstractNetPacket.getSystemMessagePacket(text))
        } catch (e: IOException) {
            error("[ALL] Send System Chat Error", e)
        }
    }

    @JvmStatic
    fun sendSystemMessageLocal(text: String, vararg obj: Any) {
        Data.game.playerManage.playerGroup.eachAll { e: Player -> e.sendSystemMessage(e.i18NBundle.getinput(text, *obj)) }
    }

    @JvmStatic
    fun sendSystemMessage(text: String, vararg obj: Any) {
        Data.game.playerManage.playerGroup.eachAll { e: Player -> e.sendSystemMessage(e.i18NBundle.getinput(text, *obj)) }
    }

    @JvmStatic
    fun sendTeamData() {
        if (Data.game.gameReConnectPaused) {
            return
        }
        try {
            val enc = NetStaticData.RwHps.abstractNetPacket.getTeamDataPacket()
            Data.game.playerManage.playerGroup.eachAll { e: Player -> e.con!!.sendTeamData(enc) }
        } catch (e: IOException) {
            error("[ALL] Send Team Error", e)
        }
    }

    /**
     * 更新一次全部玩家Ping
     */
    @JvmStatic
    fun sendPlayerPing() {
        Data.game.playerManage.playerGroup.eachAll { e: Player -> e.con!!.sendPing() }
    }

    /**
     * 检查一次全部玩家的数据
     */
    @JvmStatic
    fun sendCheckData() {
        NetStaticData.groupNet.broadcast(GameData.getGameCheck())
    }

    @JvmStatic
    @JvmOverloads
    fun sendSync(displayInformation: Boolean = true) {
        try {
            Data.game.gameReConnectPaused = true
            if (displayInformation) {
                sendSystemMessage("同步中 请耐心等待 不要退出 期间会短暂卡住！！ 需要30s-60s")
            }
            NetStaticData.groupNet.broadcast(GameData.getGameData())
        } catch (e: Exception) {
            error("[Player] Send GameSave ReConnect Error", e)
        } finally {
            Data.game.gameReConnectPaused = false
        }
    }

    @JvmStatic
    @JvmOverloads
    fun upDataGameData(unitData: Boolean = false) {
        Data.game.playerManage.playerGroup.eachAll { e: Player ->
            try {
                e.con!!.sendServerInfo(unitData)
            } catch (err: IOException) {
                error("[ALL] Send System Info Error", err)
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun killAllPlayer(msg: String = "Game Over") {
        Data.game.playerManage.playerGroup.eachAll { e: Player ->
            try {
                e.kickPlayer(msg)
            } catch (err: IOException) {
                error("[ALL] Kick All Player Error", e)
            }
        }
    }

    @JvmStatic
    fun disAllPlayer() {
        Data.game.playerManage.playerGroup.eachAll { e: Player -> e.con!!.disconnect() }
    }

    @JvmStatic
    fun testPreparationPlayer() {
        val timer = Timer()
        timer.schedule(RandyTask(timer), 200, 200)
    }

    /**
     * 检测玩家是否全部收到StartGame-Packet
     * @property loadTime 失败次数
     * @property loadTimeMaxTry 尝试最大次数
     * @property start 是否收到
     */
    private class RandyTask(private val timer: Timer) : TimerTask() {
        private var loadTime = 0
        private val loadTimeMaxTry = Data.game.playerManage.playerGroup.size * 3
        private var start = true

        override fun run() {
            start = true

            Data.game.playerManage.playerGroup.eachAll { p: Player ->
                if (!p.start) {
                    loadTime += 1
                    start = false
                }
            }

            if (loadTime > loadTimeMaxTry) {
                sendSystemMessageLocal("start.testNo")
                val timerNew = Timer()

                TimeTaskData.CallTickTask = SendGameTickCommand()
                timerNew.schedule(TimeTaskData.CallTickTask, 100, Data.config.TickTime.toLong())
                TimeTaskData.CallTickPool = timerNew

                stop()
            }

            if (start) {
                sendSystemMessageLocal("start.testYes")
                val timerNew = Timer()

                TimeTaskData.CallTickTask = SendGameTickCommand()
                timerNew.schedule(TimeTaskData.CallTickTask, 0, Data.config.TickTime.toLong())
                TimeTaskData.CallTickPool = timerNew

                stop()
            }
        }

        private fun stop() {
            cancel()
            timer.cancel()
        }
    }

    // Welcome to Bugs RW-HPS ! ---- 来自于 1.2.0-M2到5.2.0-M1-DEV才被修复的Bug
    /**
     *
     * @property oneSay Boolean
     * @property forcedReturn Boolean
     * @constructor
     */
    private class SendGameTickCommand : TimerTask() {
        private var oneSay = true
        private var forcedClose = false

        private val tick = Data.config.Tick

        @Volatile
        private var forcedReturn = false
        private val comm = Seq<GameCommandPacket>(16)

        init {
            Threads.newTimedTask(CallTimeTask.AutoCheckTask,0,1,TimeUnit.SECONDS) {
                var lastWinTeam: Int = -1
                var lastWinCount = 90
                Data.game.playerManage.runPlayerArrayDataRunnable(true) {
                    if (it == null) {
                        return@runPlayerArrayDataRunnable
                    }
                    if (it.survive && it.team != lastWinTeam) {
                        lastWinTeam = it.team
                        lastWinCount++
                    }
                }
                if (lastWinCount == 1) {
                    val last = Data.game.playerManage.getPlayersNameOnTheSameTeam(lastWinTeam).toArray(String::class.java).contentToString()
                    Log.clog("Last Win Player: {0}",last)
                    sendSystemMessageLocal("survive.player",last)
                    Threads.closeTimeTask(CallTimeTask.AutoCheckTask)
                }
                Data.game.playerManage.updateControlIdentifier()
            }

            /*
            Threads.newTimedTask(CallTimeTask.TestStatus,0,5,TimeUnit.SECONDS) {
                Data.game.playerManage.runPlayerArrayDataRunnable(true) {
                    if (it == null) {
                        return@runPlayerArrayDataRunnable
                    }
                    try {
                        Log.clog("Name: ${it.name}")
                        Log.clog("credits: ${it.credits}")
                        Log.clog("survive: ${it.survive}")
                        Log.clog("unitsKilled: ${it.unitsKilled}")
                        Log.clog("buildingsKilled: ${it.buildingsKilled}")
                    } catch (e: Exception) {
                        Log.error(e)
                    }
                }
            }*/
        }

        override fun run() {
            // 检测人数是否符合Gameover

            when (Data.game.playerManage.playerGroup.size) {
                0 -> gr()
                1,2 -> if (oneSay) {
                    oneSay = false
                    sendSystemMessageLocal("gameOver.oneMin")
                    newCountdown(CallTimeTask.GameOverTask, 1, TimeUnit.MINUTES) {gr()}
                }
                else -> {
                    if (Threads.containsTimeTask(CallTimeTask.GameOverTask)) {
                        oneSay = true
                        Threads.closeTimeTask(CallTimeTask.GameOverTask)
                    }
                }
            }

            if (Data.config.MaxGameIngTime != -1 && Time.concurrentSecond() > Data.game.endTime) {
                if (!forcedClose) {
                    sendSystemMessageLocal("gameOver.forced")
                }
                forcedClose = true
                if (Time.concurrentSecond() > Data.game.endTime + 60) {
                    gr()
                    return
                }
            }

            // When synchronized; when suspended; when stopped; refuse to send Task
            if (Data.game.gameReConnectPaused || forcedReturn || Data.game.gamePaused) {
                return
            }

            val time = Data.game.tickGame.getAndAdd(tick)
            try {
                when (val size = Data.game.gameCommandCache.size) {
                    0 -> NetStaticData.groupNet.broadcast(NetStaticData.RwHps.abstractNetPacket.getTickPacket(time))
                    1 -> NetStaticData.groupNet.broadcast(NetStaticData.RwHps.abstractNetPacket.getGameTickCommandPacket(time, Data.game.gameCommandCache.removeAt(0)))
                    else -> {
                        val lterator = Data.game.gameCommandCache.listIterator()
                        var sizeTemp = size
                        while (sizeTemp-- != 0) {
                            comm.add(lterator.next())
                            lterator.remove()
                        }
                        NetStaticData.groupNet.broadcast(NetStaticData.RwHps.abstractNetPacket.getGameTickCommandsPacket(time, comm))
                        comm.clear()
                    }
                }
            } catch (e: Exception) {
                error("[ALL] Send Tick Failed", e)
            }
        }

        override fun cancel(): Boolean {
            Threads.closeTimeTask(CallTimeTask.GameOverTask)
            return super.cancel()
        }

        fun gr() {
            if (forcedReturn) {
                cancel()
                TimeTaskData.stopCallTickTask()
                return
            }
            forcedReturn = true
            Threads.closeTimeTask(CallTimeTask.GameOverTask)
            Threads.closeTimeTask(CallTimeTask.AutoCheckTask)

            cancel()
            TimeTaskData.stopCallTickTask()

            Events.fire(GameOverEvent())
        }
    }
}
