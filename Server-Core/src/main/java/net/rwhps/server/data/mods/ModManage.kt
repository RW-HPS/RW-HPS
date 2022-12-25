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
import net.rwhps.server.game.simulation.gameFramework.GameData
import net.rwhps.server.game.simulation.gameFramework.GameNet
import net.rwhps.server.game.simulation.gameFramework.GameUnitData
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.struct.ObjectMap
import net.rwhps.server.struct.OrderedMap
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.alone.annotations.DidNotFinish
import net.rwhps.server.util.alone.annotations.NeedToRefactor
import net.rwhps.server.util.log.Log

/**
 * Mods 加载管理器
 * 这里加载的 Mod 默认启用
 *
 * @author RW-HPS/Dr
 */
object ModManage {
    /** 游戏核心单位的命名(便于分辨) */
    private val coreName = "RW-HPS CoreUnits"
    /** 游戏单位校验数据*/
    private var enabledMods = OrderedMap<String,ObjectMap<String, Int>>()
    /** 启用的MOD(名字)-暂时无效 */
    private var enabledModsName = Seq<String>()
    /** 加载的单位数量 */
    private var loadUnitsCount = 0
    /** MOD的名字列表 */
    private var modList = Seq<String>()

    /**
     * 从 GAME-Hess 获取Mod数据
     *
     * @return 获取到的非原版数量
     */
    @JvmStatic
    fun load(): Int {
        enabledMods = GameUnitData.getUnitData(coreName)
        enabledModsName.clear()

        //enabledModsName.add(coreName)
        modList = enabledMods.keys().toSeq()

        return enabledMods.size -1
    }

    /**
     * 加载 Mod 数据到 游戏单位数据
     * @return 加载单位数
     */
    @JvmStatic
    fun loadUnits(): Int {
        var loadCount = 0
        enabledMods.forEach {
            //if (enabledModsName.contains(it.key)) {
                loadUnitsCount += it.value.size
            //}
        }

        val stream: GameOutputStream = Data.utilData
        stream.reset()
        stream.writeInt(1)
        stream.writeInt(loadUnitsCount)

        enabledMods.forEach {
            val modGroup = it.key
            //if (enabledModsName.contains(modGroup)) {
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
                    loadCount++
                    Log.debug("Load OK", if (core) "Core Units" else modGroup!!)
                } catch (e: Exception) {
                    Log.error(e)
                }
            //}
        }

        return loadCount
    }

    /**
     * 重新从 [Data.Plugin_Mods_Path] 读取Mod
     * @return 读取到的Mod数量
     */
    @JvmStatic
    fun reLoadMods(): Int {
        GameData.clean()
        GameUnitData.reloadUnitData()
        GameNet.newConnect()

        return load().run { loadUnits() }
    }

    /**
     * 清理
     */
    @NeedToRefactor
    @JvmStatic
    fun clear() {
        loadUnitsCount = 0
    }

    /**
     * 获取 Mod 列表
     * @return Seq<String>
     */
    @JvmStatic
    fun getModsList(): Seq<String> = modList

    /**
     * 加入 Mod 到白名单 [enabledModsName]
     * @return Seq<String>
     */
    @DidNotFinish
    @JvmStatic
    fun addMod(): Seq<String> = modList
}