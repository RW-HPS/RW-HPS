/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.data.plugin

import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.io.packet.Packet
import net.rwhps.server.net.Administration.PlayerAdminInfo
import net.rwhps.server.net.Administration.PlayerInfo
import net.rwhps.server.net.core.NetConnectProofOfWork
import net.rwhps.server.struct.ObjectMap
import net.rwhps.server.struct.OrderedMap
import net.rwhps.server.struct.Seq
import net.rwhps.server.struct.SerializerTypeAll.TypeSerializer
import java.io.IOException

/**
 * @author RW-HPS/Dr
 */
internal object DefaultSerializers {
    @JvmStatic
    fun register() {
        registrationBasis()
        registerMap()
        registerCustomClass()
    }

    private fun registrationBasis() {
        AbstractPluginData.setSerializer(String::class.java, object : TypeSerializer<String> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, objectData: String?) {
                stream.writeString(objectData ?: "")
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): String {
                return stream.readString()
            }
        })
        AbstractPluginData.setSerializer(Integer::class.java, object : TypeSerializer<Int> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, objectData: Int) {
                stream.writeInt(objectData)
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): Int {
                return stream.readInt()
            }
        })
        AbstractPluginData.setSerializer(java.lang.Float::class.java, object : TypeSerializer<Float> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, objectData: Float) {
                stream.writeFloat(objectData)
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): Float {
                return stream.readFloat()
            }
        })
        AbstractPluginData.setSerializer(java.lang.Long::class.java, object : TypeSerializer<Long> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, objectData: Long) {
                stream.writeLong(objectData)
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): Long {
                return stream.readLong()
            }
        })
        AbstractPluginData.setSerializer(java.lang.Boolean::class.java, object : TypeSerializer<Boolean> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, objectData: Boolean) {
                stream.writeBoolean(objectData)
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): Boolean {
                return stream.readBoolean()
            }
        })
    }

    private fun registerMap() {
        AbstractPluginData.setSerializer(Seq::class.java, object : TypeSerializer<Seq<*>> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, objectData: Seq<*>) {
                stream.writeInt(objectData.size)
                if (objectData.size != 0) {
                    val first = objectData.first()!!
                    val ser = AbstractPluginData.getSerializer(first.javaClass) ?: throw getError(first.javaClass.toString())
                    stream.writeString(first.javaClass.name)
                    for (element in objectData) {
                        ser.write(stream, element)
                    }
                }
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): Seq<*>? {
                 return try {
                    val size = stream.readInt()
                    val arr = Seq<Any>(size)
                    if (size == 0) {
                        return arr
                    }
                    val type = stream.readString()
                    val ser = AbstractPluginData.getSerializer(lookup(type))
                     requireNotNull(ser) {
                         "$type does not have a serializer registered!"
                     }

                     for (i in 0 until size) {
                        arr.add(ser.read(stream))
                    }
                    arr
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                    null
                }
            }
        })
        AbstractPluginData.setSerializer(ObjectMap::class.java, object : TypeSerializer<ObjectMap<*, *>> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, map: ObjectMap<*, *>) {
                stream.writeInt(map.size)
                if (map.size == 0) {
                    return
                }
                val entry = map.entries().next()
                val keySer = AbstractPluginData.getSerializer(entry.key.javaClass) ?: throw getError(entry.key.javaClass.toString())
                val valSer = AbstractPluginData.getSerializer(entry.value.javaClass) ?: throw getError(entry.value.javaClass.toString())
                stream.writeString(entry.key.javaClass.name)
                stream.writeString(entry.value.javaClass.name)
                for (e in map.entries()) {
                    val en = e as ObjectMap.Entry<*, *>
                    keySer.write(stream, en.key)
                    valSer.write(stream, en.value)
                }
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): ObjectMap<*, *>? {
                return try {
                    val size = stream.readInt()
                    val map = ObjectMap<Any?, Any?>()
                    if (size == 0) {
                        return map
                    }
                    val keySerName = stream.readString()
                    val valSerName = stream.readString()
                    val keySer = AbstractPluginData.getSerializer(lookup(keySerName)) ?: throw getError(keySerName)
                    val valSer = AbstractPluginData.getSerializer(lookup(valSerName)) ?: throw getError(valSerName)
                    for (i in 0 until size) {
                        val key = keySer.read(stream)
                        val `val` = valSer.read(stream)
                        map.put(key, `val`)
                    }
                    map
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                    null
                }
            }
        })
        AbstractPluginData.setSerializer(OrderedMap::class.java, object : TypeSerializer<OrderedMap<*, *>> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, map: OrderedMap<*, *>) {
                stream.writeInt(map.size)
                if (map.size == 0) {
                    return
                }
                val entry = map.entries().next()
                val keySer = AbstractPluginData.getSerializer(entry.key.javaClass) ?: throw getError(entry.key.javaClass.toString())
                val valSer = AbstractPluginData.getSerializer(entry.value.javaClass) ?: throw getError(entry.value.javaClass.toString())
                stream.writeString(entry.key.javaClass.name)
                stream.writeString(entry.value.javaClass.name)
                for (e in map.entries()) {
                    val en = e as ObjectMap.Entry<*, *>
                    keySer.write(stream, en.key)
                    valSer.write(stream, en.value)
                }
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): OrderedMap<*, *>? {
                return try {
                    val size = stream.readInt()
                    val map = OrderedMap<Any?, Any?>()
                    if (size == 0) {
                        return map
                    }
                    val keySerName = stream.readString()
                    val valSerName = stream.readString()
                    val keySer = AbstractPluginData.getSerializer(lookup(keySerName)) ?: throw getError(keySerName)
                    val valSer = AbstractPluginData.getSerializer(lookup(valSerName)) ?: throw getError(valSerName)
                    for (i in 0 until size) {
                        val key = keySer.read(stream)
                        val `val` = valSer.read(stream)
                        map.put(key, `val`)
                    }
                    map
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                    null
                }
            }
        })
    }

    private fun registerCustomClass() {
        AbstractPluginData.setSerializer(PlayerInfo::class.java, object : TypeSerializer<PlayerInfo> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, objectData: PlayerInfo) {
                AbstractPluginData.getSerializer(String::class.java)!!.write(stream, objectData.uuid)
                stream.writeLong(objectData.timesKicked)
                stream.writeLong(objectData.timesJoined)
                stream.writeLong(objectData.timeMute)
                stream.writeBoolean(objectData.admin)
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): PlayerInfo {
                val objectData = PlayerInfo(AbstractPluginData.getSerializer(String::class.java)!!.read(stream) as String)
                objectData.timesKicked = stream.readLong()
                objectData.timesJoined = stream.readLong()
                objectData.timeMute = stream.readLong()
                objectData.admin = stream.readBoolean()
                return objectData
            }
        })

        AbstractPluginData.setSerializer(PlayerAdminInfo::class.java, object : TypeSerializer<PlayerAdminInfo> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, objectData: PlayerAdminInfo) {
                stream.writeString(objectData.uuid)
                stream.writeBoolean(objectData.admin)
                stream.writeBoolean(objectData.superAdmin)
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): PlayerAdminInfo {
                return PlayerAdminInfo(stream.readString(), stream.readBoolean(), stream.readBoolean())
            }
        })

        AbstractPluginData.setSerializer(Packet::class.java, Packet.serializer)
        AbstractPluginData.setSerializer(NetConnectProofOfWork::class.java, NetConnectProofOfWork.serializer)
    }

    @Throws(ClassNotFoundException::class)
    private fun lookup(name: String): Class<*> {
        return Class.forName(name)
    }

    fun getError(className: String): IllegalArgumentException {
        return IllegalArgumentException("$className does not have a serializer registered!")
    }
}