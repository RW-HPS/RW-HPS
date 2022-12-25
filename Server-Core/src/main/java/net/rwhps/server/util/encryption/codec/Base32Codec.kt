/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.encryption.codec

import java.util.*

/**
 * Base32 - encodes and decodes RFC4648 Base32 (see https://datatracker.ietf.org/doc/html/rfc4648#section-6 )<br></br>
 * base32就是用32（2的5次方）个特定ASCII码来表示256个ASCII码。<br></br>
 * 所以，5个ASCII字符经过base32编码后会变为8个字符（公约数为40），长度增加3/5.不足8n用“=”补足。<br></br>
 * 根据RFC4648 Base32规范，支持两种模式：
 *
 *  * Base 32 Alphabet                 (ABCDEFGHIJKLMNOPQRSTUVWXYZ234567)
 *  * "Extended Hex" Base 32 Alphabet  (0123456789ABCDEFGHIJKLMNOPQRSTUV)
 */
internal class Base32Codec : Encoder<ByteArray, String>, Decoder<CharSequence, ByteArray> {
    /**
     * 编码数据
     *
     * @param data   数据
     * @param useHex 是否使用Hex Alphabet
     * @return 编码后的Base32字符串
     */
    fun encode(data: ByteArray, useHex: Boolean = false): String {
        val encoder = if (useHex) {
            Base32Encoder.HEX_ENCODER
        } else {
            Base32Encoder.ENCODER
        }
        return encoder.encode(data)
    }

    override fun encode(data: ByteArray): String {
        return encode(data,false)
    }

    /**
     * 解码数据
     *
     * @param encoded base32字符串
     * @param useHex  是否使用Hex Alphabet
     * @return 解码后的内容
     */
    fun decode(encoded: CharSequence, useHex: Boolean = false): ByteArray {
        val decoder = if (useHex) {
            Base32Decoder.HEX_DECODER
        } else {
            Base32Decoder.DECODER
        }
        return decoder.decode(encoded)
    }

    override fun decode(encoded: CharSequence): ByteArray {
        return decode(encoded,false)
    }

    /**
     * Bas32编码器
     *
     * @param alphabet 自定义编码字母表，见 [.DEFAULT_ALPHABET]和 [.HEX_ALPHABET]
     * @param pad      补位字符
     */
    class Base32Encoder(alphabet: String, private val pad: Char?) : Encoder<ByteArray, String> {
        private val alphabet: CharArray

        init {
            this.alphabet = alphabet.toCharArray()
        }

        override fun encode(data: ByteArray): String {
            var i = 0
            var index = 0
            var digit: Int
            var currByte: Int
            var nextByte: Int
            var encodeLen = data.size * 8 / 5
            if (encodeLen != 0) {
                encodeLen += 1 + BASE32_FILL[data.size * 8 % 5]
            }
            val base32 = StringBuilder(encodeLen)
            while (i < data.size) {
                // unsign
                currByte = if (data[i] >= 0) data[i].toInt() else data[i] + 256

                /* Is the current digit going to span a byte boundary? */
                if (index > 3) {
                    nextByte = if (i + 1 < data.size) {
                        if (data[i + 1] >= 0) data[i + 1].toInt() else data[i + 1] + 256
                    } else {
                        0
                    }
                    digit = currByte and (0xFF shr index)
                    index = (index + 5) % 8
                    digit = digit shl index
                    digit = digit or (nextByte shr 8 - index)
                    i++
                } else {
                    digit = currByte shr 8 - (index + 5) and 0x1F
                    index = (index + 5) % 8
                    if (index == 0) {
                        i++
                    }
                }
                base32.append(alphabet[digit])
            }
            if (null != pad) {
                // 末尾补充不足长度的
                while (base32.length < encodeLen) {
                    base32.append(pad)
                }
            }
            return base32.toString()
        }

        companion object {
            private const val DEFAULT_PAD = '='
            private val BASE32_FILL = intArrayOf(-1, 4, 1, 6, 3)
            val ENCODER = Base32Encoder(DEFAULT_ALPHABET, DEFAULT_PAD)
            val HEX_ENCODER = Base32Encoder(HEX_ALPHABET, DEFAULT_PAD)
        }
    }

    /**
     * Base32解码器
     *
     * @param alphabet 编码字母表
     */
    class Base32Decoder(alphabet: String) : Decoder<CharSequence, ByteArray> {
        private val lookupTable: ByteArray = ByteArray(128)

        init {
            Arrays.fill(lookupTable, (-1).toByte())

            val length = alphabet.length
            var c: Char
            for (i in 0 until length) {
                c = alphabet[i]
                lookupTable[c.code - BASE_CHAR.code] = i.toByte()
                // 支持小写字母解码
                if (c in 'A'..'Z') {
                    lookupTable[c.lowercaseChar() - BASE_CHAR] = i.toByte()
                }
            }
        }

        override fun decode(encoded: CharSequence): ByteArray {
            var index: Int
            var lookup: Int
            var digit: Int
            val base32 = encoded.toString()
            val len = if (base32.endsWith("=")) base32.indexOf("=") * 5 / 8 else base32.length * 5 / 8
            val bytes = ByteArray(len)
            var i = 0
            index = 0
            var offset = 0
            while (i < base32.length) {
                lookup = base32[i].code - BASE_CHAR.code

                /* Skip chars outside the lookup table */
                if (lookup < 0 || lookup >= lookupTable.size) {
                    i++
                    continue
                }
                digit = lookupTable[lookup].toInt()

                /* If this digit is not in the table, ignore it */
                if (digit < 0) {
                    i++
                    continue
                }
                if (index <= 3) {
                    index = (index + 5) % 8
                    if (index == 0) {
                        bytes[offset] = (bytes[offset].toInt() or digit).toByte()
                        offset++
                        if (offset >= bytes.size) {
                            break
                        }
                    } else {
                        bytes[offset] = (bytes[offset].toInt() or (digit shl 8 - index)).toByte()
                    }
                } else {
                    index = (index + 5) % 8
                    bytes[offset] = (bytes[offset].toInt() or (digit ushr index)).toByte()
                    offset++
                    if (offset >= bytes.size) {
                        break
                    }
                    bytes[offset] = (bytes[offset].toInt() or (digit shl 8 - index)).toByte()
                }
                i++
            }
            return bytes
        }

        companion object {
            private const val BASE_CHAR = '0'
            val DECODER = Base32Decoder(DEFAULT_ALPHABET)
            val HEX_DECODER = Base32Decoder(HEX_ALPHABET)
        }
    }

    companion object {
        private const val DEFAULT_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        private const val HEX_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUV"
    }
}