package net.rwhps.server.game

import net.rwhps.server.data.global.Data
import net.rwhps.server.dependent.LibraryManager
import net.rwhps.server.game.simulation.gameFramework.GameData
import net.rwhps.server.net.HttpRequestOkHttp
import net.rwhps.server.util.compression.CompressionDecoderUtils
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.log.Log

/**
 * 游戏 资源文件 初始化
 *
 * @author RW-HPS/Dr
 */
object GameStartInit {
    /** Res的MD5 */
    private val resMd5 = "6d61d95d9fd7ef679d0013efad1466de"
    /** Assets的MD5 */
    private val assetsMd5 = "b594e7e8d2a0ad925c8ac0e00edbdbad"

    fun init(): Boolean {
        try {
            // 清除无用缓存
            GameData.cleanCache()

            // 加载游戏依赖
            val gameCoreLibs = FileUtil.getFolder(Data.Plugin_GameCore_Lib_Path,true)
            CompressionDecoderUtils.zipStream(GameStartInit::class.java.getResourceAsStream("/libs.zip")!!).use {
                it.getSpecifiedSuffixInThePackage("jar",true).each { k, v ->
                    gameCoreLibs.toFile(k).run {
                        if (!exists()) {
                            writeFileByte(v,true)
                        }
                    }
                }
            }

            // 加载jar
            val libMg = LibraryManager()
            gameCoreLibs.fileList.eachAll {
                libMg.customImportLib(it)
            }
            libMg.loadToClassLoader()


            val resFile = FileUtil.getFolder(Data.Plugin_GameCore_Data_Path).toFile("Game-Res.zip")
            val assetsFile = FileUtil.getFolder(Data.Plugin_GameCore_Data_Path).toFile("Game-Assets.zip")
            val temp = FileUtil.getFolder(Data.Plugin_GameCore_Data_Path,true)

            /* 鉴别两个文件的MD5, 不相同则重下 */
            if (resFile.md5 != resMd5) {
                resFile.file.delete()
            }
            if (assetsFile.md5 != assetsMd5) {
                assetsFile.file.delete()
            }

            val resTask: (FileUtil,Boolean)->Unit = { file, res ->
                (if (res) "res" else "assets").let { resName ->
                    if (!file.exists()) {
                        temp.toFolder(resName).file.delete()

                        HttpRequestOkHttp.downUrl(Data.urlData.readString("Get.Res")+file.name,file.file).also {
                            Log.clog("$resName : {0}",it)
                        }
                    }
                    if (!temp.toFolder(resName).toFile("Check").exists()) {
                        CompressionDecoderUtils.zip(file.file).use {
                            it.getZipAllBytes().each { filePath, bytes ->
                                temp.toFile(filePath).writeFileByte(bytes)
                            }
                        }
                        temp.toFolder(resName).toFile("Check").mkdir()
                    }
                }
            }

            resTask(resFile,true)
            resTask(assetsFile,false)
        } catch (e: Exception) {
            Log.fatal(e)
            return false
        }
        return true
    }
}