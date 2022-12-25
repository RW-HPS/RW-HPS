/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.player

import net.rwhps.server.data.global.Data
import net.rwhps.server.data.global.NetStaticData
import net.rwhps.server.data.plugin.Value
import net.rwhps.server.data.totalizer.TimeAndNumber
import net.rwhps.server.func.Prov
import net.rwhps.server.game.event.EventType
import net.rwhps.server.game.simulation.pivatedata.PrivateClass_Player
import net.rwhps.server.net.core.IRwHps
import net.rwhps.server.net.netconnectprotocol.realize.GameVersionServer
import net.rwhps.server.net.netconnectprotocol.realize.GameVersionServerJump
import net.rwhps.server.struct.ObjectMap
import net.rwhps.server.util.I18NBundle
import net.rwhps.server.util.IsUtil
import net.rwhps.server.util.Time
import net.rwhps.server.util.game.Events
import net.rwhps.server.util.log.exp.ImplementedException
import net.rwhps.server.util.log.exp.NetException
import org.jetbrains.annotations.Nls
import java.util.*

/**
 *
 * @author RW-HPS/Dr
 */
class Player(
    @JvmField var con: GameVersionServer?,
    /** Player connection UUID  */
    @JvmField val uuid: String,
    /** Player name  */
    @JvmField val name: String,
    /**   */
    @JvmField val i18NBundle: I18NBundle,
) {
    internal var playerPrivateData: PrivateClass_Player? = null

    /** is Admin  */
	@JvmField
    var isAdmin = false
    var superAdmin = false

    /** Team number  */
	var team = 0
        set(value) { watch = (value == -3) ; field = value }
    /** List position  */
	var site = 0
        set(value) { color = value ; field = value }

    /** */
    var credits
        get() =  if (!Data.game.isStartGame) Data.game.credits else playerPrivateData?.credits ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound PlayerData")
        set(value) {
            if (!Data.game.isStartGame) {
                Data.game.credits = value
            } else {
                if (playerPrivateData == null) {
                    throw ImplementedException.PlayerImplementedException("[Player] No Bound PlayerData")
                }
                playerPrivateData!!.credits = value
            }
        }
    /** Is the player alive  */
    val survive get() = if (!Data.game.isStartGame) true else playerPrivateData?.survive ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound PlayerData")
    /** 单位击杀数 */
    val unitsKilled get() = playerPrivateData?.unitsKilled ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound PlayerData")
    /** 建筑毁灭数 */
    val buildingsKilled get() = playerPrivateData?.buildingsKilled ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound PlayerData")
    /** 单实验单位击杀数 */
    val experimentalsKilled get() = playerPrivateData?.experimentalsKilled ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound PlayerData")
    /** 单位被击杀数 */
    val unitsLost get() = playerPrivateData?.unitsLost ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound PlayerData")
    /** 建筑被毁灭数 */
    val buildingsLost get() = playerPrivateData?.buildingsLost ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound PlayerData")
    /** 单实验单位被击杀数 */
    val experimentalsLost get() = playerPrivateData?.experimentalsLost ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound PlayerData")

    /** */
    var startUnit = Data.game.initUnit
    /** */
    var color = 0
    /** Ping */
    var ping = 50

    /** (Markers)  */
    @Volatile var start = false
        set(value) {
            field = value
            if (name == Data.headlessName) {
                Events.fire(EventType.HessStartEvent())
            }
        }
    var watch = false
        private set

    /** Last move time  */
	@Volatile var lastMoveTime: Int = 0
    /** Mute expiration time */
	var muteTime: Long = 0
    /** Kick expiration time */
	var kickTime: Long = 0
	var timeTemp: Long = 0
    var lastMessageTime: Long = 0
    var lastSentMessage: String? = ""
	var noSay = false

    /** 点石成金 */
    var turnStoneIntoGold = Data.config.Turnstoneintogold



    /** Shared control  */
    @Volatile var sharedControl = false
    val controlThePlayer: Boolean
        get() {
            return sharedControl || if (con == null || Time.concurrentSecond()-lastMoveTime > 120) {
                true
            } else {
                con!!.isDis
            }
        }

    var lastVoteTime: Int = 0

    val reConnectData = TimeAndNumber(300,3)

    private val customData = ObjectMap<String, Value<*>>()

    @Throws(ImplementedException.PlayerImplementedException::class)
    fun sendSystemMessage(@Nls text: String) {
        con?.sendSystemMessage(text) ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound Connection")
    }

    @Throws(ImplementedException.PlayerImplementedException::class)
    fun sendMessage(player: Player, @Nls text: String) {
        con?.sendChatMessage(text, player.name, player.team) ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound Connection")
    }

    @Throws(ImplementedException.PlayerImplementedException::class)
    fun sendTeamData() {
        con?.sendTeamData(NetStaticData.RwHps.abstractNetPacket.getTeamDataPacket()) ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound Connection")
    }

    @Throws(ImplementedException.PlayerImplementedException::class)
    fun sendPopUps(@Nls msg: String,run: ((String) -> Unit)) {
        con?.sendRelayServerType(msg,run) ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound Connection")
    }

    @Throws(ImplementedException.PlayerImplementedException::class)
    fun sync() {
        con?.sync() ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound Connection")
    }

    @Throws(ImplementedException.PlayerImplementedException::class)
    @JvmOverloads
    fun kickPlayer(@Nls text: String, time: Int = 0) {
        kickTime = Time.getTimeFutureMillis(time * 1000L)
        con?.sendKick(text) ?: throw ImplementedException.PlayerImplementedException("[Player] No Bound Connection")
    }



    fun getinput(input: String, vararg params: Any?): String {
        return i18NBundle.getinput(input,params)
    }


    fun <T> addData(dataName: String, value: T) {
        customData.put(dataName,Value(value))
    }
    @Suppress("UNCHECKED_CAST")
    fun <T> getData(dataName: String): T? {
        return customData[dataName]?.data as T
    }
    @Suppress("UNCHECKED_CAST")
    fun <T> getData(dataName: String, defValue: T): T {
        return (customData[dataName]?.data ?:defValue) as T
    }
    @Suppress("UNCHECKED_CAST")
    fun <T> getData(dataName: String, defProv: Prov<T>): T {
        return (customData[dataName]?.data ?:defProv.get()) as T
    }
    fun removeData(dataName: String) {
        customData.remove(dataName)
    }

    /**
     * [Deprecated]
     * Local player connects to new server
     * For [IRwHps.NetType.ServerProtocol] :
     *  At this time, the local server only forwards the player data and has nothing to do with the local player.
     *  The player will not exist in [Data.game.playerManage.playerGroup] and [Data.game.playerManage.playerAll]
     *  Player ⇄ LocalServer ⇄ NewServer
     *
     * For [IRwHps.NetType.ServerTestProtocol] :
     *  At this time, the local server does not participate in the forwarding, and the client directly disconnects the server and joins the new server.
     *  The player will not exist in [Data.game.playerManage.playerGroup] and [Data.game.playerManage.playerAll]
     *  Player ⇄ NewServer
     *
     * @param ip
     * @param port
     */
    @JvmOverloads
    @Throws(NetException::class)
    fun playerJumpsToAnotherServer(ip: String, port: Int = 5123) {
        if (!IsUtil.isDomainName(ip)) {
            throw NetException("[ERROR_DOMAIN] Error Domain")
        }
        if (con == null) {
            throw NetException("[CONNECT_CLOSE] Connect disconnect")
        }

        if (con is GameVersionServerJump) {
            (con as GameVersionServerJump).jumpNewServer("$ip:$port")
        }
        throw ImplementedException("[PlayerJumpsToAnotherServer] NOT SUPPORT")
    }

    // 买得起吗
    fun canBuy(price: Int): Boolean {
        return credits >= price
    }


    fun isEnemy(other: Player): Boolean {
        return this.team != other.team
    }

    fun clear() {
        con = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return  if (other == null || javaClass != other.javaClass) {
                    false
                } else if (other is Player) {
                    uuid == other.uuid
                } else {
                    uuid == other.toString()
                }
    }

    override fun hashCode(): Int {
        return Objects.hash(uuid)
    }
}