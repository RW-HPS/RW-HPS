/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.plugin

import net.rwhps.server.data.plugin.PluginEventManage.Companion.add
import net.rwhps.server.func.Cons
import net.rwhps.server.plugin.Plugin
import net.rwhps.server.plugin.PluginsLoad.Companion.addPluginClass
import net.rwhps.server.plugin.PluginsLoad.Companion.resultPluginData
import net.rwhps.server.plugin.PluginsLoad.PluginLoadData
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.alone.annotations.DidNotFinish
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.log.Log.error
import java.io.IOException

/**
 * Plugin Manager
 * @author RW-HPS/Dr
 */
object PluginManage {
    private val pluginEventManage = PluginEventManage()
    private var pluginData: Seq<PluginLoadData>? = null
    val loadSize: Int
        get() = pluginData!!.size

    fun run(cons: Cons<PluginLoadData?>) {
        pluginData!!.eachAll { t: PluginLoadData? -> cons[t] }
    }

    fun init(fileUtil: FileUtil) {
        pluginData = resultPluginData(fileUtil)
    }

    fun addPluginClass(name: String,author: String,description: String, version: String, main: Plugin,mkdir: Boolean , skip: Boolean = false) {
        addPluginClass(name,author,description,version,main,mkdir,skip,pluginData!!)
    }

    /** 最先执行 可以进行Plugin的数据读取  -1  */
    fun runOnEnable() {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.onEnable() }
    }

    /** 注册要在服务器端使用的任何命令，例如从控制台 */
    fun runRegisterCoreCommands(handler: CommandHandler) {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerCoreCommands(handler) }
    }
    /** 注册要在服务器端使用的任何命令，例如从控制台-Server */
    fun runRegisterServerCommands(handler: CommandHandler) {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerServerCommands(handler) }
    }
    /** 注册要在服务器端使用的任何命令，例如从控制台-Relay */
    fun runRegisterRelayCommands(handler: CommandHandler) {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerRelayCommands(handler) }
    }

    /** 注册要在客户端使用的任何命令，例如来自游戏内玩家 */
    fun runRegisterServerClientCommands(handler: CommandHandler) {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerServerClientCommands(handler) }
    }
    /** 注册要在客户端使用的任何命令，例如来自RELAY内玩家 */
    fun runRegisterRelayClientCommands(handler: CommandHandler) {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerRelayClientCommands(handler) }
    }

    /** 注册事件 -4  */
    fun runRegisterEvents() {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerEvents()?.let { add(it)} }
    }
    /** 注册事件 -4  */
    fun runRegisterGlobalEvents() {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.registerGlobalEvents()?.let { add(it)} }
    }

    /** 创建所有插件并注册命令后调用 -5  */
    fun runInit() {
        pluginData!!.eachAll { e: PluginLoadData -> e.main.init() }
    }

    /** Server退出时执行 可以进行Plugin的数据保存  -6  */
    fun runOnDisable() {
        pluginData!!.eachAll { e: PluginLoadData ->
            //e.main.pluginData.save()
            e.main.onDisable()
        }
    }

    @DidNotFinish
    fun removePlugin(name: String) {
        pluginData!!.eachFind({ e: PluginLoadData -> e.name.equals(name, ignoreCase = true) }) { p: PluginLoadData ->
            pluginData!!.remove(p)
            //p.main.onUnLoad();
            try {
                //p.main.classLoader!!.close()
            } catch (e: IOException) {
                error("卸载失败 : " + p.name)
            }
        }
        //TODO 完全卸载Plugin
    }
}