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
import net.rwhps.server.util.encryption.codec.Base16Codec
import java.math.BigInteger
import java.nio.charset.Charset

/**
 * 十六进制（简写为hex或下标16）在数学中是一种逢16进1的进位制，一般用数字0到9和字母A到F表示（其中:A~F即10~15）。<br></br>
 * 例如十进制数57，在二进制写作111001，在16进制写作39。<br></br>
 * 像java,c这样的语言为了区分十六进制和十进制数值,会在十六进制数的前面加上 0x,比如0x20是十进制的32,而不是十进制的20<br></br>
 *
 *
 * 参考：https://my.oschina.net/xinxingegeya/blog/287476
 */
object HexUtil {
    /**
     * 判断给定字符串是否为16进制数<br></br>
     * 如果是，需要使用对应数字类型对象的`decode`方法解码<br></br>
     * 例如：`Integer.decode`方法解码int类型的16进制数字
     *
     * @param value 值
     * @return 是否为16进制
     */
    @JvmStatic
    fun isHexNumber(value: String): Boolean {
        val index = if (value.startsWith("-")) 1 else 0
        if (value.startsWith("0x", index) || value.startsWith("0X", index) || value.startsWith("#", index)) {
            try {
                java.lang.Long.decode(value)
            } catch (e: NumberFormatException) {
                return false
            }
            return true
        }
        return false
    }

    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param str     字符串
     * @param charset 编码
     * @return 十六进制char[]
     */
    @JvmStatic
    fun encodeHex(str: String, charset: Charset?): CharArray {
        return encodeHex(ExtractUtil.bytes(str, charset), true)
    }
    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param data        byte[]
     * @param toLowerCase `true` 传换成小写格式 ， `false` 传换成大写格式
     * @return 十六进制char[]
     */
    /**
     * 将字节数组转换为十六进制字符数组
     *
     * @param data byte[]
     * @return 十六进制char[]
     */
    @JvmOverloads
    @JvmStatic
    fun encodeHex(data: ByteArray, toLowerCase: Boolean = true): CharArray {
        return (if (toLowerCase) Base16Codec.CODEC_LOWER else Base16Codec.CODEC_UPPER).encode(data)
    }
    /**
     * 将字符串转换为十六进制字符串，结果为小写
     *
     * @param data    需要被编码的字符串
     * @param charset 编码
     * @return 十六进制String
     */
    /**
     * 将字符串转换为十六进制字符串，结果为小写，默认编码是UTF-8
     *
     * @param data 被编码的字符串
     * @return 十六进制String
     */
    @JvmOverloads
    @JvmStatic
    fun encodeHexStr(data: String, charset: Charset = Data.UTF_8): String {
        return encodeHexStr(ExtractUtil.bytes(data, charset), true)
    }
    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param data        byte[]
     * @param toLowerCase `true` 传换成小写格式 ， `false` 传换成大写格式
     * @return 十六进制String
     */
    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param data byte[]
     * @return 十六进制String
     */
    @JvmOverloads
    @JvmStatic
    fun encodeHexStr(data: ByteArray, toLowerCase: Boolean = true): String {
        return String(encodeHex(data, toLowerCase))
    }
    /**
     * 将十六进制字符数组转换为字符串
     *
     * @param hexStr  十六进制String
     * @param charset 编码
     * @return 字符串
     */
    /**
     * 将十六进制字符数组转换为字符串，默认编码UTF-8
     *
     * @param hexStr 十六进制String
     * @return 字符串
     */
    @JvmOverloads
    @JvmStatic
    fun decodeHexStr(hexStr: String, charset: Charset = Data.UTF_8): String {
        return if (IsUtil.isBlank(hexStr)) {
            hexStr
        } else {
            ExtractUtil.str(decodeHex(hexStr), charset)
        }
    }

    /**
     * 将十六进制字符数组转换为字符串
     *
     * @param hexData 十六进制char[]
     * @param charset 编码
     * @return 字符串
     */
    @JvmStatic
    fun decodeHexStr(hexData: CharArray, charset: Charset?): String {
        return ExtractUtil.str(decodeHex(hexData), charset)
    }

    /**
     * 将十六进制字符串解码为byte[]
     *
     * @param hexStr 十六进制String
     * @return byte[]
     */
    @JvmStatic
    fun decodeHex(hexStr: String): ByteArray {
        return decodeHex(hexStr)
    }

