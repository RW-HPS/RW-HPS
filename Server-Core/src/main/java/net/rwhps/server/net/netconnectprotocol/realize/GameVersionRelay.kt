/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.netconnectprotocol.realize

import com.vdurmont.emoji.EmojiManager
import net.rwhps.server.data.global.Cache
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.global.Relay
import net.rwhps.server.data.player.PlayerRelay
import net.rwhps.server.data.totalizer.TimeAndNumber
import net.rwhps.server.game.GameMaps
import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.io.output.CompressOutputStream
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.core.ConnectionAgreement
import net.rwhps.server.net.core.DataPermissionStatus.RelayStatus
import net.rwhps.server.net.core.NetConnectProofOfWork
import net.rwhps.server.net.core.server.AbstractNetConnect
import net.rwhps.server.net.core.server.AbstractNetConnectData
import net.rwhps.server.net.core.server.AbstractNetConnectRelay
import net.rwhps.server.net.netconnectprotocol.UniversalAnalysisOfGamePackages
import net.rwhps.server.net.netconnectprotocol.internal.relay.fromRelayJumpsToAnotherServer
import net.rwhps.server.net.netconnectprotocol.internal.relay.relayServerInitInfo
import net.rwhps.server.net.netconnectprotocol.internal.relay.relayServerTypeInternal
import net.rwhps.server.net.netconnectprotocol.internal.relay.relayServerTypeReplyInternal
import net.rwhps.server.struct.ObjectMap
import net.rwhps.server.util.GameOtherUtil.getBetaVersion
import net.rwhps.server.util.IsUtil
import net.rwhps.server.util.PacketType
import net.rwhps.server.util.Time
import net.rwhps.server.util.alone.annotations.MainProtocolImplementation
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.log.Log.debug
import net.rwhps.server.util.log.Log.error
import java.io.IOException
import java.util.*
import java.util.stream.IntStream

/**
 * Many thanks to them for providing cloud computing for the project
 * This is essential to complete the RW-HPS Relay test
 * @Thanks : [SimpFun Cloud](https://vps.tiexiu.xyz)
 * @Thanks : [Github 1dNDN](https://github.com/1dNDN)
 *
 * This test was done on :
 * Relay-CN (V. 6.1.0)
 * 2022.7.22 10:00
 */

/**
 * Relay protocol implementation
 * Direct forwarding consumes more bandwidth and the same effect as using VPN forwarding
 *
 * @property permissionStatus       Connection authentication status
 * @property netConnectAuthenticate Connection validity verification
 * @property relay                  Relay instance
 * @property site                   Connect the forwarding location within the RELAY
 * @property cachePacket            Cached Package
 * @property relaySelect            Function1<String, Unit>?
 * @property name                   player's name
 * @property registerPlayerId       UUID-Hash code after player registration
 * @property noUp                   Boolean
 * @property betaGameVersion        Is it a beta version
 * @property clientVersion          clientVersion
 * @property playerRelay            PlayerRelay?
 * @property version                Protocol version
 * @constructor
 *
 * @author RW-HPS/Dr
 */
@MainProtocolImplementation
open class GameVersionRelay(connectionAgreement: ConnectionAgreement) : AbstractNetConnect(connectionAgreement), AbstractNetConnectData, AbstractNetConnectRelay {
    override var permissionStatus: RelayStatus = RelayStatus.InitialConnection
        internal set

    /** Client computing proves non-Bot */
    private var netConnectAuthenticate: NetConnectProofOfWork? = null

    override var relay: Relay? = null
        protected set

    protected var site = 0

    protected var cachePacket: Packet? = null
        private set

    protected var relaySelect: ((String) -> Unit)? = null

    override var name = "NOT NAME"
        protected set
    override var registerPlayerId: String? = null
        protected set
    override var betaGameVersion = false
        protected set
    override var clientVersion = 151
        protected set

    var playerRelay: PlayerRelay? = null
        internal set

    // 172
    protected val version2 = 999

    // Lazy loading reduces memory usage
    /** The server kicks the player's time data */
    lateinit var relayKickData: ObjectMap<String,Int>
        protected set
    /** Server exits player data */
    lateinit var relayPlayersData: ObjectMap<String,PlayerRelay>
        protected set

