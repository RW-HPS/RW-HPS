/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.netconnectprotocol.realize

import net.rwhps.server.core.Call
import net.rwhps.server.core.thread.CallTimeTask
import net.rwhps.server.core.thread.Threads
import net.rwhps.server.data.HessModuleManage
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.player.Player
import net.rwhps.server.data.totalizer.TimeAndNumber
import net.rwhps.server.game.GameUnitType
import net.rwhps.server.game.GameUnitType.GameUnits
import net.rwhps.server.game.event.EventType.*
import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.io.output.CompressOutputStream
import net.rwhps.server.io.packet.GameCommandPacket
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.core.ConnectionAgreement
import net.rwhps.server.net.core.DataPermissionStatus.ServerStatus
import net.rwhps.server.net.core.server.AbstractNetConnect
import net.rwhps.server.net.core.server.AbstractNetConnectData
import net.rwhps.server.net.core.server.AbstractNetConnectServer
import net.rwhps.server.net.netconnectprotocol.internal.relay.relayServerTypeInternal
import net.rwhps.server.net.netconnectprotocol.internal.relay.relayServerTypeReplyInternal
import net.rwhps.server.util.IsUtil
import net.rwhps.server.util.PacketType
import net.rwhps.server.util.RandomUtil
import net.rwhps.server.util.alone.annotations.MainProtocolImplementation
import net.rwhps.server.util.encryption.Game
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.game.CommandHandler.CommandResponse
import net.rwhps.server.util.game.Events
import net.rwhps.server.util.log.ColorCodes
import net.rwhps.server.util.log.Log
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

/**
 * Common server implementation
 *
 * @property supportedversionBeta   Server is Beta
 * @property supportedversionGame   Server Support Version String
 * @property supportedVersionInt    Server Support Version Int
 * @property sync                   Key lock to prevent concurrency
 * @property connectKey             Authentication KEY
 * @property relaySelect            Popup callback
 * @property player                 Player
 * @property permissionStatus       ServerStatus
 * @property version                Protocol version
 * @constructor
 *
 * @date 2020/9/5 17:02:33
 * @author RW-HPS/Dr
 */
@MainProtocolImplementation
open class GameVersionServer(connectionAgreement: ConnectionAgreement) : AbstractNetConnect(connectionAgreement), AbstractNetConnectData, AbstractNetConnectServer {
    open val supportedversionBeta = false
    open val supportedversionGame = "1.15"
    open val supportedVersionInt  = 176

    override val name: String get() = player.name
    override val registerPlayerId: String? get() = player.uuid

    override val betaGameVersion: Boolean get() = supportedversionBeta
    override val clientVersion: Int get() = supportedVersionInt



    protected val sync = ReentrantLock(true)

    /** 玩家连接校验 */
    protected var connectKey: String? = null

    /** 玩家弹窗 */
    protected var relaySelect: ((String) -> Unit)? = null
    protected val turnStoneIntoGold = TimeAndNumber(5,10)


    /** 玩家  */
    override lateinit var player: Player
    override var permissionStatus: ServerStatus = ServerStatus.InitialConnection
        internal set

    override val version: String
        get() = "1.15 RW-HPS"

    override fun sendSystemMessage(msg: String) {
        if (!this::player.isInitialized || !player.noSay) {
            try {
                sendPacket(NetStaticData.RwHps.abstractNetPacket.getSystemMessagePacket(msg))
            } catch (e: IOException) {
                Log.error("[Player] Send System Chat Error", e)
            }
        }
    }

