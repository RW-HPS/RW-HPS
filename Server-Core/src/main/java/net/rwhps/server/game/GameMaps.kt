/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.game

import net.rwhps.server.data.global.Data
import net.rwhps.server.util.file.FileUtil.Companion.getFolder
import net.rwhps.server.util.zip.zip.ZipDecoder
import net.rwhps.server.util.log.Log.error
/**
 * 游戏的地图数据
 * @author RW-HPS/Dr
 */
class GameMaps {
    /** 地图类型  */
    @JvmField
    var mapType = MapType.defaultMap

    /** 地图数据  */
    @JvmField
    var mapData: MapData? = null

    /** 地图名  */
    @JvmField
    var mapName = "Crossing Large (10p)"

    /** 地图人数  */
    @JvmField
    var mapPlayer = "[z;p10]"

    /** 地图的类型  */
    enum class MapType {
        /**
         * defaultMap : 官方地图
         * customMap  : 自定义的地图
         * savedGames : 游戏里保存的进度
         */
        defaultMap, customMap, savedGames
    }

    enum class MapFileType {
        /**
         * file : 以文件的形式提供
         * zip  : 以ZIP提供(文件在ZIP内)
         * web  : 以WebFile的形式提供(需要下载)
         */
        file, zip, web
    }

    class MapData {
        @JvmField
        val mapType: MapType
        @JvmField
        val mapFileType: MapFileType
        @JvmField
        val mapFileName: String
        @JvmField
        val zipFileName: String?
        @JvmField
        var mapSize = 0
        @JvmField
        var bytesMap: ByteArray? = null

        constructor(mapType: MapType, mapFileType: MapFileType, mapFileName: String) {
            this.mapType = mapType
            this.mapFileType = mapFileType
            this.mapFileName = mapFileName
            zipFileName = null
        }

        constructor(mapType: MapType, mapFileType: MapFileType, mapFileName: String, zipFileName: String) {
            this.mapType = mapType
            this.mapFileType = mapFileType
            this.mapFileName = mapFileName
            this.zipFileName = zipFileName
        }

        /**
         * 根据地图类型获取文件后缀
         * @return 后缀
         */
        val type: String
            get() = if ("savedGames" == mapType.name) ".save" else ".tmx"

        /**
         * 懒加载
         * 读取地图到内部bytes
         */
        fun readMap() {
            val fileUtil = getFolder(Data.Plugin_Maps_Path)
            when (mapFileType) {
                MapFileType.file -> try {
                    bytesMap = fileUtil.toFile(mapFileName + type).readFileByte()
                    mapSize = bytesMap!!.size
                } catch (e: Exception) {
                    error("Read Map Bytes Error", e)
                }
                MapFileType.zip -> try {
                    bytesMap =
                        ZipDecoder(fileUtil.toFile(zipFileName!!).file).getTheFileBytesOfTheSpecifiedSuffixInTheZip(this)
                    mapSize = bytesMap!!.size
                } catch (e: Exception) {
                    error("Read Map Bytes Error", e)
                }
                MapFileType.web -> {
                }
            }
        }

        /**
         * 清理服务器使用的地图数据
         */
        fun clean() {
            mapSize = 0
            bytesMap = null
        }
    }
}
