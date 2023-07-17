/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.netconnectprotocol.realize

import com.vdurmont.emoji.EmojiManager
import net.rwhps.server.data.HessModuleManage
import net.rwhps.server.data.global.Cache
import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.global.Relay
import net.rwhps.server.data.player.PlayerRelay
import net.rwhps.server.game.GameMaps
import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.io.output.CompressOutputStream
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.core.ConnectionAgreement
import net.rwhps.server.net.core.DataPermissionStatus.RelayStatus
import net.rwhps.server.net.core.server.AbstractNetConnect
import net.rwhps.server.net.core.server.AbstractNetConnectData
import net.rwhps.server.net.core.server.AbstractNetConnectRelay
import net.rwhps.server.net.netconnectprotocol.UniversalAnalysisOfGamePackages
import net.rwhps.server.net.netconnectprotocol.internal.relay.fromRelayJumpsToAnotherServer
import net.rwhps.server.net.netconnectprotocol.internal.relay.relayServerInitInfo
import net.rwhps.server.net.netconnectprotocol.internal.relay.relayServerTypeInternal
import net.rwhps.server.net.netconnectprotocol.internal.relay.relayServerTypeReplyInternal
import net.rwhps.server.net.netconnectprotocol.internal.server.chatUserMessagePacketInternal
import net.rwhps.server.util.GameOtherUtils.getBetaVersion
import net.rwhps.server.util.IsUtils
import net.rwhps.server.util.PacketType
import net.rwhps.server.util.Time
import net.rwhps.server.util.algorithms.NetConnectProofOfWork
import net.rwhps.server.util.annotations.MainProtocolImplementation
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.Log.debug
import net.rwhps.server.util.log.Log.error
import java.io.IOException
import java.util.*

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
 * @property betaGameVersion        Is it a beta version
 * @property clientVersion          clientVersion
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

    var site = -1
        protected set

    protected var cachePacket: Packet? = null
        private set

    private var relaySelect: ((String) -> Unit)? = null

    override var name = "NOT NAME"
        protected set

    var playerRelay: PlayerRelay? = null
        internal set

    override var registerPlayerId: String? = null
        protected set
    override var betaGameVersion = false
        protected set
    override var clientVersion = 151
        protected set

    // 172
    protected val version2 = 172

    override fun setCachePacket(packet: Packet) {
        cachePacket = packet
    }

    override fun setlastSentPacket(packet: Packet) {
        /* 此协议下不被使用 */
    }

    override val version: String
        get() = "1.15 RELAY"

    override fun sendRelayServerInfo() {
        val cPacket: Packet? = Cache.packetCache["sendRelayServerInfo"]
        if (IsUtils.notIsBlank(cPacket)) {
            sendPacket(cPacket!!)
            return
        }
        try {
            val packetCache = relayServerInitInfo()
            Cache.packetCache["sendRelayServerInfo"] = packetCache
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
                if (IsUtils.isBlank(queryString) || "RELAYCN".equals(queryString, ignoreCase = true)) {
                    sendRelayServerType(Data.i18NBundle.getinput("relay.hi", Data.SERVER_CORE_VERSION))
                } else {
                    idCustom(queryString)
                }
            } else {
                this.relay = relay
                addRelayConnect()
            }
        }
    }

    override fun sendVerifyClientValidity() {
        netConnectAuthenticate = Relay.randPow
        val netConnectAuthenticate: NetConnectProofOfWork = netConnectAuthenticate!!
        val authenticateType = netConnectAuthenticate.authenticateType.toInt()
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
                    val checkStatus =  netConnectAuthenticate!!.check(inStream.readInt(),inStream.readInt(),inStream.readString())
                    if (checkStatus) {
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
    override fun receiveChat(packet: Packet) {
        GameInputStream(packet).use { inStream ->

            val message: String = inStream.readString()

            if (relay!!.allmute) {
                return
            }

            run command@ {
                if (message.startsWith(".") || message.startsWith("-")) {
                    val response = Data.RELAY_COMMAND.handleMessage(message, this) ?: return@command

                    when (response.type) {
                        CommandHandler.ResponseType.noCommand,
                        CommandHandler.ResponseType.valid -> {
                            // Ignore
                        }
                        CommandHandler.ResponseType.manyArguments -> {
                            val msg = "Too many arguments. Usage: " + response.command.text + " " + response.command.paramText
                            sendPacket(NetStaticData.RwHps.abstractNetPacket.getSystemMessagePacket(msg))
                        }
                        CommandHandler.ResponseType.fewArguments -> {
                            val msg = "Too few arguments. Usage: " + response.command.text + " " + response.command.paramText
                            sendPacket(NetStaticData.RwHps.abstractNetPacket.getSystemMessagePacket(msg))
                        }
                        else -> {
                            sendPackageToHOST(packet)
                        }
                    }
                }
            }

            sendPackageToHOST(packet)
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
                Log.clog("sendRelayServerId -> relay : null")
                relay = NetStaticData.relay
            }


            if (site != -1) {
                relay!!.removeAbstractNetConnect(site)
                site = -1
            }
            
            relay!!.admin = this

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
                o.writeString("{{RW-HPS Relay}}.Room ID : ${Data.configRelay.MainID}" + relay!!.id)
                o.writeBoolean(false)
                o.writeIsString(registerPlayerId)
            } else {
                // packetVersion
                o.writeByte(1)
                // allowThisConnectionForwarding
                o.writeBoolean(true)
                // removeThisConnection
                o.writeBoolean(true)
                // useServerId
                o.writeIsString(relay!!.serverUuid)
                // useMods
                o.writeBoolean(relay!!.isMod) //MOD
                // showPublicly
                o.writeBoolean(false)
                // relayMessageOnServer
                o.writeIsString("{{RW-HPS Relay}}.Room ID : ${Data.configRelay.MainID}" + relay!!.id)
                // useMulticast
                o.writeBoolean(false)
            }

            sendPacket(o.createPacket(PacketType.RELAY_BECOME_SERVER)) //+108+140
            //getRelayT4(Data.localeUtil.getinput("relay.server.admin.connect",relay.getId()));
            sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(Data.i18NBundle.getinput("relay.server.admin.connect", Data.configRelay.MainID+relay!!.id, Data.configRelay.MainID+relay!!.internalID.toString()), "RELAY_CN-ADMIN", 5))
            sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(Data.i18NBundle.getinput("relay", Data.configRelay.MainID+relay!!.id), "RELAY_CN-ADMIN", 5))

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
            sendPacket(chatUserMessagePacketInternal(msg))
        } catch (e: Exception) {
            error(e)
        }
    }

    override fun getPingData(packet: Packet) {
        try {
            GameInputStream(packet).use { inStream ->
                val out = GameOutputStream()
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
        try {
            permissionStatus = RelayStatus.PlayerPermission

            inputPassword = false
            if (relay == null) {
                Log.clog("?????")
                relay = NetStaticData.relay
            }


            site = relay!!.setAddPosition()
            relay!!.setAbstractNetConnect(this)

            val o = GameOutputStream()
            if (clientVersion >= version2) {
                o.writeByte(1)
                o.writeInt(site)
                // ?
                o.writeString(registerPlayerId!!)
                //o.writeBoolean(false)
                // User UUID
                o.writeIsString(null)
                o.writeIsString(ip)
                relay!!.admin!!.sendPacket(o.createPacket(PacketType.FORWARD_CLIENT_ADD))
            } else {
                o.writeByte(0)
                o.writeInt(site)
                o.writeString(registerPlayerId!!)
                o.writeIsString(null)
                relay!!.admin!!.sendPacket(o.createPacket(PacketType.FORWARD_CLIENT_ADD))
            }

            sendPackageToHOST(cachePacket!!)
            connectionAgreement.add(relay!!.groupNet)
            sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(Data.i18NBundle.getinput("relay", Data.configRelay.MainID+relay!!.id), "RELAY_CN-ADMIN", 5))

        } catch (e: Exception) {
            permissionStatus = RelayStatus.CertifiedEnd

            connectionAgreement.remove(relay!!.groupNet)

            error("[Relay] addRelayConnect", e)

            relayDirectInspection()
            return
        } finally {
            //cachePacket = null;
        }

        this.relay!!.setAddSize()
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
                    registerPlayerId = stream.readString()
                }
                return
            } catch (e: Exception) {
                error("[No UUID-Hex]", e)
            }
        } else {
            if (permissionStatus.ordinal >= RelayStatus.PlayerPermission.ordinal) {
                // Relay-EX
                if (playerRelay == null) {
                    playerRelay = relay!!.relayPlayersData[registerPlayerId] ?: PlayerRelay(this, registerPlayerId!!, name).also {
                        relay!!.relayPlayersData[registerPlayerId!!] = it
                    }

                    playerRelay!!.nowName = name
                    playerRelay!!.disconnect = false
                }

                if (relay!!.relayKickData.containsKey("BAN$ip")) {
                    kick("您被这个房间BAN了 请换一个房间")
                    return
                }

                var time: Int? = relay!!.relayKickData["KICK$registerPlayerId"]
                if (time == null) {
                    time = relay!!.relayKickData["KICK${connectionAgreement.ipLong24}"]
                }

                if (time != null) {
                    if (time > Time.concurrentSecond()) {
                        kick("您被这个房间踢出了 请稍等一段时间 或者换一个房间")
                        return
                    } else {
                        relay!!.relayKickData.remove("KICK$registerPlayerId")
                        relay!!.relayKickData.remove("KICK${connectionAgreement.ipLong24}")
                    }
                }
            }
        }
        sendPackageToHOST(packet)
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
            } else {
                o.writeByte(0)
                o.writeInt(site)
                o.writeString(registerPlayerId!!)
                o.writeIsString(registerPlayerId!!)
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

                if (type == PacketType.DISCONNECT.typeInt) {
                    return
                }

                val bytes = inStream.readStreamBytes()

                val abstractNetConnect = relay!!.getAbstractNetConnect(target)

                if (PacketType.KICK.typeInt == type) {
                    val gameOutputStream = GameOutputStream()
                    gameOutputStream.writeString(GameInputStream(bytes).readString().replace("\\d".toRegex(), ""))
                    abstractNetConnect?.sendPacket(gameOutputStream.createPacket(type))
                    relayPlayerDisconnect()
                    return
                }

                Packet(type, bytes).let { sendPacketData ->
                    abstractNetConnect?.sendPacket(sendPacketData)
                    sendPacketExtractInformation(sendPacketData,abstractNetConnect)
                }
            }
        } catch (e: Exception) {
            error("[RELAY NormalForwarding Error]",e)
        }
    }

    override fun sendPackageToHOST(packet: Packet) {
        try {
            val o = GameOutputStream()
            o.writeInt(site)
            o.writeInt(packet.bytes.size + 8)
            o.writeInt(packet.bytes.size)
            o.writeInt(packet.type.typeInt)
            o.writeBytes(packet.bytes)
            relay!!.admin?.sendPacket(o.createPacket(PacketType.PACKET_FORWARD_CLIENT_FROM))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun relayPlayerDisconnect() {
        try {
            val out = GameOutputStream()
            out.writeByte(0)
            out.writeInt(site)
            sendPackageToHOST(out.createPacket(PacketType.FORWARD_CLIENT_REMOVE))
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
        if (relay != null && (permissionStatus == RelayStatus.PlayerPermission || permissionStatus == RelayStatus.HostPermission)) {
            relay!!.setRemoveSize()

            // 避免多个玩家断开导致 NPE
            synchronized(relay!!) {
                var errorClose = false

                try {
                    if (this !== relay!!.admin) {
                        relay!!.removeAbstractNetConnect(site)

                        if (relay!!.admin != null) {
                            sendPackageToHOST(NetStaticData.RwHps.abstractNetPacket.getExitPacket())
                            if (!relay!!.isStartGame) {
                                relay!!.relayPlayersData.remove(registerPlayerId)
                            }
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
                } catch (e: Exception) {
                    if (!relay!!.closeRoom) {
                        debug("[Relay Close Error]", e)
                    }
                    errorClose =true
                }
                if ((relay!!.getSize() <= 0 && !relay!!.closeRoom) || errorClose) {
                    debug("[Relay] Gameover")
                    relay!!.re()
                }
                super.close(relay!!.groupNet)
            }
        } else {
            super.close(null)
        }
    }

    open fun sendPacketExtractInformation(packet: Packet, abstractNetConnect: GameVersionRelay?) {
        when (packet.type) {
            PacketType.TEAM_LIST -> {
                if (!relay!!.isStartGame) {
                    abstractNetConnect?.let {
                        UniversalAnalysisOfGamePackages.getPacketTeamData(GameInputStream(packet.bytes, it.clientVersion), it.playerRelay!!)
                    }
                }
            }
            PacketType.RETURN_TO_BATTLEROOM -> {
                if (relay!!.isStartGame) {
                    relay!!.isStartGame = false
                }
            }
            PacketType.START_GAME -> {
                relay!!.isStartGame = true
            }
            else -> {
                // 没其他必要
            }
        }

    }

    private fun idCustom(inId: String) {
        // 过滤 制表符 空格 换行符
        var id = inId.replace("\\s".toRegex(), "")
        if ("old".equals(id, ignoreCase = true)) {
            id = Relay.serverRelayOld[registerPlayerId!!, ""]
        } else {
            Relay.serverRelayOld[registerPlayerId!!] = id
        }

        if (id.isEmpty()) {
            sendRelayServerType(Data.i18NBundle.getinput("relay.server.error.id", "什么都没输入就点击确认"))
            return
        }
        if (EmojiManager.containsEmoji(id)) {
            sendRelayServerType(Data.i18NBundle.getinput("relay.server.error.id", "不能使用Emoji"))
            return
        }


        if (id.startsWith("RA")) {
            if (Data.configRelay.MainServer) {
                sendPacket(fromRelayJumpsToAnotherServer("${id[1]}.relay.der.kim/$id"))
                return
            } else {
                id = id.substring(2)
            }
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
                    if (Relay.getCheckRelay(id) || id.startsWith("A",true)) {
                        sendRelayServerType(Data.i18NBundle.getinput("relay.id.re"))
                    } else {
                        newRelayId(id, true)
                    }
                }
            } else {
                if (checkLength(id)) {
                    if (Relay.getCheckRelay(id) || id.startsWith("A",true)) {
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

        if (id.startsWith("newup", ignoreCase = true)) {
            id = id.substring(5)
        } else if (id.startsWith("newsup", ignoreCase = true)) {
            id = id.substring(6)
        } else if (id.startsWith("modup", ignoreCase = true)) {
            mods = true
            id = id.substring(5)
        } else if (id.startsWith("modsup", ignoreCase = true)) {
            mods = true
            id = id.substring(6)
        } else if (id.startsWith("new", ignoreCase = true)) {
            id = id.substring(3)
        } else if (id.startsWith("news", ignoreCase = true)) {
            id = id.substring(4)
        } else if (id.startsWith("mod", ignoreCase = true)) {
            mods = true
            id = id.substring(3)
        } else if (id.startsWith("mods", ignoreCase = true)) {
            mods = true
            id = id.substring(4)
        } else {
            newRoom = false
        }

        if (newRoom) {
            val custom = CustomRelayData()

            try {
                if (id.startsWith("P", ignoreCase = true)) {
                    id = id.substring(1)
                    val arry = if (id.contains("，")) id.split("，") else id.split(",")
                    custom.maxPlayerSize = arry[0].toInt()
                    if (custom.maxPlayerSize !in 0..100) {
                        sendRelayServerType(Data.i18NBundle.getinput("relay.id.maxPlayer.re"))
                        return
                    }
                    if (arry.size > 1) {
                        if (arry[1].contains("I", ignoreCase = true)) {
                            val ay = arry[1].split("I", ignoreCase = true)
                            if (ay.size > 1) {
                                custom.maxUnitSizt = ay[0].toInt()
                                custom.income = ay[1].toFloat()
                            } else {
                                custom.income = ay[0].toFloat()
                            }
                        } else {
                            custom.maxUnitSizt = arry[1].toInt()
                        }
                        if (custom.maxUnitSizt !in 0..Int.MAX_VALUE) {
                            sendRelayServerType(Data.i18NBundle.getinput("relay.id.maxUnit.re"))
                            return
                        }
                    }
                }
                if (id.startsWith("I", ignoreCase = true)) {
                    id = id.substring(1)
                    val arry = if (id.contains("，")) id.split("，") else id.split(",")
                    custom.income = arry[0].toFloat()
                    if (custom.income !in 0F..Float.MAX_VALUE) {
                        sendRelayServerType(Data.i18NBundle.getinput("relay.id.income.re"))
                        return
                    }
                }
            } catch (e: NumberFormatException) {
                sendRelayServerType(Data.i18NBundle.getinput("relay.server.error", e.message))
                return
            }

//            if (newRoom) { // newRoom 在上一级if(756)中已经为true,else后面的部分永远不会被执行
                newRelayId(mod = mods, customRelayData = custom)

                if (custom.maxPlayerSize != -1 || custom.maxUnitSizt != 200) {
                    sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket("自定义人数: ${custom.maxPlayerSize} 自定义单位: ${custom.maxUnitSizt}", "RELAY_CN-Custom", 5))
                }
//            } else {
//                try {
//                    if (id.contains(".")) {
//                        sendRelayServerType(Data.i18NBundle.getinput("relay.server.error", "不能包含 [ . ]"))
//                        return
//                    }
//                    relay = Relay.getRelay(id)
//                    if (relay != null) {
//                        addRelayConnect()
//                    } else {
//                        sendRelayServerType(Data.i18NBundle.getinput("relay.server.no", id))
//                    }
//                } catch (e: Exception) {
//                    debug(e)
//                    sendRelayServerType(Data.i18NBundle.getinput("relay.server.error", e.message))
//                }
//            }
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
        // 更新最小玩家
        relay!!.updateMinSize()
        relay!!.getAbstractNetConnect(relay!!.minSize)?.let {
            it.sendRelayServerId()
            relay!!.abstractNetConnectIntMap.values.forEach { obj: GameVersionRelay -> obj.addReRelayConnect() }
            this.addReRelayConnect()
        }
    }

    private fun newRelayId(id: String? = null, mod: Boolean, customRelayData: CustomRelayData = CustomRelayData()) {
        val maxPlayer = if (customRelayData.maxPlayerSize == -1) 10 else customRelayData.maxPlayerSize

        relay = if (IsUtils.isBlank(id)) {
            Relay.getRelay( playerName = name, isMod = mod, betaGameVersion = betaGameVersion, version = clientVersion, maxPlayer = maxPlayer)
        } else {
            Relay.getRelay( id!!, name, mod, betaGameVersion, clientVersion, maxPlayer)
        }

        relay!!.isMod = mod

        if (customRelayData.maxPlayerSize != -1 || customRelayData.income != 1F) {
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
        o2.writeInt(HessModuleManage.hps.gameDataLink.credits)
        o2.writeInt(HessModuleManage.hps.gameDataLink.fog)
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
        o.writeInt(customRelayData.maxPlayerSize)
        o.flushEncodeData(CompressOutputStream.getGzipOutputStream("teams", true).also { for (i in 0 until customRelayData.maxPlayerSize) it.writeBoolean(false)})
        o.writeInt(HessModuleManage.hps.gameDataLink.fog)
        o.writeInt(HessModuleManage.hps.gameDataLink.credits)
        o.writeBoolean(true)
        o.writeInt(1)
        o.writeByte(5)
        // RELAY Custom MaxUnit
        o.writeInt(customRelayData.maxUnitSizt)
        o.writeInt(customRelayData.maxUnitSizt)
        o.writeInt(HessModuleManage.hps.gameDataLink.startingunits)
        // RELAY Custom income
        o.writeFloat(customRelayData.income)
        o.writeBoolean(!HessModuleManage.hps.gameDataLink.nukes)
        o.writeBoolean(false)
        o.writeBoolean(false)
        o.writeBoolean(HessModuleManage.hps.gameDataLink.sharedcontrol)
        o.writeBoolean(Data.game.gamePaused) // 在Hess那里找不到对应的
        sendPacket(o.createPacket(PacketType.TEAM_LIST))
    }

    private data class CustomRelayData(
        var maxPlayerSize: Int = -1,
        var maxUnitSizt: Int = 200,
        var income: Float = 1f,
    )

}