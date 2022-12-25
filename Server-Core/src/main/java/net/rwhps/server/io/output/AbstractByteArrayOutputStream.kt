/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.io.output

import net.rwhps.server.struct.Seq
import net.rwhps.server.util.io.IOUtils
import net.rwhps.server.util.io.IOUtils.EOF
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import kotlin.math.max
import kotlin.math.min

/**
 * 实现输出流的基类，其中数据写入字节数组。缓冲区会随着数据的增长而自动增长
 * 忽略 [AbstractByteArrayOutputStream] 无效中的方法 [java.io.ByteArrayOutputStream.toString]
 * 可以在流关闭后调用生成一个 `IOException`.
 * [AbstractByteArrayOutputStream]是替代实现的基础 原来的实现是[java.io.ByteArrayOutputStream]
 * [java.io.ByteArrayOutputStream]在开始时只分配 32 个字节
 * 与[java.io.ByteArrayOutputStream]不同的是它没有重新分配整个内存块，但分配额外的缓冲区
 * 这样就不需要对缓冲区进行垃圾收集，而且内容也没有复制到新得到缓冲区
 */
abstract class AbstractByteArrayOutputStream : OutputStream() {
    /** 缓冲区列表，它会增长并且永远不会减少  */
    private val buffers = Seq<ByteArray>(2)

    /** 当前缓冲区的索引  */
    private var currentBufferIndex = 0

    /** 填充缓冲区中所有的总字节数  */
    private var filledBufferSum = 0

    /** 当前缓冲区  */
    private var currentBuffer: ByteArray? = null

    /** 写入的总字节数  */
    protected var count = 0

    /** 缓冲区是否可以在重置后重新使用的标志  */
    private var reuseBuffers = true

    /**
     * 通过分配一个新的或回收一个现有的使新缓冲区可用
     * @param newCount  缓冲区的大小（如果创建一个）
     */
    protected fun needNewBuffer(newCount: Int) {
        if (currentBufferIndex < buffers.size - 1) {
            //回收旧缓冲区
            filledBufferSum += currentBuffer!!.size
            currentBufferIndex++
            currentBuffer = buffers[currentBufferIndex]
        } else {
            //创建新缓冲区
            val newBufferSize: Int
            if (currentBuffer == null) {
                newBufferSize = newCount
                filledBufferSum = 0
            } else {
                newBufferSize = max(currentBuffer!!.size shl 1, newCount - filledBufferSum)
                filledBufferSum += currentBuffer!!.size
            }
            currentBufferIndex++
            currentBuffer = ByteArray(newBufferSize)
            buffers.add(currentBuffer!!)
        }
    }

    /**
     * 将字节写入字节数组
     * @param b 要写入的字节
     * @param off 起始偏移
     * @param len 要写入的字节数
     */
    abstract override fun write(b: ByteArray, off: Int, len: Int)

    /**
     * 将数组写入字节数组
     * @param b 要写入的数组
     */
    abstract fun writeBytes(b: ByteArray)

    /**
     * 将字节写入字节数组
     * @param b 要写入的字节
     * @param off 起始偏移
     * @param len 要写入的字节数
     */
    protected fun writeImpl(b: ByteArray, off: Int, len: Int) {
        val newCount = count + len
        var remaining = len
        var inBufferPos = count - filledBufferSum
        while (remaining > 0) {
            val part = min(remaining, currentBuffer!!.size - inBufferPos)
            System.arraycopy(b, off + len - remaining, currentBuffer!!, inBufferPos, part)
            remaining -= part
            if (remaining > 0) {
                needNewBuffer(newCount)
                inBufferPos = 0
            }
        }
        count = newCount
    }

    /**
     * 写一个字节到数组
     * @param b 要写入的字节
     */
    abstract override fun write(b: Int)

    /**
     * 写一个字节到数组
     * @param b 要写入的字节
     */
    protected fun writeImpl(b: Int) {
        var inBufferPos = count - filledBufferSum
        if (inBufferPos == currentBuffer!!.size) {
            needNewBuffer(count + 1)
            inBufferPos = 0
        }
        currentBuffer!![inBufferPos] = b.toByte()
        count++
    }

    /**
     * 将指定输入流的全部内容写入这个字节流 输入流中的字节直接读入到这个流的内部缓冲区.
     * @param in 要从中读取的输入流
     * @return 从输入流读取的总字节数(和写入这个流的字节数)
     * @throws IOException 如果在读取输入流时发生IO错误
     */
    @Throws(IOException::class)
    abstract fun write(`in`: InputStream): Int

