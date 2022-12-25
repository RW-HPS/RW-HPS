/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.io

import net.rwhps.server.io.input.CompressInputStream
import net.rwhps.server.io.input.DisableSyncByteArrayInputStream
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.util.inline.ifNullResult
import java.io.*

/**
 * Read Bytes
 * @author RW-HPS/Dr
 */
open class GameInputStream : Closeable {
    protected val buffer: DisableSyncByteArrayInputStream
    protected val stream: DataInputStream
    val parseVersion: Int

    @JvmOverloads
    internal constructor(buffer: DisableSyncByteArrayInputStream, parseVersion: Int = 0) {
        this.buffer = buffer
        this.stream = DataInputStream(buffer)
        this.parseVersion = parseVersion
    }

    @JvmOverloads
    constructor(packet: Packet, parseVersion: Int = 0) {
        this.buffer = DisableSyncByteArrayInputStream(packet.bytes)
        this.stream = DataInputStream(buffer)
        this.parseVersion = parseVersion
    }

    @JvmOverloads
    constructor(bytes: ByteArray, parseVersion: Int = 0) {
        this.buffer = DisableSyncByteArrayInputStream(bytes)
        this.stream = DataInputStream(buffer)
        this.parseVersion = parseVersion
    }
    /**
     * Read a Byte (1 byte)
     * @return Byte
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun readByte(): Int {
        return stream.readByte().toInt()
    }

    /**
     * Read a Boolean (1 byte)
     * @return Boolean
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun readBoolean(): Boolean {
        return stream.readBoolean()
    }

    /**
     * Read an Int (4 byte)
     * @return Int
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun readInt(): Int {
        return stream.readInt()
    }

    @Throws(IOException::class)
    open fun readBackwardsInt(): Int {
        val ch1: Int = buffer.read()
        val ch2: Int = buffer.read()
        val ch3: Int = buffer.read()
        val ch4: Int = buffer.read()
        if (ch1 or ch2 or ch3 or ch4 < 0) throw EOFException()
        return (ch4 shl 24) + (ch3 shl 16) + (ch2 shl 8) + (ch1 shl 0)
    }

    /**
     * Read a Short (2 byte)
     * @return Short
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun readShort(): Short {
        return stream.readShort()
    }
    @Throws(IOException::class)
    open fun readBackwardsShort(): Short {
        val ch1: Int = buffer.read()
        val ch2: Int = buffer.read()
        if (ch1 or ch2 < 0) throw EOFException()
        return ((ch2 shl 8) + (ch1 shl 0)).toShort()
    }

    /**
     * Read a Float (4 byte)
     * @return Float
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun readFloat(): Float {
        return stream.readFloat()
    }

    /**
     * Read a Long (8 byte)
     * @return Long
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun readLong(): Long {
        return stream.readLong()
    }

    /**
     * Read a String
     * Length 2 byte
     * Data X byte
     * @return String
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun readString(): String {
        return stream.readUTF()
    }

    /**
     * Judge and read a String
     * Boolean (1 bytes)
     *    - True :
     *        - Length 2 byte
     *        - Data X byte
     *    -False
     *        - ""
     * @return String
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readIsString(): String {
        return if (readBoolean()) readString() else ""
    }

    @Throws(IOException::class)
    fun readIsInt(): Int {
        return if (readBoolean()) readInt() else 0
    }

    /**
     * Skip the specified length
     * @param skip length
     * @throws IOException
     */
    @Throws(IOException::class)
    fun skip(skip: Long) {
        this.buffer.skip(skip)
    }

    /**
     * Read bytes of specified length
     * @param readBytesLength length
     * @return ByteArray
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readNBytes(readBytesLength: Int): ByteArray {
        return this.buffer.readNBytes(readBytesLength)
    }

    /**
     * Read all the remaining bytes
     * @return ByteArray
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readAllBytes(): ByteArray {
        return this.buffer.readAllBytes()
    }

    @Throws(IOException::class)
    open fun readStreamBytes(): ByteArray {
        return buffer.readNBytes(readInt())
    }

    @Throws(IOException::class)
    fun readStreamBytesNew(): ByteArray {
        readInt() // Relay type
        return readStreamBytes()
    }

    @Throws(IOException::class)
    fun readEnum(clazz: Class<*>): Enum<*>? {
        return readInt().let {
            if (it < 0) {
                null
            } else {
                clazz.enumConstants[it].ifNullResult({
                    it as Enum<*>
                }) {
                    null
                }
            }
        }
    }

    /**
     * Write this stream from the current position to the new stream
     * @param out OutputStream
     * @throws IOException
     */
    @Throws(IOException::class)
    fun transferTo(out: OutputStream) {
        this.buffer.transferTo(out)
    }

    /**
     * Write this stream from the current position to the specified length to the new stream
     * @param out OutputStream
     * @throws IOException
     */
    @Throws(IOException::class)
    fun transferToFixedLength(out: OutputStream, length: Int) {
        this.buffer.transferToFixedLength(out,length)
    }

    @Throws(IOException::class)
    open fun getDecodeStream(bl: Boolean): GameInputStream {
        readString()
        val bytes = readStreamBytes()
        return CompressInputStream.getGzipInputStream(bl, bytes)
    }

    @Throws(IOException::class)
    fun getStream(): GameInputStream {
        val bytes = readStreamBytesNew()
        return CompressInputStream.getGzipInputStream(false, bytes)
    }

    @Throws(IOException::class)
    fun getDecodeBytes(): ByteArray {
        readString()
        return readStreamBytes()
    }



    fun getSize(): Long {
        return this.buffer.available().toLong()
    }


    override fun toString(): String {
        return "GameInputStream{" +
                "buffer=" + buffer +
                ", stream=" + stream +
                '}'
    }

    @Throws(IOException::class)
    override fun close() {
        buffer.close()
        stream.close()
    }
}