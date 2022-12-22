/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollChannelOption
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.DefaultEventExecutorGroup
import io.netty.util.concurrent.EventExecutorGroup
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.net.code.rudp.PacketDecoderTest
import net.rwhps.server.net.core.AbstractNet
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.net.handler.rudp.StartGameNetUdp
import net.rwhps.server.net.handler.tcp.StartGameNetTcp
import net.rwhps.server.net.handler.tcp.StartGamePortDivider
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.Log.clog
import java.net.BindException
import java.net.ServerSocket
import net.rwhps.server.util.log.Log.error
/**
 * NetGameServer Service
 * Open interfaces at least to the outside world, and try to integrate internally as much as possible
 *
 * @author RW-HPS/Dr
 */
class StartNet {
    private val connectChannel = Seq<Channel>(4)
    private var serverSocket: ServerSocket? = null
    private var startGameNetUdp: StartGameNetUdp? = null
    private val start: AbstractNet
    private var errorIgnore = false

    internal val ioGroup: EventExecutorGroup = DefaultEventExecutorGroup(32)

    constructor() {
        start = if (Data.config.WebGameBypassPort) StartGamePortDivider(this) else StartGameNetTcp(this)
    }

    constructor(abstractNetClass: Class<out AbstractNet>) {
        val startNet: AbstractNet? =
            try {
                net.rwhps.server.util.ReflectionUtils.accessibleConstructor(abstractNetClass,StartNet::class.java).newInstance(this)
            } catch (e: Exception) {
                Log.fatal("[StartNet Load Error] Use default implementation",e)
                null
            }
        this.start = startNet ?:StartGameNetTcp(this)
    }

    /**
     * Start the Game Server on the specified port
     * @param port Port
     */
    fun openPort(port: Int) {
        Data.config.RunPid = Data.core.pid
        Data.config.save()

        openPort(port,1,0)
    }

    /**
     * Start the Game Server in the specified port range
     * @param port MainPort
     * @param startPort Start Port
     * @param endPort End Port
     */
    fun openPort(port: Int,startPort:Int,endPort:Int) {
        clog(Data.i18NBundle.getinput("server.start.open"))
        val bossGroup: EventLoopGroup = getEventLoopGroup(4)
        val workerGroup: EventLoopGroup = getEventLoopGroup()
        val runClass: Class<out ServerChannel>

        if (Epoll.isAvailable()) {
            runClass = EpollServerSocketChannel::class.java
        } else {
            runClass = NioServerSocketChannel::class.java
            clog("无法使用Epoll 效率可能略低")
        }
        try {
            val serverBootstrapTcp = ServerBootstrap()
            val child = serverBootstrapTcp.group(bossGroup, workerGroup)
                .channel(runClass)
                //.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(start)

            val epoolStartThread = Epoll.isAvailable() && NetStaticData.ServerNetType.ordinal in IRwHps.NetType.RelayProtocol.ordinal..IRwHps.NetType.RelayMulticastProtocol.ordinal
            if (epoolStartThread) {
                child.option(EpollChannelOption.SO_REUSEPORT, true)
            }

            clog(Data.i18NBundle.getinput("server.start.openPort"))

            val channelFutureTcp = serverBootstrapTcp.bind(port)
            for (i in startPort..endPort) {
                serverBootstrapTcp.bind(i)
            }

            if (epoolStartThread) {
                val cpuNum = Runtime.getRuntime().availableProcessors()
                for (i in 0 until cpuNum) {
                    serverBootstrapTcp.bind(port)
                }
            }

            val start = channelFutureTcp.channel()
            connectChannel.add(start)
            clog(Data.i18NBundle.getinput("server.start.end"))

            /*
             * No Fix DeadLock :(
             * io.netty.util.concurrent.DefaultPromise.await(DefaultPromise.java:253)
             */
            start.closeFuture().sync()
        } catch (e: InterruptedException) {
            if (!errorIgnore) error("[TCP Start Error]", e)
        } catch (bindError: BindException) {
            if (!errorIgnore) error("[Port Bind Error]", bindError)
        } catch (e: Exception) {
            if (!errorIgnore) error("[NET Error]", e)
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }

    fun startUdp(port: Int) {
        val group = NioEventLoopGroup()
        val bootstrap = Bootstrap()
        bootstrap.group(group)
            .channel(NioDatagramChannel::class.java)
            .option(ChannelOption.SO_BROADCAST, true)
            .handler(object : ChannelInitializer<NioDatagramChannel?>() {
                @Throws(Exception::class)
                override fun initChannel(nioDatagramChannel: NioDatagramChannel?) {
                    if (nioDatagramChannel == null) {
                        return
                    }
                    nioDatagramChannel.pipeline().addLast(PacketDecoderTest())
                }
            })
        try {
            //4.bind到指定端口，并返回一个channel，该端口就是监听UDP报文的端口
            val channel: Channel = bootstrap.bind(port).sync().channel()
            //5.等待channel的close
            channel.closeFuture().sync()
            //6.关闭group
            group.shutdownGracefully()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Get the number of connections
     * @return Int
     */
    fun getConnectSize(): Int {
        return start.getConnectSize()
    }

    fun stop() {
        errorIgnore = true
        connectChannel.eachAll { obj: Channel -> obj.close().sync() }
        errorIgnore = false
    }

    private fun getEventLoopGroup(size: Int = 0): EventLoopGroup {
        return if (Epoll.isAvailable()) {
            EpollEventLoopGroup(size)
        } else {
            NioEventLoopGroup(size)
        }
    }
}
