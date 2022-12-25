/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.code.rudp

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.DatagramPacket
import io.netty.handler.codec.MessageToMessageDecoder
import net.rwhps.server.io.input.ClearableAndReusableDisableSyncByteArrayInputStream

class PacketDecoderTest : MessageToMessageDecoder<DatagramPacket>() {

    private val inputStream = ClearableAndReusableDisableSyncByteArrayInputStream()

    // TODO
    // 解析 RUDP包 并写入 ClearableAndReusableDisableSyncByteArrayInputStream
    // 最后提取出 Packet 向下传递
    @Throws(Exception::class)
    override fun decode(channelHandlerContext: ChannelHandlerContext, datagramPacket: DatagramPacket, list: List<Any>) {
        val bufferIn = datagramPacket.content()

        val bytes = ByteArray(bufferIn.readableBytes())
        bufferIn.readBytes(bytes)

        //val segment = Segment.parse(bytes)

    }
}