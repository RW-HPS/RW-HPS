/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net

import io.netty.channel.Channel
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor
import net.rwhps.server.data.global.Data
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.core.ConnectionAgreement
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.log.Log.error
import java.io.IOException
import java.util.concurrent.Executors
import net.rwhps.server.util.log.Log.error

/**
 * Bulk support for connections
 *
 * @author RW-HPS/Dr
 */
class GroupNet {
    private val channelGroup: ChannelGroup = DefaultChannelGroup( GlobalEventExecutor.INSTANCE)
    private val singleTcpThreadExecutor = Executors.newSingleThreadExecutor()
    private val singleUdpThreadExecutor = Executors.newSingleThreadExecutor()
    private val protocol = Seq<ConnectionAgreement>(8)

    /**
     * Add a user to the group list
     * @param channel Channel
     */
    fun add(channel: Channel) {
        channelGroup.add(channel)
    }

    /**
     * Add a user to the group list
     * @param connectionAgreement UDP Channel
     */
    fun add(connectionAgreement: ConnectionAgreement) {
        protocol.add(connectionAgreement)
    }

    fun broadcast(msg: Any) {
        if (Data.config.SingleUserRelay) {
            return
        }
        broadcastAndUDP(msg)
    }

    /**
     * Group Message
     * @param msg Message
     */
    fun broadcastAndUDP(msg: Any) {
        singleTcpThreadExecutor.execute { channelGroup.writeAndFlush(msg) }
        singleUdpThreadExecutor.execute {
            protocol.eachAll { e: ConnectionAgreement ->
                try {
                    e.send((msg as Packet))
                } catch (ioException: IOException) {
                    protocol.remove(e)
                    try {
                        e.close(this)
                    } catch (exception: IOException) {
                        error("[Server] UDP GrepNet Error Close", e)
                    }
                }
            }
        }
    }

    /**
     * Delete a TCP channel
     * @param channel Channel
     */
    fun remove(channel: Channel) {
        channelGroup.remove(channel)
    }

    /**
     * Delete a UDP channel
     * @param connectionAgreement UDP Protocol
     */
    fun remove(connectionAgreement: ConnectionAgreement) {
        protocol.remove(connectionAgreement)
    }

    /**
     * Refresh Operation
     * @return Whether succeed
     */
    fun flush(): ChannelGroup {
        return channelGroup.flush()
    }

    /**
     * Close all
     */
    fun disconnect() {
        channelGroup.disconnect()
    }
}
