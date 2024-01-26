/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.core

import net.rwhps.server.core.thread.CallTimeTask
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.player.PlayerHess
import net.rwhps.server.game.simulation.core.AbstractGameModule
import net.rwhps.server.struct.list.Seq
import net.rwhps.server.util.Time
import net.rwhps.server.util.log.Log
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * @author Dr (dr@der.kim)
 */
class CallHess(private val gameModule: AbstractGameModule) {
    /**
     * 向服务器玩家发送指定消息
     *
     * @param text String
     */
    fun sendSystemMessage(text: String) {
        gameModule.room.playerManage.playerGroup.eachAll { e: PlayerHess -> e.sendSystemMessage(text) }
    }

    /**
     * 向服务器玩家发送从 i18NBundle 获取过的消息
     *
     * @param text String
     * @param obj Array<out Any>
     */
    fun sendSystemMessageLocal(text: String, vararg obj: Any) {
        gameModule.room.playerManage.playerGroup.eachAll { e: PlayerHess ->
            e.sendSystemMessage(e.i18NBundle.getinput(text, *obj))
        }
    }

    fun sendSystemTeamMessageLocal(team: Int, text: String, vararg obj: Any) {
        gameModule.room.playerManage.playerGroup.eachAllFind({ e: PlayerHess -> e.team == team }) { p: PlayerHess ->
            p.sendSystemMessage("[TEAM] " + p.i18NBundle.getinput(text, *obj))
        }
    }

    fun sendSystemMessage(text: String, vararg obj: Any) {
        gameModule.room.playerManage.playerGroup.eachAll { e: PlayerHess ->
            e.sendSystemMessage(e.i18NBundle.getinput(text, *obj))
        }
    }

    fun syncAllPlayer() {
        gameModule.gameLinkFunction.allPlayerSync()
    }

    /**
     * 踢掉全部玩家
     *
     * @param msg 踢出消息
     */
    @JvmOverloads
    fun killAllPlayer(msg: String = "Game Over") {
        val playerGroup = Seq<PlayerHess>()
        playerGroup.addAll(gameModule.room.playerManage.playerGroup)
        playerGroup.eachAll { e: PlayerHess ->
            try {
                e.kickPlayer(msg)
            } catch (err: IOException) {
                Log.error("[ALL] Kick All Player Error", e)
            }
        }
    }

    fun startCheckThread() {
        var aiEndTime = 0
        Threads.newTimedTask(CallTimeTask.AutoCheckTask, 0, 1, TimeUnit.SECONDS) {
            if ((Data.configServer.maxGameIngTime != -1 && Time.concurrentSecond() > gameModule.room.endTime)) {
                if (gameModule.room.flagData.forcedCloseSendMsg) {
                    sendSystemMessageLocal("gameOver.forced")
                }
                gameModule.room.flagData.forcedCloseSendMsg = false
                if (Time.concurrentSecond() > gameModule.room.endTime + 60) {
                    gameModule.room.gr()
                    return@newTimedTask
                }
            }

            if (gameModule.room.flagData.ai) {
                if (gameModule.room.flagData.aiWarn) {
                    gameModule.room.flagData.aiWarn = false
                    sendSystemMessageLocal("gameOver.ai")
                }
                if (gameModule.room.playerManage.playerGroup.size == 0) {
                    if (aiEndTime == 0) {
                        aiEndTime = Time.concurrentSecond() + Data.configServer.maxOnlyAIGameIngTime
                    } else if (Data.configServer.maxOnlyAIGameIngTime != -1 && Time.concurrentSecond() > aiEndTime) {
                        gameModule.room.gr()
                        return@newTimedTask
                    }
                } else {
                    aiEndTime = 0
                }
            } else {
                when (gameModule.room.playerManage.playerGroup.size) {
                    0 -> gameModule.room.gr()
                    1 -> if (gameModule.room.flagData.oneSay) {
                        gameModule.room.flagData.oneSay = false
                        sendSystemMessageLocal("gameOver.oneMin")
                        Threads.newCountdown(CallTimeTask.GameOverTask, 1, TimeUnit.MINUTES) { gameModule.room.gr() }
                    }
                    else -> {
                        if (Threads.containsTimeTask(CallTimeTask.GameOverTask)) {
                            gameModule.room.flagData.oneSay = true
                            Threads.closeTimeTask(CallTimeTask.GameOverTask)
                        }
                    }
                }
            }

            if (gameModule.room.flagData.sendGameStatusFlag) {
                gameModule.room.gameOverData = gameModule.gameHessData.getGameOverData() ?: return@newTimedTask

                gameModule.room.flagData.sendGameStatusFlag = false

                val last = gameModule.room.gameOverData!!.winPlayerList.toArray(String::class.java).contentToString()

                Log.clog("[${gameModule.room.roomID}] Last Win Player: {0}", last)
                sendSystemMessageLocal("survive.player", last)
            }
        }
    }
}
