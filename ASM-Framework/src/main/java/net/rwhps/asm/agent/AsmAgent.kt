/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.asm.agent

import net.rwhps.asm.api.Redirection
import net.rwhps.asm.api.Transformer
import net.rwhps.asm.redirections.AsmRedirections
import net.rwhps.asm.transformer.AllMethodsTransformer
import net.rwhps.asm.transformer.AsmUtil
import net.rwhps.asm.transformer.PartialMethodTransformer
import org.objectweb.asm.ClassWriter
import java.io.FileOutputStream
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.IllegalClassFormatException
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

/**
 * A JavaAgent calling the [AllMethodsTransformer].
 */
class AsmAgent : ClassFileTransformer {

    private val transformer: Transformer = AllMethodsTransformer()
    private val partialMethodTransformer: Transformer = PartialMethodTransformer()

    @Throws(IllegalClassFormatException::class)
    override fun transform(loader: ClassLoader, className: String, classBeingRedefined: Class<*>?, protectionDomain: ProtectionDomain, classfileBuffer: ByteArray): ByteArray {
        // 这个是单纯的 Debug 用的
        if (className.contains("coSSSSSSSS")) {
            val node = AsmUtil.read(classfileBuffer)
            transformer.transform(node)
            return AsmUtil.write(node, ClassWriter.COMPUTE_FRAMES).also { a -> FileOutputStream("a.class").also { it.write(a); it.flush() }}
        }
        // 覆写 lwjgl 并跳过 RW-HPS 包
        if (className.contains("lwjgl") && !className.contains("rwhps")
            // 匹配 Class 并且替换全部方法
            || allMethod.contains(className)) {
            val node = AsmUtil.read(classfileBuffer)
            transformer.transform(node)
            // TODO: make writer.getClassLoader() return the given loader?
            return AsmUtil.write(node, ClassWriter.COMPUTE_FRAMES)
        }
        // 匹配指定 Class 的指定方法
        if (partialMethod.containsKey(className)) {
            val node = AsmUtil.read(classfileBuffer)
            partialMethodTransformer.transform(node, partialMethod[className])
            return AsmUtil.write(node, ClassWriter.COMPUTE_FRAMES)
                //.also { a -> FileOutputStream("${className.replace("/","")}.class").also { it.write(a); it.flush() }}
        }
        return classfileBuffer
    }

    companion object {
        private val partialMethod = HashMap<String, ArrayList<Array<String>>>()
        val allMethod = ArrayList<String>()

        /**
         * 加入 指定 Class 的指定方法代理
         *
         *
         * @param desc String
         * @param p Array<String>
         * @param redirection Redirection
         */
        @JvmStatic
        @JvmOverloads
        fun addPartialMethod(desc: String, p: Array<String>, redirection: Redirection? = null) {
            if (partialMethod.containsKey(desc)) {
                partialMethod[desc]!!.add(p)
            } else {
                partialMethod[desc] = ArrayList<Array<String>>().also { it.add(p) }
            }
            redirection?.let {
                // 这里构造 Desc 并且 加入对应的 取代
                AsmRedirections.redirect("L"+desc+";"+p[0]+p[1], it)
            }
        }

        fun agentmain(instrumentation: Instrumentation) {
            instrumentation.addTransformer(AsmAgent())
        }
    }
}