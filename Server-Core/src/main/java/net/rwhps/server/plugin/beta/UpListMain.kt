/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.plugin.beta

import net.rwhps.server.core.thread.CallTimeTask
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.json.Json
import net.rwhps.server.func.StrCons
import net.rwhps.server.game.HessModuleManage
import net.rwhps.server.net.HttpRequestOkHttp
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.net.core.server.AbstractNetConnectServer
import net.rwhps.server.plugin.Plugin
import net.rwhps.server.util.IpUtils
import net.rwhps.server.util.StringFilteringUtil.cutting
import net.rwhps.server.util.algorithms.Base64
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.log.Log
import java.util.concurrent.TimeUnit

/**
 * 为 服务器进入官方列表提供API支持
 * 并不打算开放源码 避免被滥用 (已经被滥用了 指漫天AD)
 * API随Server版本而进行版本更新
 *
 * DEV 保证 :
 * V5开始将不会进行较大版本更新 一切由 [-4] 错误码 解决
 * @author Dr (dr@der.kim)
 */
internal class UpListMain: Plugin() {
    private val version = "Version=HPS#1"
    private val privateIp: String
        get() {
            var privateIpTemp = IpUtils.getPrivateIp()
            if (privateIpTemp.isNullOrBlank()) {
                privateIpTemp = "10.0.0.1"
            }
            return privateIpTemp
        }
    private var port = Data.config.port.toString()

    private var versionBeta = false
    private var versionGame = "1.15"
    private var versionGameInt = 176

    private var upServerList = false

    /* DATA Cache */
    private lateinit var serverID: String
    private lateinit var addData: String
    private lateinit var openData: String
    private lateinit var updateData: String
    private lateinit var removeData: String

    override fun init() {
        AddLang(this)
    }

    override fun registerCoreCommands(handler: CommandHandler) {
        handler.removeCommand("upserverlist")
        handler.removeCommand("upserverlistnew")

        handler.register("uplist", "[command...]", "serverCommands.upserverlist") { args: Array<String>?, log: StrCons ->
            if (!args.isNullOrEmpty()) {
                when (args[0]) {
                    "add" -> NetStaticData.checkServerStartNet { if (args.size > 1) add(log, args[1]) else add(log) }
                    "update" -> NetStaticData.checkServerStartNet { update() }
                    "remove" -> NetStaticData.checkServerStartNet { remove(log) }
                    "help" -> log(Data.i18NBundle.getinput("uplist.help"))
                    else -> log("Check UpList Command ! use 'uplist help'")
                }
            } else {
                log("Check UpList Command ! use 'uplist help'")
            }
        }
    }

    private fun initUpListData(urlIn: String = ""): Boolean {
        if (NetStaticData.ServerNetType.ordinal in IRwHps.NetType.ServerProtocol.ordinal .. IRwHps.NetType.ServerTestProtocol.ordinal) {
            (NetStaticData.RwHps.typeConnect.abstractNetConnect as AbstractNetConnectServer).run {
                versionBeta = supportedversionBeta
                versionGame = supportedversionGame
                versionGameInt = supportedVersionInt
            }

        } else {
            versionBeta = false
            versionGame = "1.15-Other"
            versionGameInt = 176
        }


        val url = urlIn.ifBlank { Data.urlData.readString("Get.Api.UpListData.Bak") }

        var resultUpList = HttpRequestOkHttp.doPost(url, version)

        if (resultUpList.isBlank() && urlIn.isBlank()) {
            resultUpList = HttpRequestOkHttp.doPost(Data.urlData.readString("Get.Api.UpListData"), version)
        }

        if (resultUpList.isBlank()) {
            Log.error("[Get UPLIST Data Error] Unexpected error Failed to initialize")
            return false
        }

        if (resultUpList.startsWith("[-1]")) {
            Log.error("[Get UPLIST Data Error] Please Check API")
            return false
        } else if (resultUpList.startsWith("[-2]")) {
            Log.error("[Get UPLIST Data Error] IP prohibited")
            return false
        } else if (resultUpList.startsWith("[-4]")) {
            Log.error("[Get UPLIST Data Error] Version Error")
            val newUrl = resultUpList.substring(8, resultUpList.length)
            return if (newUrl == "Error") {
                Log.error("[Get UPLIST Data Error] Version Error & New Error")
                false
            } else {
                initUpListData(newUrl)
            }
        } else if (resultUpList.startsWith("[-5]")) {
            Log.error(resultUpList)
        }

        val json = Json(resultUpList)

        serverID = Base64.decodeString(json.getString("id"))
        addData = Base64.decodeString(json.getString("add"))
        openData = Base64.decodeString(json.getString("open"))
        updateData = Base64.decodeString(json.getString("update"))
        removeData = Base64.decodeString(json.getString("remove"))
        return true
    }

