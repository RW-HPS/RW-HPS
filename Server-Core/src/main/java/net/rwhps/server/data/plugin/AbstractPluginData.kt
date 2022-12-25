/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */
package net.rwhps.server.data.plugin

import net.rwhps.server.data.plugin.DefaultSerializers.register
import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.io.input.ReusableDisableSyncByteArrayInputStream
import net.rwhps.server.io.output.ByteArrayOutputStream
import net.rwhps.server.struct.ObjectMap
import net.rwhps.server.struct.OrderedMap
import net.rwhps.server.struct.SerializerTypeAll
import net.rwhps.server.util.IsUtil.isBlank
import net.rwhps.server.util.alone.annotations.NeedToRefactor
import net.rwhps.server.util.compression.gzip.GzipDecoder.getGzipInputStream
import net.rwhps.server.util.compression.gzip.GzipEncoder.getGzipOutputStream
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.log.Log.error
import net.rwhps.server.util.log.Log.warn
import net.rwhps.server.util.log.exp.VariableException
import java.io.*

/**
 * [PluginData] 的默认实现. 使用 '[AbstractPluginData.setData]' 自带创建 [Value] 并跟踪其改动.
 * 实现注意
 * 此类型处于实验性阶段. 使用其中定义的属性和函数是安全的, 但将来可能会新增成员抽象函数.
 * @author RW-HPS/Dr
 */
@NeedToRefactor
internal open class AbstractPluginData {
    private val pluginData = OrderedMap<String, Value<*>>()
    private val byteInputStream = ReusableDisableSyncByteArrayInputStream()
    private val byteStream = ByteArrayOutputStream()
    private val dataOutput = GameOutputStream(byteStream)
    private var fileUtil: FileUtil? = null

    /**
     * 这个 [PluginData] 保存时使用的文件.
     * @param fileUtil FileUtil
     */
    fun setFileUtil(fileUtil: FileUtil) {
        this.fileUtil = fileUtil
        fileUtil.createNewFile()
        this.read()
    }

    /**
     * 向PluginData中加入一个value
     * @param name value的名字
     * @param data 需要存储的数据
     * @param <T> data的类
    </T> */
    inline fun <reified T> setData(name: String, data: T) {
        if (SERIALIZERS.containsKey(T::class.java)) {
            pluginData.put(name, Value(data))
        } else {
            throw VariableException.ObjectMapRuntimeException("UNSUPPORTED_SERIALIZATION")
        }
    }

    /**
     * 向PluginData中获取一个value
     * @param name value的名字
     * @param <T> data的类
     * @return value
    </T> */
    @Suppress("UNCHECKED_CAST")
    fun <T> getData(name: String): T {
        return pluginData[name].data as T
    }

