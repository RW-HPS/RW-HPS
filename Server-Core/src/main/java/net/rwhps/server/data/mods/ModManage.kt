/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.mods

import net.rwhps.server.data.global.Data
import net.rwhps.server.game.simulation.gameFramework.GameUnitData
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.alone.annotations.NeedToRefactor
import net.rwhps.server.util.log.Log

/**
 * Mods 加载管理器
 */
object ModManage {
    private val coreName = "RW-HPS CoreUnits"
    private var enabledMods =
        net.rwhps.server.struct.OrderedMap<String, net.rwhps.server.struct.ObjectMap<String, Int>>()
    private var enabledModsName = Seq<String>()
    private var loadUnitsCount = 0

    @JvmStatic
    fun load(): Int {
        enabledMods = GameUnitData.getUnitData(coreName)
        enabledModsName.clear()

        enabledModsName.add(coreName)

        return enabledMods.size -1
    }

    @JvmStatic
    fun loadUnits() {
        enabledMods.forEach {
            if (enabledModsName.contains(it.key)) {
                loadUnitsCount += it.value.size
            }
        }

        val stream: GameOutputStream = Data.utilData
        stream.reset()
        stream.writeInt(1)
        stream.writeInt(loadUnitsCount)

        enabledMods.forEach {
            val modGroup = it.key
//            if (enabledModsName.contains(modGroup)) {
                val modData = it.value
                try {
                    val core = modGroup == coreName
                    modData.forEach { iniData ->
                        stream.writeString(iniData.key)
                        stream.writeInt(iniData.value)
                        stream.writeBoolean(true)
                        if (core) {
                            stream.writeBoolean(false)
                        } else {
                            stream.writeBoolean(true)
                            stream.writeString(modGroup)
                        }
                        stream.writeLong(0)
                        stream.writeLong(0)
                    }
                    Log.debug("Load OK", if (core) "Core Units" else modGroup!!)
                } catch (e: Exception) {
                    Log.error(e)
                }
            }
//        }
    }

    @NeedToRefactor
    @JvmStatic
    fun reLoadMods(): Int {
        //modsData.clear()
        //loadUnitsCount = 0

        //val loadCount = load(fileMods!!)
        //loadUnits()
        return 0
    }

    @JvmStatic
    fun clear() {
        loadUnitsCount = 0
    }

    @JvmStatic
    fun getModsList(): Seq<String> = enabledMods.keys().toSeq()
}
