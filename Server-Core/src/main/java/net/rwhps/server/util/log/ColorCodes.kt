/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.log

import net.rwhps.server.struct.ObjectMap

/**
 * @author RW-HPS/Dr
 */
internal object ColorCodes {
    private const val FLUSH = "\u001b[H\u001b[2J"
    private const val RESET = "\u001B[0m"
    private const val BOLD = "\u001B[1m"
    private const val ITALIC = "\u001B[3m"
    private const val UNDERLINED = "\u001B[4m"
    private const val BLACK = "\u001B[30m"
    private const val RED = "\u001B[31m"
    private const val GREEN = "\u001B[32m"
    private const val YELLOW = "\u001B[33m"
    private const val BLUE = "\u001B[34m"
    private const val PURPLE = "\u001B[35m"
    private const val CYAN = "\u001B[36m"
    private const val LIGHT_RED = "\u001B[91m"
    private const val LIGHT_GREEN = "\u001B[92m"
    private const val LIGHT_YELLOW = "\u001B[93m"
    private const val LIGHT_BLUE = "\u001B[94m"
    private const val LIGHT_MAGENTA = "\u001B[95m"
    private const val LIGHT_CYAN = "\u001B[96m"
    private const val WHITE = "\u001B[37m"
    private const val BACK_DEFAULT = "\u001B[49m"
    private const val BACK_RED = "\u001B[41m"
    private const val BACK_GREEN = "\u001B[42m"
    private const val BACK_YELLOW = "\u001B[43m"
    private const val BACK_BLUE = "\u001B[44m"
    internal val CODES: Array<String>
    internal val VALUES: Array<String>

    init {
        //WIN :(
        val map: ObjectMap<String, String> = if (System.getProperty("rwhps.log.color","true").toBoolean()) {
            ObjectMap.of(
                "ff", FLUSH,
                "fr", RESET,
                "fb", BOLD,
                "fi", ITALIC,
                "fu", UNDERLINED,
                "bk", BLACK,
                "r", RED,
                "g", GREEN,
                "y", YELLOW,
                "b", BLUE,
                "p", PURPLE,
                "c", CYAN,
                "lr", LIGHT_RED,
                "lg", LIGHT_GREEN,
                "ly", LIGHT_YELLOW,
                "lm", LIGHT_MAGENTA,
                "lb", LIGHT_BLUE,
                "lc", LIGHT_CYAN,
                "w", WHITE,
                "bd", BACK_DEFAULT,
                "br", BACK_RED,
                "bg", BACK_GREEN,
                "by", BACK_YELLOW,
                "bb", BACK_BLUE
            )
        } else {
            ObjectMap.of(
                "ff", "",
                "fr", "",
                "fb", "",
                "fi", "",
                "fu", "",
                "bk", "",
                "r", "",
                "g", "",
                "y", "",
                "b", "",
                "p", "",
                "c", "",
                "lr", "",
                "lg", "",
                "ly", "",
                "lm", "",
                "lb", "",
                "lc", "",
                "w", "",
                "bd", "",
                "br", "",
                "bg", "",
                "by", "",
                "bb", ""
            )
        }


        CODES = map.keys().toSeq().toArray(String::class.java)
        VALUES = map.values().toSeq().toArray(String::class.java)
    }
}