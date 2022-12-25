/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.compression.lz77

import net.rwhps.server.game.GameMaps
import net.rwhps.server.io.input.DisableSyncByteArrayInputStream
import net.rwhps.server.struct.OrderedMap
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.compression.core.AbstractDecoder
import net.rwhps.server.util.io.IoRead
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.exp.FileException
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * 7z
 * @property lz77File SevenZFile
 * @author RW-HPS/Dr
 */
internal open class Lz77FileDecoder : AbstractDecoder {
    private val lz77File: SevenZFile

    constructor(inStream: InputStream) {
        lz77File = SevenZFile(SeekableInMemoryByteChannel(IoRead.readInputStreamBytes(inStream)))
    }

    constructor(file: File) {
        lz77File = SevenZFile(file)
    }

    override fun close() {
        this.lz77File.close()
    }

    override fun getSpecifiedSuffixInThePackage(endWith: String, withSuffix: Boolean): OrderedMap<String, ByteArray> {
        val data = OrderedMap<String, ByteArray>(8)
        var lz77Entry: SevenZArchiveEntry?
        try {
            IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                while (lz77File.nextEntry.also { lz77Entry = it } != null) {
                    val nameCache = lz77Entry!!.name
                    var name = nameCache.split("/").toTypedArray()[nameCache.split("/").toTypedArray().size - 1]
                    if (name.endsWith(endWith)) {
                        name = if (withSuffix) {
                            name
                        } else {
                            name.substring(0, name.length - name.substring(name.lastIndexOf(".")).length)
                        }
                        data.put(name, multiplexingReadStream.readInputStreamBytes(lz77File.getInputStream(lz77Entry)))
                    }
                }
            }
        } catch (e: IOException) {
            Log.error(e)
        }
        return data
    }

    override fun getSpecifiedSuffixInThePackageAllFileNameAndPath(endWithSeq: Seq<String>): OrderedMap<String, ByteArray> {
        val data = OrderedMap<String, ByteArray>(8)
        try {
            var lz77Entry: SevenZArchiveEntry?
            IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                while (lz77File.nextEntry.also { lz77Entry = it } != null) {
                    val nameCache = lz77Entry!!.name

                    endWithSeq.eachAllFind({ nameCache.endsWith(it) }) { _: String ->
                        //Log.debug(nameCache,name)
                        data.put(nameCache, multiplexingReadStream.readInputStreamBytes(lz77File.getInputStream(lz77Entry)))
                    }
                }
            }
        } catch (e: IOException) {
            Log.error(e)
        }
        return data
    }

    override fun getTheFileNameOfTheSpecifiedSuffixInTheZip(endWith: String): Seq<String> {
        val data = Seq<String>(8)
        var lz77Entry: SevenZArchiveEntry?
        while (lz77File.nextEntry.also { lz77Entry = it } != null) {
            val name = lz77Entry!!.name
            if (name.endsWith(endWith)) {
                data.add(name.substring(0, name.length - name.substring(name.lastIndexOf(".")).length))
            }
        }
        return data
    }

    @Throws(Exception::class)
    override fun getTheFileBytesOfTheSpecifiedSuffixInTheZip(mapData: GameMaps.MapData): ByteArray {
        var lz77Entry: SevenZArchiveEntry?
        while (lz77File.nextEntry.also { lz77Entry = it } != null) {
            val name = lz77Entry!!.name
            if (name.endsWith(mapData.type) && name.contains(mapData.mapFileName)) {
                return IoRead.readInputStreamBytes(lz77File.getInputStream(lz77Entry))
            }
        }
        throw FileException("CANNOT_FIND_FILE")
    }

    override fun getZipNameInputStream(nameIn: String): InputStream? {
        var lz77Entry: SevenZArchiveEntry?
        try {
            IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                while (lz77File.nextEntry.also { lz77Entry = it } != null) {
                    val nameCache = lz77Entry!!.name
                    Log.clog(nameCache)
                    //val name = nameCache.split("/").toTypedArray()[nameCache.split("/").toTypedArray().size - 1]
                    if (nameIn == nameCache) {
                        return DisableSyncByteArrayInputStream(multiplexingReadStream.readInputStreamBytes(lz77File.getInputStream(lz77Entry)))
                    }
                }
            }
        } catch (e: IOException) {
            Log.error(e)
        }
        return null
    }

    override fun getZipAllBytes(withPath: Boolean): OrderedMap<String, ByteArray> {
        val data = OrderedMap<String, ByteArray>(8)
        try {
            var lz77Entry: SevenZArchiveEntry?
            IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                while (lz77File.nextEntry.also { lz77Entry = it } != null) {
                    val nameCache = lz77Entry!!.name
                    val name = if (withPath) {
                        nameCache
                    } else {
                        nameCache.split("/").toTypedArray()[nameCache.split("/").toTypedArray().size - 1]
                    }
                    val bytes = multiplexingReadStream.readInputStreamBytes(lz77File.getInputStream(lz77Entry))
                    data.put(name,bytes)
                }
            }
        } catch (e: IOException) {
            Log.error(e)
        }
        return data
    }
}