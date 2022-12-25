/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net.core

import net.rwhps.server.io.GameInputStream
import net.rwhps.server.io.GameOutputStream
import net.rwhps.server.struct.SerializerTypeAll
import net.rwhps.server.util.RandomUtil
import net.rwhps.server.util.StringFilteringUtil
import net.rwhps.server.util.Time
import net.rwhps.server.util.encryption.digest.DigestUtil
import net.rwhps.server.util.log.exp.ImplementedException
import java.io.IOException
import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom

/**
 * Proof-of-Work Verify if it is a client
 *
 * I don't want this part to be abused
 * Cherish what you have at the moment
 *
 * @property resultInt Int
 * @property authenticateType Int
 * @property initInt_1 Int
 * @property initInt_2 Int
 * @property outcome String
 * @property fixedInitial String
 * @property off Int
 * @property maximumNumberOfCalculations Int
 *
 * @author RW-HPS/Dr
 */
internal class NetConnectProofOfWork {
    // Uniqueness
    var resultInt: Int = Time.concurrentSecond()
        private set
    val authenticateType: Byte

    val initInt_1: Int
    val initInt_2: Int

    val outcome: String
    val fixedInitial: String
    val off: Int
    val maximumNumberOfCalculations: Int

    constructor() {
        val rand = ThreadLocalRandom.current()

        // No use 4/6
        val authenticateType = rand.nextInt(0, 6).let {
            if (it == 2) 5 else it
        }

        initInt_1 = if (authenticateType == 0 || authenticateType in 2..4 || authenticateType == 6) rand.nextInt() else 0
        initInt_2 = if (authenticateType == 1 || authenticateType in 2..4) rand.nextInt() else 0

        when (authenticateType) {
            in 3..4 -> {
                outcome = StringFilteringUtil.cutting(
                    BigInteger(1, DigestUtil.sha256("$initInt_1|$initInt_2")).toString(16).uppercase(), 14
                )
                fixedInitial = ""
                off = 0
                maximumNumberOfCalculations = 0
            }
            in 5..6 -> {
                fixedInitial = RandomUtil.getRandomIetterString(4).let { if (authenticateType == 6) "$it$initInt_1" else it }
                off = rand.nextInt(0, 10)
                maximumNumberOfCalculations = rand.nextInt(0, 10000000)
                outcome = StringFilteringUtil.cutting(
                    BigInteger(1, DigestUtil.sha256(fixedInitial + "" + off)).toString(16).uppercase(), 14
                )
            }
            else -> {
                outcome = ""
                fixedInitial = ""
                off = 0
                maximumNumberOfCalculations = 0
            }
        }
        this.authenticateType = authenticateType.toByte()
    }

    // Type 0/1
    private constructor(authenticateType: Byte, initInt_1: Int, initInt_2: Int) {
        this.authenticateType = authenticateType
        this.initInt_1 = initInt_1
        this.initInt_2 = initInt_2

        this.outcome = ""
        fixedInitial = ""
        off = 0
        maximumNumberOfCalculations = 0
    }

    // Type 3/4
    private constructor(authenticateType: Byte, initInt_1: Int, initInt_2: Int, outcome: String) {
        this.authenticateType = authenticateType
        this.initInt_1 = initInt_1
        this.initInt_2 = initInt_2
        this.outcome = outcome

        fixedInitial = ""
        off = 0
        maximumNumberOfCalculations = 0
    }

    // Type 5/6
    private constructor(authenticateType: Byte, fixedInitial: String, off: Int, maximumNumberOfCalculations: Int, outcome: String) {
        this.authenticateType = authenticateType
        this.fixedInitial = fixedInitial
        this.off = off
        this.maximumNumberOfCalculations = maximumNumberOfCalculations
        this.outcome = outcome

        this.initInt_1 = 0
        this.initInt_2 = 0
    }

    fun check(resultInt: Int, authenticateType: Int, offIn: String): Boolean {
        // The fucking check code is wrong, get out of here
        if (this.resultInt != resultInt || this.authenticateType.toInt() != authenticateType) {
            return false
        }

        when (authenticateType) {
            0 -> return initInt_1.toString() == offIn
            1 -> return initInt_2.toString() == offIn
            2 -> throw ImplementedException("too much trouble")
            3,4 -> return outcome == offIn
            5,6 -> return this.off == offIn.toInt()
            else -> {}
        }

        return true
    }

    companion object {
        /**
         * Serialize Deserialize NetConnectAuthenticate
         * thrift CPU
         */
        internal val serializer = object : SerializerTypeAll.TypeSerializer<NetConnectProofOfWork> {
            @Throws(IOException::class)
            override fun write(stream: GameOutputStream, objectData: NetConnectProofOfWork) {
                stream.writeByte(objectData.authenticateType)
                when (objectData.authenticateType.toInt()) {
                    0 -> stream.writeInt(objectData.initInt_1)
                    1 -> stream.writeInt(objectData.initInt_2)
                    3,4 -> {
                        stream.writeInt(objectData.initInt_1)
                        stream.writeInt(objectData.initInt_2)
                        stream.writeString(objectData.outcome)
                    }
                    5,6 -> {
                        stream.writeString(objectData.fixedInitial)
                        stream.writeInt(objectData.off)
                        stream.writeInt(objectData.maximumNumberOfCalculations)
                        stream.writeString(objectData.outcome)
                    }
                    else -> {}
                }
            }

            @Throws(IOException::class)
            override fun read(stream: GameInputStream): NetConnectProofOfWork {
                val authenticateType = stream.readByte().toByte()
                return when (authenticateType.toInt()) {
                    0 -> NetConnectProofOfWork(authenticateType,stream.readInt(),0)
                    1 -> NetConnectProofOfWork(authenticateType,0,stream.readInt())
                    3,4 -> NetConnectProofOfWork(authenticateType,stream.readInt(),stream.readInt(),stream.readString())
                    5,6 -> NetConnectProofOfWork(authenticateType,stream.readString(),stream.readInt(),stream.readInt(),stream.readString())
                    else -> NetConnectProofOfWork()
                }
            }
        }
    }
}