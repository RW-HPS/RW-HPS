/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.encryption.digest

import net.rwhps.server.data.global.Data
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

/**
 * 摘要算法
 */
object DigestUtil {
    /**
     * 计算32位MD5摘要值
     *
     * @param data 被摘要数据
     * @return MD5摘要
     */
    @JvmStatic
    fun md5(data: ByteArray): ByteArray {
        return MD5().digest(data)
    }
    /**
     * 计算32位MD5摘要值
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return MD5摘要
     */
    @JvmOverloads
    @JvmStatic
    fun md5(data: String, charset: String = "UTF-8"): ByteArray {
        return MD5().digest(data, charset)
    }

    /**
     * 计算32位MD5摘要值
     *
     * @param data 被摘要数据
     * @return MD5摘要
     */
    @JvmStatic
    fun md5(data: InputStream): ByteArray {
        return MD5().digest(data)
    }

    /**
     * 计算32位MD5摘要值
     *
     * @param file 被摘要文件
     * @return MD5摘要
     */
    @JvmStatic
    fun md5(file: File): ByteArray {
        return MD5().digest(file)
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return MD5摘要的16进制表示
     */
    @JvmStatic
    fun md5Hex(data: ByteArray): String {
        return MD5().digestHex(data)
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return MD5摘要的16进制表示
     */
    @JvmStatic
    fun md5Hex(data: String, charset: String): String {
        return MD5().digestHex(data, charset)
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return MD5摘要的16进制表示
     */
    @JvmStatic
    fun md5Hex(data: String, charset: Charset): String {
        return MD5().digestHex(data, charset)
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return MD5摘要的16进制表示
     */
    @JvmStatic
    fun md5Hex(data: String): String {
        return md5Hex(data, "UTF-8")
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return MD5摘要的16进制表示
     */
    @JvmStatic
    fun md5Hex(data: InputStream): String {
        return MD5().digestHex(data)
    }

    /**
     * 计算32位MD5摘要值，并转为16进制字符串
     *
     * @param file 被摘要文件
     * @return MD5摘要的16进制表示
     */
    @JvmStatic
    fun md5Hex(file: File): String {
        return MD5().digestHex(file)
    }

    /**
     * 计算16位MD5摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return MD5摘要的16进制表示
     */
    @JvmStatic
    fun md5Hex16(data: ByteArray): String {
        return MD5().digestHex16(data)
    }
    /**
     * 计算16位MD5摘要值，并转为16进制字符串
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return MD5摘要的16进制表示
     */
    @JvmOverloads
    @JvmStatic
    fun md5Hex16(data: String, charset: Charset = Data.UTF_8): String {
        return MD5().digestHex16(data, charset)
    }

    /**
     * 计算16位MD5摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return MD5摘要的16进制表示
     */
    @JvmStatic
    fun md5Hex16(data: InputStream): String {
        return MD5().digestHex16(data)
    }

    /**
     * 计算16位MD5摘要值，并转为16进制字符串
     *
     * @param file 被摘要文件
     * @return MD5摘要的16进制表示
     */
    @JvmStatic
    fun md5Hex16(file: File): String {
        return MD5().digestHex16(file)
    }

    /**
     * 32位MD5转16位MD5
     *
     * @param md5Hex 32位MD5
     * @return 16位MD5
     */
    @JvmStatic
    fun md5HexTo16(md5Hex: String): String {
        return md5Hex.substring(8, 24)
    }
   
    /**
     * 计算SHA-1摘要值
     *
     * @param data 被摘要数据
     * @return SHA-1摘要
     */
    @JvmStatic
    fun sha1(data: ByteArray): ByteArray {
        return Digester(DigestAlgorithm.SHA1).digest(data)
    }
    /**
     * 计算SHA-1摘要值
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return SHA-1摘要
     */
    @JvmOverloads
    @JvmStatic
    fun sha1(data: String, charset: String = "UTF-8"): ByteArray {
        return Digester(DigestAlgorithm.SHA1).digest(data, charset)
    }

    /**
     * 计算SHA-1摘要值
     *
     * @param data 被摘要数据
     * @return SHA-1摘要
     */
    @JvmStatic
    fun sha1(data: InputStream): ByteArray {
        return Digester(DigestAlgorithm.SHA1).digest(data)
    }

    /**
     * 计算SHA-1摘要值
     *
     * @param file 被摘要文件
     * @return SHA-1摘要
     */
    @JvmStatic
    fun sha1(file: File): ByteArray {
        return Digester(DigestAlgorithm.SHA1).digest(
            file
        )
    }

    /**
     * 计算SHA-1摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-1摘要的16进制表示
     */
    fun sha1Hex(data: ByteArray): String {
        return Digester(DigestAlgorithm.SHA1).digestHex(data)
    }
    /**
     * 计算SHA-1摘要值，并转为16进制字符串
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return SHA-1摘要的16进制表示
     */
    @JvmOverloads
    @JvmStatic
    fun sha1Hex(data: String, charset: String = "UTF-8"): String {
        return Digester(DigestAlgorithm.SHA1).digestHex(
            data, charset
        )
    }

    /**
     * 计算SHA-1摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-1摘要的16进制表示
     */
    @JvmStatic
    fun sha1Hex(data: InputStream): String {
        return Digester(DigestAlgorithm.SHA1).digestHex(data)
    }

    /**
     * 计算SHA-1摘要值，并转为16进制字符串
     *
     * @param file 被摘要文件
     * @return SHA-1摘要的16进制表示
     */
    @JvmStatic
    fun sha1Hex(file: File): String {
        return Digester(DigestAlgorithm.SHA1).digestHex(file)
    }
   
    /**
     * 计算SHA-256摘要值
     *
     * @param data 被摘要数据
     * @return SHA-256摘要
     */
    @JvmStatic
    fun sha256(data: ByteArray): ByteArray {
        return Digester(DigestAlgorithm.SHA256).digest(data)
    }
    /**
     * 计算SHA-256摘要值
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return SHA-256摘要
     */
    @JvmOverloads
    @JvmStatic
    fun sha256(data: String, charset: String = "UTF-8"): ByteArray {
        return Digester(DigestAlgorithm.SHA256).digest(data, charset)
    }

    /**
     * 计算SHA-256摘要值
     *
     * @param data 被摘要数据
     * @return SHA-256摘要
     */
    @JvmStatic
    fun sha256(data: InputStream): ByteArray {
        return Digester(DigestAlgorithm.SHA256).digest(data)
    }

    /**
     * 计算SHA-256摘要值
     *
     * @param file 被摘要文件
     * @return SHA-256摘要
     */
    @JvmStatic
    fun sha256(file: File): ByteArray {
        return Digester(DigestAlgorithm.SHA256).digest(file)
    }

    /**
     * 计算SHA-1摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-256摘要的16进制表示
     */
    @JvmStatic
    fun sha256Hex(data: ByteArray): String {
        return Digester(DigestAlgorithm.SHA256).digestHex(data)
    }
    /**
     * 计算SHA-256摘要值，并转为16进制字符串
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return SHA-256摘要的16进制表示
     */
    @JvmOverloads
    @JvmStatic
    fun sha256Hex(data: String, charset: String = "UTF-8"): String {
        return Digester(DigestAlgorithm.SHA256).digestHex(data, charset)
    }

    /**
     * 计算SHA-256摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-256摘要的16进制表示
     */
    @JvmStatic
    fun sha256Hex(data: InputStream): String {
        return Digester(DigestAlgorithm.SHA256).digestHex(data)
    }

    /**
     * 计算SHA-256摘要值，并转为16进制字符串
     *
     * @param file 被摘要文件
     * @return SHA-256摘要的16进制表示
     */
    @JvmStatic
    fun sha256Hex(file: File): String {
        return Digester(DigestAlgorithm.SHA256).digestHex(file)
    }

    /**
     * 新建摘要器
     *
     * @param algorithm 签名算法
     * @return Digester
     */
    @JvmStatic
    fun digester(algorithm: DigestAlgorithm): Digester {
        return Digester(algorithm)
    }

    /**
     * 新建摘要器
     *
     * @param algorithm 签名算法
     * @return Digester
     */
    @JvmStatic
    fun digester(algorithm: String): Digester {
        return Digester(algorithm)
    }

    /**
     * 计算SHA-512摘要值
     *
     * @param data 被摘要数据
     * @return SHA-512摘要
     */
    @JvmStatic
    fun sha512(data: ByteArray): ByteArray {
        return Digester(DigestAlgorithm.SHA512).digest(data)
    }
    /**
     * 计算SHA-512摘要值
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return SHA-512摘要
     */
    @JvmOverloads
    @JvmStatic
    fun sha512(data: String, charset: String = "UTF-8"): ByteArray {
        return Digester(DigestAlgorithm.SHA512).digest(
            data, charset
        )
    }

    /**
     * 计算SHA-512摘要值
     *
     * @param data 被摘要数据
     * @return SHA-512摘要
     */
    @JvmStatic
    fun sha512(data: InputStream): ByteArray {
        return Digester(DigestAlgorithm.SHA512).digest(data)
    }

    /**
     * 计算SHA-512摘要值
     *
     * @param file 被摘要文件
     * @return SHA-512摘要
     */
    @JvmStatic
    fun sha512(file: File): ByteArray {
        return Digester(DigestAlgorithm.SHA512).digest(file)
    }

    /**
     * 计算SHA-1摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-512摘要的16进制表示
     */
    @JvmStatic
    fun sha512Hex(data: ByteArray): String {
        return Digester(DigestAlgorithm.SHA512).digestHex(data)
    }
    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param data    被摘要数据
     * @param charset 编码
     * @return SHA-512摘要的16进制表示
     */
    @JvmOverloads
    @JvmStatic
    fun sha512Hex(data: String, charset: String = "UTF-8"): String {
        return Digester(DigestAlgorithm.SHA512).digestHex(data, charset)
    }

    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @return SHA-512摘要的16进制表示
     */
    @JvmStatic
    fun sha512Hex(data: InputStream): String {
        return Digester(DigestAlgorithm.SHA512).digestHex(data)
    }

    /**
     * 计算SHA-512摘要值，并转为16进制字符串
     *
     * @param file 被摘要文件
     * @return SHA-512摘要的16进制表示
     */
    @JvmStatic
    fun sha512Hex(file: File): String {
        return Digester(DigestAlgorithm.SHA512).digestHex(file)
    }
}