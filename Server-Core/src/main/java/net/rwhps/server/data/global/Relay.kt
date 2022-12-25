/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.global

import net.rwhps.server.data.global.Data.LINE_SEPARATOR
import net.rwhps.server.math.Rand
import net.rwhps.server.net.GroupNet
import net.rwhps.server.net.core.DataPermissionStatus
import net.rwhps.server.net.core.NetConnectProofOfWork
import net.rwhps.server.net.netconnectprotocol.realize.GameVersionRelay
import net.rwhps.server.struct.IntMap
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.IsUtil.isNumeric
import net.rwhps.server.util.Time
import net.rwhps.server.util.Time.concurrentSecond
import net.rwhps.server.util.log.Log.debug
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

class Relay {
    /**  */
    @JvmField
    val groupNet: GroupNet
    val abstractNetConnectIntMap = IntMap<GameVersionRelay>(10,true)


    var admin: GameVersionRelay? = null
        set(value) {
            field = value
            value?.permissionStatus = DataPermissionStatus.RelayStatus.HostPermission
        }

    var closeRoom = false

    val roomCreateTime = Time.concurrentSecond()

   // val serverUuid = UUID.randomUUID().toString()
    val serverUuid = Data.SERVER_RELAY_UUID
    val internalID : Int
    val id: String
    var isMod = false
    var minSize = 1
        private set
    var isStartGame: Boolean = false
        set(value) {
            if (field && value) {
                return
            }
            field = value
            startGameTime = Time.concurrentSecond()+300
        }
    var startGameTime = 0
        private set

    var allmute = false

    private val site = AtomicInteger(0)
    private val size = AtomicInteger()

    private constructor(internalID : Int, id: String, playerName: String, isMod: Boolean, betaGameVersion: Boolean, version: Int, maxPlayer: Int) {
        serverRelayData.put(internalID, this)
        this.internalID = internalID
        this.id = id
        groupNet = GroupNet()
        this.isMod = isMod
    }

    /**
     * 仅供内部使用
     * 作为单房间实例化
     * @param id String
     * @param groupNet GroupNet
     * @constructor
     */
    internal constructor(id: String, groupNet: GroupNet) {
        this.internalID = 0
        this.id = id
        this.groupNet = groupNet
    }

    val allIP: String
        get() {
            val str = StringBuilder(10)
            str.append(LINE_SEPARATOR)
                .append(admin!!.name)
                .append(" / ")
                .append("IP: ").append(admin!!.ip)
                .append(" / ")
                .append("Protocol: ").append(admin!!.useConnectionAgreement)
                .append(" / ")
                .append("Admin: true")
            abstractNetConnectIntMap.values.forEach(Consumer { e: GameVersionRelay ->
                str.append(LINE_SEPARATOR)
                    .append(e.name)
                    .append(" / ")
                    .append("IP: ").append(e.ip)
                    .append(" / ")
                    .append("Protocol: ").append(e.useConnectionAgreement)
                    .append(" / ")
                    .append("Admin: false")
            })
            return str.toString()
        }

    fun re() {
        removeRoom()
    }

    fun removeRoom(run: ()->Unit = {}) {
        try {
            admin?.disconnect()
            abstractNetConnectIntMap.values.forEach { it.disconnect() }
            closeRoom = true
            abstractNetConnectIntMap.clear()
            site.set(0)
            admin = null
            isStartGame = false
        } catch (e: Exception) {
        } finally {
            serverRelayData.remove(internalID)
            run()
        }
    }

    fun getAbstractNetConnect(site: Int): GameVersionRelay? {
        return abstractNetConnectIntMap[site]
    }

    fun setAbstractNetConnect(abstractNetConnect: GameVersionRelay): GameVersionRelay? {
        return abstractNetConnectIntMap.put(site.get(), abstractNetConnect)
    }

