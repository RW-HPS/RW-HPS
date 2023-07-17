/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.command

import net.rwhps.server.command.relay.RelayCommands
import net.rwhps.server.core.Core
import net.rwhps.server.core.Initialization
import net.rwhps.server.core.NetServer
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.HessModuleManage
import net.rwhps.server.data.ModManage
import net.rwhps.server.data.bean.BeanCoreConfig
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.plugin.PluginManage
import net.rwhps.server.dependent.HotLoadClass
import net.rwhps.server.func.StrCons
import net.rwhps.server.game.event.global.ServerStartTypeEvent
import net.rwhps.server.net.NetService
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.net.handler.tcp.StartHttp
import net.rwhps.server.plugin.PluginLoadData
import net.rwhps.server.plugin.center.PluginCenter
import net.rwhps.server.util.SystemUtils
import net.rwhps.server.util.annotations.NeedHelp
import net.rwhps.server.util.file.FileUtils
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.log.Log
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author RW-HPS/Dr
 */
class CoreCommands(handler: CommandHandler) {
    private fun registerCore(handler: CommandHandler) {
        handler.register("help", "serverCommands.help") { _: Array<String>?, log: StrCons ->
            log["Commands:"]
            for (command in handler.commandList) {
                if (command.description.startsWith("#")) {
                    log["   " + command.text + (if (command.paramText.isEmpty()) "" else " ") + command.paramText + " - " + command.description.substring(1)]
                } else {
                    if ("HIDE" == command.description) {
                        continue
                    }
                    log["   " + command.text + (if (command.paramText.isEmpty()) "" else " ") + command.paramText + " - " + Data.i18NBundle.getinput(command.description)]
                }
            }
        }

        @NeedHelp
        handler.register("stop", "HIDE") { _: Array<String>?, log: StrCons ->
            if (NetStaticData.netService.size == 0) {
                log["Server does not start"]
                return@register
            }
            NetServer.closeServer()
        }

        handler.register("version", "serverCommands.version") { _: Array<String>?, log: StrCons ->
            log[localeUtil.getinput("status.versionS", SystemUtils.javaHeap / 1024 / 1024, Data.SERVER_CORE_VERSION, NetStaticData.ServerNetType.name)]
            if (NetStaticData.ServerNetType.ordinal in IRwHps.NetType.ServerProtocol.ordinal..IRwHps.NetType.ServerTestProtocol.ordinal) {
                HessModuleManage.hessLoaderMap.values.forEach {
                    log[localeUtil.getinput("status.versionS.server",
                        it.room.mapName,
                        it.room.playerManage.playerAll.size,
                        NetStaticData.ServerNetType.name,
                        it.useClassLoader,
                        it.room.roomID)]
                }
            } else if (NetStaticData.ServerNetType == IRwHps.NetType.RelayProtocol || NetStaticData.ServerNetType == IRwHps.NetType.RelayMulticastProtocol) {
                val size = AtomicInteger()
                NetStaticData.netService.eachAll { e: NetService -> size.addAndGet(e.getConnectSize()) }
                log[localeUtil.getinput("status.versionS.relay", size.get())]
            }
        }
        handler.register("setlanguage","[HK/CN/RU/EN]" ,"serverCommands.setlanguage") { arg: Array<String>, _: StrCons ->
            Initialization.initServerLanguage(Data.core.settings,arg[0])
        }

        handler.register("reloadconfig", "serverCommands.reloadconfig") { Data.config = BeanCoreConfig.stringToClass() }
        handler.register("exit", "serverCommands.exit") { Core.exit() }
    }

    private fun registerInfo(handler: CommandHandler) {
        handler.register("plugins", "serverCommands.plugins") { _: Array<String>?, log: StrCons ->
            PluginManage.run { e: PluginLoadData? ->
                log[localeUtil.getinput("plugin.info", e!!.name, e.description, e.author, e.version)]
            }
        }
        handler.register("mods", "serverCommands.mods") { _: Array<String>?, log: StrCons ->
            for ((index, name) in ModManage.getModsList().withIndex()){
                log[localeUtil.getinput("mod.info", index,name)]
            }
        }
        handler.register("maps", "serverCommands.maps") { _: Array<String>?, log: StrCons ->
            val response = StringBuilder()
            val i = AtomicInteger(0)
            Data.game.mapsData.keys.forEach { k: String? ->
                response.append(localeUtil.getinput("maps.info", i.get(), k)).append(Data.LINE_SEPARATOR)
                i.getAndIncrement()
            }
            log[response.toString()]
        }
    }

    private fun registerCorex(handler: CommandHandler) {
        handler.register("plugin", "<TEXT...>", "serverCommands.plugin") { arg: Array<String>, log: StrCons ->
            PluginCenter.pluginCenter.command(arg[0], log)
        }
    }

    private fun registerStartServer(handler: CommandHandler) {
        handler.register("startrelay", "serverCommands.start") { _: Array<String>?, log: StrCons ->
            if (NetStaticData.netService.size > 0) {
                log["The server is not closed, please close"]
                return@register
            }

            /* Register Relay Protocol Command */
            RelayCommands(handler)

            Log.set(Data.config.Log.uppercase(Locale.getDefault()))
            //Data.game = Rules(Data.config)
            Data.game.init()

            NetStaticData.ServerNetType = IRwHps.NetType.RelayProtocol

            handler.handleMessage("startnetservice true 5201 5500") //5200 6500
        }
        handler.register("startrelaytest", "serverCommands.start") { _: Array<String>?, log: StrCons ->
            if (NetStaticData.netService.size > 0) {
                log["The server is not closed, please close"]
                return@register
            }

            /* Register Relay Protocol Command */
            RelayCommands(handler)

            Log.set(Data.config.Log.uppercase(Locale.getDefault()))
            //Data.game = Rules(Data.config)
            Data.game.init()

            NetStaticData.ServerNetType = IRwHps.NetType.RelayMulticastProtocol

            handler.handleMessage("startnetservice true 5200 5500")
        }

        handler.register("startnetservice", "<isPort> [sPort] [ePort]","HIDE") { arg: Array<String>?, _: StrCons? ->
            if (arg != null) {
                if (arg[0].toBoolean()) {
                    val netServiceTcp = NetService()
                    Threads.newThreadCoreNet {
                        if (arg.size > 2) {
                            netServiceTcp.openPort(Data.config.Port, arg[1].toInt(), arg[2].toInt())
                        } else {
                            netServiceTcp.openPort(Data.config.Port)
                        }
                    }
                }

                Threads.newThreadCoreNet {
                    if (Data.config.WebService) {
                        val netServiceTcp1 = NetService(StartHttp::class.java)
                        netServiceTcp1.openPort(Data.config.SeparateWebPort)
                    }
                }
                PluginManage.runGlobalEventManage(ServerStartTypeEvent(NetStaticData.ServerNetType))
            } else {
                Log.clog("[Start Service] No parameter")
            }
        }
    }

    companion object {
        private val localeUtil = Data.i18NBundle
    }

    init {
        registerCore(handler)
        registerCorex(handler)
        registerInfo(handler)
        registerStartServer(handler)


        // Test (孵化器）
        handler.register("log", "[a...]", "serverCommands.exit") { _: Array<String>, _: StrCons ->
            HotLoadClass().load(FileUtils.getFile("a.class").readFileByte())
        }
    }
}