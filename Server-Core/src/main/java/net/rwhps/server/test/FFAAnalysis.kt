/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.test

import net.rwhps.server.game.GameUnitType
import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.util.compression.CompressionDecoderUtils
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.log.Log

class FFAAnalysis {
    fun test() {
        val bytes = GameOutputStream()
        CompressionDecoderUtils.zipStream(FileUtil.getFile("ffa.zip").getInputsStream()).getSpecifiedSuffixInThePackage("bin").also {
            for (i in 0..3542) {
                bytes.writeBytes(it[i.toString()])
            }
        }

        val read = GameInputStream(bytes.getPacketBytes())
        while (read.getSize() > 0) {
            val leghtSkip = read.readInt()
            if (read.readInt() != 10) {
                read.skip(leghtSkip.toLong())
            } else {
                read.readInt()
                //Log.debug("Tick",)
                read.readInt().also {
                    //Log.debug("Command Length",it)

                    for (i in 0 until it) {
                        read.getDecodeStream(false).also { c ->
                            c.readByte()
                            //Log.debug("Team",)
                            c.readBoolean().also {
                                //Log.debug("Build",it)
                                if (it) {
                                    c.readInt()
                                   // Log.debug("Build GameActions", c.readEnum(GameUnitType.GameActions::class.java)!!.name)
                                    val em = c.readEnum(GameUnitType.GameUnits::class.java)

                                    if (em == GameUnitType.GameUnits.damagingBorder || em == GameUnitType.GameUnits.zoneMarker) {
                                        Log.debug("Build GameUnits", em.name)
                                        Log.debug("Get Size", c.getSize())

                                        Log.debug("X", c.readFloat())
                                        Log.debug("Y", c.readFloat())
                                        Log.debug("Tg", c.readLong())

                                        Log.debug("?", c.readByte())
                                        Log.debug("?", c.readFloat())
                                        Log.debug("?", c.readFloat())

                                        Log.debug("?", c.readBoolean())
                                        Log.debug("?", c.readBoolean())
                                        Log.debug("?", c.readBoolean())
                                        Log.debug("?", c.readBoolean())
                                        Log.debug("?", c.readBoolean())
                                        Log.debug("?", c.readBoolean())

                                        Log.debug("?", c.readInt())
                                        Log.debug("?", c.readInt())

                                        Log.debug("?", c.readBoolean())
                                        Log.debug("?", c.readBoolean())

                                        Log.debug("?", c.readInt())

                                        Log.debug("?", c.readBoolean())
                                        Log.debug("?", c.readBoolean())

                                        Log.debug("?", c.readLong())
                                        Log.debug("?", c.readString())
                                        Log.debug("?", c.readBoolean())
                                        Log.debug("?", c.readShort())

                                        Log.debug("S?", c.readBoolean())
                                        Log.debug("S?", c.readByte())
                                        Log.debug("S?", c.readFloat())
                                        Log.debug("S?", c.readFloat())

                                        Log.debug("SA?", c.readInt())
                                        Log.debug("SA?", c.readInt())
                                        Log.debug("SA?", c.readBoolean())
                                        Log.debug("Get Size", c.getSize())

                                    }
                                }
                            }

                        }
                    }
                }
            }


        }


    }
}