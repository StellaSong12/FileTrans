package com.netease.filetrans.udp

import android.content.Context
import android.os.Environment
import android.util.Log
import com.netease.filetrans.udp.UdpActivity.Companion.getBroadcastAddress
import java.io.IOException
import java.net.*

/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/25
 *   description :
 */
class UDPBroadcaster(var mContext: Context) {
    private val TAG:String = UDPBroadcaster::class.java.simpleName
    private var mDestPort = 0
    private var mSocket: DatagramSocket? = null
    private val ROOT_PATH:String = Environment.getExternalStorageDirectory().path
    /**
     * 打开
     */
    fun open(localPort: Int, destPort: Int): Boolean {
        mDestPort = destPort
        try {
            mSocket = DatagramSocket(localPort)
            mSocket?.broadcast = true
            mSocket?.reuseAddress = true
            return true
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 关闭
     */
    fun close(): Boolean {
        if (mSocket != null && mSocket?.isClosed?.not() as Boolean) {
            mSocket?.close()
        }
        return true
    }

    /**
     * 发送广播包
     */
    fun sendPacket(buffer: ByteArray, host: String): Boolean {
        try {
            val addr = getBroadcastAddress(mContext, host)
            Log.d("$TAG addr",addr.toString())
            val packet = DatagramPacket(buffer, buffer.size)
            packet.address = addr
            packet.port = mDestPort
            mSocket?.send(packet)
            return true
        } catch (e1: UnknownHostException) {
            e1.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 接收广播
     */
    fun recvPacket(buffer: ByteArray): Boolean {
        val packet = DatagramPacket(buffer, buffer.size)
        try {
            mSocket?.receive(packet)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }
}