/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.command.server

import net.rwhps.server.core.Call
import net.rwhps.server.core.Call.sendSystemMessage
import net.rwhps.server.core.Call.sendSystemMessageLocal
import net.rwhps.server.core.Call.sendTeamData
import net.rwhps.server.core.Call.sendTeamMessage
import net.rwhps.server.core.Call.testPreparationPlayer
import net.rwhps.server.core.Call.upDataGameData
import net.rwhps.server.core.thread.CallTimeTask
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.Data.LINE_SEPARATOR
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.player.Player
import net.rwhps.server.data.plugin.PluginManage
import net.rwhps.server.game.GameMaps.MapType
import net.rwhps.server.game.event.EventType.GameStartEvent
import net.rwhps.server.util.IsUtil.notIsBlank
import net.rwhps.server.util.IsUtil.notIsNumeric
import net.rwhps.server.util.Time
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.game.Events
import net.rwhps.server.util.log.Log.error
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.IntStream

/**
 * @author RW-HPS/Dr
 */
internal class ClientCommands(handler: CommandHandler) {
    private val localeUtil = Data.i18NBundle
    private fun isAdmin(player: Player): Boolean {
        if (player.isAdmin) {
            return true
        }
        player.sendSystemMessage(player.i18NBundle.getinput("err.noAdmin"))
        return false
    }

    private fun checkSiteNumb(int: String, player: Player): Boolean {
        if (notIsNumeric(int)) {
            player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
            return false
        }
        if (int.toInt() < 0) {
            player.sendSystemMessage(player.i18NBundle.getinput("err.noInt"))
            return false
        }
        if (int.toInt() > (Data.config.MaxPlayer)) {
            player.sendSystemMessage(player.i18NBundle.getinput("err.maxPlayer"))
            return false
        }
        return true
    }

