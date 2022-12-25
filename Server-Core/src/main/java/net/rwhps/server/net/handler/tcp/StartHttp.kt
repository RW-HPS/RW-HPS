/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.handler.tcp

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import net.rwhps.server.data.global.Data
import net.rwhps.server.net.core.AbstractNet
import net.rwhps.server.net.http.SendWeb
import net.rwhps.server.net.http.WebData
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.log.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine

internal class StartHttp : AbstractNet() {
    var socketChannel: SocketChannel? = null
    private var sslContext: SSLContext? = null

    private fun isHttpReq(head: String): Boolean {
        return head.startsWith("GET ") || head.startsWith("POST ") || head.startsWith("DELETE ") || head.startsWith("HEAD ") || head.startsWith("PUT ")
    }

    override fun initChannel(socketChannel: SocketChannel) {
        if (Data.config.SSL) {
            if (sslContext == null) {
                sslContext = getSslContext()
            }
            val sslEngine: SSLEngine = sslContext!!.createSSLEngine()
            sslEngine.useClientMode = false
            socketChannel.pipeline().addLast("ssl", SslHandler(sslEngine))
        }
        socketChannel.pipeline().addLast("http",object : SimpleChannelInboundHandler<Any>() {
            @Throws(Exception::class)
            override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
                val firstData: ByteBuf = msg as ByteBuf

                val headS: String = firstData.toString(StandardCharsets.UTF_8)
                if (isHttpReq(headS)) {
                    if (headS.startsWith("GET ${WebData.WS_URI}")) {
                        firstData.retain()
                        val head = headS.split("\\R")[0].split(" ")[1]
                        ctx.channel().pipeline().addLast(
                            HttpServerCodec(),
                            ChunkedWriteHandler(),
                            HttpObjectAggregator(1048576),
                            WebSocketServerProtocolHandler(head)
                        )
                        ctx.channel().pipeline().addFirst(
                            IdleStateHandler(10, 0, 0),
                            object : ChannelDuplexHandler() {
                                @Throws(Exception::class)
                                override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
                                    val evt1: IdleStateEvent = evt as IdleStateEvent
                                    if (evt1.state() === IdleState.READER_IDLE) {
                                        Log.clog("已经10秒没有读到数据了,主动断开连接" + ctx.channel())
                                        ctx.channel().close()
                                    }
                                }
                            }
                        )
                        val wsProcessing = WebData.runWebSocketInstance(head)
                        if (wsProcessing != null) {
                            ctx.channel().pipeline().addLast(wsProcessing)
                        } else {
                            ctx.close()
                        }
                    } else {
                        ctx.channel().pipeline().addLast(HttpServerCodec(), HttpObjectAggregator(1048576))
                        ctx.channel().pipeline().addLast(object : SimpleChannelInboundHandler<Any>() {
                            @Throws(Exception::class)
                            override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
                                if (msg is HttpRequest) {
                                    val request = msg

                                    val url = request.uri()

                                    if (request.method().equals(HttpMethod.GET)) {
                                        WebData.runWebGetInstance(url, request, SendWeb(ctx.channel(), request))
                                        return
                                    } else if (request.method().equals(HttpMethod.POST)) {
                                        if (msg is HttpContent) {
                                            val httpContent = msg as HttpContent
                                            val content = httpContent.content()
                                            val buf = StringBuilder()
                                            buf.append(content.toString(CharsetUtil.UTF_8))
                                            WebData.runWebPostInstance(url, buf.toString(), request, SendWeb(ctx.channel(), request))
                                            return
                                        }
                                    }
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                            }
                        })
                    }
                }
                ctx.pipeline().remove("http")
                ctx.fireChannelRead(msg)
            }

            @Deprecated("Deprecated in Java")
            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            }
        })
    }

    private fun getSslContext(protocol: String = "TLSv1.2"): SSLContext {
        val filePass = Data.config.SSLPasswd.toCharArray()
        val sslContext: SSLContext = SSLContext.getInstance(protocol)
        val keyStore: KeyStore = KeyStore.getInstance("JKS")
        keyStore.load(FileUtil.getFile("ssl.jks").getInputsStream(), filePass)
        val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, filePass)
        sslContext.init(kmf.keyManagers, null, null)
        return sslContext
    }
}