/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.io.input

import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.inandout.GameInputStreamAndOutputStream
import net.rwhps.server.util.compression.gzip.GzipDecoder

object CompressInputStream {
    @JvmStatic
    internal fun getGzipInputStream(isGzip: Boolean, bytes: ByteArray): GameInputStream {
        return GameInputStream(
            if (isGzip) {
                DisableSyncByteArrayInputStream(GzipDecoder.getUnGzipBytes(bytes))
            } else {
                DisableSyncByteArrayInputStream(bytes)
            }
        )
    }

    @JvmStatic
    internal fun getGzipInputStreamAndOutputStream(isGzip: Boolean, bytes: ByteArray): GameInputStreamAndOutputStream {
        return GameInputStreamAndOutputStream(
            if (isGzip) {
                DisableSyncByteArrayInputStream(GzipDecoder.getUnGzipBytes(bytes))
            } else {
                DisableSyncByteArrayInputStream(bytes)
            }
        )
    }
}