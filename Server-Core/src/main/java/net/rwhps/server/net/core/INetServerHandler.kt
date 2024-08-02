/*
 * Copyright 2020-2024 RW-HPS Team and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/deng-rui/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.core

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInboundHandler
import io.netty.channel.SimpleChannelInboundHandler

/**
 *
 *
 * @date 2023/11/25 11:29
 * @author Dr (dr@der.kim)
 */
@ChannelHandler.Sharable
abstract class INetServerHandler(
        protected val abstractNet: AbstractNet
) : SimpleChannelInboundHandler<Any?>(), ChannelInboundHandler