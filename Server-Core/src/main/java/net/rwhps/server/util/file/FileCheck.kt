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
import java.io.FileNotFoundException
import java.io.IOException

object FileCheck {
    /**
     * Requires parameter attributes for a file copy operation.
     *
     * @param source the source file
     * @throws NullPointerException if any of the given `File`s are `null`.
     * @throws FileNotFoundException if the source does not exist.
     */
    @Throws(FileNotFoundException::class)
    internal fun requireFileCopy(source: File) {
        requireExistsChecked(source, "source")
    }

    /**
     * Requires that the given `File` is a file.
     *
     * @param file The `File` to check.
     * @param name The parameter name to use in the exception message.
     * @return the given file.
     * @throws NullPointerException if the given `File` is `null`.
     * @throws IllegalArgumentException if the given `File` does not exist or is not a directory.
     */
    internal fun requireFile(file: File, name: String): File {
        require(file.isFile) { "Parameter '$name' is not a file: $file" }
        return file
    }

    /**
     * Check if the Path of the two instances are the same
     *
     * @param file1 The first file to compare.
     * @param file2 The second file to compare.
     * @throws IllegalArgumentException if the given files' canonical representations are equal.
     */
    @Throws(IOException::class)
    internal fun requireCanonicalPathsNotEquals(file1: File, file2: File) {
        val canonicalPath = file1.canonicalPath
        require(canonicalPath != file2.canonicalPath) {
            String.format(
                "File canonical paths are equal: '%s' (file1='%s', file2='%s')",
                canonicalPath, file1, file2
            )
        }
    }

    /**
     * Requires that two file lengths are equal.
     *
     * @param srcFile Source file.
     * @param destFile Destination file.
     * @param srcLen Source file length.
     * @param dstLen Destination file length
     * @throws IOException Thrown when the given sizes are not equal.
     */
    @Throws(IOException::class)
    internal fun requireEqualSizes(srcFile: File, destFile: File, srcLen: Long, dstLen: Long) {
        if (srcLen != dstLen) {
            throw IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "' Expected length: " + srcLen + " Actual: " + dstLen)
        }
    }

    /**
     * Requires that the given `File` exists and throws an [FileNotFoundException] if it doesn't.
     *
     * @param file The `File` to check.
     * @param fileParamName The parameter name to use in the exception message in case of `null` input.
     * @return the given file.
     * @throws NullPointerException if the given `File` is `null`.
     * @throws FileNotFoundException if the given `File` does not exist.
     */
    @Throws(FileNotFoundException::class)
    internal fun requireExistsChecked(file: File, fileParamName: String): File {
        if (!file.exists()) {
            throw FileNotFoundException("File system element for parameter '$fileParamName' does not exist: '$file'")
        }
        return file
    }
}