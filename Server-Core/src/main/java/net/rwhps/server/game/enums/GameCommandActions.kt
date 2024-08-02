/*
 * Copyright 2020-2024 RW-HPS Team and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.game.enums

import net.rwhps.server.util.EnumUtils

/**
 * @date 2023/8/19 11:02
 * @author Dr (dr@der.kim)
 */
enum class GameCommandActions {
    MOVE,
    ATTACK,
    BUILD,
    REPAIR,
    LOADINTO,
    UNLOADAT,
    RECLAIM,
    ATTACKMOVE,
    LOADUP,
    PATROL,
    GUARD,
    GUARDAT,
    TOUCHTARGET,
    FOLLOW,
    TRIGGERACTION,
    TRIGGERACTIONWHENINRANGE,
    SETPASSIVETARGET,
    UNKNOWN;

    companion object {
        fun from(name: String): GameCommandActions = EnumUtils.from(entries, name, UNKNOWN)!!

        fun from(index: Int): GameCommandActions = EnumUtils.from(entries, index, UNKNOWN)
    }
}