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
import net.rwhps.server.util.ExtractUtil
import net.rwhps.server.util.HexUtil
import net.rwhps.server.util.encryption.SecureUtil
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.io.IoRead
import net.rwhps.server.util.log.exp.CryptoException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.Provider
import kotlin.math.max

/**
 * 摘要算法
 */
open class Digester {
    /**
     * 获得 [MessageDigest]
     *
     * @return [MessageDigest]
     */
    private var digest: MessageDigest? = null

    /** 盐值  */
    protected var salt: ByteArray = ByteArray(0)

    /** 加盐位置，即将盐值字符串放置在数据的index数，默认0  */
    protected var saltPosition = 0

    /** 散列次数  */
    protected var digestCount = 0

    /**
     * 构造
     *
     * @param algorithm 算法枚举
     */
    constructor(algorithm: DigestAlgorithm) : this(algorithm.type)

    /**
     * 构造
     *
     * @param algorithm 算法
     * @param provider 算法提供者，null表示JDK默认，可以引入Bouncy Castle等来提供更多算法支持
     */
    constructor(algorithm: DigestAlgorithm, provider: Provider?) {
        init(algorithm.type, provider)
    }
    /**
     * 构造
     *
     * @param algorithm 算法
     * @param provider 算法提供者，null表示JDK默认，可以引入Bouncy Castle等来提供更多算法支持
     */
    @JvmOverloads
    constructor(algorithm: String, provider: Provider? = null) {
        init(algorithm, provider)
    }

    /**
     * 初始化
     *
     * @param algorithm 算法
     * @param provider 算法提供者，null表示JDK默认，可以引入Bouncy Castle等来提供更多算法支持
     * @return Digester
     * @throws CryptoException Cause by IOException
     */
    fun init(algorithm: String, provider: Provider?): Digester {
        digest = if (null == provider) {
            SecureUtil.createMessageDigest(algorithm)
        } else {
            try {
                MessageDigest.getInstance(algorithm, provider)
            } catch (e: NoSuchAlgorithmException) {
                throw CryptoException(e)
            }
        }
        return this
    }

    /**
     * 设置加盐内容
     *
     * @param salt 盐值
     * @return this
     */
    fun setSalt(salt: ByteArray): Digester {
        this.salt = salt
        return this
    }

    /**
     * 设置加盐的位置，只有盐值存在时有效<br></br>
     * 加盐的位置指盐位于数据byte数组中的位置，例如：
     * data: 0123456
     * 则当saltPosition = 2时，盐位于data的1和2中间，即第二个空隙，即：
     * data: 01[salt]23456
     *
     * @param saltPosition 盐的位置
     * @return this
     */
    fun setSaltPosition(saltPosition: Int): Digester {
        this.saltPosition = saltPosition
        return this
    }

    /**
     * 设置重复计算摘要值次数
     *
     * @param digestCount 摘要值次数
     * @return this
     */
    fun setDigestCount(digestCount: Int): Digester {
        this.digestCount = digestCount
        return this
    }

    /**
     * 重置[MessageDigest]
     *
     * @return this
     */
    fun reset(): Digester {
        digest!!.reset()
        return this
    }

    /**
     * 生成文件摘要
     *
     * @param data 被摘要数据
     * @return 摘要
     */
    fun digest(data: String): ByteArray {
        return digest(data, Data.UTF_8)
    }

    /**
     * 生成文件摘要
     *
     * @param data 被摘要数据
     * @param charsetName 编码
     * @return 摘要
     */
    fun digest(data: String, charsetName: String): ByteArray {
        return digest(data, Charset.forName(charsetName))
    }

    /**
     * 生成文件摘要
     *
     * @param data 被摘要数据
     * @param charset 编码
     * @return 摘要
     */
    fun digest(data: String, charset: Charset): ByteArray {
        return digest(ExtractUtil.bytes(data, charset))
    }

    /**
     * 生成文件摘要
     *
     * @param data 被摘要数据
     * @return 摘要
     */
    fun digestHex(data: String): String {
        return digestHex(data, Data.UTF_8)
    }

