/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.core

import net.rwhps.server.core.thread.Threads.close
import net.rwhps.server.data.global.Data
import kotlin.system.exitProcess

/**
 * @author RW-HPS/Dr
 */
object Core {
    /**
     * Exit and save data
     */
    @JvmStatic
    fun exit() {
        NetServer.closeServer()
        Data.core.save()
        close()
        exitProcess(0)
    }

    /**
     * Force quit
     */
    @JvmStatic
    fun mandatoryExit() {
        exitProcess(0)
    }
}