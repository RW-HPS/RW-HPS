/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

internal class GameInputStreamTest {

//    @Test
    fun readByte() {
        val buffer = ByteArrayOutputStream()
        val stream = DataOutputStream(buffer)

        stream.writeByte(10)
        val bytes = buffer.toByteArray()

        assertEquals(GameInputStream(bytes).readByte(),DataInputStream(ByteArrayInputStream(bytes)).read()) { "[GameInputStream] readByte Error"}
    }

//    @Test
    fun readBoolean() {
        val buffer = ByteArrayOutputStream()
        val stream = DataOutputStream(buffer)

        stream.writeBoolean(true)
        val bytes = buffer.toByteArray()

        assertEquals(GameInputStream(bytes).readBoolean(),DataInputStream(ByteArrayInputStream(bytes)).readBoolean()) { "[GameInputStream] readBoolean Error"}
    }

//    @Test
    fun readInt() {
        val buffer = ByteArrayOutputStream()
        val stream = DataOutputStream(buffer)

        stream.writeInt(66)
        val bytes = buffer.toByteArray()

        assertEquals(GameInputStream(bytes).readInt(),DataInputStream(ByteArrayInputStream(bytes)).readInt()) { "[GameInputStream] readInt Error"}
    }

//    @Test
    fun readShort() {
        val buffer = ByteArrayOutputStream()
        val stream = DataOutputStream(buffer)

        stream.writeShort(5)
        val bytes = buffer.toByteArray()

        assertEquals(GameInputStream(bytes).readShort(),DataInputStream(ByteArrayInputStream(bytes)).readShort()) { "[GameInputStream] readShort Error"}
    }

//    @Test
    fun readFloat() {
        val buffer = ByteArrayOutputStream()
        val stream = DataOutputStream(buffer)

        stream.writeFloat(6.66F)
        val bytes = buffer.toByteArray()

        assertEquals(GameInputStream(bytes).readFloat(),DataInputStream(ByteArrayInputStream(bytes)).readFloat()) { "[GameInputStream] readFloat Error"}
    }

//    @Test
    fun readLong() {
        val buffer = ByteArrayOutputStream()
        val stream = DataOutputStream(buffer)

        stream.writeLong(6666666)
        val bytes = buffer.toByteArray()

        assertEquals(GameInputStream(bytes).readLong(),DataInputStream(ByteArrayInputStream(bytes)).readLong()) { "[GameInputStream] readLong Error"}
    }

//    @Test
    fun readString() {
        val buffer = ByteArrayOutputStream()
        val stream = DataOutputStream(buffer)

        stream.writeUTF("RW-HPS Test !")
        val bytes = buffer.toByteArray()

        assertEquals(GameInputStream(bytes).readString(),DataInputStream(ByteArrayInputStream(bytes)).readUTF()) { "[GameInputStream] readString Error"}
    }

//    @Test
    fun isReadString() {
        val buffer = ByteArrayOutputStream()
        val stream = DataOutputStream(buffer)

        stream.writeBoolean(true)
        stream.writeUTF("RW-HPS Test !")
        val bytes = buffer.toByteArray()

        assertEquals(GameInputStream(bytes).readIsString(),DataInputStream(ByteArrayInputStream(bytes)).use { if (it.readBoolean()) it.readUTF() else "" }) { "[GameInputStream] readString Error"}
    }

    @Disabled
    @Test
    fun skip() {
    }

    @Disabled
    @Test
    fun readNBytes() {
    }

    @Disabled
    @Test
    fun readAllBytes() {
    }

    @Disabled
    @Test
    fun readStreamBytes() {
    }

    @Disabled
    @Test
    fun readStreamBytesNew() {
    }

    @Disabled
    @Test
    fun readEnum() {
    }

    @Disabled
    @Test
    fun transferTo() {
    }

    @Disabled
    @Test
    fun transferToFixedLength() {
    }

    @Disabled
    @Test
    fun getDecodeStream() {
    }

    @Disabled
    @Test
    fun getStream() {
    }

    @Disabled
    @Test
    fun getDecodeBytes() {
    }

    @Disabled
    @Test
    fun getSize() {
    }

    @Disabled
    @Test
    fun testToString() {
    }

    @Disabled
    @Test
    fun close() {
        //
    }
}