    /**
     * 生成文件摘要，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @param charsetName 编码
     * @return 摘要
     */
    fun digestHex(data: String, charsetName: String): String {
        return digestHex(data, Charset.forName(charsetName))
    }

    /**
     * 生成文件摘要，并转为16进制字符串
     *
     * @param data 被摘要数据
     * @param charset 编码
     * @return 摘要
     */
    fun digestHex(data: String, charset: Charset): String {
        return HexUtil.encodeHexStr(digest(data, charset))
    }

    /**
     * 生成文件摘要<br></br>
     * 使用默认缓存大小，见 [IoRead.DEFAULT_BUFFER_SIZE]
     *
     * @param file 被摘要文件
     * @return 摘要bytes
     * @throws CryptoException Cause by IOException
     */
    @Throws(IOException::class)
    fun digest(file: File): ByteArray {
        return FileUtil(file,false).readFileByte()
    }

    /**
     * 生成文件摘要，并转为16进制字符串<br></br>
     * 使用默认缓存大小，见 [IoRead.DEFAULT_BUFFER_SIZE]
     *
     * @param file 被摘要文件
     * @return 摘要
     */
    fun digestHex(file: File): String {
        return HexUtil.encodeHexStr(digest(file))
    }

    /**
     * 生成摘要，考虑加盐和重复摘要次数
     *
     * @param data 数据bytes
     * @return 摘要bytes
     */
    fun digest(data: ByteArray): ByteArray {
        val result: ByteArray = if (saltPosition <= 0) {
            // 加盐在开头，自动忽略空盐值
            doDigest(salt, data)
        } else if (saltPosition >= data.size) {
            // 加盐在末尾，自动忽略空盐值
            doDigest(data, salt)
        } else if (salt.isNotEmpty()) {
            // 加盐在中间
            digest!!.update(data, 0, saltPosition)
            digest!!.update(salt)
            digest!!.update(data, saltPosition, data.size - saltPosition)
            digest!!.digest()
        } else {
            // 无加盐
            doDigest(data)
        }
        return resetAndRepeatDigest(result)
    }

    /**
     * 生成摘要，并转为16进制字符串<br></br>
     *
     * @param data 被摘要数据
     * @return 摘要
     */
    fun digestHex(data: ByteArray): String {
        return HexUtil.encodeHexStr(digest(data))
    }

    /**
     * 生成摘要
     *
     * @param data [InputStream] 数据流
     * @return 摘要bytes
     * @throws IOException IO异常
     */
    @Throws(IOException::class)
    fun digest(data: InputStream): ByteArray {
        val result: ByteArray = IoRead.readInputStreamBytes(data)
        return digest(result)
    }

    /**
     * 生成摘要，并转为16进制字符串<br></br>
     * 使用默认缓存大小，见 [IoRead.DEFAULT_BUFFER_SIZE]
     *
     * @param data 被摘要数据
     * @return 摘要
     */
    fun digestHex(data: InputStream): String {
        return HexUtil.encodeHexStr(digest(data))
    }

    /**
     * 获取散列长度，0表示不支持此方法
     *
     * @return 散列长度，0表示不支持此方法
     */
    val digestLength: Int get() = digest!!.digestLength

    /**
     * 生成摘要
     *
     * @param datas 数据bytes
     * @return 摘要bytes
     */
    private fun doDigest(vararg datas: ByteArray): ByteArray {
        for (data in datas) {
            digest!!.update(data)
        }
        return digest!!.digest()
    }

    /**
     * 重复计算摘要，取决于[.digestCount] 值<br></br>
     * 每次计算摘要前都会重置[.digest]
     *
     * @param digestDataIn 第一次摘要过的数据
     * @return 摘要
     */
    private fun resetAndRepeatDigest(digestDataIn: ByteArray): ByteArray {
        var digestData = digestDataIn
        val digestCount = max(1, digestCount)
        reset()
        for (i in 0 until digestCount - 1) {
            digestData = doDigest(digestData)
            reset()
        }
        return digestData
    }
}