    protected val bindSimpFun = TimeAndNumber(60,5)
    protected var qqName: String = ""

    override fun setCachePacket(packet: Packet) {
        cachePacket = packet
    }

    override fun setlastSentPacket(packet: Packet) {
        /* 此协议下不被使用 */
    }

    override val version: String
        get() = "1.14 RELAY"

    override fun sendRelayServerInfo() {
        val cPacket: Packet? = Cache.packetCache["sendRelayServerInfo"]
        if (IsUtil.notIsBlank(cPacket)) {
            sendPacket(cPacket!!)
            return
        }
        try {
            val packetCache = relayServerInitInfo()
            Cache.packetCache.put("sendRelayServerInfo",packetCache)
            sendPacket(packetCache)
        } catch (e: Exception) {
            error(e)
        }
    }

    @Throws(IOException::class)
    override fun relayDirectInspection(relay: Relay?) {
        GameInputStream(cachePacket!!).use { inStream ->
            inStream.readString()
            val packetVersion = inStream.readInt()
            clientVersion = inStream.readInt()
            betaGameVersion = getBetaVersion(clientVersion)

            if (packetVersion >= 1) {
                inStream.skip(4)
            }
            var queryString = ""
            if (packetVersion >= 2) {
                queryString = inStream.readIsString()
            }
            if (packetVersion >= 3) {
                // Player Name
                inStream.readString()
            }
            if (relay == null) {
                if (IsUtil.isBlank(queryString) || "RELAYCN".equals(queryString, ignoreCase = true)) {
                    if (qqName.isNotEmpty()) {
                        sendRelayServerType(Data.i18NBundle.getinput("relay.hi.bind", qqName))
                    } else {
                        sendRelayServerType(Data.i18NBundle.getinput("relay.hi", Data.SERVER_CORE_VERSION))
                    }
                } else {
                    idCustom(queryString)
                }
            } else {
                this.relay = relay
                addRelayConnect()
                this.relay!!.setAddSize()
            }
        }
    }

    override fun sendVerifyClientValidity() {
        netConnectAuthenticate = Relay.randPow
        val netConnectAuthenticate: NetConnectProofOfWork = netConnectAuthenticate!!
        val authenticateType = netConnectAuthenticate.authenticateType.toInt()
        debug(authenticateType)
        try {
            val o = GameOutputStream()
            // 返回相同
            o.writeInt(netConnectAuthenticate.resultInt)
            o.writeInt(authenticateType)
            if (authenticateType == 0 || netConnectAuthenticate.authenticateType in 2..4 || authenticateType == 6) {
                o.writeBoolean(true)
                o.writeInt(netConnectAuthenticate.initInt_1)
            } else {
                o.writeBoolean(false)
            }
            if (authenticateType == 1 || netConnectAuthenticate.authenticateType in 2..4) {
                o.writeBoolean(true)
                o.writeInt(netConnectAuthenticate.initInt_2)
            } else {
                o.writeBoolean(false)
            }
            if (netConnectAuthenticate.authenticateType in 5..6) {
                o.writeString(netConnectAuthenticate.outcome)
                o.writeString(netConnectAuthenticate.fixedInitial)
                o.writeInt(netConnectAuthenticate.maximumNumberOfCalculations)
            }

            o.writeBoolean(false)

            sendPacket(o.createPacket(PacketType.RELAY_POW))
        } catch (e: Exception) {
            error(e)
        }
    }

