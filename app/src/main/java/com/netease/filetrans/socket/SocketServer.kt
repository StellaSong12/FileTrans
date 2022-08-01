package com.netease.filetrans.socket

import android.text.TextUtils
import android.util.Log
import com.netease.filetrans.FileUtil
import com.netease.filetrans.MD5Util
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/28
 *   description :
 */
class SocketServer(val addLog: (String) -> Unit, val onStart: () -> Unit, val getFile: (InputStream?) -> Unit, val sendFile: (OutputStream?) -> Unit) {

    private var isEnable = false
    private val threadPool: ExecutorService = Executors.newCachedThreadPool()
    private var socket: ServerSocket? = null
    var filePath : String? = null
    var mLocalPort : Int = -1

    /**
     * 开启server
     */
    fun startServerAsync() {
        isEnable = true
        Thread { doProcSync() }.start()
    }

    /**
     * 关闭server
     */
    @Throws(IOException::class)
    fun stopServerAsync() {
        if (!isEnable) {
            return
        }
        isEnable = true
        socket!!.close()
        socket = null
    }

    private fun doProcSync() {
        try {
            socket = ServerSocket(0).also { socket ->
                // Store the chosen port.
                mLocalPort = socket.localPort
                addLog.invoke("mLocalPort : " + mLocalPort)
                onStart.invoke()
            }
            while (isEnable) {
                val remotePeer = socket!!.accept()
                addLog.invoke("doProcSync: " + remotePeer.remoteSocketAddress.toString())
                threadPool.submit {
                    try {
                        sendFile.invoke(remotePeer.getOutputStream()) // 发文件
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                threadPool.submit {
                    try {
                        getFile.invoke(remotePeer.getInputStream()) // 收文件
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}