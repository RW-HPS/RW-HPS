/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.asm.api;

import net.rwhps.asm.transformer.AllMethodsTransformer;
import net.rwhps.asm.RedirectionManagerImpl;

public class RedirectionApi {
    private RedirectionApi() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /*
        Not using a ServiceLoader for now, modularized environments with
        multiple ClassLoaders cause some issues.
    */
    private static final RedirectionManager REDIRECTION_MANAGER = new RedirectionManagerImpl();

    /**
     * @return an implementation of the {@link RedirectionManager}.
     */
    public static RedirectionManager getRedirectionManager() {
        return REDIRECTION_MANAGER;
    }

    /**
     * {@link AllMethodsTransformer}.
     *
     * @see Redirection
     */
    @SuppressWarnings("unused") // used by the transformer
    public static Object invoke(Object obj, String desc, Class<?> type,
                                Object... args) throws Throwable {
        return REDIRECTION_MANAGER.invoke(obj, desc, type, args);
    }

}
