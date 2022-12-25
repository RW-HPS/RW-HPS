/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.encryption

import net.rwhps.server.util.log.exp.CryptoException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object SecureUtil {
    /**
     * 创建[MessageDigest]
     *
     * @param algorithm 算法
     * @return [MessageDigest]
     * @since 4.5.2
     */
    fun createMessageDigest(algorithm: String?): MessageDigest {
        val messageDigest: MessageDigest = try {
            MessageDigest.getInstance(algorithm)
        } catch (e: NoSuchAlgorithmException) {
            throw CryptoException(e)
        }
        return messageDigest
    }
}