    override fun receiveVerifyClientValidity(packet: Packet): Boolean {
        try {
            GameInputStream(packet).use { inStream ->
                if (netConnectAuthenticate != null) {
                    if (netConnectAuthenticate!!.check(inStream.readInt(),inStream.readInt(),inStream.readString())) {
                        netConnectAuthenticate = null
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            error(e)
        }
        return false
    }

    override fun sendRelayServerType(msg: String, run: ((String) -> Unit)?) {
        try {
            sendPacket(relayServerTypeInternal(msg))

            relaySelect = run

            inputPassword = true
        } catch (e: Exception) {
            error(e)
        }
    }

    @Throws(IOException::class)
    override fun receiveChat(p: Packet) {
        GameInputStream(p).use { inStream ->
            if (playerRelay != null) {
                val message: String = inStream.readString()

                if (relay == null || relay!!.allmute) {
                    return
                }
/*
// RCN 选择性功能
                if (relay!!.relayData.uplistStatus != Relay.RelayData.UpListStatus.UpIng && (!relay!!.isStartGame || relay!!.startGameTime > Time.concurrentSecond())) {
                    if (Data.banWord.badWord(message).size > 0) {
                        if (playerRelay!!.messageCount.checkStatus()) {
                            // KICK 玩家
                            relay!!.admin!!.relayKickData.put("KICK$registerPlayerId",Time.concurrentSecond()+120)
                            relay!!.admin!!.relayKickData.put("KICK${connectionAgreement.ipLong24}", Time.concurrentSecond() + 120)
                            kick("含有屏蔽词的话不应该连续发送")
                            return
                        } else {
                            // 提醒玩家 并累计数
                            sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket("您已经连续多次发言包含屏蔽词 您不应该这样做", "RELAY_CN-Check", 5))
                            playerRelay!!.messageCount.count++
                            return
                        }
                    }
                }
*/
                if (message.startsWith(".") || message.startsWith("-")) {
                    val response = Data.RELAY_COMMAND.handleMessage(message, this)
                    if (response == null || response.type == CommandHandler.ResponseType.noCommand) {
                    } else if (response.type == CommandHandler.ResponseType.valid) {
                        return
                    } else if (response.type != CommandHandler.ResponseType.valid) {
                        val text: String = when (response.type) {
                            CommandHandler.ResponseType.manyArguments -> "Too many arguments. Usage: " + response.command.text + " " + response.command.paramText
                            CommandHandler.ResponseType.fewArguments -> "Too few arguments. Usage: " + response.command.text + " " + response.command.paramText
                            else -> {
                                sendResultPing(p)
                                return
                            }
                        }
                        sendPacket(NetStaticData.RwHps.abstractNetPacket.getSystemMessagePacket(text))
                    }
                } else {
                    // 判定这句和最后一次发言的相同程度
                    if (playerRelay!!.lastSentMessage == message) {
                        // 检测是否达到上限 60s 五次
                        if (playerRelay!!.messageSimilarityCount.checkStatus()) {
                            // KICK 玩家
                            relay!!.admin!!.relayKickData.put("KICK$registerPlayerId",Time.concurrentSecond()+120)
                            relay!!.admin!!.relayKickData.put("KICK${connectionAgreement.ipLong24}", Time.concurrentSecond() + 120)
                            kick("相同的话不应该连续发送")
                            return
                        } else {
                            // 提醒玩家 并累计数
                            sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket("您已经连续多次发言重复 您不应该这样做", "RELAY_CN-Check", 5))
                            playerRelay!!.messageSimilarityCount.count++
                            return
                        }
                    }
                    playerRelay!!.lastSentMessage = message
                }

                sendResultPing(p)
            } else {
                disconnect()
            }
        }
    }

    override fun sendRelayServerTypeReply(packet: Packet) {
        try {
            inputPassword = false

            val id = relayServerTypeReplyInternal(packet)
            if (relaySelect == null) {
                idCustom(id)
            } else {
                relaySelect!!(id)
            }
        } catch (e: Exception) {
            error(e)
        }
    }

    override fun sendRelayServerId() {
        try {
            inputPassword = false
            if (relay == null) {
                //Log.debug("sendRelayServerId","Relay == null");
                relay = NetStaticData.relay
            }
            if (relay!!.admin != null) {
                // Log.debug("sendRelayServerId","Admin != null");
                relay!!.removeAbstractNetConnect(site)
                relayKickData = relay!!.admin!!.relayKickData
                relayPlayersData = relay!!.admin!!.relayPlayersData
            } else {
                relayKickData = ObjectMap()
                relayPlayersData = ObjectMap()
            }

            //Log.debug("sendRelayServerId","Set Admin");
            relay!!.admin = this
            //Log.debug(this == relay.getAdmin());
            val o = GameOutputStream()
            if (clientVersion >= version2) {
                o.writeByte(2)
                o.writeBoolean(true)
                o.writeBoolean(true)
                o.writeBoolean(true)
                o.writeString(relay!!.serverUuid)
                o.writeBoolean(relay!!.isMod) //MOD
                o.writeBoolean(false)
                o.writeBoolean(true)
                o.writeString("{{RW-HPS Relay}}.Room ID : ${Data.configRelayPublish.MainID}" + relay!!.id)
                o.writeBoolean(false)
                o.writeIsString(registerPlayerId)
            } else {
                o.writeByte(1)
                o.writeBoolean(true)
                o.writeBoolean(true)
                o.writeBoolean(true)
                o.writeString(relay!!.serverUuid)
                o.writeBoolean(relay!!.isMod) //MOD
                // List OPEN
                o.writeBoolean(false)
                o.writeBoolean(true)
                o.writeString("{{RW-HPS Relay}}.Room ID : ${Data.configRelayPublish.MainID}" + relay!!.id)
                // 多播
                o.writeBoolean(false)
            }

            sendPacket(o.createPacket(PacketType.FORWARD_HOST_SET)) //+108+140
            //getRelayT4(Data.localeUtil.getinput("relay.server.admin.connect",relay.getId()));
            sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(Data.i18NBundle.getinput("relay.server.admin.connect", Data.configRelayPublish.MainID+relay!!.id, Data.configRelayPublish.MainID+relay!!.internalID.toString()), "RELAY_CN-ADMIN", 5))
            sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(Data.i18NBundle.getinput("relay", Data.configRelayPublish.MainID+relay!!.id), "RELAY_CN-ADMIN", 5))
            //ping();

            //debug(name)
            if (name.equals("SERVER", ignoreCase = true) || name.equals("RELAY", ignoreCase = true)) {
                relay!!.groupNet.disconnect() // Close Room
                disconnect() // Close Connect & Reset Room
            }
        } catch (e: Exception) {
            error(e)
        }
    }

