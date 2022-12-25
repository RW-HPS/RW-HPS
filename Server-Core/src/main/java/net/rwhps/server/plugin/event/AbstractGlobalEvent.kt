/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.plugin.event

import net.rwhps.server.net.core.ConnectionAgreement
import net.rwhps.server.net.core.IRwHps

interface AbstractGlobalEvent {
    /** 游戏核心初始化完毕 [同步-Sync]  */
    fun registerGameLibLoadEvent() { /* Optional use of plugins */ }

    /** 服务器初始化 [同步-Sync]  */
    fun registerServerLoadEvent() { /* Optional use of plugins */ }

    /** 启动了新协议 [同步-Sync]  */
    fun registerServerStartTypeEvent(serverNetType: IRwHps.NetType) { /* Optional use of plugins */ }

    /**
     * 玩家加入 [同步-Synchronization]
     * @param connectionAgreement connectionAgreement
     */
    fun registerNewConnectEvent(connectionAgreement: ConnectionAgreement): Boolean = false

    /**
     * 玩家加入 [同步-Synchronization]
     * @param connectionAgreement connectionAgreement
     */
    fun registerNewCloseEvent(connectionAgreement: ConnectionAgreement) { /* Optional use of plugins */ }
}