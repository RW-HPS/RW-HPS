/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.log

import net.rwhps.server.data.global.Data
import net.rwhps.server.util.alone.annotations.NeedToRefactor
import net.rwhps.server.util.file.FileUtil

/**
 * 保存 Log 部分
 * 只保存十次, 在每次启动时保存
 */
class SaveLog {
    private val archiveLog = FileUtil.getFolder(Data.Plugin_Log_Path).toFile("archive-10.zip")
    private var count = 0

    init {
        if (archiveLog.exists()) {
            archiveLog.zipDecoder.apply {
                count = getZipAllBytes().size
                close()
            }
        }
    }

    /**
     * 设计目标
     *   自动向前走, 只保留十个,取代旧的
     */
    @NeedToRefactor
    fun save() {
        if (archiveLog.exists()) {
            if (count > 10) {

            }
        }
    }
}