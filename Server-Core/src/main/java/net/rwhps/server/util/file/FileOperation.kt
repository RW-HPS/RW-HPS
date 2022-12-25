/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.file

import java.io.File
import java.io.IOException
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object FileOperation {
    /**
     * Copies a file to a new location preserving the file date.
     *
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. If the destination file exists, then this method
     * will overwrite it.
     *
     * **Note:** This method tries to preserve the file's last modified date/times using
     * [File.setLastModified], however it is not guaranteed that the operation will succeed. If the
     * modification operation fails, the methods throws IOException.
     *
     *
     * @param srcFile an existing file to copy, must not be `null`.
     * @param destFile the new file, must not be `null`.
     * @throws NullPointerException if any of the given `File`s are `null`.
     * @throws IOException if source or destination is invalid.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeeded.
     * @throws IOException if the output file length is not the same as the input file length after the copy completes.
     * @see .copyFileToDirectory
     * @see .copyFile
     */
    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(srcFile: File, destFile: File) {
        copyFile(srcFile, destFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
    }

    /**
     * Copies a file to a new location.
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. If the destination file exists, you can overwrite
     * it if you use [StandardCopyOption.REPLACE_EXISTING].
     *
     * @param srcFile an existing file to copy, must not be `null`.
     * @param destFile the new file, must not be `null`.
     * @param copyOptions options specifying how the copy should be done, for example [StandardCopyOption]..
     * @throws NullPointerException if any of the given `File`s are `null`.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IllegalArgumentException if source is not a file.
     * @throws IOException if the output file length is not the same as the input file length after the copy completes.
     * @throws IOException if an I/O error occurs.
     * @see StandardCopyOption
     */
    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(srcFile: File, destFile: File, vararg copyOptions: CopyOption) {
        FileCheck.requireFileCopy(srcFile)
        FileCheck.requireFile(srcFile, "srcFile")
        FileCheck.requireCanonicalPathsNotEquals(srcFile, destFile)
        // On Windows, the last modified time is copied by default.
        Files.copy(srcFile.toPath(), destFile.toPath(), *copyOptions)
        //
        FileCheck.requireEqualSizes(srcFile, destFile, srcFile.length(), destFile.length())
    }

    /**
     * Delete files recursively
     *
     * @param file File
     *
     * @throws IOException
     * @throws SecurityException
     */
    @JvmStatic
    @Throws(IOException::class,SecurityException::class)
    fun recursiveDelete(file: File) {
        if (!file.exists()) {
            return
        }

        if (file.isDirectory) {
            file.listFiles()?.run {
                for (f in this) {
                    recursiveDelete(f)
                }
            }
        }
        file.delete()
    }
}