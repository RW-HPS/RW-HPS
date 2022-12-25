/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.game.event

import net.rwhps.server.data.player.Player

/**
 * @author RW-HPS/Dr
 */
class EventType {
    /** 玩家加入  */
    class PlayerJoinEvent(val player: Player)

    /** 玩家重连  */
    class PlayerReJoinEvent(val player: Player)

    /** 玩家连接时. */
    class PlayerConnectEvent(val player: Player)

    /** 玩家离开时  */
    class PlayerLeaveEvent(val player: Player)

    /** 玩家发言时  */
    class PlayerChatEvent(val player: Player, val message: String)

    /** 开始游戏  */
    class GameStartEvent
    /** 无头开始游戏  */
    class HessStartEvent()

    /** 结束游戏  */
    class GameOverEvent

    /** 玩家被ban  */
    class PlayerBanEvent(val player: Player)

    /** 玩家被解除ban  */
    class PlayerUnbanEvent(val player: Player)

    /** 玩家被banIp  */
    class PlayerIpBanEvent(val player: Player)

    /** 玩家被解banIp  */
    class PlayerIpUnbanEvent(val ip: String)

    /** 玩家加入时的名字过滤 */
    class PlayerJoinNameEvent(val name: String) {
        @JvmField
        var resultName = ""
    }

    /** 玩家操作单位事件 */
    class PlayerOperationUnitEvent(val player: Player, val playerUnit: EventLambdaType.PlayerUnit)
}