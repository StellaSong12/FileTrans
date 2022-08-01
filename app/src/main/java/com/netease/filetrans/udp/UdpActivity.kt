package com.netease.filetrans.udp

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.format.Formatter.formatIpAddress
import android.util.Log
import android.widget.ScrollView
import com.netease.filetrans.databinding.ActivityUdpBinding
import java.lang.reflect.InvocationTargetException
import java.net.DatagramPacket
import java.net.InetAddress

import java.net.NetworkInterface

import java.net.SocketException
import java.util.*
import java.util.regex.Pattern


class UdpActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityUdpBinding
    lateinit var mUDPBroadCast: UDPBroadcaster
    private var sendBuffer: String = "This is UDP Server " + System.currentTimeMillis()

    companion object {
        val SEND_PORT: Int = 8008
        val DEST_PORT: Int = 8009

        fun start(context: Context) {
            context.startActivity(Intent(context, UdpActivity::class.java))
        }

        fun getBroadcastAddress(context: Context, host: String): InetAddress {
            if (isWifiApEnabled(context)) { //判断wifi热点是否打开
                return InetAddress.getByName(host)  //直接返回
            }
            val wifi: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp: DhcpInfo = wifi.dhcpInfo ?: return InetAddress.getByName("255.255.255.255")
            val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
            val quads = ByteArray(4)
            for (k in 0..3) {
                quads[k] = ((broadcast shr k * 8) and 0xFF).toByte()
            }
            return InetAddress.getByAddress(quads)
        }

        /**
         * check whether the wifiAp is Enable
         */
        private fun isWifiApEnabled(context: Context): Boolean {
            try {
                val manager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val method = manager.javaClass.getMethod("isWifiApEnabled")
                return method.invoke(manager) as Boolean
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityUdpBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mUDPBroadCast = UDPBroadcaster(this)

        mBinding.btLocalIp.setOnClickListener {
            mBinding.tvLocalIp.text = getLocalIPAddress()
        }

        mBinding.btSend.setOnClickListener {
            sendUDPBroadcast()
        }

        mBinding.btReceive.setOnClickListener {
            recvUDPBroadcast()
        }
    }

    fun getLocalIPAddress(): String? {
        var enumeration: Enumeration<NetworkInterface>? = null
        try {
            enumeration = NetworkInterface.getNetworkInterfaces()
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        if (enumeration != null) {
            // 遍历所用的网络接口
            while (enumeration.hasMoreElements()) {
                val nif = enumeration.nextElement() // 得到每一个网络接口绑定的地址
                val inetAddresses = nif.inetAddresses
                // 遍历每一个接口绑定的所有ip
                if (inetAddresses != null) while (inetAddresses.hasMoreElements()) {
                    val ip = inetAddresses.nextElement()
                    if (!ip.isLoopbackAddress && isIPv4Address(ip.hostAddress)) {
                        return ip.hostAddress
                    }
                }
            }
        }
        return ""
    }

    /**
     * Ipv4 address check.
     */
    private val IPV4_PATTERN: Pattern = Pattern.compile("^(" +
            "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
            "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")

    /**
     * Check if valid IPV4 address.
     *
     * @param input the address string to check for validity.
     * @return True if the input parameter is a valid IPv4 address.
     */
    fun isIPv4Address(input: String?): Boolean {
        return IPV4_PATTERN.matcher(input).matches()
    }

    private fun sendUDPBroadcast() {
        mUDPBroadCast.open(SEND_PORT, DEST_PORT) //打开广播
        val buffer: ByteArray = sendBuffer.toByteArray()
        Thread {
            try {
                Thread.sleep(500) //500ms 延时
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mUDPBroadCast.sendPacket(buffer, mBinding.etIpToSend.text.toString().trim()) //发送广播包
            addLog("$TAG data: ${String(buffer)}")
            mUDPBroadCast.close() //关闭广播
        }.start()
    }

    private fun recvUDPBroadcast() {
        mUDPBroadCast.open(DEST_PORT, SEND_PORT)
        var buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)
        Thread {
            try {
                Thread.sleep(500) //500ms延时
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mUDPBroadCast.recvPacket(packet.data) //接收广播
            val data = String(packet.data)
            addLog("$TAG data: $data")
            addLog("$TAG addr: ${packet.address}")
            addLog("$TAG port: ${packet.port}")
            mUDPBroadCast.close() //退出接收广播
        }.start()
    }

    private fun addLog(log: String) {
        var mLog: String = log
        if (mLog.endsWith("\n").not()) {
            mLog += "\n"
        }
        mBinding.scrollView.post {
            mBinding.tvReceive.append(mLog)
            mBinding.scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}