    /**
     * 向PluginData中获取一个value
     * @param name value的名字
     * @param data 默认返回的数据
     * @param <T> data的类
     * @return value
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getData(name: String, data: T): T {
        return pluginData[name, { Value(data) }].data as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getData(name: String, data: ()->T): T {
        return pluginData[name, { Value(data()) }].data as T
    }

    fun read() {
        if (isBlank(fileUtil) || fileUtil!!.notExists() || fileUtil!!.length() < 1) {
            return
        }
        try {
            fileUtil!!.getInputsStream().use { stream -> read(stream) }
        } catch (e: Exception) {
            error("[Read BIN Error]",e)
        }
    }

    fun read(inStream: InputStream?) {
        try {
            DataInputStream(getGzipInputStream(inStream!!)).use { stream ->
                val amount = stream.readInt()
                for (i in 0 until amount) {
                    var length: Int
                    var bytes: ByteArray
                    val key = stream.readUTF()
                    val type = stream.readByte()
                    when (type.toInt()) {
                        0 -> pluginData.put(key, Value(stream.readBoolean()))
                        1 -> pluginData.put(key, Value(stream.readInt()))
                        2 -> pluginData.put(key, Value(stream.readLong()))
                        3 -> pluginData.put(key, Value(stream.readFloat()))
                        4 -> pluginData.put(key, Value(stream.readUTF()))
                        5 -> {
                            /* 把String转为Class,来进行反序列化 */
                            val classCache: Class<*> = Class.forName(stream.readUTF().replace("cn.rwhps.server", "net.rwhps.server"))
                            length = stream.readInt()
                            bytes = ByteArray(length)
                            stream.read(bytes)
                            pluginData.put(key, Value(getObject(classCache, bytes)))
                        }

                        else -> throw IllegalStateException("Unexpected value: $type")
                    }
                }
            }
        } catch (e: EOFException) {
            // 忽略
            warn("数据文件为空 若第一次启动可忽略")
        } catch (e: Exception) {
            error("Read Data", e)
        }
    }

    fun save() {
        if (isBlank(fileUtil) || fileUtil!!.notExists()) {
            return
        }
        try {
            DataOutputStream(getGzipOutputStream(fileUtil!!.writeByteOutputStream(false))).use { stream ->
                stream.writeInt(pluginData.size)
                for (entry in pluginData.entries()) {
                    stream.writeUTF(entry.key)
                    when (val value = entry.value.data!!) {
                        is Boolean -> {
                            stream.writeByte(0)
                            stream.writeBoolean(value)
                        }
                        is Int -> {
                            stream.writeByte(1)
                            stream.writeInt(value)
                        }
                        is Long -> {
                            stream.writeByte(2)
                            stream.writeLong(value)
                        }
                        is Float -> {
                            stream.writeByte(3)
                            stream.writeFloat(value)
                        }
                        is String -> {
                            stream.writeByte(4)
                            stream.writeUTF(value)
                        }
                        else -> {
                            try {
                                val bytes = putBytes(value)
                                stream.writeByte(5)
                                /* 去除ToString后的前缀(class com.xxx~) */
                                stream.writeUTF(value.javaClass.toString().replace("class ", ""))
                                stream.writeInt(bytes.size)
                                stream.write(bytes)
                            } catch (e: IOException) {
                                error("Save Error", e)
                            }
                        }
                    }
                }
                stream.flush()
            }
        } catch (e: Exception) {
            fileUtil!!.file.delete()
            error("Write Data", e)
            throw RuntimeException()
        }
    }

    fun cleanRam() {
        pluginData.clear()
    }

    private fun <T> getObject(type: Class<T>, bytes: ByteArray): T? {
        if (!SERIALIZERS.containsKey(type)) {
            error(IllegalArgumentException("Type $type does not have a serializer registered!"))
            return null
        }
        val serializer = SERIALIZERS[type]
        return try {
            byteInputStream.setBytes(bytes)
            val obj = serializer.read(GameInputStream(byteInputStream)) ?: return null
            @Suppress("UNCHECKED_CAST")
            obj as T
        } catch (e: Exception) {
            null
        }
    }

    @Throws(IOException::class)
    private fun putBytes(value: Any, type: Class<*> = value.javaClass): ByteArray {
        if (!SERIALIZERS.containsKey(type)) {
            error(IllegalArgumentException("Type $type does not have a serializer registered!"))
        }
        byteStream.reset()
        val serializer: SerializerTypeAll.TypeSerializer<Any> = SERIALIZERS[type] as SerializerTypeAll.TypeSerializer<in Any>
        serializer.write(dataOutput, value)
        return byteStream.toByteArray()
    }

    companion object {
        private val SERIALIZERS = ObjectMap<Class<*>, SerializerTypeAll.TypeSerializer<Any>>()

        init {
            register()
        }

        internal fun getSerializer(type: Class<*>): SerializerTypeAll.TypeSerializer<Any>? {
            return SERIALIZERS[type]
        }

        internal fun <T> setSerializer(type: Class<*>, ser: SerializerTypeAll.TypeSerializer<T>) {
            @Suppress("UNCHECKED_CAST")
            SERIALIZERS.put(type, ser as SerializerTypeAll.TypeSerializer<Any>)
        }
    }
}