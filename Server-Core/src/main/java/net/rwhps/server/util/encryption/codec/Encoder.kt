/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.encryption.codec
/**
 * 编码接口
 * 编码器必须实现本接口 来完成统一调用默认值
 *
 * @param <T> 被编码的数据类型
 * @param <R> 编码后的数据类型
 */
interface Encoder<T, R> {
    /**
     * 执行编码
     *
     * @param data 被编码的数据
     * @return 编码后的数据
     */
    fun encode(data: T): R
}