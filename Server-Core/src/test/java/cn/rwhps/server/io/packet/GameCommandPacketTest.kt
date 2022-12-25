/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package cn.rwhps.server.io.packet

import net.rwhps.server.io.packet.GameCommandPacket
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GameCommandPacketTest {
    @Test
    fun newTest() {
        val bytes = byteArrayOf(0,1,2,3,4,5,6,7,8,9)
        val packet = GameCommandPacket(100, bytes)

        assertEquals(packet.sendBy,100) { "[GameCommandPacket] sendBy Error"}
        assertEquals(packet.bytes,bytes) { "[GameCommandPacket] Bytes Error"}

        packet.toString()
    }
}