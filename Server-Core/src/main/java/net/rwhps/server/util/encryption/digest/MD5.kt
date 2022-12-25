/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *  
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.encryption.digest

import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

/**
 * MD5算法
 */
class MD5
    /**
     * 构造默认
     */
    () : Digester(DigestAlgorithm.MD5) {
    /**
     * 构造
     *
     * @param salt 盐值
     * @param digestCount 摘要次数，当此值小于等于1,默认为1。
     */
    constructor(salt: ByteArray, digestCount: Int) : this(salt, 0, digestCount)
    /**
     * 构造
     *
     * @param salt 盐值
     * @param saltPosition 加盐位置，即将盐值字符串放置在数据的index数，默认0
     * @param digestCount 摘要次数，当此值小于等于1,默认为1。
     */
    constructor(salt: ByteArray, saltPosition: Int , digestCount: Int) : this() {
        this.salt = salt
        this.saltPosition = saltPosition
        this.digestCount = digestCount
    }

    /**
     * 生成16位MD5摘要
     *
     * @param data 数据
     * @return 16位MD5摘要
     */
    fun digestHex16(data: String): String {
        return DigestUtil.md5HexTo16(digestHex(data))
    }

    /**
     * 生成16位MD5摘要
     *
     * @param data 数据
     * @param charset 编码
     * @return 16位MD5摘要
     */
    fun digestHex16(data: String, charset: Charset): String {
        return DigestUtil.md5HexTo16(digestHex(data, charset))
    }

    /**
     * 生成16位MD5摘要
     *
     * @param data 数据
     * @return 16位MD5摘要
     */
    fun digestHex16(data: InputStream): String {
        return DigestUtil.md5HexTo16(digestHex(data))
    }

    /**
     * 生成16位MD5摘要
     *
     * @param data 数据
     * @return 16位MD5摘要
     */
    fun digestHex16(data: File): String {
        return DigestUtil.md5HexTo16(digestHex(data))
    }

    /**
     * 生成16位MD5摘要
     *
     * @param data 数据
     * @return 16位MD5摘要
     */
    fun digestHex16(data: ByteArray): String {
        return DigestUtil.md5HexTo16(digestHex(data))
    }
}