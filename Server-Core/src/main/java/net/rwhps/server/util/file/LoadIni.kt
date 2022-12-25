/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.file

import net.rwhps.server.struct.OrderedMap
import net.rwhps.server.util.io.IoReadConversion
import net.rwhps.server.util.log.Log
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

/**
 * @author RW-HPS/Dr
 */
class LoadIni {
    private val data = OrderedMap<String, Any>()

    constructor(fileUtil: FileUtil) {
        try {
            load(fileUtil.readInputsStream())
        } catch (e: Exception) {
            Log.fatal("[Load Language Error]", e)
        }
    }

    constructor(inputStream: InputStream) {
        try {
            load(IoReadConversion.fileToReadStream(inputStream))
        } catch (e: Exception) {
            Log.fatal("[Load Language Error]", e)
        }
    }

    private fun load(inputStreamReader: InputStreamReader) {
        try {
            val p = Properties()
            p.load(inputStreamReader)
            p.forEach { key: Any, value: Any -> data.put(key as String, value as String) }
        } catch (e: Exception) {
            Log.error("[Load Language File] Error",e)
        }
    }
    
    private fun readObject(input: String): String? {
        return data[input]?.toString()
    }

    @JvmOverloads
    fun readString(input: String, def: String = ""): String {
        return readObject(input) ?:def
    }

    fun readInt(input: String, def: Int): Int {
        return readObject(input)?.toInt() ?:def
    }

    fun readBoolean(input: String, def: Boolean): Boolean {
        return readObject(input)?.toBoolean() ?:def
    }

    fun readFloat(input: String, def: Float): Float {
        return readObject(input)?.toFloat() ?:def
    }

    fun readLong(input: String, def: Long): Long {
        return readObject(input)?.toLong() ?:def
    }

    fun setObject(input: String, key: Any) {
        data.put(input, key)
    }
}