    /**
     * 将指定输入流的全部内容写入这个字节流 输入流中的字节直接读入到这个流的内部缓冲区.
     * @param `in` 要从中读取的输入流
     * @return 从输入流读取的总字节数(和写入这个流的字节数)
     * @throws IOException 如果在读取输入流时发生IO错误
     */
    @Throws(IOException::class)
    protected fun writeImpl(inStream: InputStream): Int {
        var readCount = 0
        var inBufferPos = count - filledBufferSum
        var n = inStream.read(currentBuffer!!, inBufferPos, currentBuffer!!.size - inBufferPos)
        while (n != EOF) {
            readCount += n
            inBufferPos += n
            count += n
            if (inBufferPos == currentBuffer!!.size) {
                needNewBuffer(currentBuffer!!.size)
                inBufferPos = 0
            }
            n = inStream.read(currentBuffer!!, inBufferPos, currentBuffer!!.size - inBufferPos)
        }
        return readCount
    }

    /**
     * 返回字节数组的当前大小
     * @return 字节数组的当前大小
     */
    abstract fun size(): Int

    /**
     * Close Stream
     *
     * @throws IOException No Error will be raised (this method should not declare this exception but must now due to backwards compatibility)
     */
    @Throws(IOException::class)
    override fun close() {
        /*
         * Same as [java.io.ByteArrayOutputStream]
         * Methods in this class can be called after the stream has been closed without throwing `IOException`.
         */
    }

    /**
     * @see java.io.ByteArrayOutputStream.reset
     */
    abstract fun reset()

    /**
     * @see java.io.ByteArrayOutputStream.reset
     */
    protected fun resetImpl() {
        count = 0
        filledBufferSum = 0
        currentBufferIndex = 0
        if (reuseBuffers) {
            currentBuffer = buffers[currentBufferIndex]
        } else {
            //扔掉旧的缓冲器
            currentBuffer = null
            val size: Int = buffers[0].size
            buffers.clear()
            needNewBuffer(size)
            reuseBuffers = true
        }
    }

    /**
     * 将此字节流的全部内容写入指定的输出流。
     * @param out  要写入的输出流
     * @throws IOException 如果发生IO错误，例如流已关闭
     * @see java.io.ByteArrayOutputStream.writeTo
     */
    @Throws(IOException::class)
    abstract fun writeTo(out: OutputStream)

    /**
     * 将此字节流的全部内容写入指定的输出流。
     * @param out  要写入的输出流
     * @throws IOException 如果发生IO错误，例如流已关闭
     * @see java.io.ByteArrayOutputStream.writeTo
     */
    @Throws(IOException::class)
    protected fun writeToImpl(out: OutputStream) {
        var remaining = count
        for (buf in buffers) {
            val c = min(buf.size, remaining)
            out.write(buf, 0, c)
            remaining -= c
            if (remaining == 0) {
                break
            }
        }
    }

    /**
     * 以字节数组的形式获取此字节流的当前内容 结果是独立这个流的
     * @return 输出这个流的当前内容，为字节数组
     * @see java.io.ByteArrayOutputStream.toByteArray
     */
    abstract fun toByteArray(): ByteArray?

    /**
     * 以字节数组的形式获取此字节流的当前内容 结果是独立这个流的
     * @return 输出这个流的当前内容，为字节数组
     * @see java.io.ByteArrayOutputStream.toByteArray
     */
    protected fun toByteArrayImpl(): ByteArray {
        var remaining = count
        if (remaining == 0) {
            return IOUtils.EMPTY_BYTE_ARRAY
        }
        val newBuf = ByteArray(remaining)
        var pos = 0
        for (buf in buffers) {
            val c = min(buf.size, remaining)
            System.arraycopy(buf, 0, newBuf, pos, c)
            pos += c
            remaining -= c
            if (remaining == 0) {
                break
            }
        }
        return newBuf
    }

    /**
     * 以字符串形式获取此字节流的当前内容 使用JVM平台默认字符集.
     * 不建议使用 因为将被抛弃
     * @return 字节数组转字符串的内容
     * @see java.io.ByteArrayOutputStream.toString
     */
    @Deprecated("", ReplaceWith("toString(Charset.defaultCharset())", "java.nio.charset.Charset"))
    override fun toString(): String {
        return String(toByteArray()!!, Charset.defaultCharset())
    }

    /**
     * 以字符串形式获取此字节流的当前内容 使用自定义的字符集.
     * @param enc 自定义的字符集
     * @return 字节数组转字符串的内容
     * @throws UnsupportedEncodingException 如果不支持编码 就会抛出这个异常
     * @see java.io.ByteArrayOutputStream.toString
     */
    @Throws(UnsupportedEncodingException::class)
    fun toString(enc: String): String {
        return String(toByteArray()!!, Charset.forName(enc))
    }

    /**
     * 以字符串形式获取此字节流的当前内容 使用自定义的字符集.
     * @param charset 自定义的Charset字符集
     * @return 字节数组转字符串的内容
     * @see java.io.ByteArrayOutputStream.toString
     */
    fun toString(charset: Charset): String {
        return String(toByteArray()!!, charset)
    }

    companion object {
        /** 默认缓冲区大小  */
        internal const val DEFAULT_SIZE = 512
    }
}