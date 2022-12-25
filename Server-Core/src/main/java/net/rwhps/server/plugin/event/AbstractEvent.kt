/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.plugin.event

import net.rwhps.server.data.player.Player

interface AbstractEvent {
    /**
     * 玩家加入 [同步-Synchronization]
     * @param player Player
     */
    fun registerPlayerJoinEvent(player: Player) { /* Optional use of plugins */ }

    /**
     * 玩家重连 [同步-Synchronization]
     * @param player Player
     */
    fun registerPlayerReJoinEvent(player: Player) { /* Optional use of plugins */ }

    /**
     * 玩家连接时 [异步-ASync]
     * @param player Player
     */
    fun registerPlayerConnectEvent(player: Player) { /* Optional use of plugins */ }

    /**
     * 玩家离开时 [异步-ASync]
     * @param player Player
     */
    fun registerPlayerLeaveEvent(player: Player) { /* Optional use of plugins */ }

    /**
     * 玩家发言时 [异步-ASync]
     * @param player
     * @param message
     */
    fun registerPlayerChatEvent(player: Player, message: String) { /* Optional use of plugins */ }

    /** 开始游戏 [同步-ASync]  */
    fun registerGameStartEvent() { /* Optional use of plugins */ }
    /** 无头开始游戏 [同步-Synchronization] */
    fun registerHessStartEvent() { /* Optional use of plugins */ }

    /** 结束游戏 [同步-ASync]  */
    fun registerGameOverEvent() { /* Optional use of plugins */ }

    /** 玩家被ban [异步-ASync]  */
    fun registerPlayerBanEvent(player: Player) { /* Optional use of plugins */ }

    /** 玩家被解除ban [异步-ASync]  */
    fun registerPlayerUnbanEvent(player: Player) { /* Optional use of plugins */ }

    /** 玩家被banIp [异步-ASync]  */
    fun registerPlayerIpBanEvent(player: Player) { /* Optional use of plugins */ }

    /** 玩家被解banIp [异步-ASync]  */
    fun registerPlayerIpUnbanEvent(ip: String) { /* Optional use of plugins */ }
}