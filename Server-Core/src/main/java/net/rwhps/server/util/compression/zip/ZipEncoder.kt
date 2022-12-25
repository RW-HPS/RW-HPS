/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.compression.zip

import net.rwhps.server.io.output.ByteArrayOutputStream
import net.rwhps.server.io.output.DisableSyncByteArrayOutputStream
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.compression.core.AbstractEncoder
import net.rwhps.server.util.compression.core.CompressionDecoder
import net.rwhps.server.util.io.IoRead.copyInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.zip.ZipOutputStream

/**
 * 打包 ZIP 生成 Bytes
 *
 * 线程不安全
 *
 * @author RW-HPS/Dr
 */
class ZipEncoder : AbstractEncoder {
    private val outputStream = DisableSyncByteArrayOutputStream()
    private val zipOutCache = ZipOutputStream(outputStream)

    /**
     * 合并压缩文件
     * @param sourceZipFiles in
     * @return byte out
     * @throws IOException err
     */
    //@JvmStatic
    @Throws(IOException::class)
    fun incrementalUpdate(updateFile: Seq<String>, vararg sourceZipFiles: String?): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { out ->
            val names = HashSet<String>()
            for (sourceZipFile in sourceZipFiles) {
                ZipFile(sourceZipFile).use { zipFile ->
                    var ze: ZipArchiveEntry
                    val enumeration: Enumeration<out ZipArchiveEntry> = zipFile.entries
                    while (enumeration.hasMoreElements()) {
                        ze = enumeration.nextElement()
                        if (!ze.isDirectory) {
                            /* 只合并第一个源压缩包里面的文件，后面若有相同的文件则跳过执行合并 */
                            if (names.contains(ze.name) || !updateFile.contains(ze.name)) {
                                continue
                            }
                            val oze = ZipArchiveEntry(ze.name)
                            out.putNextEntry(oze)
                            if (ze.size > 0) {
                                copyInputStream(zipFile.getInputStream(ze), out)
                                out.closeEntry()
                                out.flush()
                            }
                            names.add(oze.name)
                        }
                    }
                }
            }
        }
        return outputStream.toByteArray()
    }

    override fun addCompressBytes(name: String, compressionDecoder: CompressionDecoder) {
        compressionDecoder.use {
            it.getZipAllBytes().forEach { addCompressBytes(it.key,it.value) }
        }
    }

    override fun addCompressBytes(name: String, inStream: InputStream) {
        addCompressBytes(name, inStream, null)
    }

    override fun addCompressBytes(name: String, bytes: ByteArray) {
        addCompressBytes(name, null, bytes)
    }

    override fun flash(): ByteArray {
        zipOutCache.use {
            it.flush()
            return outputStream.toByteArray()
        }
    }


    private fun addCompressBytes(name: String, inStream: InputStream?, bytes: ByteArray?) {
        val oze = ZipArchiveEntry(name)
        zipOutCache.putNextEntry(oze)
        if (inStream == null) {
            zipOutCache.write(bytes!!)
        } else {
            copyInputStream(inStream, zipOutCache)
        }
        zipOutCache.closeEntry()
    }
}