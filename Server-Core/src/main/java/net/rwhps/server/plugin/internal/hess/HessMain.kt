/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.plugin.internal.hess

import net.rwhps.server.data.EventGlobalManage
import net.rwhps.server.data.HessModuleManage
import net.rwhps.server.data.global.Data
import net.rwhps.server.func.StrCons
import net.rwhps.server.plugin.Plugin
import net.rwhps.server.plugin.internal.hess.service.event.GameHeadlessEventGlobal
import net.rwhps.server.util.classload.GameModularReusableLoadClass
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.game.GameStartInit
import net.rwhps.server.util.log.Log
import java.util.*

/**
 * @author RW-HPS/Dr
 */
class HessMain : Plugin() {
    override fun registerGlobalEvents(eventManage: EventGlobalManage) = eventManage.registerListener(GameHeadlessEventGlobal())

    override fun registerCoreCommands(handler: CommandHandler) {
        handler.register("start", "serverCommands.start") { _: Array<String>?, log: StrCons ->
            if (Data.startServer) {
                log["The server is not closed, please close"]
                return@register
            }
            Data.startServer = true

            // Start Hess Core
            val load = GameModularReusableLoadClass(
                Thread.currentThread().contextClassLoader,
                Thread.currentThread().contextClassLoader.parent
            )
            GameStartInit.init(load)
            Log.clog(Data.i18NBundle.getinput("server.load.headless"))
            // 设置 RW-HPS 主要使用的 Hess
            HessModuleManage.hpsLoader = load.toString()
            GameStartInit.start(load)

            Log.set(Data.config.Log.uppercase(Locale.getDefault()))
        }
    }

    override fun registerServerCommands(handler: CommandHandler) {
        serverServerCommands = handler

    }

    override fun registerServerClientCommands(handler: CommandHandler) {
        serverClientCommands = handler
    }



    companion object {
        internal lateinit var serverServerCommands: CommandHandler
        internal lateinit var serverClientCommands: CommandHandler
    }
}