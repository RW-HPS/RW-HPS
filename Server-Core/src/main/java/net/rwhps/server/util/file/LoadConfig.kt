/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.file

import net.rwhps.server.data.json.Json
import net.rwhps.server.data.json.Json.Companion.toJson
import net.rwhps.server.struct.OrderedMap
import net.rwhps.server.util.IsUtil.isBlank
import net.rwhps.server.util.file.FileUtil.Companion.getFolder
import net.rwhps.server.util.log.Log.clog
import net.rwhps.server.util.log.Log.error

/**
 * @author RW-HPS/Dr
 */
class LoadConfig {
    private val data = OrderedMap<String, Any>()
    private val fileUtil: FileUtil

    constructor(file: String, isFile: Boolean) {
        fileUtil = if (isFile) getFolder(file) else FileUtil(file)
        reLoadConfig()
    }

    constructor(file: String, name: String) {
        fileUtil = getFolder(file).toFile(name)
        reLoadConfig()
    }

    fun reLoadConfig() {
        if (fileUtil.notExists() || fileUtil.readFileStringData().isEmpty()) {
            error("NO Config.Json Use default configuration")
            return
        }
        val json = Json(fileUtil.readFileStringData())
        //json对象转Map
        json.getInnerMap().forEach { (key: String, value: Any) -> data.put(key, value) }
    }

    private fun load(input: String, def: Any): String {
        val result = data[input]
        if (result == null) {
            clog("NO KEY- Please check the file", input)
            data.put(input, def)
            return def.toString()
        }
        return result.toString()
    }

    @JvmOverloads
    fun readString(input: String, def: Any = ""): String {
        val str = load(input, def)
        return if (isBlank(str)) "" else str
    }

    fun readInt(input: String, def: Int): Int {
        val str = load(input, def)
        return str.toInt()
    }

    fun readBoolean(input: String, def: Boolean): Boolean {
        val str = load(input, def)
        return str.toBoolean()
    }

    fun readFloat(input: String, def: Float): Float {
        val str = load(input, def)
        return str.toFloat()
    }

    fun readLong(input: String, def: Long): Long {
        val str = load(input, def)
        return str.toLong()
    }

    fun setObject(input: String, key: Any) {
        data.put(input, key)
    }

    fun save() {
        val map: MutableMap<String, Any> = HashMap()
        data.each { key: String, value: Any -> map[key] = value }
        fileUtil.writeFile(toJson(map))
        clog("SAVE CONFIG OK")
    }
}