/*
 * Copyright 2020-2023 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util

import net.rwhps.server.data.global.ArrayData
import net.rwhps.server.data.global.RegexData
import java.math.BigInteger
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.UnknownHostException
import java.util.*
import java.util.regex.Pattern

/**
 * IP 工具类
 * @author Dr (dr@der.kim)
 */
object IpUtils {
    private val IPV4_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")
    private const val IPV4_MAX_OCTET_VALUE = 255

    private const val IPV6_MAX_HEX_GROUPS = 8
    private const val IPV6_MAX_HEX_DIGITS_PER_GROUP = 4
    private const val MAX_UNSIGNED_SHORT = 0xffff
    private const val BASE_16 = 16

    private val REG_NAME_PART_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9-]*$")

    /**
     * 获取本机内网ip
     * @return
     * @throws UnknownHostException
     * @author RW-HPS/fgsqme
     */
    @JvmStatic
    @Throws(UnknownHostException::class)
    fun getPrivateIp(): String? {
        val allNetInterfaces = NetworkInterface.getNetworkInterfaces()
        var ip: InetAddress?
        while (allNetInterfaces.hasMoreElements()) {
            val netInterface: NetworkInterface = allNetInterfaces.nextElement()
            if (netInterface.isLoopback || netInterface.isVirtual || !netInterface.isUp) {
                continue
            } else {
                val addresses = netInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    ip = addresses.nextElement()
                    if (ip is Inet4Address) {
                        return ip.hostAddress
                    }
                }
            }
        }
        return null
    }

    @JvmStatic
    fun ipToLong24(strIp: String, separateAddress: Boolean = true): String {
        return ipToLong(strIp, separateAddress)
    }

    @JvmStatic
    fun longToIp(strLong: String): String {
        return longToIP(strLong.toLong())
    }


    /**
     * 通过字符串判断是否为ipv4
     */
    fun isIPv4(address: String): Boolean {
        val ipv4Pattern = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
        )
        return ipv4Pattern.matcher(address).matches()
    }

    /**
     * 通过字符串判断是否为ipv6
     */
    fun isIPv6(address: String): Boolean {
        val ipv6Pattern = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}([0-9a-fA-F]{1,4}|:)$|" +
                    "^([0-9a-fA-F]{1,4}:){1,7}:([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}$|" +
                    "^([0-9a-fA-F]{1,4}:){1,6}:([0-9a-fA-F]{1,4}:){0,7}|^::1$|" +
                    "^::([fF]{4}:)?([0-9]{1,3}\\.){3}[0-9]{1,3}$|" +
                    "^([0-9a-fA-F]{1,4}:){1,4}:[0-9]{1,3}\\.([0-9]{1,3}\\.){2}[0-9]{1,3}$"
        )
        return ipv6Pattern.matcher(address).matches()
    }

    /**
     * 通过long值判断是否为ipv4
     */
    private fun isIPv4(ipAsLong: Long): Boolean {
        // IPv4的长整数范围在 0.0.0.0 到 255.255.255.255 之间
        return ipAsLong >= 0L && ipAsLong <= 4294967295L
    }

    /**
     * 通过long值判断是否为ipv6
     */
    private fun isIPv6(ipAsLong: Long): Boolean {
        // IPv6的长整数范围大于IPv4，具体范围需要根据实际情况调整
        return ipAsLong > 4294967295L
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
        if (isIPv4(strIp)) {
            val ip = strIp.split(".").toTypedArray()
            return (((ip[0].toLong() shl 24) + (ip[1].toLong() shl 16) + (ip[2].toLong() shl 8)) + if (separateAddress) ip[3].toLong() else 0).toString()
        }
        if (isIPv6(strIp)) {
            val address = InetAddress.getByName(strIp)
            val bytes = address.address
            val bigInteger = BigInteger(1, bytes)
            return bigInteger.toLong().toString()
        }
        return ""
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

    /**
     * Checks whether a given string is a valid host name according to
     * RFC 3986.
     *
     *
     * Accepted are IP addresses (v4 and v6) as well as what the
     * RFC calls a "reg-name". Percent encoded names don't seem to be
     * valid names in UNC paths.
     *
     * @see "https://tools.ietf.org/html/rfc3986.section-3.2.2"
     *
     * @param name the hostname to validate
     * @return true if the given name is a valid host name
     */
    @JvmStatic
    fun isValidHostName(name: String): Boolean {
        return isIPv6Address(name) || isRFC3986HostName(name)
    }

    /**
     * Checks whether a given string represents a valid IPv4 address.
     *
     * @param name the name to validate
     * @return true if the given name is a valid IPv4 address
     */
    // mostly copied from org.apache.commons.validator.routines.InetAddressValidator#isValidInet4Address
    private fun isIPv4Address(name: String): Boolean {
        val m = IPV4_PATTERN.matcher(name)
        if (!m.matches() || m.groupCount() != 4) {
            return false
        }

        // verify that address subgroups are legal
        for (i in 1 .. 4) {
            val ipSegment = m.group(i)
            val iIpSegment = ipSegment.toInt()
            if (iIpSegment > IPV4_MAX_OCTET_VALUE) {
                return false
            }
            if (ipSegment.length > 1 && ipSegment.startsWith("0")) {
                return false
            }
        }
        return true
    }

    /**
     * Checks whether a given string represents a valid IPv6 address.
     *
     * @param inet6Address the name to validate
     * @return true if the given name is a valid IPv6 address
     */
    private fun isIPv6Address(inet6Address: String): Boolean {
        val containsCompressedZeroes = inet6Address.contains("::")
        if (containsCompressedZeroes && inet6Address.indexOf("::") != inet6Address.lastIndexOf("::")) {
            return false
        }
        if (inet6Address.startsWith(":") && !inet6Address.startsWith("::") || inet6Address.endsWith(":") && !inet6Address.endsWith("::")) {
            return false
        }
        var octets = inet6Address.split(RegexData.colon).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (containsCompressedZeroes) {
            val octetList: ArrayList<String> = ArrayList(listOf(*octets))
            if (inet6Address.endsWith("::")) {
                // String.split() drops ending empty segments
                octetList.add("")
            } else if (inet6Address.startsWith("::") && octetList.isNotEmpty()) {
                octetList.removeAt(0)
            }
            octets = octetList.toArray(ArrayData.stringArray)
        }
        if (octets.size > IPV6_MAX_HEX_GROUPS) {
            return false
        }
        var validOctets = 0
        // consecutive empty chunks
        var emptyOctets = 0
        for (index in octets.indices) {
            val octet = octets[index]
            if (octet.isEmpty()) {
                emptyOctets++
                if (emptyOctets > 1) {
                    return false
                }
            } else {
                emptyOctets = 0
                // Is last chunk an IPv4 address?
                if (index == octets.size - 1 && octet.contains(".")) {
                    if (!isIPv4Address(octet)) {
                        return false
                    }
                    validOctets += 2
                    continue
                }
                if (octet.length > IPV6_MAX_HEX_DIGITS_PER_GROUP) {
                    return false
                }
                val octetInt: Int = try {
                    octet.toInt(BASE_16)
                } catch (e: NumberFormatException) {
                    return false
                }
                if (octetInt < 0 || octetInt > MAX_UNSIGNED_SHORT) {
                    return false
                }
            }
            validOctets++
        }
        return validOctets <= IPV6_MAX_HEX_GROUPS && (validOctets >= IPV6_MAX_HEX_GROUPS || containsCompressedZeroes)
    }

    /**
     * Checks whether a given string is a valid host name according to
     * RFC 3986 - not accepting IP addresses.
     *
     * @see "https://tools.ietf.org/html/rfc3986.section-3.2.2"
     *
     * @param name the hostname to validate
     * @return true if the given name is a valid host name
     */
    private fun isRFC3986HostName(name: String): Boolean {
        val parts = name.split("\\.".toRegex()).toTypedArray()
        for (i in parts.indices) {
            if (parts[i].isEmpty()) {
                // trailing dot is legal, otherwise we've hit a .. sequence
                return i == parts.size - 1
            }
            if (!REG_NAME_PART_PATTERN.matcher(parts[i]).matches()) {
                return false
            }
        }
        return true
    }
}