    init {
        handler.register("help", "clientCommands.help") { _: Array<String>?, player: Player ->
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
        handler.register("map", "<MapNumber...>", "clientCommands.map") { args: Array<String>, player: Player ->
            if (isAdmin(player)) {
                if (Data.game.isStartGame || Data.game.mapLock) {
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
                if (mapPlayer != null) {
                    val data = mapPlayer.split("@").toTypedArray()
                    Data.game.maps.mapName = data[0]
                    Data.game.maps.mapPlayer = data[1]
                    Data.game.maps.mapType = MapType.defaultMap
                } else {
                    if (Data.game.mapsData.size == 0) {
                        return@register
                    }
                    if (notIsNumeric(inputMapName)) {
                        player.sendSystemMessage(localeUtil.getinput("err.noNumber"))
                        return@register
                    }
                    val name = Data.game.mapsData.keys().toSeq()[inputMapName.toInt()]
                    val data = Data.game.mapsData[name]
                    Data.game.maps.mapData = data
                    Data.game.maps.mapType = data.mapType
                    Data.game.maps.mapName = name
                    Data.game.maps.mapPlayer = ""
                    player.sendSystemMessage(player.i18NBundle.getinput("map.custom.info"))
                }
                Call.sendSystemMessage(localeUtil.getinput("map.to",player.name,Data.game.maps.mapName))
                upDataGameData()
            }
        }
        handler.register("maps", "[page]", "clientCommands.maps") { _: Array<String>?, player: Player ->
            if (Data.game.isStartGame) {
                player.sendSystemMessage(localeUtil.getinput("err.startGame"))
                return@register
            }
            if (Data.game.mapsData.size == 0) {
                return@register
            }
            val response = StringBuilder()
            val i = AtomicInteger(0)
            response.append(localeUtil.getinput("maps.top"))
            Data.game.mapsData.keys().forEach { k: String ->
                response.append(localeUtil.getinput("maps.info", i.get(), k)).append(LINE_SEPARATOR)
                i.getAndIncrement()
            }
            player.sendSystemMessage(response.toString())
        }
        handler.register("afk", "clientCommands.afk") { _: Array<String>?, player: Player ->
            if (!Data.game.isAfk) {
                player.sendSystemMessage(localeUtil.getinput("ban.comm", "afk"))
                return@register
            }
            if (player.isAdmin) {
                player.sendSystemMessage(localeUtil.getinput("afk.adminNo"))
            } else {
                if (Data.game.isStartGame) {
                    player.sendSystemMessage(localeUtil.getinput("err.startGame"))
                    return@register
                }
                if (Threads.containsTimeTask(CallTimeTask.PlayerAfkTask)) {
                    return@register
                }
                val admin = AtomicBoolean(true)
                Data.game.playerManage.playerGroup.eachAllFind({ p: Player -> p.isAdmin },{ _: Player -> admin.set(false) })
                if (admin.get() && Data.config.OneAdmin) {
                    player.isAdmin = true
                    upDataGameData()
                    sendSystemMessageLocal("afk.end.noAdmin", player.name)
                    return@register
                }
                Threads.newCountdown(CallTimeTask.PlayerAfkTask, 30, TimeUnit.SECONDS) {
                    Data.game.playerManage.playerGroup.eachAllFind(
                        { p: Player -> p.isAdmin }) { i: Player ->
                        i.isAdmin = false
                        player.isAdmin = true
                        upDataGameData()
                        sendSystemMessageLocal("afk.end.ok", player.name)
                    }
                }
                sendSystemMessageLocal("afk.start", player.name)
            }
        }
        handler.register("give", "<PlayerSerialNumber>", "clientCommands.give") { args: Array<String>, player: Player ->
            if (isAdmin(player)) {
                if (Data.game.isStartGame) {
                    player.sendSystemMessage(localeUtil.getinput("err.startGame"))
                    return@register
                }
                if (notIsNumeric(args[0])) {
                    player.sendSystemMessage(localeUtil.getinput("err.noNumber"))
                    return@register
                }
                val playerSite = args[0].toInt() - 1
                val newAdmin = Data.game.playerManage.getPlayerArray(playerSite)
                if (notIsBlank(newAdmin)) {
                    if (newAdmin!!.headlessDevice) {
                        player.sendSystemMessage(player.i18NBundle.getinput("err.player.operating.no"))
                        return@register
                    }
                    player.isAdmin = false
                    newAdmin.isAdmin = true
                    upDataGameData()
                    sendSystemMessage("give.ok", player.name)
                } else {
                    player.sendSystemMessage(player.getinput("give.noPlayer", player.name))
                }
            }
        }
        handler.register("nosay", "<on/off>", "clientCommands.noSay") { args: Array<String>, player: Player ->
            player.noSay = "on" == args[0]
            player.sendSystemMessage(localeUtil.getinput("server.noSay", if (player.noSay) "开启" else "关闭"))
        }
        handler.register("am", "<on/off>", "clientCommands.am") { args: Array<String>, player: Player ->
            if (isAdmin(player)) {
                Data.game.playerManage.amTeam = "on" == args[0]
                if (Data.game.playerManage.amTeam) {
                    Data.game.lockTeam = true
                    Data.game.playerManage.amYesPlayerTeam()
                } else {
                    Data.game.playerManage.amNoPlayerTeam()
                }
                player.sendSystemMessage(localeUtil.getinput("server.amTeam", if (Data.game.playerManage.amTeam) "开启" else "关闭"))
            }

        }
        handler.register("income", "<income>", "clientCommands.income") { args: Array<String>, player: Player ->
            if (isAdmin(player)) {
                if (Data.config.IncomeLock) {
                    player.sendSystemMessage("倍率已锁定")
                    return@register
                }
                if (Data.game.isStartGame) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                    return@register
                }
                Data.game.income = args[0].toFloat()
                upDataGameData()
            }
        }
        handler.register("status", "clientCommands.status") { _: Array<String>?, player: Player ->
            player.sendSystemMessage(
                player.i18NBundle.getinput(
                    "status.version",
                    Data.game.playerManage.playerGroup.size,
                    Data.core.admin.bannedIPs.size,
                    Data.SERVER_CORE_VERSION,
                    NetStaticData.RwHps.typeConnect.version
                )
            )
        }
        handler.register("kick", "<PlayerSerialNumber>", "clientCommands.kick") { args: Array<String>, player: Player ->
            if (Data.game.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                return@register
            }
            if (isAdmin(player)) {
                if (notIsNumeric(args[0])) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
                    return@register
                }

                val site = args[0].toInt() - 1
                val kickPlayer = Data.game.playerManage.getPlayerArray(site)
                if (kickPlayer != null) {
                    if (kickPlayer.headlessDevice) {
                        player.sendSystemMessage(player.i18NBundle.getinput("err.player.operating.no"))
                        return@register
                    }
                    try {
                        kickPlayer.kickPlayer(localeUtil.getinput("kick.you"),60)
                    } catch (e: IOException) {
                        error("[Player] Send Kick Player Error", e)
                    }
                }
            }
        }
        handler.register("sync",  "clientCommands.sync") { _: Array<String>?, player: Player ->
            run {
                if (Data.game.playerManage.playerGroup.size == 0) {
                    player.sendSystemMessage("Only one player, Ban Sync")
                } else {
                    player.sync()
                }
            }
        }
        handler.register("summon", "<unitName>", "clientCommands.kick") { args: Array<String>, player: Player ->
            if (!Data.game.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.noStartGame"))
                return@register
            }
            if (player.superAdmin) {
                val unit = args[0]
                player.sendSystemMessage("请Ping地图需要生成位置")
                player.addData("Summon", unit)
            }
        }
        handler.register("pause", "clientCommands.pause") { _: Array<String>, player: Player ->
            if (isAdmin(player)) {
                if (!Data.game.isStartGame) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.noStartGame"))
                    return@register
                }
                Data.game.gamePaused = true
                Call.sendSystemMessage(player.i18NBundle.getinput("pause.ok"))
            }
        }
        handler.register("unpause", "clientCommands.unpause") { _: Array<String>, player: Player ->
            if (isAdmin(player)) {
                if (!Data.game.isStartGame) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.noStartGame"))
                    return@register
                }
                Data.game.gamePaused = false
                Call.sendSystemMessage(player.i18NBundle.getinput("unpause.ok"))
            }
        }
        handler.register("color", "<colorID>","clientCommands.color") { args: Array<String>, player: Player ->
            if (Data.game.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                return@register
            }

            if (checkSiteNumb(args[0],player)) {
                player.color = args[0].toInt() -1
                sendTeamData()
            }
        }
        handler.register("iunit", "<SerialNumber> <unitID>","clientCommands.iunit") { args: Array<String>, player: Player ->
            //(type == 1) ? 1 : (type == 2) ? 2 : (type ==3) ? 3 : (type == 4) ? 4 : 100
            //Call.sendSystemMessage(player.i18NBundle.getinput("unpause.ok"))
            if (isAdmin(player)) {
                if (Data.game.isStartGame) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                    return@register
                }

                if (checkSiteNumb(args[0], player)) {
                    val site = args[0].toInt() - 1
                    val inPlayer: Player? = Data.game.playerManage.getPlayerArray(site)

                    if (inPlayer == null) {
                        player.sendSystemMessage(player.i18NBundle.getinput("err.player.no.site", site))
                        return@register
                    }

                    inPlayer.startUnit = if (IntStream.of(1, 2, 3, 4, 100, 101, 102, 103)
                            .anyMatch { it == args[1].toInt() }
                    ) args[1].toInt() else 1
                    sendTeamData()
                }
            }
        }


        handler.register("i", "<i...>", "HIDE") { args: Array<String>?, player: Player ->
            if (args.contentToString().contains("同步:对象ID:")) {
                player.sync()
            }
        }

        /* QC */
        handler.register("credits", "<money>", "HIDE") { args: Array<String>, player: Player ->
            if (isAdmin(player)) {
                if (notIsNumeric(args[0])) {
                    player.sendSystemMessage(localeUtil.getinput("err.noNumber"))
                    return@register
                }
                when (args[0].toInt()) {
                    0 -> Data.game.credits = 1
                    1000 -> Data.game.credits = 2
                    2000 -> Data.game.credits = 3
                    5000 -> Data.game.credits = 4
                    10000 -> Data.game.credits = 5
                    50000 -> Data.game.credits = 6
                    100000 -> Data.game.credits = 7
                    200000 -> Data.game.credits = 8
                    4000 -> Data.game.credits = 0
                    else -> {
                    }
                }
                upDataGameData()
            }
        }
        handler.register("nukes", "<boolean>", "HIDE") { args: Array<String>, player: Player ->
            if (isAdmin(player)) {
                Data.game.noNukes = !java.lang.Boolean.parseBoolean(args[0])
                upDataGameData()
            }
        }
        handler.register("addai", "HIDE") { _: Array<String>?, player: Player ->
            player.sendSystemMessage(player.i18NBundle.getinput("err.nosupr"))
            // 给他踢了 (不)
            //player.kickPlayer(player.i18NBundle.getinput("err.nosupr"),60)
        }
        handler.register("fog", "<type>", "HIDE") { args: Array<String>, player: Player ->
            if (isAdmin(player)) {
                Data.game.mist = if ("off" == args[0]) 0 else if ("basic" == args[0]) 1 else 2
                upDataGameData()
            }
        }
        handler.register("sharedcontrol", "<boolean>", "HIDE") { args: Array<String>, player: Player ->
            if (isAdmin(player)) {
                Data.game.sharedControl = java.lang.Boolean.parseBoolean(args[0])
                upDataGameData()
            }
        }
        handler.register("startingunits", "<type>", "HIDE") { args: Array<String>, player: Player ->
            if (isAdmin(player)) {
                if (notIsNumeric(args[0])) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
                    return@register
                }
                //Data.game.initUnit = (type == 1) ? 1 : (type == 2) ? 2 : (type ==3) ? 3 : (type == 4) ? 4 : 100;
                Data.game.initUnit = args[0].toInt()
                Data.game.playerManage.runPlayerArrayDataRunnable { it?.startUnit = args[0].toInt() }
                upDataGameData()
            }
        }
        handler.register("start", "clientCommands.start") { _: Array<String>?, player: Player ->
            if (isAdmin(player)) {
                if (Data.game.isStartGame) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                    return@register
                }

                Threads.closeTimeTask(CallTimeTask.AutoStartTask)

                if (Threads.containsTimeTask(CallTimeTask.PlayerAfkTask)) {
                    Threads.closeTimeTask(CallTimeTask.PlayerAfkTask)
                    Call.sendSystemMessageLocal("afk.clear", player.name)
                }

                if (Data.config.StartMinPlayerSize != -1 &&
                    Data.config.StartMinPlayerSize > Data.game.playerManage.playerGroup.size) {
                    player.sendSystemMessage(player.i18NBundle.getinput("start.playerNo", Data.config.StartMinPlayerSize))
                    return@register
                }

                Data.game.isStartGame = true

                if (Data.game.maps.mapData != null) {
                    Data.game.maps.mapData!!.readMap()
                }

                val enc = NetStaticData.RwHps.abstractNetPacket.getTeamDataPacket()

                Data.game.playerManage.playerGroup.eachAll { e: Player ->
                    try {
                        e.con!!.sendTeamData(enc)
                        e.con!!.sendStartGame()
                        e.lastMoveTime = Time.concurrentSecond()
                    } catch (err: IOException) {
                        error("Start Error", err)
                    }
                }
                if (Data.game.sharedControl) {
                    Data.game.playerManage.playerGroup.eachAll { it.sharedControl = true }
                }
                Data.game.playerManage.updateControlIdentifier()
                Events.fire(GameStartEvent())
                testPreparationPlayer()
            }
        }
        handler.register("t", "<text...>", "clientCommands.t") { args: Array<String>, player: Player ->
            val response = StringBuilder(args[0])
            var i = 1
            val lens = args.size
            while (i < lens) {
                response.append(" ").append(args[i])
                i++
            }
            sendTeamMessage(player.team, player, response.toString())
        }
        handler.register("surrender", "clientCommands.surrender") { _: Array<String>?, player: Player ->
            if (Data.game.isStartGame) {
                // 我放弃了 建议来人修复这个阴间Vote
				/*if (Data.vote == null) {
                    if (Vote.testVoet(player)) {
                        Data.vote = Vote("surrender",player)
                    } else {
                        player.sendSystemMessage("[警告] 您正在尝试投降, 如果您确认要进行投票进行投降, 那么 请再点击一次投降")
                    }
				} else {
					Data.vote!!.toVote(player,"y")
				}*/
                player.con!!.sendSurrender()
            } else {
                player.sendSystemMessage(player.i18NBundle.getinput("err.noStartGame"))
            }
        }
        handler.register("teamlock", "[on/off]","clientCommands.teamlock") { args: Array<String>, player: Player ->
            if (isAdmin(player)) {
                Data.game.lockTeam = ("on" == args[0] || "true" == args[0])
                player.sendSystemMessage(player.i18NBundle.getinput("teamlock.info",Data.game.lockTeam))
            }
        }
        handler.register("killme", "clientCommands.killMe") { _: Array<String>?, player: Player ->
            if (Data.game.isStartGame) {
                player.con!!.sendSurrender()
            } else {
                player.sendSystemMessage(player.i18NBundle.getinput("err.noStartGame"))
            }
        }
        // TODO Vote
        /*
        handler.register("vote", "<gameover/kick> [player-site]", "clientCommands.vote") { args: Array<String>, player: Player ->
            when (args[0].lowercase(Locale.getDefault())) {
                "gameover" -> Data.Vote = Vote(player, args[0])
                "kick" -> if (args.size > 1 && isNumeric(args[1])) {
                    Data.Vote = Vote(player, args[0], args[1])
                } else {
                    player.sendSystemMessage(player.localeUtil.getinput("err.commandError"))
                }
                else -> player.sendSystemMessage(player.localeUtil.getinput("err.command"))
            }
        }

         */
        handler.register("move", "<PlayerSerialNumber> <ToSerialNumber> <Team>", "HIDE") { args: Array<String>, player: Player ->
            if (Data.game.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                return@register
            }
            if (isAdmin(player)) {
                if (notIsNumeric(args[0]) && notIsNumeric(args[1]) && notIsNumeric(args[2])) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
                    return@register
                }

                if (args[0].toInt()  == Data.config.MaxPlayer+1 || args[1].toInt()  == Data.config.MaxPlayer+1) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.player.operating.no"))
                    return@register
                }

                Data.game.playerManage.movePlayerSite(args[0].toInt(),args[1].toInt(),args[2].toInt(),true)
            }
        }
        handler.register("self_move", "<ToSerialNumber> <?>", "HIDE") { args: Array<String>, player: Player ->
            if (Data.game.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                return@register
            }
            if (Data.game.lockTeam) {
                return@register
            }
            if (notIsNumeric(args[0]) && notIsNumeric(args[1])) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
                return@register
            }
            Data.game.playerManage.movePlayerSite(player.site+1,args[0].toInt(),args[1].toInt())
        }
        handler.register("team", "<PlayerSiteNumber> <ToTeamNumber>", "HIDE") { args: Array<String>, player: Player ->
            if (Data.game.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                return@register
            }
            if (isAdmin(player)) {
                if (notIsNumeric(args[0]) && notIsNumeric(args[1])) {
                    player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
                    return@register
                }
                val playerSite = args[0].toInt() - 1
                val newSite = args[1].toInt() - 1
                if (playerSite < Data.game.maxPlayer && newSite < Data.game.maxPlayer) {
                    if (newSite > -1) {
                        val newTeamPlayer = Data.game.playerManage.getPlayerArray(playerSite)
                        if (newTeamPlayer != null) {
                            newTeamPlayer.team = newSite
                        }
                    }
                    sendTeamData()
                }
            }
        }
        handler.register("self_team", "<ToTeamNumber>", "HIDE") { args: Array<String>, player: Player ->
            if (Data.game.isStartGame) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.startGame"))
                return@register
            }
            if (Data.game.lockTeam) {
                return@register
            }
            if (notIsNumeric(args[0])) {
                player.sendSystemMessage(player.i18NBundle.getinput("err.noNumber"))
                return@register
            }
            val newSite = args[0].toInt() - 1
            if (newSite < Data.game.maxPlayer) {
                player.team = newSite
                sendTeamData()
            }
        }

        PluginManage.runRegisterServerClientCommands(handler)
    }
}
