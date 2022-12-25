/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.log

import net.rwhps.server.data.global.Data
import net.rwhps.server.plugin.Plugin
import net.rwhps.server.util.Time.getMilliFormat
import net.rwhps.server.util.file.FileUtil
import java.io.PrintWriter
import java.io.StringWriter
import java.text.MessageFormat

/**
 * Log Util
 * @author RW-HPS/Dr
 * @version 1.1
 * @date 2020年3月8日星期日 3:54
 * 练手轮子? :P
 */
object Log {
    /** 默认 WARN  */
    private var LOG_GRADE = 5
    private lateinit var logPrint: (Boolean,String) -> Unit
    private val LOG_CACHE = StringBuilder()

    @JvmStatic
	fun set(log: String) {
        LOG_GRADE = Logg.valueOf(log).getLogg()
    }

    @JvmStatic
	fun setCopyPrint(system: Boolean) {
        logPrint =
            if (system) {
                { error:Boolean, text: String ->
                    println(text)

                    if (error) {
                        // Remove Color
                        var textCache = text
                        for (i in ColorCodes.VALUES.indices) {
                            textCache = textCache.replace(ColorCodes.VALUES[i], "")
                        }
                        LOG_CACHE.append(textCache).append(Data.LINE_SEPARATOR)
                    }
                }
            } else {
                { _:Boolean, text: String ->
                    println(text)
                }
            }
    }

    @JvmStatic
	val logCache: String
        get() {
            val result = LOG_CACHE.toString()
            LOG_CACHE.delete(0, LOG_CACHE.length)
            return result
        }

    /**
     * Log：
     * tag 标题 默认警告级
     */
    @JvmStatic
    fun skipping(e: Any) {
        skipping("SKIPPING",e)
    }
    @JvmStatic
    fun skipping(tag: Any, e: Any) {
        logs(9, tag, e)
    }

    @JvmStatic
    fun fatal(e: Exception) {
        log(7, "FATAL", e)
    }
    @JvmStatic
    fun fatal(tag: Any, e: Exception) {
        log(7, tag, e)
    }
    @JvmStatic
    fun fatal(e: Any) {
        logs(7, "FATAL", e)
    }
    @JvmStatic
    fun fatal(tag: Any, e: Any) {
        logs(7, tag, e)
    }

    @JvmStatic
    fun error(e: Exception) {
        log(6, "ERROR", e)
    }
    @JvmStatic
    fun error(tag: Any, e: Exception) {
        log(6, tag, e)
    }
    @JvmStatic
    fun error(e: Any) {
        logs(6, "ERROR", e)
    }

    @JvmStatic
	fun error(tag: Any, e: Any) {
        logs(6, tag, e)
    }

    @JvmStatic
    fun warn(e: Exception) {
        log(5, "WARN", e)
    }
    @JvmStatic
    fun warn(tag: Any, e: Exception) {
        log(5, tag, e)
    }
    @JvmStatic
    fun warn(e: Any) {
        logs(5, "WARN", e)
    }
    @JvmStatic
	fun warn(tag: Any, e: Any) {
        logs(5, tag, e)
    }

    @JvmStatic
    fun info(e: Exception) {
        log(4, "INFO", e)
    }
    @JvmStatic
    fun info(tag: Any, e: Exception) {
        log(4, tag, e)
    }
    @JvmStatic
    fun info(e: Any) {
        logs(4, "INFO", e)
    }
    @JvmStatic
	fun info(tag: Any, e: Any) {
        logs(4, tag, e)
    }

    @JvmStatic
    fun debug(e: Exception) {
        log(3, "DEBUG", e)
    }
    @JvmStatic
    fun debug(tag: Any, e: Exception) {
        log(3, tag, e)
    }
    @JvmStatic
    fun debug(e: Any) {
        logs(3, "DEBUG", e)
    }
    @JvmStatic
	fun debug(tag: Any, e: Any) {
        logs(3, tag, e)
    }

    @JvmStatic
    fun track(e: Exception) {
        log(2, "TRACK", e)
    }
    @JvmStatic
    fun track(tag: Any, e: Exception) {
        log(2, tag, e)
    }

    @JvmStatic
    fun all(e: Exception) {
        log(1, "ALL", e)
    }
    @JvmStatic
    fun all(tag: Any, e: Exception) {
        log(1, tag, e)
    }
    @JvmStatic
    fun all(e: Any) {
        logs(1, "ALL", e)
    }
    @JvmStatic
    fun all(tag: Any, e: Any) {
        logs(1, tag, e)
    }

    @JvmStatic
    fun clog(text: String) {
        val textCache = "[" + getMilliFormat(1) + "] " + text
        this.logPrint(false,formatColors("$textCache&fr"))
    }

    @JvmStatic
	fun clog(text: String, vararg obj: Any?) {
        clog(MessageFormat(text).format(obj))
    }

    @JvmStatic
    fun testPrint(`object`: Any) {
        info(`object`)
    }

    @JvmStatic
    fun testPlugin(plugin: Plugin) {
        plugin.init()
    }

    @JvmStatic
    fun saveLog() {
        val fileUtil = FileUtil.getFolder(Data.Plugin_Log_Path).toFile("Log.txt")
        fileUtil.writeFile(logCache, fileUtil.file.length() <= 512 * 1024)
    }

    /**
     * WLog：
     * @param i Warning level -INT
     * @param tag Title / Default warning level-String
     * @param e Exception
     * i>=Set the level to write to the file
     */
    private fun log(i: Int, tag: Any, e: Exception) {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        e.printStackTrace(printWriter)
        logs(i, tag, stringWriter.buffer, true)
    }

    private fun logs(i: Int, tag: Any, e: Any, error: Boolean = false) {
        if (this.LOG_GRADE > i && !error) {
            return
        }
        val sb = StringBuilder()
        val lines = e.toString().split(Data.LINE_SEPARATOR).toTypedArray()
        val stack = Throwable().stackTrace
        var i1 = 0
        while (i1 < stack.size) {
            val ste = stack[i1]
            val className = ste.className + "." + ste.methodName
            if (!className.contains("net.rwhps.server.util.log.Log")) {
                sb.append("[").append(ste.fileName).append("] : ")
                    .append(ste.methodName).append(" : ").append(ste.lineNumber).append(Data.LINE_SEPARATOR)
                break
            }
            i1++
        }
        // [Time] Tag:
        // Info
        sb.append("[").append(getMilliFormat(1)).append("] ").append(tag).append(": ")

        // 避免换行
        if (lines.isNotEmpty()) {
            sb.append(Data.LINE_SEPARATOR)
        }

        for (line in lines) {
            sb.append(line)
                .append(Data.LINE_SEPARATOR)
        }

        // 去掉最后的换行
        sb.deleteCharAt(sb.length -1)

        this.logPrint(error,sb.toString())
    }

    private fun formatColors(text: String): String {
        var textCache = text
        for (i in ColorCodes.CODES.indices) {
            textCache = textCache.replace("&${ColorCodes.CODES[i]}" , ColorCodes.VALUES[i])
            textCache = textCache.replace("[${ColorCodes.CODES[i]}]", ColorCodes.VALUES[i])
        }
        return textCache
    }

    private enum class Logg(private val logg: Int) {
        /* Log level defaults to WARN */
        /* ALL during development */
        OFF(8), FATAL(7), ERROR(6), WARN(5), INFO(4), DEBUG(3), TRACE(2), ALL(1);

        open fun getLogg(): Int {
            return logg
        }
    }

    private interface LogPrint<T> {
        /**
         * 接管Log逻辑
         * @param t TEXT
         */
        fun write(t: T)
    }
}