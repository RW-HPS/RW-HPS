/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.compression.core

import net.rwhps.server.game.GameMaps
import net.rwhps.server.struct.OrderedMap
import net.rwhps.server.struct.Seq
import java.io.InputStream

/**
 * 解压 通用接口
 * @author RW-HPS/Dr
 */
interface AbstractDecoder {
    /**
     * 关闭 Close
     */
    fun close()

    /**
     * 获取ZIP内的指定后辍的文件名(无后辍)与bytes
     * @param endWith String
     * @return OrderedMap<String, ByteArray>
     */
    fun getSpecifiedSuffixInThePackage(endWith: String, withSuffix: Boolean): OrderedMap<String, ByteArray>

    /**
     * 获取ZIP内的指定后辍的文件名(全名+路径)与bytes
     * @return OrderedMap<String, ByteArray>
     */
    fun getSpecifiedSuffixInThePackageAllFileNameAndPath(endWithSeq: Seq<String>): OrderedMap<String, ByteArray>

    /**
     * 获取ZIP内满足后辍的文件名(无后辍)
     * @param endWith String
     * @return Seq<String>
     */
    fun getTheFileNameOfTheSpecifiedSuffixInTheZip(endWith: String): Seq<String>

    /**
     * 获取地图文件字节
     * @param mapData MapData
     * @return ByteArray
     */
    @Throws(Exception::class)
    fun getTheFileBytesOfTheSpecifiedSuffixInTheZip(mapData: GameMaps.MapData): ByteArray

    /**
     * 获取 ZIP 内指定文件名的字节流
     * @param nameIn String
     * @return InputStream?
     */
    fun getZipNameInputStream(nameIn: String): InputStream?

    /**
     * 获取 ZIP 全部数据
     * @return OrderedMap<String, ByteArray>
     */
    fun getZipAllBytes(withPath: Boolean): OrderedMap<String, ByteArray>
}