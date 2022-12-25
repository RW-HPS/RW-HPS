/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.plugin

import net.rwhps.server.data.global.Data
import net.rwhps.server.data.json.Json
import net.rwhps.server.dependent.LibraryManager
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.IsUtil
import net.rwhps.server.util.compression.CompressionDecoderUtils
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.Log.error
import net.rwhps.server.util.log.Log.warn
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.*

/**
 * 在这里完成 插件的加载
 * @author RW-HPS/Dr
 */
class PluginsLoad {
    private fun loadJar(jarFileList: Seq<File>): Seq<PluginLoadData> {
        val data = Seq<PluginLoadData>()
        val dataName = Seq<String>()
        val dataImport = Seq<PluginImportData>()
        for (file in jarFileList) {
            val zip = CompressionDecoderUtils.zip(file)
            try {
                val imp = zip.getZipNameInputStream("plugin.json")
                if (imp == null) {
                    error("Invalid jar file", file.name)
                    continue
                }
                // 读取 插件需要的 依赖
                // 进行注入进服务端
                val imports = zip.getZipNameInputStream("imports.json")
                if (imports != null) {
                    val importsJson = Json(FileUtil.readFileString(imports)).getArraySeqData("imports")
                    val lib = LibraryManager()
                    importsJson.eachAll {
                        lib.implementation(it.getString("group"), it.getString("module"), it.getString("version"))
                    }
                    lib.loadToClassLoader()
                }

                val json = Json(FileUtil.readFileString(imp))
                if (!GetVersion(Data.SERVER_CORE_VERSION).getIfVersion(json.getString("supportedVersions"))) {
                    warn("Plugin版本不兼容 Plugin名字为: ", json.getString("name"))
                    continue
                }
                if (IsUtil.isBlank(json.getString("import"))) {
                    val mainPlugin = loadClass(file, json.getString("main"))
                    data.add(PluginLoadData(
                        json.getString("name"),
                        json.getString("author"),
                        json.getString("description"),
                        json.getString("version"),
                        mainPlugin
                    ))
                    dataName.add(json.getString("name"))
                } else {
                    dataImport.add(PluginImportData(json, file))
                }
            } catch (e: Exception) {
                error("Failed to load", e)
            } finally {
                zip.close()
            }
        }

        // 检查是否加载了依赖插件
        var i = 0
        val count = dataImport.size
        while (i < count) {
            dataImport.eachAll { e: PluginImportData ->
                if (dataName.contains(e.pluginData.getString("import"))) {
                    try {
                        val mainPlugin = loadClass(e.file, e.pluginData.getString("main"))
                        data.add(PluginLoadData(
                            e.pluginData.getString("name"),
                            e.pluginData.getString("author"),
                            e.pluginData.getString("description"),
                            e.pluginData.getString("version"),
                            mainPlugin
                        ))
                        dataName.add(e.pluginData.getString("name"))
                        dataImport.remove(e)
                    } catch (err: Exception) {
                        error("Failed to load", e)
                    }
                }
            }
            i++
        }
        return data
    }

    /**
     * 根据包名加载实例
     * @param file Jar文件
     * @param main 主类的包名
     * @return Plugin实例
     * @throws InstantiationException 如果声明基础构造函数的类表示抽象类
     * @throws InvocationTargetException 如果基础构造函数引发异常
     * @throws ExceptionInInitializerError 如果此方法引发的初始化失败
     */
    @Throws(InstantiationException::class, InvocationTargetException::class, ExceptionInInitializerError::class)
    private fun loadClass(file: File, main: String): Plugin {
        val classLoader = URLClassLoader(arrayOf(file.toURI().toURL()), ClassLoader.getSystemClassLoader())
        Log.info(file.name)
        val classMain = classLoader.loadClass(main)
        //mainPlugin.classLoader = classLoader
        return classMain.getDeclaredConstructor().newInstance() as Plugin
    }

    /**
     * Plugin导入的数据
     * @property pluginData Json示例
     * @property file Jar的文件
     * @constructor PluginImportData
     */
    private class PluginImportData(@JvmField val pluginData: Json, @JvmField val file: File)

    class PluginLoadData(
        @JvmField val name: String,
        @JvmField val author: String,
        @JvmField val description: String,
        @JvmField val version: String,
        @JvmField val main: Plugin,
        private val mkdir: Boolean = true,
        private val skip: Boolean = false
        ) {
        init {
            if (mkdir) {
                main.pluginDataFileUtil = FileUtil.getFolder(Data.Plugin_Plugins_Path,true).toFolder(this.name)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            return if (other == null || javaClass != other.javaClass) {
                false
            } else name == (other as PluginLoadData).name
        }

        override fun hashCode(): Int {
            return Objects.hash(name)
        }
    }

    companion object {
        /**
         * 返回文件夹内指定后辍的文件并包装为PluginLoadData
         * @param f FileUtil
         * @return Seq<PluginLoadData>
         */
        @JvmStatic
        internal fun resultPluginData(f: FileUtil): Seq<PluginLoadData> {
            val jarFileList = Seq<File>()
            val list = f.fileList
            for (file in list) {
                if (file.name.endsWith("jar")) {
                    jarFileList.add(file)
                }
            }
            return PluginsLoad().loadJar(jarFileList)
        }

        @JvmStatic
        internal fun addPluginClass(name: String,author: String,description: String, version: String, main: Plugin,mkdir: Boolean,skip: Boolean,list: Seq<PluginLoadData>) {
            list.add(PluginLoadData(name, author, description, version, main, mkdir , skip))
        }
    }
}