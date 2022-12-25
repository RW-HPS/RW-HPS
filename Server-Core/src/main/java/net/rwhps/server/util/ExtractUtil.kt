/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util

import net.rwhps.server.data.global.Data
import java.nio.charset.Charset


/**
 * @author RW-HPS/Dr
 */
object ExtractUtil {
    private val defCharset: Charset = Charset.defaultCharset()

    /**
     * 编码字符串，编码为UTF-8
     *
     * @param str 字符串
     * @return 编码后的字节码
     */
    @JvmStatic
    fun utf8Bytes(str: CharSequence): ByteArray {
        return bytes(str, Data.UTF_8)
    }

    /**
     * 编码字符串<br></br>
     * 使用系统默认编码
     *
     * @param str 字符串
     * @return 编码后的字节码
     */
    @JvmStatic
    fun bytes(str: CharSequence): ByteArray {
        return bytes(str, Charset.defaultCharset())
    }

    /**
     * 编码字符串
     *
     * @param str     字符串
     * @param charset 字符集，如果此字段为空，则解码的结果取决于平台
     * @return 编码后的字节码
     */
    @JvmStatic
    fun bytes(str: CharSequence, charset: String?): ByteArray {
        return bytes(str, if (IsUtil.isBlank(charset)) {
            Charset.defaultCharset()
        } else {
            Charset.forName(charset)
        })
    }

    /**
     * 编码字符串
     *
     * @param str     字符串
     * @param charset 字符集，如果此字段为空，则解码的结果取决于平台
     * @return 编码后的字节码
     */
    @JvmStatic
    fun bytes(str: CharSequence, charset: Charset?): ByteArray {
        return if (null == charset) {
            str.toString().toByteArray()
        } else {
            str.toString().toByteArray(charset)
        }
    }

    /**
     * 解码字节码
     *
     * @param data    字符串
     * @param charset 字符集，如果此字段为空，则解码的结果取决于平台
     * @return 解码后的字符串
     */
    @JvmStatic
    fun str(data: ByteArray, charset: Charset?): String {
        return if (null == charset) { String(data) } else String(data, charset)
    }

    private fun byteToUnsignedInt(data: Byte): Int {
        return data.toInt() and 0xff
    }

    /**
     * 合并byte数组
     */
    @JvmStatic
    fun unitByteArray(byte1: ByteArray, byte2: ByteArray): ByteArray {
        val unitByte = ByteArray(byte1.size + byte2.size)
        System.arraycopy(byte1, 0, unitByte, 0, byte1.size)
        System.arraycopy(byte2, 0, unitByte, byte1.size, byte2.size)
        return unitByte
    }

    @JvmStatic
	fun hexToByteArray(inHex: String): ByteArray {
        var inHexRemoveAir = inHex.replace(" ", "")
        var hexLength = inHexRemoveAir.length
        val result: ByteArray
        if (hexLength % 2 != 1) {
            //偶数
            result = ByteArray(hexLength / 2)
        } else {
            //奇数
            hexLength++
            result = ByteArray(hexLength / 2)
            inHexRemoveAir = "0$inHexRemoveAir"
        }
        var j = 0
        var i = 0
        while (i < hexLength) {
            result[j] = inHexRemoveAir.substring(i, i + 2).toInt(16).toByte()
            j++
            i += 2
        }
        return result
    }

    @JvmStatic
    fun bytesToHex(byte: Byte): String {
        return bytesToHex(byteArrayOf(byte))
    }

    @JvmStatic
	fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuffer()
        for (aByte in bytes) {
            val hex = Integer.toHexString(aByte.toInt() and 0xFF)
            if (hex.length < 2) {
                sb.append(0)
            }
            sb.append(hex).append(" ")
        }
        return sb.toString()
    }
}