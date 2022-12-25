/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.dependent.redirections.slick

import net.rwhps.server.io.input.DisableSyncByteArrayInputStream
import net.rwhps.server.util.compression.core.CompressionDecoder
import org.newdawn.slick.util.ResourceLocation
import java.io.InputStream
import java.net.URL

/**
 * 通过ZIP打包来构成一个文件系统管理器
 *
 * @property compressionDecoder CompressionDecoder
 * @property a OrderedMap<String, ByteArray>
 * @constructor
 * @author RW-HPS/Dr
 */
class ZipFileSystemLocation(private val compressionDecoder: CompressionDecoder) : ResourceLocation {
    val a = compressionDecoder.getZipAllBytes()
    override fun getResourceAsStream(ref: String): InputStream? {
        return a.get(ref)?.let { DisableSyncByteArrayInputStream(it) }
    }

    override fun getResource(ref: String?): URL? {
        return null
    }
}