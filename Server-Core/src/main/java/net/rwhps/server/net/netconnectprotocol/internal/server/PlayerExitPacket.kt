/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

@file:JvmName("ServerPacket")
@file:JvmMultifileClass

package net.rwhps.server.net.netconnectprotocol.internal.server

import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.util.PacketType
import java.io.IOException

/**
 * Player Exit Server
 * @author RW-HPS/Dr
 */

/**
 * Player Exit Server Packet
 * @return Packet
 * @throws IOException
 */
@Throws(IOException::class)
internal fun playerExitPacketInternal(): Packet {
    val o = GameOutputStream()
    o.writeString("exited")
    return o.createPacket(PacketType.DISCONNECT)
}