    private fun add(log: StrCons, port: String = "") {
        if (!upServerList) {
            if (initUpListData()) {
                this.port = port.ifBlank { Data.config.port.toString() }
                Threads.newThreadCore { upServerList = true; uplist() }
            }
        } else {
            log("Already on the list")
        }
    }

    private fun uplist() {
        var addData0 = addData.replace("{RW-HPS.RW.VERSION}", versionGame)
        addData0 = addData0.replace("{RW-HPS.RW.VERSION.INT}", versionGameInt.toString())
        addData0 = addData0.replace("{RW-HPS.RW.IS.VERSION}", versionBeta.toString())
        addData0 = addData0.replace("{RW-HPS.RW.IS.PASSWD}", Data.configServer.passwd.isNotBlank().toString())
        addData0 = addData0.replace("{RW-HPS.S.NAME}", cutting(Data.config.serverName, 15))
        addData0 = addData0.replace("{RW-HPS.S.PRIVATE.IP}", privateIp)
        addData0 = addData0.replace("{RW-HPS.S.PORT}", port)


        addData0 = addData0.replace("{RW-HPS.RW.MAP.NAME}", HessModuleManage.hps.room.mapName)
        addData0 = addData0.replace("{RW-HPS.PLAYER.SIZE}", HessModuleManage.hps.room.playerManage.playerGroup.size.toString())

        addData0 = addData0.replace("{RW-HPS.PLAYER.SIZE.MAX}", Data.configServer.maxPlayer.toString())


        Log.debug(addData0)

        val addGs1 = HttpRequestOkHttp.doPostRw("http://gs1.corrodinggames.com/masterserver/1.4/interface", addData0).contains(serverID)
        val addGs4 = HttpRequestOkHttp.doPostRw("http://gs4.corrodinggames.net/masterserver/1.4/interface", addData0).contains(serverID)
        if (addGs1 || addGs4) {
            if (addGs1 && addGs4) {
                Log.clog(Data.i18NBundle.getinput("err.yesList"))
            } else {
                Log.clog(Data.i18NBundle.getinput("err.ynList"))
            }
        } else {
            Log.clog(Data.i18NBundle.getinput("err.noList"))
        }

        val openData0 = openData.replace("{RW-HPS.S.PORT}", port)

        val checkPortGs1 = HttpRequestOkHttp.doPostRw("http://gs1.corrodinggames.com/masterserver/1.4/interface", openData0)
            .contains("true")
        val checkPortGs4 = HttpRequestOkHttp.doPostRw("http://gs4.corrodinggames.net/masterserver/1.4/interface", openData0)
            .contains("true")
        if (checkPortGs1 || checkPortGs4) {
            Log.clog(Data.i18NBundle.getinput("err.yesOpen"))
        } else {
            Log.clog(Data.i18NBundle.getinput("err.noOpen"))
        }

        Threads.newTimedTask(CallTimeTask.CustomUpServerListTask, 50, 50, TimeUnit.SECONDS) { update() }
    }

