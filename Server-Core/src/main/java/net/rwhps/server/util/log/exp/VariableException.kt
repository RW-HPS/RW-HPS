/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.log.exp

import net.rwhps.server.util.log.ErrorCode

/**
 * @author RW-HPS/Dr
 */
class VariableException(info: String) : RuntimeException(info) {
    class ArrayRuntimeException(info: String) : Exception(info)

    class ObjectMapRuntimeException(info: String) : RuntimeException(info)

    class MapRuntimeException(type: String) : RuntimeException(ErrorCode.valueOf(type).error)

    class RepeatAddException(info: String) : Exception(info)

    class TabooAddException(info: String) : Exception(info)

    class TypeMismatchException(info: String) : Exception(info)
}