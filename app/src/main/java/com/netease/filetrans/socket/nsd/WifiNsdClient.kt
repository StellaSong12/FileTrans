package com.netease.filetrans.socket.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdManager.ResolveListener
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log

/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/27
 *   description :
 */

class WifiNsdClient(clientCallBack: ClientCallBack?) : WifiClient(clientCallBack) {
    private var mNsdManager: NsdManager? = null
    private var serviceName: String? = null
    private val serviceMap = HashMap<String, String>()

    override fun discover(context: Context, serviceName: String?, serviceType: String?) {
        this.serviceName = serviceName
        mNsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        mNsdManager!!.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, nsDicListener)
    }

    private val nsDicListener: DiscoveryListener = object : DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {
            this@WifiNsdClient.clientCallBack!!.startDiscover()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {

        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            this@WifiNsdClient.clientCallBack!!.onFailed("Start Discovery Failed", errorCode)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {

        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            // judge found the serviceName
            mNsdManager!!.resolveService(serviceInfo, resolveListener)
        }

        override fun onDiscoveryStopped(serviceType: String) {

        }
    }
    private val resolveListener: ResolveListener = object : ResolveListener {
        override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, i: Int) {
            this@WifiNsdClient.clientCallBack!!.onFailed("resolve failed", i)
        }

        override fun onServiceResolved(nsdServiceInfo: NsdServiceInfo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val map = nsdServiceInfo.attributes
                for ((key, value) in map) {
                    serviceMap[key] = String(value, 0, value.size)
                }
                if (this@WifiNsdClient.clientCallBack!!.connectCondition(serviceMap)) {
                    this@WifiNsdClient.clientCallBack.onSuccess(nsdServiceInfo.host, nsdServiceInfo.port, serviceMap)
                }
            } else {
                this@WifiNsdClient.clientCallBack!!.onSuccess(nsdServiceInfo.host, nsdServiceInfo.port, serviceMap)
            }
        }
    }

    override fun unDiscover() {
        if (mNsdManager != null) {
            mNsdManager!!.stopServiceDiscovery(nsDicListener)
        }
    }

    companion object {
        private val TAG = WifiNsdClient::class.java.name
    }
}
