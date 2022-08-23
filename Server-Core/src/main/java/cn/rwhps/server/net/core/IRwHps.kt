/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package cn.rwhps.server.net.core

/**
 * RW-HPS Protocol 接口. 是 RW-HPS 协议实现的接口.
 *
 * ## 获取实例
 *
 * 通常在服务器ServerLoad事件后 就可以使用 [ServiceLoader.getIRwHpsObject] 方法获取实例
 *
 * ## 使用 [IRwHps] 的接口
 *
 * [IRwHps] 中的接口通常是稳定
 *
 * ## 稳定性
 *
 * ### 使用稳定
 *
 * 所有接口默认是可以稳定使用的
 *
 * ### 继承不稳定
 *
 * **[IRwHps] 可能会增加新的抽象属性或函数. 因此不适合被继承或实现.**
 *
 * @author RW-HPS/Dr
 */
interface IRwHps {
    /**
     * 服务器的协议
     */
    val typeConnect: TypeConnect

    /**
     * 服务器Packet协议
     */
    val abstractNetPacket: AbstractNetPacket

    enum class NetType {
        /** (默认) Server协议 原汁原味服务器实现 */
        ServerProtocol,
        /** Server协议 Server服务器实现 加入测试功能 */
        ServerTestProtocol,
        /** Relay协议 普通RELAY实现 */
        RelayProtocol,
        /** Relay协议 多播RELAY实现 */
        RelayMulticastProtocol,
        /** ServerFFA协议 混战服务器实现 */
        ServerFFAProtocol,
        /** 无实现 找不到对应实现 */
        NullProtocol;

        companion object {
            fun from(type: String?): NetType = values().find { it.name == type } ?: NullProtocol
        }
    }
}