/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.encryption.codec

import net.rwhps.server.data.global.Data
import net.rwhps.server.util.ExtractUtil
import java.nio.charset.Charset


/**
 * Base32 - encodes and decodes RFC4648 Base32 (see https://datatracker.ietf.org/doc/html/rfc4648#section-6 )<br></br>
 * base32就是用32（2的5次方）个特定ASCII码来表示256个ASCII码。<br></br>
 * 所以，5个ASCII字符经过base32编码后会变为8个字符（公约数为40），长度增加3/5.不足8n用“=”补足。<br></br>
 * 根据RFC4648 Base32规范，支持两种模式：
 *
 *  * Base 32 Alphabet                 (ABCDEFGHIJKLMNOPQRSTUVWXYZ234567)
 *  * "Extended Hex" Base 32 Alphabet  (0123456789ABCDEFGHIJKLMNOPQRSTUV)
 */
object Base32 {
    private val INSTANCE = Base32Codec()
    /**
     * 编码
     *
     * @param bytes 数据
     * @return base32
     */
    fun encode(bytes: ByteArray): String {
        return INSTANCE.encode(bytes)
    }
    /**
     * base32编码
     *
     * @param source  被编码的base32字符串
     * @param charset 字符集
     * @return 被加密后的字符串
     */
    @JvmOverloads
    fun encode(source: String, charset: Charset = Data.UTF_8): String {
        return encode(ExtractUtil.bytes(source, charset))
    }

    /**
     * 编码
     *
     * @param bytes 数据（Hex模式）
     * @return base32
     */
    fun encodeHex(bytes: ByteArray): String {
        return INSTANCE.encode(bytes, true)
    }
    /**
     * base32编码（Hex模式）
     *
     * @param source  被编码的base32字符串
     * @param charset 字符集
     * @return 被加密后的字符串
     */
    @JvmOverloads
    fun encodeHex(source: String, charset: Charset = Data.UTF_8): String {
        return encodeHex(ExtractUtil.bytes(source, charset))
    }
    
    /**
     * 解码
     *
     * @param base32 base32编码
     * @return 数据
     */
    fun decode(base32: String): ByteArray {
        return INSTANCE.decode(base32)
    }
    /**
     * base32解码
     *
     * @param source  被解码的base32字符串
     * @param charset 字符集
     * @return 被加密后的字符串
     */
    @JvmOverloads
    fun decodeStr(source: String, charset: Charset = Data.UTF_8): String {
        return ExtractUtil.str(decode(source), charset)
    }

    /**
     * 解码
     *
     * @param base32 base32编码
     * @return 数据
     */
    fun decodeHex(base32: String): ByteArray {
        return INSTANCE.decode(base32, true)
    }
    /**
     * base32解码
     *
     * @param source  被解码的base32字符串
     * @param charset 字符集
     * @return 被加密后的字符串
     */
    @JvmOverloads
    fun decodeStrHex(source: String, charset: Charset = Data.UTF_8): String {
        return ExtractUtil.str(decodeHex(source), charset)
    }
}