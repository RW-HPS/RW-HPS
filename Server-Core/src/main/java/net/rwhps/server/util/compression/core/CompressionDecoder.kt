/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.compression.core

import net.rwhps.server.game.GameMaps.MapData
import net.rwhps.server.struct.OrderedMap
import net.rwhps.server.struct.Seq
import java.io.Closeable
import java.io.InputStream

/**
 * 解码
 * @author RW-HPS/Dr
 */
class CompressionDecoder(private val zipRead: AbstractDecoder) : Closeable {

    override fun close() {
        zipRead.close()
    }

    /**
     * 获取ZIP内的指定后辍的文件名与bytes
     * @param endWith String
     * @param withSuffix 要不要携带文件后缀
     * @return OrderedMap<String, ByteArray>
     */
    @JvmOverloads
    fun getSpecifiedSuffixInThePackage(endWith: String, withSuffix: Boolean= false): OrderedMap<String, ByteArray> {
        return zipRead.getSpecifiedSuffixInThePackage(endWith,withSuffix)
    }


    /**
     * 获取ZIP内的指定后辍的文件名(全名+路径)与bytes
     * @param endWith String
     * @return OrderedMap<String, ByteArray>
     */
    fun getSpecifiedSuffixInThePackageAllFileNameAndPath(endWithSeq: Seq<String>): OrderedMap<String, ByteArray> {
        return zipRead.getSpecifiedSuffixInThePackageAllFileNameAndPath(endWithSeq)
    }

    /**
     * 获取ZIP内满足后辍的文件名(无后辍)
     * @param endWith String
     * @return Seq<String>
     */
    fun getTheFileNameOfTheSpecifiedSuffixInTheZip(endWith: String): Seq<String> {
        return zipRead.getTheFileNameOfTheSpecifiedSuffixInTheZip(endWith)
    }

    /**
     * 获取地图文件字节
     * @param mapData MapData
     * @return ByteArray
     */
    @Throws(Exception::class)
    fun getTheFileBytesOfTheSpecifiedSuffixInTheZip(mapData: MapData): ByteArray {
        return zipRead.getTheFileBytesOfTheSpecifiedSuffixInTheZip(mapData)
    }

    fun getZipNameInputStream(name: String): InputStream? {
        return zipRead.getZipNameInputStream(name)
    }

    @JvmOverloads
    fun getZipAllBytes(withPath: Boolean = true): OrderedMap<String, ByteArray> {
        return zipRead.getZipAllBytes(withPath)
    }
}