/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.json

import com.google.gson.Gson
import net.rwhps.server.struct.ObjectMap
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.IsUtil
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.inline.getDataResult
import net.rwhps.server.util.inline.getDataResultObject
import net.rwhps.server.util.inline.ifNullResult
import net.rwhps.server.util.inline.toPrettyPrintingJson
import org.json.JSONArray
import org.json.JSONObject

//Json
//写的越久，BUG越多，伤痕越疼，脾气越差/-活得越久 故事越多 伤痕越疼，脾气越差
/**
 * @author RW-HPS/Dr
 */
class Json {
    private val jsonObject: JSONObject

    constructor(json: String) {
        jsonObject = JSONObject(json)
    }

    constructor(json: LinkedHashMap<*, *>) {
        jsonObject = JSONObject(json)
    }

    constructor(json: JSONObject) {
        jsonObject = json
    }

    @JvmOverloads
    fun getString(str: String, defaultValue: String = ""): String {
        return jsonObject.getDataResult(defaultValue) {
            it.getString(str)
        }!!
    }
    @JvmOverloads
    fun getInt(str: String, defaultValue: Int? = null): Int? {
        return jsonObject.getDataResult(defaultValue) {
            it.getInt(str)
        }
    }
    @JvmOverloads
    fun getLong(str: String, defaultValue: Long? = null): Long? {
        return jsonObject.getDataResult(defaultValue) {
            it.getLong(str)
        }
    }
    @JvmOverloads
    fun getBoolean(str: String, defaultValue: Boolean? = null): Boolean? {
        return jsonObject.getDataResult(defaultValue) {
            it.getBoolean(str)
        }
    }


    fun getInnerMap(): Map<String, Any> {
        return jsonObject.toMap()
    }

    fun getArrayData(str: String, key: String): JsonArray {
        val rArray = JsonArray(jsonObject.getDataResultObject({ JSONArray() }) {
            it.getJSONArray(str)
        })
        return rArray
    }

    fun getArraySeqData(str: String): Seq<Json> {
        val result = Seq<Json>()
        val rArray = (jsonObject as LinkedHashMap<*, *>)[str].ifNullResult({ it as ArrayList<*> }) { arrayListOf<Any>() }
        for (o in rArray) {
            val r = o as LinkedHashMap<*, *>
            if (IsUtil.notIsBlank(r)) {
                result.add(Json(r))
            }
        }
        return result
    }

    fun getArraySeqData(): Seq<Json> {
        val result = Seq<Json>()
        val rArray = (jsonObject as LinkedHashMap<*, *>)["result"].ifNullResult({ it as ArrayList<*> }) { arrayListOf<Any>() }
        for (o in rArray) {
            val r = o as LinkedHashMap<*, *>
            if (IsUtil.notIsBlank(r)) {
                result.add(Json(r))
            }
        }
        return result
    }

    companion object {
        /**
         * Map to JSON formatted
         * @param map Map<String, Any>
         * @return Json
         */
        @JvmStatic
        fun toJson(map: Map<String, Any>): String {
            return map.toPrettyPrintingJson()
        }

        /**
         * Map to JSON formatted
         * @param map Map<String, Any>
         * @return Json
         */
        @JvmStatic
        fun toJson(map: ObjectMap<String, Any>): String {
            return HashMap<String,Any>().also { map.each { key, value -> it[key] = value } }.toPrettyPrintingJson()
        }

        @JvmStatic
        fun <T> stringToClass(fileUtil: FileUtil,clazz: Class<T>): Any {
            val gson = Gson()
            val json = fileUtil.readFileStringData()
            return gson.fromJson(if (IsUtil.notIsBlank(json)) json else "{}", clazz.javaClass)
        }

    }
}