    override fun getRelayT4(msg: String) {
        try {
            val o = GameOutputStream()
            // 理论上是随机数？
            //o.writeString("RW-HPS Relay TEST?");
            o.writeString(msg)
            o.writeByte(0)
            sendPacket(o.createPacket(PacketType.CHAT_RECEIVE))
        } catch (e: Exception) {
            error(e)
        }
    }

    override fun getPingData(packet: Packet) {
        try {
            GameInputStream(packet).use { inStream ->
                val out = GameOutputStream()
                //out.writeBytes(inStream.readNBytes(8))
                out.transferToFixedLength(inStream,8)
                out.writeByte(1)
                out.writeByte(60)
                sendPacket(out.createPacket(PacketType.HEART_BEAT_RESPONSE))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun addGroup(packet: Packet) {
        relay!!.groupNet.broadcastAndUDP(packet)
    }

    override fun addGroupPing(packet: Packet) {
        try {
            relay!!.groupNet.broadcastAndUDP(packet)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun addRelayConnect() {
        permissionStatus = RelayStatus.PlayerPermission

        try {
            inputPassword = false
            if (relay == null) {
                relay = NetStaticData.relay
            }
            relay!!.setAddSite()
            relay!!.setAbstractNetConnect(this)
            site = relay!!.getSite()
            val o = GameOutputStream()
            if (clientVersion >= version2) {
                o.writeByte(1)
                o.writeInt(site)
                // ?
                o.writeString(registerPlayerId!!)
                //o.writeBoolean(false)
                // User UUID
                o.writeIsString(registerPlayerId!!)
                o.writeIsString(ip)
                relay!!.admin!!.sendPacket(o.createPacket(PacketType.FORWARD_CLIENT_ADD))
                //Log.clog("172")
            } else {
                o.writeByte(0)
                o.writeInt(site)
                o.writeString(registerPlayerId!!)
                o.writeBoolean(false)
                relay!!.admin!!.sendPacket(o.createPacket(PacketType.FORWARD_CLIENT_ADD))
            }

            val o1 = GameOutputStream()
            o1.writeInt(site)
            o1.writeInt(cachePacket!!.bytes.size + 8)
            o1.writeInt(cachePacket!!.bytes.size)
            o1.writeInt(PacketType.PREREGISTER_INFO_RECEIVE.typeInt)
            o1.writeBytes(cachePacket!!.bytes)
            relay!!.admin!!.sendPacket(o1.createPacket(PacketType.PACKET_FORWARD_CLIENT_FROM))
            connectionAgreement.add(relay!!.groupNet)
            sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(Data.i18NBundle.getinput("relay", Data.configRelayPublish.MainID+relay!!.id), "RELAY_CN-ADMIN", 5))
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            //cachePacket = null;
        }
    }

    override fun relayRegisterConnection(packet: Packet) {
        if (registerPlayerId.isNullOrBlank()) {
            try {
                GameInputStream(packet).use { stream ->
                    stream.readString()
                    stream.skip(12)
                    name = stream.readString()
                    stream.readIsString()
                    stream.readString()
                    if (IsUtil.isBlank(registerPlayerId)) {
                        registerPlayerId = stream.readString()
                    }
                }
            } catch (e: Exception) {
            }
        }
        if (permissionStatus.ordinal >= RelayStatus.PlayerPermission.ordinal) {
            // Relay-EX
            if (playerRelay == null) {
                playerRelay = relay!!.admin!!.relayPlayersData[registerPlayerId] ?: PlayerRelay(this, registerPlayerId!!, name).also {
                    relay!!.admin!!.relayPlayersData.put(registerPlayerId,it)
                }

                playerRelay!!.nowName = name
                playerRelay!!.disconnect = false


                if (relay!!.admin!!.relayKickData.containsKey("BAN$ip")) {
                    kick("您被这个房间BAN了 请稍等一段时间 或者换一个房间")
                    return
                }
                if (relay!!.admin!!.relayKickData.containsKey("KICK$registerPlayerId")) {
                    if (relay!!.admin!!.relayKickData["KICK$registerPlayerId"] > Time.concurrentSecond()) {
                        kick("您被这个房间踢出了 请稍等一段时间 或者换一个房间")
                        return
                    } else {
                        relay!!.admin!!.relayKickData.remove("KICK$registerPlayerId")
                        relay!!.admin!!.relayKickData.remove("KICK${connectionAgreement.ipLong24}")
                    }
                }
            }
            sendResultPing(packet)
        }
    }

    override fun addReRelayConnect() {
        try {
            val o = GameOutputStream()
            if (clientVersion >= version2) {
                o.writeByte(1)
                o.writeInt(site)
                // ?
                o.writeString(registerPlayerId!!)
                // User UUID
                o.writeIsString(registerPlayerId!!)
                o.writeIsString(ip)
                relay!!.admin!!.sendPacket(o.createPacket(PacketType.FORWARD_CLIENT_ADD))
                //Log.clog("172")
            } else {
                o.writeByte(0)
                o.writeInt(site)
                o.writeString(registerPlayerId!!)
                o.writeBoolean(false)
                relay!!.admin!!.sendPacket(o.createPacket(PacketType.FORWARD_CLIENT_ADD))
            }
            relay!!.admin!!.sendPacket(o.createPacket(PacketType.FORWARD_CLIENT_ADD))
            val o1 = GameOutputStream()
            o1.writeInt(site)
            o1.writeInt(cachePacket!!.bytes.size + 8)
            o1.writeInt(cachePacket!!.bytes.size)
            o1.writeInt(160)
            o1.writeBytes(cachePacket!!.bytes)
            relay!!.admin!!.sendPacket(o1.createPacket(PacketType.PACKET_FORWARD_CLIENT_FROM))
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            //cachePacket = null;
        }
    }

    override fun addRelaySend(packet: Packet) {
        try {
            GameInputStream(packet).use { inStream ->
                val target = inStream.readInt()
                val type = inStream.readInt()
                if (IntStream.of(PacketType.DISCONNECT.typeInt, PacketType.HEART_BEAT.typeInt).anyMatch { i: Int -> i == type }) {
                    return
                }
                inStream.skip(4)
                val bytes = inStream.readAllBytes()
                val abstractNetConnect = relay!!.getAbstractNetConnect(target)
                if (PacketType.KICK.typeInt == type) {
                    val gameOutputStream = GameOutputStream()
                    gameOutputStream.writeString(GameInputStream(bytes).readString().replace("\\d".toRegex(), ""))
                    abstractNetConnect?.sendPacket(gameOutputStream.createPacket(type))
                    relayPlayerDisconnect()
                    return
                }
                abstractNetConnect?.sendPacket(Packet(type, bytes))

                when (type) {
                    PacketType.KICK.typeInt -> {
                        abstractNetConnect?.relayPlayerDisconnect()
                    }
                    PacketType.TEAM_LIST.typeInt -> {
                        if (!relay!!.isStartGame) {
                            abstractNetConnect?.let { UniversalAnalysisOfGamePackages.getPacketTeamData(GameInputStream(bytes,it.clientVersion),it.playerRelay!!) }
                        }
                        return
                    }
                    PacketType.RETURN_TO_BATTLEROOM.typeInt -> {
                        relay!!.isStartGame = false
                    }
                    else -> {}
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (_: NullPointerException) {
        }
    }

    override fun sendResultPing(packet: Packet) {
        try {
            val o = GameOutputStream()
            o.writeInt(site)
            o.writeInt(packet.bytes.size + 8)
            o.writeInt(packet.bytes.size)
            o.writeInt(packet.type.typeInt)
            o.writeBytes(packet.bytes)
            relay!!.admin!!.sendPacket(o.createPacket(PacketType.PACKET_FORWARD_CLIENT_FROM))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun sendCustomPacket(packet: Packet) {
        try {
            sendPacket(packet)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun relayPlayerDisconnect() {
        try {
            val out = GameOutputStream()
            out.writeByte(0)
            out.writeInt(site)
            relay!!.admin!!.sendPacket(out.createPacket(PacketType.FORWARD_CLIENT_REMOVE))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun multicastAnalysis(packet: Packet) {
        // Protocol not supported
    }

    internal fun kick(msg: String) {
        val o = GameOutputStream()
        o.writeString(msg)
        sendPacket(o.createPacket(PacketType.KICK))
        disconnect()
    }

    override fun disconnect() {
        if (super.isDis) {
            return
        }
        super.isDis = true
        if (relay != null) {
            relay!!.setRemoveSize()
            if (this !== relay!!.admin) {
                relay!!.removeAbstractNetConnect(site)
                try {
                    relay!!.updateMinSize()
                    sendResultPing(NetStaticData.RwHps.abstractNetPacket.getExitPacket())
                } catch (e: IOException) {
                    error("[Relay disconnect] Send Exited", e)
                }

                playerRelay!!.disconnect = true
                if (!relay!!.isStartGame) {
                    relay!!.admin!!.relayPlayersData.remove(registerPlayerId)
                }
            } else {
                Relay.serverRelayIpData.remove(ip)
                // 房间开始游戏 或者 在列表
                if (relay!!.isStartGame) {
                    if (relay!!.getSize() > 0) {
                        // Move Room Admin
                        adminMoveNew()
                    }
                } else {
                    // Close Room
                    relay!!.groupNet.disconnect()
                }
            }
            if (relay!!.getSize() <= 0 && !relay!!.closeRoom) {
                debug("[Relay] Gameover")
                relay!!.re()
            }
            super.close(relay!!.groupNet)
        } else {
            super.close(null)
        }
    }

    private fun idCustom(inId: String) {
        // 过滤 制表符 空格 换行符
        var id = inId.replace("\\s".toRegex(), "")

        if (id.isEmpty()) {
            sendRelayServerType(Data.i18NBundle.getinput("relay.server.error.id", "什么都没输入就点击确认"))
            return
        }
        if (EmojiManager.containsEmoji(id)) {
            sendRelayServerType(Data.i18NBundle.getinput("relay.server.error.id", "不能使用Emoji"))
            return
        }


        if (id.startsWith("RA")) {
            id = id.substring(1)
            sendPacket(fromRelayJumpsToAnotherServer("${id[0]}.relay.der.kim/${id.substring(0)}"))
            return
        } else if ("R".equals(id[0].toString(), ignoreCase = true)) {
            id = id.substring(1)
        }

        if (id.isEmpty()) {
            sendRelayServerType(Data.i18NBundle.getinput("relay.server.no", "R"))
            return
        }

        if ("C".equals(id[0].toString(), ignoreCase = true)) {
            id = id.substring(1)
            if (id.isEmpty()) {
                sendRelayServerType(Data.i18NBundle.getinput("relay.id.re"))
                return
            }
            if ("M".equals(id[0].toString(), ignoreCase = true)) {
                id = id.substring(1)
                if (checkLength(id)) {
                    if (Relay.getCheckRelay(id) || id.startsWith("A",true) || id.startsWith("B",true)) {
                        sendRelayServerType(Data.i18NBundle.getinput("relay.id.re"))
                    } else {
                        newRelayId(id, true)
                    }
                }
            } else {
                if (checkLength(id)) {
                    if (Relay.getCheckRelay(id) || id.startsWith("A",true) || id.startsWith("B",true)) {
                        sendRelayServerType(Data.i18NBundle.getinput("relay.id.re"))
                    } else {
                        newRelayId(id, false)
                    }
                }
            }
            return
        }

        var mods = false
        var newRoom = true

        if (id.startsWith("new", ignoreCase = true)) {
            id = id.substring(3)
        } else if (id.startsWith("mod", ignoreCase = true)) {
            mods = true
            id = id.substring(3)
        } else if (id.startsWith("mods", ignoreCase = true)) {
            mods = true
            id = id.substring(4)
        } else {
            newRoom = false
        }

        val custom = CustomRelayData()

        try {
            if (id.startsWith("P", ignoreCase = true)) {
                id = id.substring(1)
                val arry = if (id.contains("，")) id.split("，") else id.split(",")
                custom.MaxPlayerSize = arry[0].toInt()
                if  (custom.MaxPlayerSize !in 0..100) {
                    sendRelayServerType(Data.i18NBundle.getinput("relay.id.maxPlayer.re"))
                    return
                }
                if (arry.size > 1) {
                    if (arry[1].contains("I", ignoreCase = true)) {
                        val ay = arry[1].split("I", ignoreCase = true)
                        if (ay.size > 1) {
                            custom.MaxUnitSizt = ay[0].toInt()
                            custom.Income = ay[1].toFloat()
                        } else {
                            custom.Income = ay[0].toFloat()
                        }
                    } else {
                        custom.MaxUnitSizt = arry[1].toInt()
                    }
                    if  (custom.MaxUnitSizt !in 0..Int.MAX_VALUE) {
                        sendRelayServerType(Data.i18NBundle.getinput("relay.id.maxUnit.re"))
                        return
                    }
                }
            }
            if (id.startsWith("I", ignoreCase = true)) {
                id = id.substring(1)
                val arry = if (id.contains("，")) id.split("，") else id.split(",")
                custom.Income = arry[0].toFloat()
                if  (custom.Income !in 0F..Float.MAX_VALUE) {
                    sendRelayServerType(Data.i18NBundle.getinput("relay.id.income.re"))
                    return
                }
            }
        } catch(e: NumberFormatException) {
            sendRelayServerType(Data.i18NBundle.getinput("relay.server.error", e.message))
            return
        }

        if (newRoom) {
            newRelayId(mods,custom)
            if (custom.MaxPlayerSize != -1 || custom.MaxUnitSizt != 200) {
                sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket("自定义人数: ${custom.MaxPlayerSize} 自定义单位: ${custom.MaxUnitSizt}", "RELAY_CN-Custom", 5))
            }
        } else {
            try {
                if (id.contains(".")) {
                    sendRelayServerType(Data.i18NBundle.getinput("relay.server.error", "不能包含 [ . ]"))
                    return
                }
                relay = Relay.getRelay(id)
                if (relay != null) {
                    addRelayConnect()
                    relay!!.setAddSize()
                } else {
                    sendRelayServerType(Data.i18NBundle.getinput("relay.server.no", id))
                }
            } catch (e: Exception) {
                debug(e)
                sendRelayServerType(Data.i18NBundle.getinput("relay.server.error", e.message))
            }
        }
    }

    private fun checkLength(str: String): Boolean {
        if (str.length > 7 || str.length < 3) {
            sendRelayServerType(Data.i18NBundle.getinput("relay.id.re"))
            return false
        }
        if (str.contains(".")) {
            sendRelayServerType(Data.i18NBundle.getinput("relay.server.error", "不能包含 [ . ]"))
            return false
        }
        return true
    }

    private fun adminMoveNew() {
        relay!!.updateMinSize()
        relay!!.getAbstractNetConnect(relay!!.minSize)!!.sendRelayServerId()
        relay!!.abstractNetConnectIntMap.values.forEach { obj: GameVersionRelay -> obj.addReRelayConnect() }
    }

    private fun newRelayId(mod: Boolean, customRelayData: CustomRelayData) {
        newRelayId(null, mod, customRelayData)
    }

    private fun newRelayId(id: String?, mod: Boolean, customRelayData: CustomRelayData = CustomRelayData()) {
        val maxPlayer = if (customRelayData.MaxPlayerSize == -1) 10 else customRelayData.MaxPlayerSize

        relay = if (IsUtil.isBlank(id)) {
            Relay.getRelay( playerName = name, isMod = mod, betaGameVersion = betaGameVersion, version = clientVersion, maxPlayer = maxPlayer)
        } else {
            Relay.getRelay( id!!, name, mod, betaGameVersion, clientVersion, maxPlayer)
        }

        relay!!.isMod = mod

        if (customRelayData.MaxPlayerSize != -1 || customRelayData.Income != 1F) {
            customModePlayerSize(customRelayData)
        }

        sendRelayServerId()
        relay!!.setAddSize()
    }

    private fun customModePlayerSize(customRelayData: CustomRelayData) {
        val registerServer = GameOutputStream()
        registerServer.writeString(Data.SERVER_ID)
        registerServer.writeInt(1)
        registerServer.writeInt(clientVersion)
        registerServer.writeInt(clientVersion)
        registerServer.writeString("com.corrodinggames.rts.server")
        registerServer.writeString(relay!!.serverUuid)
        registerServer.writeInt("Dr @ 2022".hashCode())
        sendPacket(registerServer.createPacket(PacketType.PREREGISTER_INFO))

        val o2 = GameOutputStream()
        o2.writeString(Data.SERVER_ID_RELAY)
        o2.writeInt(clientVersion)
        o2.writeInt(GameMaps.MapType.customMap.ordinal)
        o2.writeString("RW-HPS RELAY Custom Mode")
        o2.writeInt(Data.game.credits)
        o2.writeInt(Data.game.mist)
        o2.writeBoolean(true)
        o2.writeInt(1)
        o2.writeByte(0)
        o2.writeBoolean(false)
        o2.writeBoolean(false)
        sendPacket(o2.createPacket(PacketType.SERVER_INFO))

        val o = GameOutputStream()
        o.writeInt(0)
        o.writeBoolean(false)
        /* RELAY Custom MaxPlayer */
        o.writeInt(customRelayData.MaxPlayerSize)
        o.flushEncodeData(CompressOutputStream.getGzipOutputStream("teams", true).also { for (i in 0 until customRelayData.MaxPlayerSize) it.writeBoolean(false)})
        o.writeInt(Data.game.mist)
        o.writeInt(Data.game.credits)
        o.writeBoolean(true)
        o.writeInt(1)
        o.writeByte(5)
        // RELAY Custom MaxUnit
        o.writeInt(customRelayData.MaxUnitSizt)
        o.writeInt(customRelayData.MaxUnitSizt)
        o.writeInt(Data.game.initUnit)
        // RELAY Custom income
        o.writeFloat(customRelayData.Income)
        o.writeBoolean(Data.game.noNukes)
        o.writeBoolean(false)
        o.writeBoolean(false)
        o.writeBoolean(Data.game.sharedControl)
        o.writeBoolean(Data.game.gamePaused)
        sendPacket(o.createPacket(PacketType.TEAM_LIST))
    }

    private data class CustomRelayData(
        var MaxPlayerSize: Int   = -1,
        var MaxUnitSizt:   Int   = 200,
        var Income:        Float = 1f
    )

}