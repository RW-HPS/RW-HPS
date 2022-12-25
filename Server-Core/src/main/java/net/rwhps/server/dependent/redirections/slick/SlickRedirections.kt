package net.rwhps.server.dependent.redirections.slick

import net.rwhps.asm.agent.AsmAgent
import net.rwhps.asm.redirections.DefaultRedirections
import net.rwhps.server.dependent.redirections.MainRedirections
import net.rwhps.server.util.alone.annotations.NeedHelp
import org.newdawn.slick.Image

@NeedHelp
class SlickRedirections : MainRedirections {
    fun register() {
        AsmAgent.addPartialMethod("org/newdawn/slick/AppGameContainer" , arrayOf("start","()V"))
        redirect(AppGameContainerUpdate.DESC, AppGameContainerUpdate())

        // 去掉游戏的鼠标光标
        AsmAgent.addPartialMethod("org/newdawn/slick/AppGameContainer" , arrayOf("setMouseCursor","(Lorg/newdawn/slick/Image;II)V"))
        redirect("Lorg/newdawn/slick/AppGameContainer;setMouseCursor(Lorg/newdawn/slick/Image;II)V")
        AsmAgent.addPartialMethod("org/newdawn/slick/AppletGameContainer" , arrayOf("setMouseCursor","(Lorg/newdawn/slick/Image;II)V"))
        redirect("Lorg/newdawn/slick/AppletGameContainer;setMouseCursor(Lorg/newdawn/slick/Image;II)V")

        // 替换版本号
        AsmAgent.addPartialMethod("org/newdawn/slick/GameContainer" , arrayOf("getBuildVersion","()I")) { _: Any?, _: String?, _: Class<*>?, _: Array<Any?>? ->
            org.newdawn.slick.util.Log.info("Slick Build : RW-HPS-Headless-Slick RwGame#84")
            0
        }

        // 禁用声音加载器
        AsmAgent.allMethod.add("org/newdawn/slick/Sound")
        redirect("Lorg/newdawn/slick/Sound;playing()Z", DefaultRedirections.BOOLEANT)

        // 替换默认图形处理器
        AsmAgent.addPartialMethod("org/newdawn/slick/opengl/pbuffer/GraphicsFactory" , arrayOf("createGraphics","(Lorg/newdawn/slick/Image;)Lorg/newdawn/slick/Graphics;")) { _: Any?, _: String?, _: Class<*>?, args: Array<Any> ->
            DrGraphics(args[0] as Image)
        }

        // 设置纹理长宽
        AsmAgent.addPartialMethod("org/newdawn/slick/opengl/TextureImpl" , arrayOf("getTextureHeight","()I"), DefaultRedirections.INT)
        AsmAgent.addPartialMethod("org/newdawn/slick/opengl/TextureImpl" , arrayOf("getTextureWidth","()I"), DefaultRedirections.INT)
    }
}