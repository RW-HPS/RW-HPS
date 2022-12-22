/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.io.packet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PacketTest {

//    @Test
    // 没必要测试 做做样子就好
    fun newTest() {
        val bytes = byteArrayOf(0,1,2,3,4,5,6,7,8,9)
        val packet = Packet(100, bytes)

        assertEquals(packet.type,100) { "[PacketTest] Type Error"}
        assertEquals(packet.bytes,bytes) { "[PacketTest] Bytes Error"}

        packet.toString()
    }
}
