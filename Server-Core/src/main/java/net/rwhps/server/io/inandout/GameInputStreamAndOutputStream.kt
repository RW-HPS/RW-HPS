/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.io.inandout

import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.io.input.CompressInputStream
import net.rwhps.server.io.input.DisableSyncByteArrayInputStream
import net.rwhps.server.io.output.DisableSyncByteArrayOutputStream
import net.rwhps.server.io.packet.Packet
import java.io.EOFException
import java.io.IOException
import java.util.zip.GZIPOutputStream

/**
 * 这个方法类 初衷是:
 * 在读取数据的同时 将数据再输入进OutStream
 *
 * 避免了边读边写 (指 内部包装)
 *
 * @property out GameOutputStream
 */
class GameInputStreamAndOutputStream : GameInputStream {
    @JvmOverloads
    constructor(packet: Packet, parseVersion: Int = 0): super(packet, parseVersion) {
    }
    @JvmOverloads
    internal constructor(buffer: DisableSyncByteArrayInputStream, parseVersion: Int = 0) : super(buffer, parseVersion) {
    }

    val out = GameOutputStream()
    
    @Throws(IOException::class)
    fun readByteN() = out.transferToFixedLength(this,1)
    
    @Throws(IOException::class)
    fun readBooleanN() = out.transferToFixedLength(this,1)
    
    @Throws(IOException::class)
    fun readIntN() = out.transferToFixedLength(this,4)

    @Throws(IOException::class)
    fun readBackwardsIntN() = out.transferToFixedLength(this,4)

    @Throws(IOException::class)
    fun readShortN() = out.transferToFixedLength(this,2)
    
    @Throws(IOException::class)
    fun readBackwardsShortN() = out.transferToFixedLength(this,2)
    
    @Throws(IOException::class)
    fun readFloatN() = out.transferToFixedLength(this,4)
    
    @Throws(IOException::class)
    fun readLongN() = out.transferToFixedLength(this,8)

    @Throws(IOException::class)
    fun readStringN() = readShort().let { out.transferToFixedLength(this,it.toInt()) }
        
    @Throws(IOException::class)
    fun readIsStringN() = if (readBoolean()) readStringN() else {}

    @Throws(IOException::class)
    fun readIsIntN() = if (readBoolean()) readIntN() else {}

   
    @Throws(IOException::class)
    fun skipN(skip: Long) = out.transferToFixedLength(this, skip.toInt())

    @Throws(IOException::class)
    fun readNBytesN(readBytesLength: Int) = out.transferToFixedLength(this,readBytesLength)

    @Throws(IOException::class)
    fun readStreamBytesN() = out.transferToFixedLength(this,readInt())

    @Throws(IOException::class)
    fun readStreamBytesNewN() {
        readInt() // Relay type
        readStreamBytesN()
    }

    @Throws(IOException::class)
    fun getDecodeStreamN(bl: Boolean) {
        readString()
        readStreamBytesN()
    }

    @Throws(IOException::class)
    fun getStreamN() = readStreamBytesNewN()

    @Throws(IOException::class)
    fun getDecodeBytesN() {
        readString()
        readStreamBytesN()
    }

    @Throws(IOException::class)
    fun getDecodeStreamNoData(bl: Boolean): GameInputStreamAndOutputStream {
        readString()
        val bytes = buffer.readNBytes(super.readInt())
        return CompressInputStream.getGzipInputStreamAndOutputStream(bl, bytes)
    }

    @Throws(IOException::class)
    fun transferTo(inputStream: GameInputStreamAndOutputStream,bl: Boolean = false) {
        if (bl) {
            val out = DisableSyncByteArrayOutputStream()
            val gzip = GZIPOutputStream(out)
            gzip.write(inputStream.out.getByteArray())
            gzip.close()
            this.out.writeBytesAndLength(out.toByteArray())
        } else {
            this.out.writeBytesAndLength(inputStream.out.getByteArray())
        }
    }



    
    @Throws(IOException::class)
    override fun readByte(): Int = stream.readByte().toInt().also { out.writeByte(it) }

    @Throws(IOException::class)
    override fun readBoolean(): Boolean = stream.readBoolean().also { out.writeBoolean(it) }

    @Throws(IOException::class)
    override fun readInt(): Int = stream.readInt().also { out.writeInt(it) }

    @Throws(IOException::class)
    override fun readBackwardsInt(): Int {
        val ch1: Int = readByte()
        val ch2: Int = readByte()
        val ch3: Int = readByte()
        val ch4: Int = readByte()
        if (ch1 or ch2 or ch3 or ch4 < 0) throw EOFException()
        return (ch4 shl 24) + (ch3 shl 16) + (ch2 shl 8) + (ch1 shl 0)
    }
    
    @Throws(IOException::class)
    override fun readShort(): Short = stream.readShort().also { out.writeShort(it) }

    @Throws(IOException::class)
    override fun readBackwardsShort(): Short {
        val ch1: Int = readByte()
        val ch2: Int = readByte()
        if (ch1 or ch2 < 0) throw EOFException()
        return ((ch2 shl 8) + (ch1 shl 0)).toShort()
    }
    
    @Throws(IOException::class)
    override fun readFloat(): Float = stream.readFloat().also { out.writeFloat(it) }

    @Throws(IOException::class)
    override fun readLong(): Long = stream.readLong().also { out.writeLong(it) }

    @Throws(IOException::class)
    override fun readString(): String = stream.readUTF().also { out.writeString(it) }

    @Throws(IOException::class)
    override fun readStreamBytes(): ByteArray {
        return buffer.readNBytes(readInt()).also { out.writeBytes(it) }
    }

    @Throws(IOException::class)
    override fun getDecodeStream(bl: Boolean): GameInputStream {
        readString()
        val bytes = readStreamBytes()
        return CompressInputStream.getGzipInputStream(bl, bytes)
    }
}