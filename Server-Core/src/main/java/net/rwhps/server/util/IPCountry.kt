/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util

import net.rwhps.server.util.compression.CompressionDecoderUtils
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.log.Log
import org.lionsoul.ip2region.DbConfig
import org.lionsoul.ip2region.DbSearcher

object IPCountry {
    private val searcher: DbSearcher = DbSearcher(DbConfig(),
        CompressionDecoderUtils.lz77Stream(FileUtil.getInternalFileStream("/ip2region.7z"))
            .getSpecifiedSuffixInThePackage("db",true)["ip2region.db"])

    fun test() {
        Log.clog(searcher.memorySearch("111.173.64.99").region.toString())
        Log.clog(searcher.memorySearch("47.106.105.236").region.toString())
        Log.clog(searcher.memorySearch("222.66.202.6").region.toString())
        Log.clog(searcher.memorySearch("120.194.55.139").region.toString())
        Log.clog(searcher.memorySearch("61.164.39.68").region.toString())
    }

    @JvmSynthetic
    @JvmStatic
    fun getIpCountry(ip: String): String {
        //中国
        val array = searcher.memorySearch(ip).region.split("|")
        if (array.size < 5) {
            return ""
        }
        return array[0]
    }

    @JvmSynthetic
    @JvmStatic
    fun getIpCountryAll(ip: String): String {
        //中国|亚洲|湖北|十堰|电信
        val array = searcher.memorySearch(ip).region.split("|")
        if (array.size < 5) {
            return ""
        }
        return "${array[0]}${array[2]}${array[3]}"
    }
}