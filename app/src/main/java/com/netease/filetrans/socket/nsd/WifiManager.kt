package com.netease.filetrans.socket.nsd

import android.content.Context
import java.util.HashMap

/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/27
 *   description :
 */

class WifiManager {
    private var serviceName: String? = null
    private var serviceType: String? = null
    private var map: HashMap<String?, String?>? = null
    private lateinit var context: Context
    private var wifiServer: WifiServer? = null
    private var wifiClient: WifiClient? = null
    var port = -1

    private constructor() {}

    private constructor(builder: Builder) {
        serviceName = builder.serviceName
        serviceType = builder.serviceType
        port = builder.port
        map = builder.map
        context = builder.context
    }

    /**
     * register server
     *
     *
     * first remove register,then register
     * resolve register multi server cause client discover multi server
     *
     * @param wifiServer
     */
    fun registerWifiServer(wifiServer: WifiServer?) {
        unRegisterWifiServer()
        this.wifiServer = wifiServer
        this.wifiServer!!.register(context, serviceName, serviceType, map, port)
    }

    /**
     * 卸载服务
     */
    fun unRegisterWifiServer() {
        if (wifiServer != null) wifiServer!!.destroy()
    }

    /**
     * 搜索服务
     */
    fun discoverServer(wifiClient: WifiClient?) {
        this.wifiClient = wifiClient
        unDiscoverServer()
        this.wifiClient?.discover(context, serviceName, serviceType)
    }

    /**
     * 关闭搜索
     */
    fun unDiscoverServer() {
        wifiClient?.unDiscover()
    }

    class Builder(val context: Context) {
        var serviceName: String? = null
        var serviceType: String? = null
        val map = HashMap<String?, String?>()
        var port = -1

        fun withServiceName(serviceName: String?): Builder {
            this.serviceName = serviceName
            return this
        }

        fun withServiceType(serviceType: String?): Builder {
            this.serviceType = serviceType
            return this
        }

        fun withPort(ip: Int): Builder {
            this.port = ip
            return this
        }

        fun withParam(key: String?, value: String?): Builder {
            map[key] = value
            return this
        }

        fun build(): WifiManager {
            return WifiManager(this)
        }
    }
}
