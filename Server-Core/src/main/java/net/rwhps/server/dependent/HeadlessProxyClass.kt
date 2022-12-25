/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.dependent

import com.corrodinggames.librocket.a
import com.corrodinggames.librocket.scripts.Root
import net.rwhps.asm.agent.AsmAgent
import net.rwhps.server.data.global.Data
import net.rwhps.server.dependent.redirections.FileLoaderRedirections
import net.rwhps.server.dependent.redirections.lwjgl.LwjglRedirections
import net.rwhps.server.dependent.redirections.slick.SlickRedirections
import net.rwhps.server.game.event.EventGlobalType
import net.rwhps.server.util.ReflectionUtils
import net.rwhps.server.util.game.Events
import net.rwhps.server.util.log.Log

class HeadlessProxyClass : AgentAttachData() {
    init {
        initProxyClass()
    }

    private fun initProxyClass() {
        /* 注册无头Lwjgl */
        LwjglRedirections().register()
        /* 注册无头Slick */
        SlickRedirections().register()
        /* 注册无头FileSystem */
        FileLoaderRedirections().register()

        // 直接空实现
        AsmAgent.allMethod.add("org/newdawn/slick/util/DefaultLogSystem")
        AsmAgent.allMethod.add("com/LibRocket")
        AsmAgent.allMethod.add("com/corrodinggames/librocket/scripts/ScriptEngine")
        // 这两个 因为 [LibRocket] 是空 所以被调用时 会 NPE
        AsmAgent.addPartialMethod("com/corrodinggames/librocket/b" , arrayOf("closeDocument","(Lcom/ElementDocument;)V"))
        AsmAgent.addPartialMethod("com/corrodinggames/librocket/b" , arrayOf("closeActiveDocument","()V"))

        /* 恢复-Root */
        AsmAgent.addPartialMethod("com/corrodinggames/librocket/scripts/Root" , arrayOf("resume","()V")) { obj: Any?, _: String?, _: Class<*>?, _: Array<Any?>? ->
            ReflectionUtils.findField(Root::class.java,"guiEngine")!!.also {
                ReflectionUtils.makeAccessible(it)
                it.get(obj)?.let { (it as a).f() }
            }
        }
        /* 恢复-Root */
        AsmAgent.addPartialMethod("com/corrodinggames/librocket/scripts/Root" , arrayOf("resumeNonMenu","()V")) { obj: Any?, _: String?, _: Class<*>?, _: Array<Any?>? ->
            ReflectionUtils.findField(Root::class.java,"guiEngine")!!.also {
                ReflectionUtils.makeAccessible(it)
                it.get(obj)?.let { (it as a).a(false) }
            }
        }


        /* 取代游戏自己打印的 */
        AsmAgent.addPartialMethod("android/util/Log", arrayOf("a","(ILjava/lang/String;Ljava/lang/String;)I")) { _: Any?, _: String?, _: Class<*>?, args: Array<Any?>? ->
            args?.let {
                args[2]?.let {
                    if (Data.config.Log == "ALL") {
                        System.out.println(it.toString())
                    }
                    if (it.toString().contains("----- Game init finished in")) {
                        loadFlagGame = GameInitStatus.LoadEndMods
                    }
                    if (it.toString().contains("Saving settings") && loadFlagGame == GameInitStatus.LoadEndMods) {
                        loadFlagGame = GameInitStatus.LoadEndGames
                        Events.fire(EventGlobalType.GameLibLoadEvent())
                    }
                    if (it.toString().startsWith("Replay: Recording replay to:")) {
                        Log.clog("Save Replay to: {0}",it.toString().replace("Replay: Recording replay to:",""))
                    }
                }
            }
            0
        }


        AsmAgent.agentmain(this.instrumentation)
    }

    /**
     * 游戏无头加载判断
     */
    private enum class GameInitStatus {
        NoLoad,
        LoadEndMods,
        LoadEndGames
    }

    companion object {
        private var loadFlagGame = GameInitStatus.NoLoad
    }
}