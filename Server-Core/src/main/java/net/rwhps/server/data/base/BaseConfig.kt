/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.base

import net.rwhps.server.data.global.Data
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.ReflectionUtils
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.inline.toGson
import net.rwhps.server.util.inline.toPrettyPrintingJson
import net.rwhps.server.util.log.Log.debug
import net.rwhps.server.util.log.Log.error
import net.rwhps.server.util.log.Log.warn
import java.lang.reflect.Field

/**
 * Save data for serialization and deserialization
 * @author RW-HPS/Dr
 */
data class BaseConfig(
    val DefStartCommand: String = "start",

    val Log: String = "WARN",

    val ServerName: String = "RW-HPS",
    val Subtitle: String = "",

    /** 端口 */
    val Port: Int = 5123,
    val Passwd: String = "",
    //val UDPSupport: Boolean = false,

    val EnterAd: String = "",
    val StartAd: String = "",
    val MaxPlayerAd: String = "",
    val StartPlayerAd: String = "",

    //val serverUpID: String

    /** 服务器最大人数 */
    val MaxPlayer: Int = 10,
    /** 服务器最大游戏时间 (s) 2*60*60 (-1 为禁用) */
    val MaxGameIngTime: Int = 7200,
    /** 服务器最小Start人数 (-1 为禁用) */
    val StartMinPlayerSize: Int = -1,
    /** 服务器最小AutoStart人数 (-1 为禁用) */
    val AutoStartMinPlayerSize: Int = 4,
    /** 最大发言长度 */
    val MaxMessageLen: Int = 40,
    /** 最大单位数 */
    val MaxUnit: Int = 200,
    val Tick: Int = 10,
    val TickTime: Int = 150,

    val DefIncome: Float = 1f,
    /** only Admin */
    val OneAdmin: Boolean = true,
    /** ip多语言支持 */
    val IpCheckMultiLanguageSupport: Boolean = false,

    /** 是否启用重连 */
    val ReConnect: Boolean = true,
    /** 是否启用胜负判定 */
    val WinOrLose: Boolean = false,
    /** 胜负判定时间 */
    val WinOrLoseTime: Int = 30000,

    val DeleteLib: Boolean = false,

    /** Single user relay disable pop-up selection */
    val SingleUserRelay: Boolean = false,
    /** Default mods configuration for single user relay */
    val SingleUserRelayMod: Boolean = false,

    /** Whether to start reading mod for the first time */
    val GameOverUpList: Boolean = false,

    /** GamePort HttpPort 共用 */
    val WebGameBypassPort: Boolean = false,

    /** 单独起 HTTP 服务 */
    val WebService: Boolean = false,
    /** 单独的 Port */
    val SeparateWebPort: Int = 5124,
    val SSL: Boolean = false,
    val SSLPasswd: String = "RW-HPS",

    val AutoReLoadMap: Boolean = false,

    val Turnstoneintogold: Boolean = false,

    val WebSupport: Boolean = false,

    var RunPid: Long = 0
) {

    private fun checkValue() {
        // 拒绝最大玩家数超过最小开始玩家数
        if (MaxPlayer < StartMinPlayerSize) {
            warn("MaxPlayer < StartMinPlayerSize , Reset !")
            coverField("StartMinPlayerSize",0)
        }
        if (MaxPlayer > 100) {
            warn("MaxPlayer > GameMaxPlayerSize , Reset !")
            //coverField("MaxPlayer",100)
        }
    }

    fun save() {
        fileUtil.writeFile(this.toPrettyPrintingJson())
    }

    fun coverField(name: String,value: Any): Boolean {
        try {
            val field: Field = ReflectionUtils.findField(this::class.java, name) ?:return false
            field.isAccessible = true
            field.set(this,value)
            field.isAccessible = false
        } catch (e: Exception) {
            error("Cover Gameover error", e)
        }
        return true
    }

    private fun allName(): Seq<String> {
        val allName = Seq<String>()
        val fields = this.javaClass.declaredFields
        for (field in fields) {
            // 过滤Kt生成的和不能被覆盖的
            if (field.name != "Companion" && field.name != "fileUtil")
            allName.add(field.name)
        }
        return allName
    }

    companion object {
        val fileUtil = FileUtil.getFolder(Data.Plugin_Data_Path).toFile("Config.json")

        @JvmStatic
        fun stringToClass(): BaseConfig {

            val config: BaseConfig = BaseConfig::class.java.toGson(fileUtil.readFileStringData())

            // PATH
            config.allName().eachAll {
                val data = System.getProperties().getProperty("rwhps.config.$it")
                if (data != null) {
                    if (config.coverField(it,data)) {
                        debug("Set OK $it = $data")
                    } else {
                        debug("Set ERROR $it = $data")
                    }
                }
            }

            config.checkValue()

            return config
        }
    }
}
