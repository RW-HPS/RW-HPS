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
import net.rwhps.server.struct.OrderedMap
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.compression.core.AbstractDecoder
import net.rwhps.server.util.io.IoRead
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.exp.FileException
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry

/**
 * Zip
 * @property zipFile ZipFile
 * @author RW-HPS/Dr
 */
internal class ZipFileDecoder : AbstractDecoder {
    private val zipFile: ZipFile

    constructor(inStream: InputStream) {
        zipFile = ZipFile(SeekableInMemoryByteChannel(IoRead.readInputStreamBytes(inStream)))
    }

    constructor(file: File) {
        zipFile = ZipFile(file)
    }

    override fun close() {
        this.zipFile.close()
    }

    override fun getSpecifiedSuffixInThePackage(endWith: String, withSuffix: Boolean): OrderedMap<String, ByteArray> {
        val data = OrderedMap<String, ByteArray>(8)
        var zipEntry: ZipArchiveEntry
        try {
            IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                val entries = zipFile.entries
                while (entries.hasMoreElements()) {
                    zipEntry = entries.nextElement() as ZipArchiveEntry
                    val nameCache = zipEntry.name
                    var name = nameCache.split("/").toTypedArray()[nameCache.split("/").toTypedArray().size - 1]
                    if (name.endsWith(endWith)) {
                        name = if (withSuffix) {
                            name
                        } else {
                            name.substring(0, name.length - name.substring(name.lastIndexOf(".")).length)
                        }
                        data.put(name,multiplexingReadStream.readInputStreamBytes(zipFile.getInputStream(zipEntry)))
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
            var zipEntry: ZipArchiveEntry
            IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                val entries = zipFile.entries
                while (entries.hasMoreElements()) {
                    zipEntry = entries.nextElement() as ZipArchiveEntry
                    val nameCache = zipEntry.name
                    endWithSeq.eachAllFind({ nameCache.endsWith(it) }) { _: String ->
                        //Log.debug(nameCache,name)
                        data.put(nameCache, multiplexingReadStream.readInputStreamBytes(zipFile.getInputStream(zipEntry)))
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
        var zipEntry: ZipEntry
        val entries = zipFile.entries
        while (entries.hasMoreElements()) {
            zipEntry = entries.nextElement() as ZipEntry
            val name = zipEntry.name
            if (name.endsWith(endWith)) {
                data.add(name.substring(0, name.length - name.substring(name.lastIndexOf(".")).length))
            }
        }
        return data
    }

    @Throws(Exception::class)
    override fun getTheFileBytesOfTheSpecifiedSuffixInTheZip(mapData: GameMaps.MapData): ByteArray {
        var zipEntry: ZipArchiveEntry
        val entries = zipFile.entries
        while (entries.hasMoreElements()) {
            zipEntry = entries.nextElement() as ZipArchiveEntry
            val name = zipEntry.name
            if (name.endsWith(mapData.type) && name.contains(mapData.mapFileName)) {
                return IoRead.readInputStreamBytes(zipFile.getInputStream(zipEntry))
            }
        }
        throw FileException("CANNOT_FIND_FILE")
    }

    override fun getZipNameInputStream(nameIn: String): InputStream? {
        try {
            val entries = zipFile.entries
            var ze: ZipEntry
            while (entries.hasMoreElements()) {
                ze = entries.nextElement()
                if (!ze.isDirectory) {
                    if (ze.name == nameIn) {
                        return zipFile.getInputStream(ze)
                    }
                }
            }
        } catch (e: Exception) {
            Log.error(e)
        }
        return null
    }

    override fun getZipAllBytes(withPath: Boolean): OrderedMap<String, ByteArray> {
        val data = OrderedMap<String, ByteArray>(8)
        try {
            var zipEntry: ZipArchiveEntry
            IoRead.MultiplexingReadStream().use { multiplexingReadStream ->
                val entries = zipFile.entriesInPhysicalOrder
                while (entries.hasMoreElements()) {
                    zipEntry = entries.nextElement() as ZipArchiveEntry
                    if (!zipEntry.isDirectory) {
                        val nameCache = zipEntry.name
                        val name = if (withPath) {
                            nameCache
                        } else {
                            nameCache.split("/").toTypedArray()[nameCache.split("/").toTypedArray().size - 1]
                        }
                        val bytes = multiplexingReadStream.readInputStreamBytes(zipFile.getInputStream(zipEntry))
                        data.put(name,bytes)
                    }
                }
            }
        } catch (e: IOException) {
            Log.error(e)
        }
        return data
    }
}