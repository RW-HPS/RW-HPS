/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.compression.core

import java.io.InputStream

/**
 * 压缩 通用接口
 * @author RW-HPS/Dr
 */
interface AbstractEncoder {
    fun addCompressBytes(name:String, compressionDecoder: CompressionDecoder)

    fun addCompressBytes(name:String, inStream: InputStream)

    fun addCompressBytes(name:String, bytes: ByteArray)

    fun flash(): ByteArray
}