    override fun sendChatMessage(msg: String, sendBy: String, team: Int) {
        try {
            sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(msg, sendBy, team))
        } catch (e: IOException) {
            Log.error("[Player] Send Player Chat Error", e)
        }
    }

    @Throws(IOException::class)
    override fun sendServerInfo(utilData: Boolean) {
        val o = GameOutputStream()
        o.writeString(Data.SERVER_ID)
        o.writeInt(supportedVersionInt)
        /* 地图 */
        o.writeInt(Data.game.maps.mapType.ordinal)
        o.writeString(Data.game.maps.mapPlayer + Data.game.maps.mapName)
        o.writeInt(Data.game.credits)
        o.writeInt(Data.game.mist)
        o.writeBoolean(true)
        o.writeInt(1)
        o.writeByte(7)
        o.writeBoolean(false)
        /* Admin Ui */
        o.writeBoolean(player.isAdmin)
        o.writeInt(Data.config.MaxUnit)
        o.writeInt(Data.config.MaxUnit)
        o.writeInt(Data.game.initUnit)
        o.writeFloat(Data.game.income)
        /* NO Nukes */
        o.writeBoolean(Data.game.noNukes)
        o.writeBoolean(false)
        o.writeBoolean(utilData)
        if (utilData) {
            o.flushEncodeData(Data.utilData)
        }

        /* 共享控制 */
        o.writeBoolean(Data.game.sharedControl)
        o.writeBoolean(false)
        o.writeBoolean(false)
        // 允许观众
        o.writeBoolean(true)
        o.writeBoolean(false)
        sendPacket(o.createPacket(PacketType.SERVER_INFO))
    }

    override fun sendTeamData(gzip: CompressOutputStream) {
        try {
            val o = GameOutputStream()
            /* Player position */
            o.writeInt(player.site)
            o.writeBoolean(Data.game.isStartGame)
            /* Largest player */
            o.writeInt(Data.game.maxPlayer)
            o.flushEncodeData(gzip)
            /* 迷雾 */
            o.writeInt(Data.game.mist)
            o.writeInt(Data.game.credits)
            o.writeBoolean(true)
            /* AI Difficulty ?*/
            o.writeInt(1)
            o.writeByte(5)
            o.writeInt(Data.config.MaxUnit)
            o.writeInt(Data.config.MaxUnit)
            /* 初始单位 */
            o.writeInt(Data.game.initUnit)
            /* 倍速 */
            o.writeFloat(Data.game.income)
            /* NO Nukes */
            o.writeBoolean(Data.game.noNukes)
            o.writeBoolean(false)
            o.writeBoolean(false)
            /* 共享控制 */
            o.writeBoolean(Data.game.sharedControl)
            /* 游戏暂停 */
            o.writeBoolean(Data.game.gamePaused)
            sendPacket(o.createPacket(PacketType.TEAM_LIST))
        } catch (e: IOException) {
            Log.error("Team", e)
        }
    }

    override fun sendSurrender() {
        Data.game.playerManage.updateControlIdentifier()
        try {
            val out = GameOutputStream()

            // Player Site
            out.writeByte(player.site)

            // Is Unit Action
            out.writeBoolean(false)

            // unknown booleans
            out.writeBoolean(false)
            // Is Cancel the operation
            out.writeBoolean(false)

            // unknown
            out.writeInt(-1)
            out.writeInt(-1)
            out.writeBoolean(false)
            out.writeBoolean(false)

            // Unit count
            out.writeInt(0)

            out.writeBoolean(false)
            out.writeBoolean(false)

            out.writeLong(-1)
            // build Unit
            out.writeString("-1")

            out.writeBoolean(false)
            // multi person control check code
            out.writeShort(Data.game.playerManage.sharedControlPlayer)

            // System action
            out.writeBoolean(true)
            out.writeByte(0)
            out.writeFloat(0)
            out.writeFloat(0)
            // action type
            out.writeInt(100)

            // Unit Count
            out.writeInt(0)
            // unknown
            out.writeBoolean(false)

            val cmd = GameCommandPacket(player.site, out.getByteArray())
            Data.game.gameCommandCache.add(cmd)
            Call.sendSystemMessage(Data.i18NBundle.getinput("player.surrender", player.name))
        } catch (ignored: Exception) {
        }
    }

    override fun sendKick(reason: String) {
        val o = GameOutputStream()
        o.writeString(reason)
        sendPacket(o.createPacket(PacketType.KICK))
        Thread.sleep(100)
        disconnect()
    }

    override fun sendPing() {
        try {
            sendPacket(NetStaticData.RwHps.abstractNetPacket.getPingPacket(player))
        } catch (e: IOException) {
            numberOfRetries++
        }
    }

    @Throws(IOException::class)
    override fun sendStartGame() {
        sendServerInfo(true)
        sendPacket(NetStaticData.RwHps.abstractNetPacket.getStartGamePacket())
        if (IsUtil.notIsBlank(Data.config.StartAd)) {
            sendSystemMessage(Data.config.StartAd)
        }
    }

    @Throws(IOException::class)
    override fun receiveChat(p: Packet) {
        GameInputStream(p).use { stream ->
            val message: String = stream.readString()
            var response: CommandResponse? = null

            Log.clog("[&by {0} &fr]: &y{1}", player.name, ColorCodes.formatColors(message,true))

            // Afk Stop
            if (player.isAdmin && Threads.containsTimeTask(CallTimeTask.PlayerAfkTask)) {
                Threads.closeTimeTask(CallTimeTask.PlayerAfkTask)
                Call.sendSystemMessage(Data.i18NBundle.getinput("afk.clear", player.name))
            }

            // Msg Command
            if (message.startsWith(".") || message.startsWith("-") || message.startsWith("_")) {
                val strEnd = min(message.length, 3)
                // 提取出消息前三位 判定是否为QC命令
                response = if ("qc" == message.substring(1, strEnd)) {
                    Data.CLIENT_COMMAND.handleMessage("/" + message.substring(5), player)
                } else {
                    Data.CLIENT_COMMAND.handleMessage("/" + message.substring(1), player)
                }
            }

            if (response == null || response.type == CommandHandler.ResponseType.noCommand) {
                if (message.length > Data.config.MaxMessageLen) {
                    sendSystemMessage(Data.i18NBundle.getinput("message.maxLen"))
                    return
                }
                Data.core.admin.filterMessage(player, message)?.let { filterMessage: String ->
                    Call.sendMessage(player, filterMessage)
                    Events.fire(PlayerChatEvent(player, filterMessage))
                }
            } else if (response.type != CommandHandler.ResponseType.valid) {
                val text: String =  when (response.type) {
                    CommandHandler.ResponseType.manyArguments -> {
                        "Too many arguments. Usage: " + response.command.text + " " + response.command.paramText
                    }
                    CommandHandler.ResponseType.fewArguments -> {
                        "Too few arguments. Usage: " + response.command.text + " " + response.command.paramText
                    }
                    else -> {
                        "Unknown command. Check .help"
                    }
                }
                player.sendSystemMessage(text)
            } else {
                //
            }
        }
    }

    @Throws(IOException::class)
    override fun receiveCommand(p: Packet) {
        //PlayerOperationUnitEvent
        sync.withLock {
            var status = 0
            try {
                GameInputStream(GameInputStream(p).getDecodeBytes()).use { inStream ->
                    val outStream = GameOutputStream()
                    outStream.writeByte(inStream.readByte())
                    val boolean1 = inStream.readBoolean()
                    outStream.writeBoolean(boolean1)
                    if (boolean1) {
                        // 操作类型
                        status = inStream.readInt()
                        outStream.writeInt(status)
                        // 单位类型
                        val unitType = inStream.readInt()
                        outStream.writeInt(unitType)
                        if (unitType == -2) {
                            val nameUnit = inStream.readString()
                            //Log.error(nameUnit)
                            outStream.writeString(nameUnit)
                        }
                        val x = inStream.readFloat()
                        val y = inStream.readFloat()
                        outStream.writeFloat(x)
                        outStream.writeFloat(y)

                        // 玩家操作
                        val gameActions = GameUnitType.GameActions.from(status)
                        // 操作单位
                        val gameUnits: GameUnits = GameUnits.from(unitType)
                        // 玩家操作单位事件
                        val playerOperationUnitEvent =
                            PlayerOperationUnitEvent(player, gameActions, gameUnits, x, y)
                        Events.fire(playerOperationUnitEvent)
                        if(!playerOperationUnitEvent.resultStatus){
                            return
                        }
                        outStream.transferToFixedLength(inStream,20)
                        outStream.writeIsString(inStream)
                    }
                    outStream.transferToFixedLength(inStream,10)
                    val boolean3 = inStream.readBoolean()
                    outStream.writeBoolean(boolean3)
                    if (boolean3) {
                        // float float
                        outStream.transferToFixedLength(inStream, 8)
                    }
                    outStream.writeBoolean(inStream.readBoolean())
                    val int2 = inStream.readInt()
                    outStream.writeInt(int2)
                    for (i in 0 until int2) {
                        outStream.transferToFixedLength(inStream,8)
                    }
                    val boolean4 = inStream.readBoolean()
                    outStream.writeBoolean(boolean4)
                    if (boolean4) {
                        outStream.writeByte(inStream.readByte())
                    }
                    // Map Ping
                    val mapPing = inStream.readBoolean()
                    outStream.writeBoolean(mapPing)
                    if (mapPing) {
                        if (player.getData<String>("Summon") != null) {
                            // 单位生成
                            gameSummon(player.getData<String>("Summon")!!, inStream.readFloat(), inStream.readFloat())
                            player.removeData("Summon")
                            return
                        } else {
                            outStream.transferToFixedLength(inStream,8)
                        }
                    }
                    outStream.transferToFixedLength(inStream,8)

                    outStream.writeString(inStream.readString())
                    outStream.writeByte(inStream.readByte())
                    inStream.readShort()
                    outStream.writeShort(Data.game.playerManage.sharedControlPlayer)
                    // TODO !
                    if (!player.turnStoneIntoGold || status != GameUnitType.GameActions.BUILD.ordinal) {
                        outStream.transferTo(inStream)
                    } else {
                        if (turnStoneIntoGold.checkStatus()) {
                            sendSystemMessage("建的太频繁了 休息一下吧 !")
                            return@use
                        } else {
                            turnStoneIntoGold.count++
                            outStream.writeBoolean(true)
                            outStream.writeByte(0)
                            outStream.writeFloat(0f)
                            outStream.writeFloat(0f)
                            //action type
                            outStream.writeInt(5)
                            outStream.writeInt(0)
                            outStream.writeBoolean(false)
                        }
                    }
                    Data.game.gameCommandCache.add(GameCommandPacket(player.site, outStream.getPacketBytes()))
                }
            } catch (e: Exception) {
                Log.error(e)
            }
        }

    }

    @Throws(IOException::class)
    @Suppress("UNCHECKED_CAST")
    override fun receiveCheckPacket(packet: Packet) {
        // 忽略 可信端 的不同步
        if (player.headlessDevice) {
            return
        }

        val syncFlag = HessModuleManage.hps.gameData.verifyGameSync(packet)

        if (syncFlag) {
            sync()
        }
    }

    @Throws(IOException::class)
    override fun getGameSaveData(packet: Packet): ByteArray {
        GameInputStream(packet).use { stream ->
            val o = GameOutputStream()
            o.writeByte(stream.readByte())
            o.writeInt(stream.readInt())
            o.writeInt(stream.readInt())
            o.writeFloat(stream.readFloat())
            o.writeFloat(stream.readFloat())
            o.writeBoolean(false)
            o.writeBoolean(false)
            stream.readBoolean()
            stream.readBoolean()
            stream.readString()
            return stream.readStreamBytes()
        }
    }

    //@Throws(IOException::class)
    override fun getPlayerInfo(p: Packet): Boolean {
        try {

            GameInputStream(p).use { stream ->
                // 游戏包名
                val packegeName = stream.readString()
                // 本包协议版本
                val packetVersion = stream.readInt()
                // 客户端版本
                val version = stream.readInt()
                // 客户端协议版本
                val agreementVersion = stream.readInt()
                // 玩家游戏名
                var name = stream.readString()
                // 连接密码
                val passwd = stream.readIsString()
                // 未知
                val unknown = stream.readString()
                // uuid
                val uuid = stream.readString()
                // 未知
                val unknown1 = stream.readInt()
                // token
                val token = stream.readString()

                Log.debug("本包协议版本", packetVersion)
                Log.debug("客户端版本", version)
                Log.debug("客户端包协议版本", agreementVersion)
                Log.debug("name", name)
                Log.debug("passwd", passwd)
                Log.debug("uuid", uuid)
                Log.debug("?", stream.readInt())
                Log.debug("token", token)
                Log.debug(token, connectKey!!)

                if (supportedVersionInt > version) {
                    sendKick("Your 'Rusted Warfare' Game is out of date, please update")
                    return false
                } else if (supportedVersionInt < version) {
                    sendKick("Your 'Rusted Warfare' client is newer then the server. Server is old version")
                    return false
                }
                if (token != connectKey) {
                    Log.debug("New Player kicked","Integrity Check Failed: expectedResponse=$connectKey  clientResponse=$token")
                    sendKick("Your 'Rusted Warfare' client is different to the server. Game can not be synchronized.")
                    return false
                }

                if (Data.game.playerManage.playerGroup.size == 0 && !Player.checkHess(uuid)) {
                    sendKick("[Headless] Not loaded yet")
                    return false
                }

                // Check Passwd
                if ("" != Data.game.passwd && !Player.checkHess(uuid)) {
                    if (passwd != Data.game.passwd) {
                        try {
                            sendErrorPasswd()
                        } catch (ioException: IOException) {
                            Log.debug("Event Passwd", ioException)
                        }
                        return true
                    }
                }

                val playerJoinName = PlayerJoinNameEvent(name)
                Events.fire(playerJoinName)
                if (IsUtil.notIsBlank(playerJoinName.resultName)) {
                    name = playerJoinName.resultName
                }

                inputPassword = false
                val re = AtomicBoolean(false)
                if (Data.game.isStartGame) {
                    Data.game.playerManage.playerAll.eachAllFind({ i: Player -> i.uuid == uuid }) { e: Player ->
                        re.set(true)
                        this.player = e
                        player.con = this
                        Data.game.playerManage.playerGroup.add(e)
                    }
                    if (!re.get()) {
                        if (IsUtil.isBlank(Data.config.StartPlayerAd)) {
                            sendKick("游戏已经开局 请等待 # The game has started, please wait")
                        } else {
                            sendKick(Data.config.StartPlayerAd)
                        }
                        return false
                    }
                } else {
                    if (Data.game.playerManage.playerGroup.size >= Data.game.maxPlayer) {
                        if (IsUtil.isBlank(Data.config.MaxPlayerAd)) {
                            sendKick("服务器没有位置 # The server has no free location")
                        } else {
                            sendKick(Data.config.MaxPlayerAd)
                        }
                        return false
                    }
                    val localeUtil = Data.i18NBundleMap["CN"]
                    /*
                    if (Data.game.ipCheckMultiLanguageSupport) {
                        val rec = Data.ip2Location.IPQuery(connectionAgreement.ip)
                        if ("OK" != rec.status) {
                            localeUtil = Data.localeUtilMap[rec.countryShort]
                        }
                    }
                     */
                    player = Data.game.playerManage.addPlayer(this, uuid, name, localeUtil)
                }

                Call.sendTeamData()
                sendServerInfo(true)

                if (IsUtil.notIsBlank(Data.config.EnterAd)) {
                    sendSystemMessage(Data.config.EnterAd)
                }
                if (re.get()) {
                    reConnect()
                }

                connectionAgreement.add(NetStaticData.groupNet)

                Events.fire(PlayerJoinEvent(player))

                return true
            }
        } finally {
            connectKey = null
        }
    }

    @Throws(IOException::class)
    override fun registerConnection(p: Packet) {
        // 生成随机Key;
        val keyLen = 6
        val key = RandomUtil.getRandomIntString(keyLen).toInt()
        connectKey = Game.connectKeyP10(key)
        GameInputStream(p).use { stream ->
            // Game Pkg Name
            stream.readString()
            // 返回都是1 有啥用
            var playerCount = stream.readInt()
            stream.readInt()
            stream.readInt()
            val o = GameOutputStream()
            o.writeString(Data.SERVER_ID)
            o.writeInt(1)
            o.writeInt(supportedVersionInt)
            o.writeInt(supportedVersionInt)
            o.writeString("com.corrodinggames.rts.server")
            o.writeString(Data.core.serverConnectUuid)
            o.writeInt(key)
            sendPacket(o.createPacket(PacketType.PREREGISTER_INFO))
        }
    }

    override fun gameSummon(unit: String, x: Float, y: Float) {
        sync.lock()
        try {
            Data.game.gameCommandCache.add(NetStaticData.RwHps.abstractNetPacket.gameSummonPacket(player.site,unit,x, y))
        } catch (e: Exception) {
            Log.error(e)
        } finally {
            sync.unlock()
        }
    }

    override fun sendErrorPasswd() {
        try {
            val o = GameOutputStream()
            o.writeInt(0)
            sendPacket(o.createPacket(PacketType.PASSWD_ERROR))
            inputPassword = true
        } catch (e: Exception) {
            Log.error("[Player] sendErrorPasswd", e)
        }
    }

    override fun disconnect() {
        if (super.isDis) {
            return
        }
        super.isDis = true
        if (this::player.isInitialized) {
            Data.game.playerManage.playerGroup.remove(player)
            // DisConnect
            player.ping = -1
            if (!Data.game.isStartGame) {
                Data.game.playerManage.playerAll.remove(player)
                player.clear()
                Data.game.playerManage.removePlayerArray(player)
            }
            Events.fire(PlayerLeaveEvent(player))
        }
        super.close(NetStaticData.groupNet)
    }

    override fun sendGameSave(packet: Packet) {
        sendPacket(packet)
    }

    override fun reConnect() {
        try {
            if (!Data.config.ReConnect) {
                sendKick("不支持重连 # Does not support reconnection")
                return
            }
            super.isDis = false
            sendPacket(NetStaticData.RwHps.abstractNetPacket.getStartGamePacket())
            sync()
        } catch (e: Exception) {
            Log.error("[Player] Send GameSave ReConnect Error", e)
        }
    }

    override fun sync() {
        try {
            Data.game.gameReConnectPaused = true
            sendSystemMessage("同步中 请耐心等待 不要退出 期间会短暂卡住！！")
            sendPacket(HessModuleManage.hps.gameData.getGameData())
        } catch (e: Exception) {
            Log.error("[Player] Send GameSave ReConnect Error", e)
        } finally {
            Data.game.gameReConnectPaused = false
        }
    }



    override fun sendRelayServerType(msg: String, run: ((String) -> Unit)?) {
        try {
            sendPacket(relayServerTypeInternal(msg))

            relaySelect = run

            inputPassword = true
        } catch (e: Exception) {
            Log.error(e)
        }
    }

    override fun sendRelayServerTypeReply(packet: Packet) {
        try {
            val id = relayServerTypeReplyInternal(packet)

            // 解决被扫列表中的RELAY 而不是主动调用
            if (relaySelect == null) {
                disconnect()
                return
            }
            relaySelect!!(id)
        } catch (e: Exception) {
            Log.error(e)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        if (player != (other as GameVersionServer).player) {
            return false
        }

        return false
    }

    override fun hashCode(): Int {
        return player.hashCode()
    }
}