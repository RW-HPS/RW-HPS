/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.dependent

import net.rwhps.server.util.log.exp.VariableException
import java.lang.instrument.Instrumentation

/**
 * Agent Core
 * @author RW-HPS/Dr
 */
open class AgentAttachData {
    protected val instrumentation: Instrumentation = instPrivate!!


    companion object {
        private var instPrivate: Instrumentation? = null
            get() {
                /* 避免Java没有初始化 Agent */
                if (field == null) {
                    throw VariableException("[Angent Init Error] Please check the JDK version, Use Java11 or Java11+")
                }
                return field
            }

        /** JRE将在启动main()之前调用方法  */
        @JvmStatic
        fun agentmain(name: String?, inst: Instrumentation?) {
            instPrivate = inst
        }
    }
}