    fun sendMsg(msg: String) {
        try {
            admin!!.sendPacket(NetStaticData.RwHps.abstractNetPacket.getSystemMessagePacket(msg))
            groupNet.broadcastAndUDP(NetStaticData.RwHps.abstractNetPacket.getSystemMessagePacket(msg))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun sendMsg(msg: String, msgName: String, team: Int) {
        try {
            admin!!.sendPacket(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(msg,msgName,team))
            groupNet.broadcastAndUDP(NetStaticData.RwHps.abstractNetPacket.getChatMessagePacket(msg,msgName,team))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun removeAbstractNetConnect(site: Int) {
        abstractNetConnectIntMap.remove(site)
    }

    fun getActiveConnectionSize(): Int {
        return abstractNetConnectIntMap.size
    }

    fun getSite(): Int {
        return site.get()
    }

    fun setAddSite() {
        site.incrementAndGet()
    }

    fun setRemoveSite() {
        site.decrementAndGet()
    }

    fun getSize(): Int {
        return size.get()
    }

    fun setAddSize() {
        size.incrementAndGet()
    }

    fun setRemoveSize() {
        size.decrementAndGet()
    }

    fun getRandAdmin(): GameVersionRelay? {
        return if (abstractNetConnectIntMap.isEmpty()) null else abstractNetConnectIntMap.toArrayValues().random()
    }

    fun updateMinSize() {
        try {
            minSize = abstractNetConnectIntMap.toArrayKey().toArray(Int::class.java).min()
        } catch (_: Exception) {
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val relay = other as Relay
        return internalID == coverRelayID(relay.id)
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    companion object {
        val serverRelayIpData = Seq<String>(true)

        private val serverRelayData = IntMap<Relay>(128,true)
        private val rand = Rand()

        private val getRelayData = ReentrantLock(true)

        @JvmStatic
        val relayAllIP: String
            get() {
                val str = StringBuilder(10)
                serverRelayData.values.forEach(Consumer { e: Relay ->
                    str.append(LINE_SEPARATOR)
                        .append(e.id)
                        .append(e.allIP)
                })
                return str.toString()
            }

        val allSize: Int
            get() {
                val size = AtomicInteger()
                serverRelayData.values.forEach(Consumer { e: Relay -> size.getAndAdd(e.getSize()) })
                return size.get()
            }
        val roomAllSize: Int
            get() {
                return serverRelayData.size
            }
        val roomNoStartSize: Int
            get() {
                val size = AtomicInteger()
                serverRelayData.values.forEach(Consumer { e: Relay -> if (!e.isStartGame) size.incrementAndGet() })
                return size.get()
            }

        @get:Synchronized
        internal val randPow: NetConnectProofOfWork
            get() { return NetConnectProofOfWork() }

        @JvmStatic
        fun getRelay(id: String): Relay? = getRelayData.withLock { serverRelayData[coverRelayID(id)] }

        @JvmStatic
        fun getCheckRelay(id: String): Boolean = getRelayData.withLock { serverRelayData.containsKey(coverRelayID(id)) }

        @JvmStatic
        fun sendAllMsg(msg: String) {
            serverRelayData.values.forEach(Consumer { e: Relay -> e.sendMsg(msg) })
        }

        @JvmStatic
        fun getAllRelayIpCountry(): Map<String,Int> {
            return mutableMapOf<String, Int>().also {
                mutableMapOf<String, AtomicInteger>().also { temp ->
                    Relay.serverRelayData.values.forEach { relay ->
                        temp.computeIfAbsent(relay.admin!!.ipCountry) { AtomicInteger(0) }.incrementAndGet()
                        relay.abstractNetConnectIntMap.values.forEach { connect ->
                            temp.computeIfAbsent(connect.ipCountry) { AtomicInteger(0) }.incrementAndGet()
                        }
                    }
                }.forEach { (country, count) ->
                    it[country] = count.get()
                }
            }
        }

        @JvmStatic
        fun getAllRelayVersion(): Map<Int,Int> {
            return mutableMapOf<Int, Int>().also {
                mutableMapOf<Int, AtomicInteger>().also { temp ->
                    Relay.serverRelayData.values.forEach { relay ->
                        temp.computeIfAbsent(relay.admin!!.clientVersion) { AtomicInteger(0) }.incrementAndGet()
                        relay.abstractNetConnectIntMap.values.forEach { connect ->
                            temp.computeIfAbsent(connect.clientVersion) { AtomicInteger(0) }.incrementAndGet()
                        }
                    }
                }.forEach { (version, count) ->
                    it[version] = count.get()
                }
            }
        }

        internal fun cleanRoom() {
            val time = concurrentSecond() - 12 * 60 * 60
            serverRelayData.forEach {
                if (it.value.roomCreateTime < time) {
                    it.value.removeRoom()
                }
            }
        }

        internal fun coverRelayID(id: String): Int {
            return if (isNumeric(id)) {
                id.toInt()
            } else {
                id.hashCode().let {
                    return if (it > 0) {
                        -it
                    } else {
                        it
                    }
                }
            }
        }

        /**
         * This method should not be open to the public for internal use only
         * @param id                Custom ID left blank self generated
         * @param playerName        Player name data
         * @param isMod             Whether mods room
         * @param betaGameVersion   Is it a beta version
         * @param maxPlayer         Maximum number of people
         * @return Relay
         */
        @Synchronized
        internal fun getRelay(id: String = "", playerName: String, isMod: Boolean, betaGameVersion: Boolean, version: Int, maxPlayer: Int): Relay {
            var idRelay = id
            val relay: Relay =  if (id.isBlank()) {
                                    while (true) {
                                        val intId = rand.random(1000, 100000)
                                        if (!getCheckRelay(intId.toString())) {
                                            idRelay = intId.toString()
                                            debug(intId)
                                            break
                                        }
                                    }
                                    Relay(coverRelayID(idRelay),idRelay,playerName,isMod,betaGameVersion,version,maxPlayer)
                                } else {
                                    Relay(coverRelayID(idRelay),idRelay,playerName,isMod,betaGameVersion,version,maxPlayer)
                                }
            return relay
        }
    }
}