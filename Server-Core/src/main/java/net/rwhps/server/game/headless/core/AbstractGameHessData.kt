/*
 * Copyright 2020-2024 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.game.headless.core

import net.rwhps.server.game.event.game.ServerGameOverEvent.GameOverData
import net.rwhps.server.net.core.server.AbstractNetConnectServer
import net.rwhps.server.util.annotations.mark.GameSimulationLayer

interface AbstractGameHessData {
    @GameSimulationLayer.GameSimulationLayer_KeyWords("got remoteSyncFrame for")
    val tickHess: Int
    val tickNetHess: Int

    val gameDelta: Long
    val gameFPS: Int

    /**
     * 获取位子上玩家是否存活
     *
     * @param position Position
     * @return Boolean
     */
    @GameSimulationLayer.GameSimulationLayer_KeyWords("is victorious!")
    fun getWin(position: Int): Boolean

    /**
     * 获取服务器Gameover信息
     *
     * @param team Team
     * @return Boolean
     */
    @GameSimulationLayer.GameSimulationLayer_KeyWords("is victorious!")
    fun getGameOverData(): GameOverData?

    @GameSimulationLayer.GameSimulationLayer_KeyWords("aiDifficulty is locked")
    fun getPlayerBirthPointXY()

    fun existPlayer(position: Int): Boolean

    fun getHeadlessAIServer(): AbstractNetConnectServer
}