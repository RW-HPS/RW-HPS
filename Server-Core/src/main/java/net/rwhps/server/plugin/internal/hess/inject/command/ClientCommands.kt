/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.plugin.internal.hess.inject.command

import com.corrodinggames.rts.game.n
import com.corrodinggames.rts.gameFramework.j.ai
import net.rwhps.server.command.ex.Vote
import net.rwhps.server.core.thread.CallTimeTask
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.HessModuleManage
import net.rwhps.server.data.MapManage
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.Data.LINE_SEPARATOR
import net.rwhps.server.data.player.PlayerHess
import net.rwhps.server.data.plugin.PluginManage
import net.rwhps.server.game.GameMaps
import net.rwhps.server.game.event.game.ServerGameStartEvent
import net.rwhps.server.plugin.internal.hess.inject.core.GameEngine
import net.rwhps.server.struct.BaseMap.Companion.toSeq
import net.rwhps.server.util.IsUtils.notIsNumeric
import net.rwhps.server.util.file.FileUtils
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.log.Log.error
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author RW-HPS/Dr
 */
internal class ClientCommands(handler: CommandHandler) {
    private val localeUtil = Data.i18NBundle

    private fun isAdmin(player: PlayerHess): Boolean {
        if (player.isAdmin) {
            return true
        }
        player.sendSystemMessage(player.i18NBundle.getinput("err.noAdmin"))
        return false
    }

    private fun checkPositionNumb(int: String, player: PlayerHess): Boolean {
        if (notIsNumeric(int)) {
            player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
            return false
        }
        if (int.toInt() < 0) {
            player.sendSystemMessage(player.i18NBundle.getinput("err.noInt"))
            return false
        }
        if (int.toInt() > (Data.configServer.MaxPlayer)) {
            player.sendSystemMessage(player.i18NBundle.getinput("err.maxPlayer"))
            return false
        }
        return true
    }

