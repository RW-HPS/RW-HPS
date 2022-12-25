package net.rwhps.server.dependent.redirections

import android.content.res.AssetManager
import com.corrodinggames.rts.gameFramework.e.c
import com.corrodinggames.rts.gameFramework.l
import com.corrodinggames.rts.gameFramework.utility.ae
import com.corrodinggames.rts.gameFramework.utility.af
import com.corrodinggames.rts.gameFramework.utility.j
import net.rwhps.asm.agent.AsmAgent
import net.rwhps.server.data.global.Data
import net.rwhps.server.dependent.redirections.slick.ZipFileSystemLocation
import net.rwhps.server.struct.Seq
import net.rwhps.server.util.alone.annotations.GameSimulationLayer
import net.rwhps.server.util.compression.CompressionDecoderUtils
import net.rwhps.server.util.file.FileUtil
import net.rwhps.server.util.log.Log
import org.newdawn.slick.util.ClasspathLocation
import org.newdawn.slick.util.FileSystemLocation
import org.newdawn.slick.util.ResourceLoader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * 通过 ASM 来覆写 游戏的文件系统来达到自定义位置
 * 不要骂了 只能这样写了
 *
 * @property font 把字体放在ZIP内来减少大小
 * @author RW-HPS/Dr
 */
@GameSimulationLayer.GameSimulationLayer_KeyWords("FileLoader: ")
class FileLoaderRedirections : MainRedirections {
    private val font = ZipFileSystemLocation(CompressionDecoderUtils.lz77Stream(FileUtil.getInternalFileStream("/font.7z")))

    init {
        // Mkdie Mods Folder
        FileUtil.getFolder(Data.Plugin_Mods_Path).mkdir()

        // Get rid of the pile of garbage that comes with you
        ResourceLoader.removeAllResourceLocations()

        ResourceLoader.addResourceLocation(ClasspathLocation())
        ResourceLoader.addResourceLocation(font)
        ResourceLoader.addResourceLocation(FileSystemLocation(FileUtil.getFolder(Data.Plugin_GameCore_Data_Path).file))
    }

    fun register() {
        // 重定向部分文件系统 (mods maps replay)
        val filePath = FileUtil.getPath(Data.Plugin_Data_Path)+"/"
        // 设置 重定向文件PATH类
        AsmAgent.addPartialMethod("com/corrodinggames/rts/gameFramework/e/c" , arrayOf("f","()Ljava/lang/String;")) { _: Any?, _: String?, _: Class<*>?, _: Array<Any?>? ->
            filePath
        }
        AsmAgent.addPartialMethod("com/corrodinggames/rts/gameFramework/e/c" , arrayOf("b","()Ljava/lang/String;")) { _: Any?, _: String?, _: Class<*>?, _: Array<Any?>? ->
            filePath
        }

        // 重定向资源文件系统 (Res FileSystem)
        val resAndAssetsPath = FileUtil.getPath(Data.Plugin_GameCore_Data_Path)+"/"
        AsmAgent.addPartialMethod("com/corrodinggames/rts/gameFramework/e/c" , arrayOf("a","(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")) { _: Any?, _: String?, _: Class<*>?, args: Array<Any> ->
            val listFiles: Seq<File> = FileUtil.getFolder(Data.Plugin_GameCore_Data_Path).toFolder(args[0].toString()).fileList
            for (file in listFiles) {
                var name: String = file.name
                if (name.contains(".")) {
                    name = name.substring(0, name.lastIndexOf(46.toChar()))
                }
                if (name == args[1]) {
                    return@addPartialMethod "${resAndAssetsPath}${args[0]}/${file.name}"
                }
            }
            return@addPartialMethod null
        }

        // 重定向资源文件系统 (Assets FileSystem)
        AsmAgent.addPartialMethod("android/content/res/AssetManager" , arrayOf("a","(Ljava/lang/String;I)Ljava/io/InputStream;")) { _: Any?, _: String?, _: Class<*>?, args: Array<Any> ->
            return@addPartialMethod FileInputStream(resAndAssetsPath+"assets/" + args[0])
        }

        AsmAgent.addPartialMethod("com/corrodinggames/rts/gameFramework/e/c" , arrayOf("f","(Ljava/lang/String;)Ljava/lang/String;")) { obj: Any, _: String?, _: Class<*>?, args: Array<Any> ->
            val obj = obj as c
            var d: String = obj.d(args[0].toString()).replace("\\","/")

            // Map Load 连续两次走这个 (存疑)
            if (args[0].toString().startsWith(obj.b())) {
                return@addPartialMethod args[0].toString()
            }

            /* 覆写 MOD 加载器路径 */
            val overrideModLoad = fun(path: String): String {
                val modFolder = "mods/units"
                if (path.startsWith(modFolder)) {
                    return if (path == modFolder) {
                        FileUtil.getPath(Data.Plugin_Mods_Path)
                    } else {
                        // 一个小问题, 来自 splicePath , 他会默认在屁股后面加一个 /
                        FileUtil.splicePath(FileUtil.getPath(Data.Plugin_Mods_Path),path.substring(modFolder.length))
                    }

                }
                return resAndAssetsPath + path
            }

            return@addPartialMethod if (l.aU) {
                if (d.startsWith("/SD/rusted_warfare_maps")) {
                    d = "/SD/mods/maps" + d.substring("/SD/rusted_warfare_maps".length)
                    l.e(obj.a + "convertAbstractPath: Changing to:" + d)
                }
                if (d.startsWith("/SD/rustedWarfare/maps")) {
                    d = "/SD/mods/maps" + d.substring("/SD/rustedWarfare/maps".length)
                    l.e(obj.a + "convertAbstractPath2: Changing to:" + d)
                }

                if (d.startsWith("/SD/") || d.startsWith("\\SD\\")) {
                    var substring = d.substring("/SD/".length)
                    if (substring.startsWith("rustedWarfare/")) {
                        substring = substring.substring("rustedWarfare/".length)
                    }
                    overrideModLoad(substring)
                } else if (obj.c(d)) {
                    d
                } else {
                    resAndAssetsPath + "assets/$d"
                }
            } else if (d.startsWith("/SD/")) {
                var substring2 = d.substring("/SD/".length)
                if (substring2.startsWith("rustedWarfare/")) {
                    substring2 = substring2.substring("rustedWarfare/".length)
                }
                overrideModLoad(substring2)
            } else {
                d
            }
        }

        // 重定向 流系统
        AsmAgent.addPartialMethod("com/corrodinggames/rts/gameFramework/e/c" , arrayOf("i","(Ljava/lang/String;)Lcom/corrodinggames/rts/gameFramework/utility/j;")) { obj: Any, _: String?, _: Class<*>?, args: Array<Any> ->
            val obj = obj as c
            var str = args[0].toString().replace(resAndAssetsPath,"").replace("\\","/")

            if (str.startsWith("assets/") || str.startsWith("assets\\")) {
                str = str.substring("assets/".length)
            }
            val str2: String = str
            val str3 = resAndAssetsPath+"assets/"+str2
            val d: AssetManager = com.corrodinggames.rts.appFramework.c.a().d()
            return@addPartialMethod try {
                try {
                    j(d.a(str2), str3, str2)
                } catch (e: FileNotFoundException) {
                    null
                }
            } catch (e2: IOException) {
                Log.error(obj.a + "Could not find asset:" + str3)
                null
            }
        }
    }
}