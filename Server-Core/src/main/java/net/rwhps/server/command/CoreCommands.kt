/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.command

import net.rwhps.server.command.relay.RelayCommands
import net.rwhps.server.command.server.ServerCommands
import net.rwhps.server.core.*
import net.rwhps.server.core.ServiceLoader
import net.rwhps.server.core.thread.CallTimeTask
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.base.BaseConfig
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.mods.ModManage
import net.rwhps.server.data.plugin.PluginManage
import net.rwhps.server.dependent.HotLoadClass
import net.rwhps.server.func.StrCons
import net.rwhps.server.game.Rules
import net.rwhps.server.game.event.EventGlobalType
import net.rwhps.server.net.StartNet
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.net.handler.tcp.StartHttp
import net.rwhps.server.plugin.PluginsLoad
import net.rwhps.server.plugin.center.PluginCenter
import net.rwhps.server.util.alone.annotations.NeedHelp
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.game.Events
import net.rwhps.server.util.log.Log
import java.util.*
import java.util.concurrent.TimeUnit
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
            if (NetStaticData.startNet.size == 0) {
                log["Server does not start"]
                return@register
            }
            NetServer.closeServer()
        }

        handler.register("version", "serverCommands.version") { _: Array<String>?, log: StrCons ->
            log[localeUtil.getinput("status.versionS", Data.core.javaHeap / 1024 / 1024, Data.SERVER_CORE_VERSION, NetStaticData.ServerNetType.name)]
            if (NetStaticData.ServerNetType.ordinal in IRwHps.NetType.ServerProtocol.ordinal..IRwHps.NetType.ServerTestProtocol.ordinal) {
                log[localeUtil.getinput("status.versionS.server", Data.game.maps.mapName, Data.game.playerManage.playerAll.size, NetStaticData.RwHps.typeConnect.version, NetStaticData.RwHps.typeConnect.abstractNetConnect.version)]
            } else if (NetStaticData.ServerNetType == IRwHps.NetType.RelayProtocol || NetStaticData.ServerNetType == IRwHps.NetType.RelayMulticastProtocol) {
                val size = AtomicInteger()
                NetStaticData.startNet.eachAll { e: StartNet -> size.addAndGet(e.getConnectSize()) }
                log[localeUtil.getinput("status.versionS.relay", size.get())]
            }
        }
        handler.register("setlanguage","[HK/CN/RU/EN]" ,"serverCommands.setlanguage") { arg: Array<String>, _: StrCons ->
            Initialization.initServerLanguage(Data.core.settings,arg[0])
        }

        handler.register("reloadconfig", "serverCommands.reloadconfig") { Data.config = BaseConfig.stringToClass() }
        handler.register("exit", "serverCommands.exit") { Core.exit() }
    }

    private fun registerInfo(handler: CommandHandler) {
        handler.register("plugins", "serverCommands.plugins") { _: Array<String>?, log: StrCons ->
            PluginManage.run { e: PluginsLoad.PluginLoadData? ->
                log[localeUtil.getinput("plugin.info", e!!.name, e.description, e.author, e.version)]
            }
        }
        handler.register("mods", "serverCommands.mods") { arg: Array<String>?, log: StrCons ->
            for ((index, name) in ModManage.getModsList().withIndex()){
                log[localeUtil.getinput("mod.info", index,name)]
            }
        }
        handler.register("maps", "serverCommands.maps") { _: Array<String>?, log: StrCons ->
            val response = StringBuilder()
            val i = AtomicInteger(0)
            Data.game.mapsData.keys().forEach { k: String? ->
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
        handler.register("start", "serverCommands.start") { _: Array<String>?, log: StrCons -> startServer(handler,IRwHps.NetType.ServerProtocol,log)}
        handler.register("starttest", "serverCommands.start") { _: Array<String>?, log: StrCons -> startServer(handler,IRwHps.NetType.ServerTestProtocol,log)}


        handler.register("startrelay", "serverCommands.start") { _: Array<String>?, log: StrCons ->
            if (NetStaticData.startNet.size > 0) {
                log["The server is not closed, please close"]
                return@register
            }

            /* Register Relay Protocol Command */
            RelayCommands(handler)

            Log.set(Data.config.Log.uppercase(Locale.getDefault()))
            Data.game = Rules(Data.config)
            Data.game.init()

            NetStaticData.ServerNetType = IRwHps.NetType.RelayProtocol
            NetStaticData.RwHps =
                ServiceLoader.getService(ServiceLoader.ServiceType.IRwHps,"IRwHps", IRwHps.NetType::class.java)
                    .newInstance(IRwHps.NetType.RelayProtocol) as IRwHps

            handler.handleMessage("startnetservice") //5200 6500
        }
        handler.register("startrelaytest", "serverCommands.start") { _: Array<String>?, log: StrCons ->
            if (NetStaticData.startNet.size > 0) {
                log["The server is not closed, please close"]
                return@register
            }

            /* Register Relay Protocol Command */
            RelayCommands(handler)

            Log.set(Data.config.Log.uppercase(Locale.getDefault()))
            Data.game = Rules(Data.config)
            Data.game.init()

            NetStaticData.ServerNetType = IRwHps.NetType.RelayMulticastProtocol
            NetStaticData.RwHps =
                ServiceLoader.getService(ServiceLoader.ServiceType.IRwHps,"IRwHps", IRwHps.NetType::class.java)
                    .newInstance(IRwHps.NetType.RelayMulticastProtocol) as IRwHps

            handler.handleMessage("startnetservice 5200 5500")
        }


        handler.register("startnetservice", "[sPort] [ePort]","HIDE") { arg: Array<String>?, _: StrCons? ->
            val startNetTcp = StartNet()
            NetStaticData.startNet.add(startNetTcp)
            Threads.newThreadCoreNet {
                if (arg != null && arg.size > 1) {
                    startNetTcp.openPort(Data.config.Port, arg[0].toInt(), arg[1].toInt())
                } else {
                    startNetTcp.openPort(Data.config.Port)
                }
            }

            Threads.newThreadCoreNet {
                if (Data.config.WebService) {
                    val startNetTcp1 = StartNet(StartHttp::class.java)
                    NetStaticData.startNet.add(startNetTcp1)
                    startNetTcp1.openPort(Data.config.SeparateWebPort)
                }
            }
            Events.fire(EventGlobalType.ServerStartTypeEvent(NetStaticData.ServerNetType))
        }
    }

    /**
     * 根据提供的协议来完成对应的初始化与设定
     *
     *
     * @param handler CommandHandler 命令头
     * @param netType NetType        协议
     * @param log StrCons            Log打印
     */
    private fun startServer(handler: CommandHandler ,netType: IRwHps.NetType, log: StrCons) {
        if (NetStaticData.startNet.size > 0) {
            log["The server is not closed, please close"]
            return
        }

        /* Register Server Protocol Command */
        ServerCommands(handler)

        Log.set(Data.config.Log.uppercase(Locale.getDefault()))
        Data.game = Rules(Data.config)
        Data.game.init()

        NetStaticData.ServerNetType = netType

        Threads.newTimedTask(CallTimeTask.CallTeamTask,0,2,TimeUnit.SECONDS,Call::sendTeamData)
        Threads.newTimedTask(CallTimeTask.CallPingTask,0,2,TimeUnit.SECONDS,Call::sendPlayerPing)

        handler.handleMessage("startnetservice")
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
            HotLoadClass().load(FileUtil.getFile("a.class").readFileByte())
        }
        handler.register("logg", "serverCommands.exit") { _: Array<String>, _: StrCons ->

        }
        handler.register("kc", "<1>", "serverCommands.exit") { arg: Array<String>, _: StrCons ->
            val site = arg[0].toInt() - 1
            val player = Data.game.playerManage.getPlayerArray(site)
            player!!.con!!.disconnect()
        }
    }
}