    /**
     * 将十六进制字符数组转换为字节数组
     *
     * @param hexData 十六进制char[]
     * @return byte[]
     * @throws RuntimeException 如果源十六进制字符数组是一个奇怪的长度，将抛出运行时异常
     */
    @JvmStatic
    fun decodeHex(hexData: CharArray?): ByteArray {
        return decodeHex(String(hexData!!))
    }

    /**
     * 将十六进制字符数组转换为字节数组
     *
     * @param hexData 十六进制字符串
     * @return byte[]
     * @throws UtilException 如果源十六进制字符数组是一个奇怪的长度，将抛出运行时异常
     * @since 5.6.6
     */
    @JvmStatic
    fun decodeHex(hexData: CharSequence): ByteArray {
        return Base16Codec.CODEC_LOWER.decode(hexData)
    }

    /**
     * 将指定int值转换为Unicode字符串形式，常用于特殊字符（例如汉字）转Unicode形式<br></br>
     * 转换的字符串如果u后不足4位，则前面用0填充，例如：
     *
     * <pre>
     * '你' =》\u4f60
    </pre> *
     *
     * @param value int值，也可以是char
     * @return Unicode表现形式
     */
    @JvmStatic
    fun toUnicodeHex(value: Int): String {
        val builder = StringBuilder(6)
        builder.append("\\u")
        val hex = toHex(value)
        val len = hex.length
        if (len < 4) {
            builder.append("0000", 0, 4 - len) // 不足4位补0
        }
        builder.append(hex)
        return builder.toString()
    }

    /**
     * 将指定char值转换为Unicode字符串形式，常用于特殊字符（例如汉字）转Unicode形式<br></br>
     * 转换的字符串如果u后不足4位，则前面用0填充，例如：
     *
     * <pre>
     * '你' =》'\u4f60'
    </pre> *
     *
     * @param ch char值
     * @return Unicode表现形式
     * @since 4.0.1
     */
    @JvmStatic
    fun toUnicodeHex(ch: Char): String {
        return Base16Codec.CODEC_LOWER.toUnicodeHex(ch)
    }

    /**
     * 转为16进制字符串
     *
     * @param value int值
     * @return 16进制字符串
     * @since 4.4.1
     */
    @JvmStatic
    fun toHex(value: Int): String {
        return Integer.toHexString(value)
    }

    /**
     * 16进制字符串转为int
     *
     * @param value 16进制字符串
     * @return 16进制字符串int值
     * @since 5.7.4
     */
    @JvmStatic
    fun hexToInt(value: String): Int {
        return value.toInt(16)
    }

    /**
     * 转为16进制字符串
     *
     * @param value int值
     * @return 16进制字符串
     * @since 4.4.1
     */
    @JvmStatic
    fun toHex(value: Long): String {
        return java.lang.Long.toHexString(value)
    }

    /**
     * 16进制字符串转为long
     *
     * @param value 16进制字符串
     * @return long值
     * @since 5.7.4
     */
    @JvmStatic
    fun hexToLong(value: String): Long {
        return value.toLong(16)
    }

    /**
     * 将byte值转为16进制并添加到[StringBuilder]中
     *
     * @param builder     [StringBuilder]
     * @param b           byte
     * @param toLowerCase 是否使用小写
     * @since 4.4.1
     */
    @JvmStatic
    fun appendHex(builder: StringBuilder, b: Byte, toLowerCase: Boolean) {
        (if (toLowerCase) Base16Codec.CODEC_LOWER else Base16Codec.CODEC_UPPER).appendHex(builder, b)
    }

    /**
     * Hex（16进制）字符串转为BigInteger
     *
     * @param hexStr Hex(16进制字符串)
     * @return [BigInteger]
     * @since 5.2.0
     */
    @JvmStatic
    fun toBigInteger(hexStr: String?): BigInteger? {
        return if (null == hexStr) {
            null
        } else BigInteger(hexStr, 16)
    }

    /**
     * 格式化Hex字符串，结果为每2位加一个空格，类似于：
     * <pre>
     * e8 8c 67 03 80 cb 22 00 95 26 8f
    </pre> *
     *
     * @param hexStr Hex字符串
     * @return 格式化后的字符串
     */
    @JvmStatic
    fun format(hexStr: String): String {
        val length = hexStr.length
        val builder = StringBuilder(length + length / 2)
        builder.append(hexStr[0]).append(hexStr[1])
        var i = 2
        while (i < length - 1) {
            builder.append(' ').append(hexStr[i]).append(hexStr[i + 1])
            i += 2
        }
        return builder.toString()
    }
}