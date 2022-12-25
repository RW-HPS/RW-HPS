/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.asm.redirections;

import net.rwhps.asm.api.Redirection;
import net.rwhps.asm.api.RedirectionManager;

import java.util.HashMap;

public class AsmRedirections {
    public static HashMap<String, Redirection> customRedirection = new HashMap<>();

    public static void redirect(String desc, Redirection redirection) {
        customRedirection.put(desc, redirection);
    }

    public static void register(RedirectionManager manager) {
        customRedirection.forEach(manager::redirect);
    }

}
