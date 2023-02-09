/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.UnknownHostException

/**
 * IP 工具类
 * @author RW-HPS/Dr
 */
object IpUtil {
    /**
     * 获取本机内网ip
     * @return
     * @throws UnknownHostException
     */
    @JvmStatic
    @Throws(UnknownHostException::class)
    fun getPrivateIp(): String? {
        val allNetInterfaces = NetworkInterface.getNetworkInterfaces()
        var ip: InetAddress? = null
        while (allNetInterfaces.hasMoreElements()) {
            val netInterface: NetworkInterface = allNetInterfaces.nextElement()
            if (netInterface.isLoopback || netInterface.isVirtual || !netInterface.isUp) {
                continue
            } else {
                val addresses = netInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    ip = addresses.nextElement()
                    if (ip is Inet4Address) return ip.getHostAddress()
                }
            }
        }
        return null
    }

    @JvmStatic
    fun ipToLong24(strIp: String, separateAddress: Boolean = true): String {
        return ipToLong(strIp,separateAddress)
    }

    @JvmStatic
    fun longToIp(strLong: String): String {
        return longToIP(strLong.toLong())
    }

    /**
     * ip地址转成long型数字
     * 将IP地址转化成整数的方法如下：
     * 1、通过String的split方法按.分隔得到4个长度的数组
     * 2、通过左移位操作（<<）给每一段的数字加权，第一段的权为2的24次方，第二段的权为2的16次方，第三段的权为2的8次方，最后一段的权为1
     *
     * 忽略最后一位 直接取段
     * @param strIp
     * @return
     */
    private fun ipToLong(strIp: String, separateAddress: Boolean): String {
        if (strIp == "0") {
            return strIp
        }
        val ip = strIp.split(".").toTypedArray()
        return (((ip[0].toLong() shl 24) + (ip[1].toLong() shl 16) + (ip[2].toLong() shl 8)) + if (separateAddress) ip[3].toLong() else 0).toString()
    }

    /**
     * 将十进制整数形式转换成127.0.0.1形式的ip地址
     * 将整数形式的IP地址转化成字符串的方法如下：
     * 1、将整数值进行右移位操作（>>>），右移24位，右移时高位补0，得到的数字即为第一段IP。
     * 2、通过与操作符（&）将整数值的高8位设为0，再右移16位，得到的数字即为第二段IP。
     * 3、通过与操作符吧整数值的高16位设为0，再右移8位，得到的数字即为第三段IP。
     * 4、通过与操作符吧整数值的高24位设为0，得到的数字即为第四段IP。
     * @param longIp
     * @return
     */
    private fun longToIP(longIp: Long): String {
        val sb = StringBuffer("")
        // 直接右移24位
        sb.append((longIp ushr 24).toString()).append(".")
        // 将高8位置0，然后右移16位
        sb.append((longIp and 0x00FFFFFF ushr 16).toString()).append(".")
        // 将高16位置0，然后右移8位
        sb.append((longIp and 0x0000FFFF ushr 8).toString()).append(".")
        // 将高24位置0
        sb.append((longIp and 0x000000FF).toString())
        return sb.toString()
    }
}