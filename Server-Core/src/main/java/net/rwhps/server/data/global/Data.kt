/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.global

import net.rwhps.server.command.ex.Vote
import net.rwhps.server.core.Application
import net.rwhps.server.data.base.BaseConfig
import net.rwhps.server.data.base.BaseRelayPublishConfig
import net.rwhps.server.data.base.BaseTestConfig
import net.rwhps.server.game.Rules
import net.rwhps.server.io.output.CompressOutputStream
import net.rwhps.server.struct.ObjectMap
import net.rwhps.server.util.I18NBundle
import net.rwhps.server.util.alone.BadWord
import net.rwhps.server.util.file.LoadIni
import net.rwhps.server.util.game.CommandHandler
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * 别问我为什么要把@JvmField和val并排
 * 问就是好看
 * @author RW-HPS/Dr
 */
object Data {
    const val Plugin_Data_Path = "/data"
    const val Plugin_Save_Path = "/data/save"
    const val Plugin_Cache_Path = "/data/cache"
    const val Plugin_Lib_Path = "/data/libs"
    const val Plugin_GameCore_Data_Path = "/data/gameData"
    const val Plugin_GameCore_Lib_Path = "$Plugin_GameCore_Data_Path/coreLibs"
    const val Plugin_Log_Path = "/data/log"
    const val Plugin_Maps_Path = "/data/maps"
    const val Plugin_Plugins_Path = "/data/plugins"
    const val Plugin_Mods_Path = "/data/mods"
    val UTF_8: Charset = StandardCharsets.UTF_8
    /*
	 * 插件默认变量
	 */
    /** 自定义包名  */
    const val SERVER_ID = "net.rwhps.server"
    const val SERVER_ID_RELAY = "net.rwhps.server.relayCustomMode.Dr"
    const val SERVER_ID_RELAY_GET = "net.rwhps.server.relayGetUUIDHex.Dr"
    const val SERVER_CORE_VERSION = "1.0.1"
    const val TOPT_KEY = "net.rwhps.server.topt # RW-HPS Team"
    const val SERVER_RELAY_UUID = "RCN Team & Tiexiu.xyz Core Team"
    /** 单位数据缓存  */
	@JvmField val utilData = CompressOutputStream.getGzipOutputStream("customUnits", false)

    @JvmField val LINE_SEPARATOR: String = System.getProperty("line.separator")

    /** 服务端 客户端命令  */
    @JvmField val SERVER_COMMAND = CommandHandler("")
    @JvmField val CLIENT_COMMAND = CommandHandler("/")
    @JvmField val LOG_COMMAND = CommandHandler("!")
    @JvmField val RELAY_COMMAND = CommandHandler(".")

    /** Map */
	@JvmField val MapsMap = ObjectMap<String, String>()

    @JvmField val core = Application()
    @JvmField val i18NBundleMap = ObjectMap<String, I18NBundle>(8)
    @JvmField val urlData: LoadIni = LoadIni(Data::class.java.getResourceAsStream("/URL.ini")!!)
    @JvmField val banWord: BadWord = BadWord()

    lateinit var config: BaseConfig
    lateinit var configTest: BaseTestConfig
    lateinit var configRelayPublish: BaseRelayPublishConfig

    /**
     * 可控变量
     */
    lateinit var i18NBundle: I18NBundle
    lateinit var game: Rules

    @JvmField var vote: Vote? = null
    var bindForcibly = false

    @Volatile var loadGame = false
    @Volatile var exitFlag = false

    val headlessName: String = "RW-HPS Core Headless"
}