    private fun update() {
        var updateData0 = updateData.replace("{RW-HPS.RW.IS.PASSWD}", Data.configServer.passwd.isNotBlank().toString())
        updateData0 = updateData0.replace("{RW-HPS.S.NAME}", cutting(Data.config.serverName, 15))
        updateData0 = updateData0.replace("{RW-HPS.S.PRIVATE.IP}", privateIp)
        updateData0 = updateData0.replace("{RW-HPS.S.PORT}", port)


        updateData0 = updateData0.replace("{RW-HPS.RW.MAP.NAME}", HessModuleManage.hps.room.mapName)
        updateData0 = updateData0.replace("{RW-HPS.S.STATUS}", if (HessModuleManage.hps.room.isStartGame) "ingame" else "battleroom")
        updateData0 = updateData0.replace("{RW-HPS.PLAYER.SIZE}", HessModuleManage.hps.room.playerManage.playerGroup.size.toString())


        updateData0 = updateData0.replace("{RW-HPS.PLAYER.SIZE.MAX}", Data.configServer.maxPlayer.toString())


        HttpRequestOkHttp.doPostRw("http://gs1.corrodinggames.com/masterserver/1.4/interface", updateData0)
        HttpRequestOkHttp.doPostRw("http://gs4.corrodinggames.net/masterserver/1.4/interface", updateData0)
    }

    private fun remove(log: StrCons) {
        if (upServerList) {
            Threads.closeTimeTask(CallTimeTask.CustomUpServerListTask)
            val removeGs1 = HttpRequestOkHttp.doPostRw("http://114.55.226.106:9898/masterserver/1.4/interface", removeData).contains("REMOVED")
            val removeGs2 = HttpRequestOkHttp.doPostRw("http://114.55.226.106:9899/masterserver/1.4/interface", removeData).contains("REMOVED")
            if (removeGs1 || removeGs2) {
                upServerList = false
                log("Deleted UPLIST")
                return
            }
            log("Delete failed, unable to stop thread")
        } else {
            log("Not uploaded No deletion is required")
        }
    }


    /**
     * Inject multiple languages into the server
     * @author Dr (dr@der.kim)
     */
    private class AddLang(val plugin: Plugin) {
        init {
            help()
        }

        private fun help() {
            loadCN(
                    "uplist.help", """       
        [uplist add] 服务器上传到列表 显示配置文件端口
        [uplist add (port)] 服务器上传到列表 服务器运行配置文件端口 显示自定义端口
        [uplist update] 立刻更新列表服务器信息
        [uplist remove] 取消服务器上传列表
        [uplist help] 获取帮助
        """.trimIndent()
            )
            loadEN(
                    "uplist.help", """
        [uplist add] Server upload to list Show profile port
        [uplist add (port)] Server upload to list Server running profile port Display custom port
        [uplist update] Update list server information immediately
        [uplist remove] Cancel server upload list
        [uplist help] Get Help
        """.trimIndent()
            )
            loadHK(
                    "uplist.help", """        
        [uplist add] 服务器上传到列表 显示配置文件端口
        [uplist add (port)] 服务器上传到列表 服务器运行配置文件端口 显示自定义端口
        [uplist update] 立刻更新列表服务器信息
        [uplist remove] 取消服务器上传列表
        [uplist help] 获取帮助
        """.trimIndent()
            )
            loadRU(
                    "uplist.help", """
        [uplist add] Загрузка сервера в список Показать порт профиля
        [uplist add (port)] Загрузка сервера в список Порт запущенного профиля сервера Показать пользовательские порты
        [uplist update] Немедленное обновление информации сервера списка
        [uplist remove] Отмена загрузки сервера в список
        [uplist help] Получить помощь
        """.trimIndent()
            )
        }

        private fun loadCN(k: String, v: String) {
            plugin.loadLang("CN", k, v)
        }

        private fun loadEN(k: String, v: String) {
            plugin.loadLang("EN", k, v)
        }

        private fun loadHK(k: String, v: String) {
            plugin.loadLang("HK", k, v)
        }

        private fun loadRU(k: String, v: String) {
            plugin.loadLang("RU", k, v)
        }
    }
}
// 给岁月以文明，而不是给文明以岁月
