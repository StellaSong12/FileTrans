package com.netease.filetrans

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/27
 *   description :
 */
object MD5Util {
    private val HEX_DIGITS = "0123456789abcdef".toCharArray()

    fun getFileMD5(file: File): String? {
        if (!file.exists() || !file.isFile) {
            return null
        }
        var digest: MessageDigest? = null
        var `in`: FileInputStream? = null
        val buffer = ByteArray(1024)
        var len: Int
        try {
            digest = MessageDigest.getInstance("MD5")
            `in` = FileInputStream(file)
            while (`in`.read(buffer, 0, 1024).also { len = it } != -1) {
                digest.update(buffer, 0, len)
            }
            `in`.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return toHex(digest.digest())
    }

    private fun toHex(data: ByteArray): String? {
        val chars = CharArray(data.size * 2)
        for (i in data.indices) {
            chars[i * 2] =
                HEX_DIGITS[data[i].toInt() shr 4 and 0xf]
            chars[i * 2 + 1] =
                HEX_DIGITS[data[i].toInt() and 0xf]
        }
        return String(chars)
    }
}