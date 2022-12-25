/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.dependent.redirections.slick

import net.rwhps.asm.api.Redirection
import net.rwhps.server.data.global.Data
import net.rwhps.server.dependent.redirections.lwjgl.LwjglProperties
import net.rwhps.server.util.ReflectionUtils
import org.newdawn.slick.AppGameContainer
import java.lang.reflect.Method

/**
 * Since Rusted Warfare [org.newdawn.slick.AppGameContainer.start] is just a while(True) loop which calls [org.newdawn.slick.AppGameContainer.gameLoop] and [Thread.yield]. Once we
 * heavy load on the CPU. This [Redirection] fixes that by sleeping for a
 * set amount of time, configurable by the SystemProperty [LwjglProperties.DISPLAY_UPDATE].
 */
class AppGameContainerUpdate @JvmOverloads constructor(private val time: Long = getTime()) : Redirection {
    var methodSetup: Method? = null
    var methodGetDelta: Method? = null
    var methodGameLoop: Method? = null

    @Throws(Throwable::class)
    override fun invoke(obj: Any, desc: String, type: Class<*>?, vararg args: Any) {
        val appGameContainer = obj as AppGameContainer
        try {
            if (methodSetup == null) {
                methodSetup = ReflectionUtils.findMethod(AppGameContainer::class.java,"setup").also { ReflectionUtils.makeAccessible(it) }
            }
            if (methodGetDelta == null) {
                methodGetDelta = ReflectionUtils.findMethod(AppGameContainer::class.java,"getDelta").also { ReflectionUtils.makeAccessible(it) }
            }
            if (methodGameLoop == null) {
                methodGameLoop = ReflectionUtils.findMethod(AppGameContainer::class.java,"gameLoop").also { ReflectionUtils.makeAccessible(it) }
            }

            methodSetup?.invoke(appGameContainer)
            methodGetDelta?.invoke(appGameContainer)

            if (updateGameFPS == null) {
                updateGameFPS = { methodGameLoop?.invoke(appGameContainer) }
            }

            while (!Data.exitFlag) {
                Thread.sleep(time)
                updateGameFPS?.run { this() }
            }
        } finally {
            appGameContainer.destroy()
        }
        // There is no direct exit here because it is called by Core.exit
    }

    companion object {
        const val DESC = "Lorg/newdawn/slick/AppGameContainer;start()V"

        internal var updateGameFPS: (()->Unit)? = null

        private fun getTime(): Long {
            return try {
                // 经过测试 最佳的 sleep 是比 游戏Tick 稍快
                // 但是存在问题 例如玩家刚进入 就开始游戏 那么数据将来不及更新导致错误
                System.getProperty(LwjglProperties.AppGameContainer_UPDATE, "8").toLong()
            } catch (nfe: NumberFormatException) {
                8L
            }
        }
    }
}