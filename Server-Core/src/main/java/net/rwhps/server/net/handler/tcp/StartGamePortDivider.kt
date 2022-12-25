/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.handler.tcp

import io.netty.channel.socket.SocketChannel
import net.rwhps.server.net.core.AbstractNet

/**
 * @author HuiAnXiaoXing
 * @author RW-HPS/Dr
 */
internal class StartGamePortDivider : AbstractNet() {
    private val divider: GamePortDivider = GamePortDivider(this)
    
    private var socketChannel: SocketChannel? = null

    override fun initChannel(socketChannel: SocketChannel) {
        this.socketChannel = socketChannel
        //CalUt.twoOver.add(ChannelInfo(ch, System.currentTimeMillis()))
        socketChannel.pipeline().addLast("GamePortDivider",divider)
    }

    fun resetGameProtocol() {
        socketChannel!!.pipeline().also { pipeline ->
            addTimeOut(pipeline)
            addPacketDecoderAndEncoder(pipeline)
            addNewServerHandlerExecutorGroup(pipeline)
        }
    }
}