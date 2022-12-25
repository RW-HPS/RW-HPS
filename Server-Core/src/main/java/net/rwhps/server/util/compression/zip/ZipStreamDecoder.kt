/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.compression.zip

import net.rwhps.server.game.GameMaps
import net.rwhps.server.io.input.DisableSyncByteArrayInputStream
import net.rwhps.server.struct.OrderedMap
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.compression.core.AbstractDecoder
import net.rwhps.server.util.io.IoRead
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.exp.FileException
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry

/**
 * Zip
 * @property zipStream ZipArchiveInputStream
 * @property reset Function0<Unit>
 * @constructor
 * @author RW-HPS/Dr
 */
internal class ZipStreamDecoder(inStream: InputStream) : AbstractDecoder {
    private var zipStream: ZipArchiveInputStream
    private val reset: ()->Unit

    init {
        val cache = BufferedInputStream(inStream)
        cache.mark(0)
        zipStream = ZipArchiveInputStream(cache)
        reset = { cache.reset() }
    }


    override fun close() {
        this.zipStream.close()
    }

    override fun getSpecifiedSuffixInThePackage(endWith: String, withSuffix: Boolean): OrderedMap<String, ByteArray> {
        reset()
        val data = OrderedMap<String, ByteArray>(8)
        var zipEntry: ZipArchiveEntry?
        try {
            IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                while (zipStream.nextZipEntry.also { zipEntry = it } != null) {
                    val nameCache = zipEntry!!.name
                    var name = nameCache.split("/").toTypedArray()[nameCache.split("/").toTypedArray().size - 1]
                    if (name.endsWith(endWith)) {
                        name = if (withSuffix) {
                            name
                        } else {
                            name.substring(0, name.length - name.substring(name.lastIndexOf(".")).length)
                        }
                        data.put(name, multiplexingReadStream.readInputStreamBytes(zipStream))
                    }
                }
            }
        } catch (e: IOException) {
            Log.error(e)
        }
        return data
    }

    override fun getSpecifiedSuffixInThePackageAllFileNameAndPath(endWithSeq: Seq<String>): OrderedMap<String, ByteArray> {
        reset()
        val data = OrderedMap<String, ByteArray>(8)
        try {
            var zipEntry: ZipArchiveEntry?
            IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                while (zipStream.nextZipEntry.also { zipEntry = it } != null) {
                    val nameCache = zipEntry!!.name

                    endWithSeq.eachAllFind({ nameCache.endsWith(it) }) { _: String ->
                        //Log.debug(nameCache,name)
                        data.put(nameCache, multiplexingReadStream.readInputStreamBytes(zipStream))
                    }
                }
            }
        } catch (e: IOException) {
            Log.error(e)
        }
        return data
    }

    override fun getTheFileNameOfTheSpecifiedSuffixInTheZip(endWith: String): Seq<String> {
        reset()
        val data = Seq<String>(8)
        var zipEntry: ZipEntry?
        while (zipStream.nextZipEntry.also { zipEntry = it } != null) {
            val name = zipEntry!!.name
            if (name.endsWith(endWith)) {
                data.add(name.substring(0, name.length - name.substring(name.lastIndexOf(".")).length))
            }
        }
        return data
    }

    @Throws(Exception::class)
    override fun getTheFileBytesOfTheSpecifiedSuffixInTheZip(mapData: GameMaps.MapData): ByteArray {
        reset()
        var zipEntry: ZipEntry?
        while (zipStream.nextZipEntry.also { zipEntry = it } != null) {
            val name = zipEntry!!.name
            if (name.endsWith(mapData.type) && name.contains(mapData.mapFileName)) {
                return IoRead.readInputStreamBytes(zipStream)
            }
        }
        throw FileException("CANNOT_FIND_FILE")
    }

    override fun getZipNameInputStream(nameIn: String): InputStream? {
        reset()
        var zipEntry: ZipArchiveEntry?
        try {
            IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                while (zipStream.nextZipEntry.also { zipEntry = it } != null) {
                    val nameCache = zipEntry!!.name
                    Log.clog(nameCache)
                    //val name = nameCache.split("/").toTypedArray()[nameCache.split("/").toTypedArray().size - 1]
                    if (nameIn == nameCache) {
                        return DisableSyncByteArrayInputStream(multiplexingReadStream.readInputStreamBytes(zipStream))
                    }
                }
            }
        } catch (e: IOException) {
            Log.error(e)
        }
        return null
    }

    override fun getZipAllBytes(withPath: Boolean): OrderedMap<String, ByteArray> {
        reset()
        val data = OrderedMap<String, ByteArray>(8)
        try {
            var zipEntry: ZipArchiveEntry?
            IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                while (zipStream.nextZipEntry.also { zipEntry = it } != null) {
                    val nameCache = zipEntry!!.name
                    val name = if (withPath) {
                        nameCache
                    } else {
                        nameCache.split("/").toTypedArray()[nameCache.split("/").toTypedArray().size - 1]
                    }
                    val bytes = multiplexingReadStream.readInputStreamBytes(zipStream)
                    data.put(name,bytes)
                }
            }
        } catch (e: IOException) {
            Log.error(e)
        }
        return data
    }
}