/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.encryption.codec

import net.rwhps.server.util.log.exp.VariableException

class Base16Codec(lowerCase: Boolean) : Encoder<ByteArray, CharArray>, Decoder<CharSequence, ByteArray> {
    private val alphabets: CharArray

    /**
     * 构造
     *
     * @param lowerCase 是否小写
     */
    init {
        alphabets = (if (lowerCase) "0123456789abcdef" else "0123456789ABCDEF").toCharArray()
    }

    override fun encode(data: ByteArray): CharArray {
        val len = data.size
        val out = CharArray(len shl 1) //len*2
        // two characters from the hex value.
        var i = 0
        var j = 0
        while (i < len) {
            out[j++] = alphabets[0xF0 and data[i].toInt() ushr 4] // 高位
            out[j++] = alphabets[0x0F and data[i].toInt()] // 低位
            i++
        }
        return out
    }

    override fun decode(encodedIn: CharSequence): ByteArray {
        var encoded = encodedIn
        var len = encoded.length
        if (len and 0x01 != 0) {
            // 如果提供的数据是奇数长度，则前面补0凑偶数
            encoded = "0$encoded"
            len = encoded.length
        }
        val out = ByteArray(len shr 1)

        // two characters form the hex value.
        var i = 0
        var j = 0
        while (j < len) {
            var f = toDigit(encoded[j], j) shl 4
            j++
            f = f or toDigit(encoded[j], j)
            j++
            out[i] = (f and 0xFF).toByte()
            i++
        }
        return out
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
     */
    fun toUnicodeHex(ch: Char): String {
        return "\\u" +  //
                alphabets[ch.code shr 12 and 15] +  //
                alphabets[ch.code shr 8 and 15] +  //
                alphabets[ch.code shr 4 and 15] +  //
                alphabets[ch.code and 15]
    }

    /**
     * 将byte值转为16进制并添加到[StringBuilder]中
     *
     * @param builder [StringBuilder]
     * @param b       byte
     */
    fun appendHex(builder: StringBuilder, b: Byte) {
        val high = b.toInt() and 0xf0 ushr 4 //高位
        val low = b.toInt() and 0x0f //低位
        builder.append(alphabets[high])
        builder.append(alphabets[low])
    }

    companion object {
        val CODEC_LOWER = Base16Codec(true)
        val CODEC_UPPER = Base16Codec(false)

        /**
         * 将十六进制字符转换成一个整数
         *
         * @param ch    十六进制char
         * @param index 十六进制字符在字符数组中的位置
         * @return 一个整数
         * @throws VariableException.ArrayRuntimeException 当ch不是一个合法的十六进制字符时，抛出运行时异常
         */
        private fun toDigit(ch: Char, index: Int): Int {
            val digit = ch.digitToIntOrNull(16) ?: -1
            if (digit < 0) {
                throw VariableException.ArrayRuntimeException("Illegal hexadecimal character $ch at index $index")
            }
            return digit
        }
    }
}