    init {
        handler.register("help", "clientCommands.help") { _: Array<String>?, player: PlayerHess ->
            val str = StringBuilder(16)
            for (command in handler.commandList) {
                if (command.description.startsWith("#")) {
                    str.append("   ").append(command.text).append(if (command.paramText.isEmpty()) "" else " ")
                        .append(command.paramText).append(" - ").append(command.description.substring(1))
                } else {
                    if ("HIDE" == command.description) {
                        continue
                    }
                    str.append("   ").append(command.text).append(if (command.paramText.isEmpty()) "" else " ")
                        .append(command.paramText).append(" - ").append(player.i18NBundle.getinput(command.description))
                        .append(LINE_SEPARATOR)
                }
            }
            player.sendSystemMessage(str.toString())
        }
        handler.register("map", "<MapNumber...>", "clientCommands.map") { args: Array<String>, player: PlayerHess ->
            if (isAdmin(player)) {
                if (room.isStartGame) {
                    return@register
                }
                val response = StringBuilder(args[0])
                var i = 1
                val lens = args.size
                while (i < lens) {
                    response.append(" ").append(args[i])
                    i++
                }
                val inputMapName = response.toString().replace("'", "").replace(" ", "").replace("-", "").replace("_", "")
                val mapPlayer = Data.MapsMap[inputMapName]
                if (inputMapName.equals("DEF",ignoreCase = true)) {
                    GameEngine.netEngine.az = "maps/skirmish/[z;p10]Crossing Large (10p).tmx"
                    GameEngine.netEngine.ay.b = "[z;p10]Crossing Large (10p).tmx"
                    room.mapName = "Crossing Large (10p)"
                    GameEngine.netEngine.ay.a = ai.a
                    MapManage.maps.mapType = GameMaps.MapType.defaultMap
                    return@register
                }
                if (mapPlayer != null) {
                    val data = mapPlayer.split("@").toTypedArray()
                    GameEngine.netEngine.az = "maps/skirmish/${data[1]}${data[0]}.tmx"
                    GameEngine.netEngine.ay.b = "${data[1]}${data[0]}.tmx"
                    room.mapName = data[0]
                    GameEngine.netEngine.ay.a = ai.a
                    MapManage.maps.mapType = GameMaps.MapType.defaultMap
                } else {
                    if (MapManage.mapsData.size == 0) {
                        return@register
                    }
                    if (notIsNumeric(inputMapName)) {
                        player.sendSystemMessage(localeUtil.getinput("err.noNumber"))
                        return@register
                    }
                    val name = MapManage.mapsData.keys.toSeq()[inputMapName.toInt()]
                    val data = MapManage.mapsData[name]!!
                    MapManage.maps.mapData = data
                    MapManage.maps.mapType = data.mapType
                    MapManage.maps.mapName = name
                    MapManage.maps.mapPlayer = ""

                    name.let {
                        GameEngine.netEngine.az = "$it.tmx"
                        GameEngine.netEngine.ay.b = it
                        room.mapName = it
                        GameEngine.netEngine.ay.a = ai.b
                    }
                    player.sendSystemMessage(player.i18NBundle.getinput("map.custom.info"))
                }
                room.call.sendSystemMessage(localeUtil.getinput("map.to",player.name,room.mapName))
                GameEngine.netEngine.L()
            }
        }
        handler.register("maps", "[page]", "clientCommands.maps") { _: Array<String>?, player: PlayerHess ->
            if (room.isStartGame) {
                player.sendSystemMessage(localeUtil.getinput("err.startGame"))
                return@register
            }
            if (MapManage.mapsData.size == 0) {
                return@register
            }
            val i = AtomicInteger(0)

            player.sendSystemMessage(localeUtil.getinput("maps.top"))
            MapManage.mapsData.keys.forEach { k: String ->
                player.sendSystemMessage(localeUtil.getinput("maps.info", i.get(), k))
                i.getAndIncrement()
            }
        }
        handler.register("afk", "clientCommands.afk") { _: Array<String>?, player: PlayerHess ->
            if (!room.isAfk) {
                player.sendSystemMessage(player.i18NBundle.getinput("ban.comm", "afk"))
                return@register
            }
            if (player.isAdmin) {
                player.sendSystemMessage(player.i18NBundle.getinput("afk.adminNo"))
            } else {
                if (room.isStartGame) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                    return@register
                }
                if (Threads.containsTimeTask(CallTimeTask.PlayerAfkTask)) {
                    return@register
                }
                val admin = AtomicBoolean(true)
                room.playerManage.playerGroup.eachAllFind({ p: PlayerHess -> p.isAdmin },{ _: PlayerHess -> admin.set(false) })
                if (admin.get()) {
                    player.isAdmin = true
                    room.call.sendSystemMessageLocal("afk.end.noAdmin", player.name)
                    return@register
                }
                Threads.newCountdown(CallTimeTask.PlayerAfkTask, 30, TimeUnit.SECONDS) {
                    room.playerManage.playerGroup.eachFind(
                        { p: PlayerHess -> p.isAdmin }) { i: PlayerHess ->
                        i.isAdmin = false
                        player.isAdmin = true
                        room.call.sendSystemMessageLocal("afk.end.ok", player.name)
                        GameEngine.netEngine.L()
                    }
                }
                room.call.sendSystemMessageLocal("afk.start", player.name)
            }
        }
        handler.register("income", "<income>", "clientCommands.income") { args: Array<String>, player: PlayerHess ->
            if (isAdmin(player)) {
                if (room.isStartGame) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.startGame.warn"))
                }
                GameEngine.data.gameDataLink.income = args[0].toFloat()
            }
        }
        handler.register("addmoney", "<PlayerPositionNumber> <money>", "clientCommands.addmoney") { args: Array<String>, player: PlayerHess ->
            if (!room.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.noStartGame"))
                return@register
            }
            if (isAdmin(player)) {
                if (notIsNumeric(args[0]) || notIsNumeric(args[1])) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
                    return@register
                }

                val site = args[0].toInt() - 1
                val addMoneyPlayer = room.playerManage.getPlayerArray(site)
                if (addMoneyPlayer != null) {
                    try {
                        addMoneyPlayer.credits += args[1].toInt()
                    } catch (e: IOException) {
                        error("[Player] addMoneyPlayer Error", e)
                    }
                }
            }
        }
        handler.register("ai", "<difficuld>", "clientCommands.income") { args: Array<String>, player: PlayerHess ->
            if (isAdmin(player)) {
                if (room.isStartGame) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                    return@register
                }
                GameEngine.data.gameDataLink.aiDifficuld = args[0].toInt()

                for (site in 0 until Data.configServer.MaxPlayer) {
                    if (room.playerManage.getPlayerArray(site) == null) {
                        if (n.k(site) != null) {
                            n.k(site).x = args[0].toInt()
                            n.k(site).z = args[0].toInt()
                        }
                    }
                }
            }
        }
        handler.register("status", "clientCommands.status") { _: Array<String>?, player: PlayerHess ->
            player.sendSystemMessage(player.i18NBundle.getinput(
                "status.version",
                room.playerManage.playerGroup.size,
                0,
                Data.SERVER_CORE_VERSION,
                "RW-HPS-Hess"
            ))
        }
        handler.register("vote", "<gameover>","clientCommands.vote") { _: Array<String>?, player: PlayerHess ->
            Data.vote = Vote("gameover",player)
        }
        handler.register("iunit", "<PlayerPositionNumber> <unitID>","clientCommands.iunit") { args: Array<String>, player: PlayerHess ->
            if (room.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                return@register
            }
            if (isAdmin(player)) {
                if (notIsNumeric(args[1])) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
                    return@register
                }

                val site = args[0].toInt() - 1
                val playerUnit = room.playerManage.getPlayerArray(site)
                if (playerUnit != null) {
                    playerUnit.startUnit = args[1].toInt()
                } else {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.player.no.site",args[0]))
                }
            }
        }
        handler.register("summon", "<unitName>", "clientCommands.kick") { args: Array<String>, player: PlayerHess ->
            if (!room.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.noStartGame"))
                return@register
            }
            if (player.isAdmin) {
                val unit = args[0]
                player.sendSystemMessage("Ping map to spawn")
                player.addData("Summon", unit)
            }
        }
        handler.register("kick", "<PlayerPositionNumber>", "clientCommands.kick") { args: Array<String>, player: PlayerHess ->
            if (room.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                return@register
            }
            if (isAdmin(player)) {
                if (notIsNumeric(args[0])) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
                    return@register
                }

                val site = args[0].toInt() - 1
                val kickPlayer = room.playerManage.getPlayerArray(site)
                if (kickPlayer != null) {
                    try {
                        kickPlayer.kickPlayer(localeUtil.getinput("kick.you"),60)
                    } catch (e: IOException) {
                        error("[Player] Send Kick Player Error", e)
                    }
                } else {
                    // Kick AI
                    n.k(site).I()
                }
            }
        }

        /* QC */
        handler.register("credits", "<money>", "HIDE") { args: Array<String>, player: PlayerHess ->
            if (isAdmin(player)) {
                if (notIsNumeric(args[0])) {
                    player.sendSystemMessage(localeUtil.getinput("err.noNumber"))
                    return@register
                }
                GameEngine.data.gameDataLink.credits = when (args[0].toInt()) {
                    0 -> 1
                    1000 -> 2
                    2000 -> 3
                    5000 -> 4
                    10000 -> 5
                    50000 -> 6
                    100000 -> 7
                    200000 -> 8
                    4000 -> 0
                    else -> 0
                }
            }
        }
        handler.register("nukes", "<boolean>", "HIDE") { args: Array<String>, player: PlayerHess ->
            if (isAdmin(player)) {
                GameEngine.data.gameDataLink.nukes = !args[0].toBoolean()
            }
        }
        handler.register("addai", "HIDE") { _: Array<String>?, player: PlayerHess ->
            if (isAdmin(player)) {
                GameEngine.root.multiplayer.addAI()
            }
        }
        handler.register("fog", "<type>", "HIDE") { args: Array<String>, player: PlayerHess ->
            if (isAdmin(player)) {
                GameEngine.data.gameDataLink.fog = if ("off" == args[0]) 0 else if ("basic" == args[0]) 1 else 2
            }
        }
        handler.register("sharedcontrol", "<boolean>", "HIDE") { args: Array<String>, player: PlayerHess ->
            if (isAdmin(player)) {
                GameEngine.data.gameDataLink.sharedcontrol = args[0].toBoolean()
            }
        }
        handler.register("startingunits", "<type>", "HIDE") { args: Array<String>, player: PlayerHess ->
            if (isAdmin(player)) {
                GameEngine.data.gameDataLink.startingunits = args[0].toInt()
            }
        }
        handler.register("start", "clientCommands.start") { _: Array<String>?, player: PlayerHess? ->
            if (player != null && isAdmin(player)) {
                if (room.isStartGame) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                    return@register
                }

                if (Data.configServer.StartMinPlayerSize != -1 &&
                    Data.configServer.StartMinPlayerSize > room.playerManage.playerGroup.size) {
                    player.sendSystemMessage(player.i18NBundle.getinput("start.playerNo", Data.configServer.StartMinPlayerSize))
                    return@register
                }
				
				GameEngine.root.multiplayer.multiplayerStart()

				GameEngine.data.eventManage.fire(ServerGameStartEvent())

            }

            if (MapManage.maps.mapType != GameMaps.MapType.defaultMap) {
                val file = FileUtils.getFolder(Data.Plugin_Maps_Path).toFile(MapManage.maps.mapName + ".tmx")
                if (file.notExists()) {
                    MapManage.maps.mapData!!.readMap()
                }
                GameEngine.netEngine.az = "/SD/rusted_warfare_maps/${MapManage.maps.mapName}.tmx"
                GameEngine.netEngine.ay.b = "${MapManage.maps.mapName}.tmx"
                GameEngine.netEngine.ay.a = ai.b
            }

        }
        handler.register("move", "<PlayerPositionNumber> <ToSerialNumber> <Team>", "HIDE") { args: Array<String>, player: PlayerHess ->
            if (room.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                return@register
            }
            if (isAdmin(player)) {
                if (notIsNumeric(args[0]) && notIsNumeric(args[1]) && notIsNumeric(args[2])) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
                    return@register
                }

                val tg = args[0].toInt() - 1
                val player = n.k(tg)
                val site = args[1].toInt() - 1
                val newTeam = args[2].toInt()
                GameEngine.netEngine.a(player, site)
                when (newTeam) {
                    -1 -> {
                        player.r = site % 2
                    }
                    -4 -> {
                        player.r = -3
                    }
                    else -> {
                        player.r = newTeam
                    }
                }
            }
        }
        handler.register("team", "<PlayerPositionNumber> <ToTeamNumber>", "HIDE") { args: Array<String>, player: PlayerHess ->
            if (room.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                return@register
            }
            if (isAdmin(player)) {
                if (notIsNumeric(args[0]) && notIsNumeric(args[1])) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
                    return@register
                }
                val playerPosition = args[0].toInt() - 1
                val newPosition = args[1].toInt() - 1
                n.k(playerPosition).r = newPosition
            }
        }

        PluginManage.runRegisterServerClientCommands(handler)
    }
    
    companion object {
        private val room = HessModuleManage.hessLoaderMap[this::class.java.classLoader.toString()]!!.room
    }
}