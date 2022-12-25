/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.compression.gzip

import net.rwhps.server.io.input.DisableSyncByteArrayInputStream
import net.rwhps.server.util.io.IoRead
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream


/**
 * @author RW-HPS/Dr
 */
object GzipDecoder {
    @JvmStatic
    @Throws(Exception::class)
    fun getGzipInputStream(inputStream: InputStream): InputStream {
        return BufferedInputStream(GZIPInputStream(inputStream))
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getUnGzipBytes(bytes: ByteArray): ByteArray {
        GZIPInputStream(DisableSyncByteArrayInputStream(bytes)).use {
            return IoRead.readInputStreamBytes(it)
        }

    }
}