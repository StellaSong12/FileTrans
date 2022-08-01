package com.netease.filetrans.socket.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.RegistrationListener
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import java.util.HashMap

/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/27
 *   description :
 */

class WifiNsdServer(callBack: ServerCallBack?) : WifiServer(callBack) {
    private var mRegistrationListener: RegistrationListener? = null
    private var serviceInfo: NsdServiceInfo? = null
    private var mNsdManager: NsdManager? = null

    override fun register(
        app: Context,
        serviceName: String?,
        serviceType: String?,
        map: HashMap<String?, String?>?,
        port: Int
    ) {
        mNsdManager = app.getSystemService(Context.NSD_SERVICE) as NsdManager
        serviceInfo = NsdServiceInfo()
        serviceInfo!!.serviceName = serviceName
        serviceInfo!!.serviceType = serviceType
        serviceInfo!!.port = port //port must be >0
        if (map != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for ((key, value) in map) {
                serviceInfo!!.setAttribute(key, value)
            }
        } else {
            Log.e(TAG, "params require sdk 21")
        }
        mRegistrationListener = object : RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                val mServiceName = NsdServiceInfo.serviceName
                callBack?.onSuccess()
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                callBack?.onError(errorCode.toString())
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
        mNsdManager!!.registerService(serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            mRegistrationListener)
    }

    override fun destroy() {
        if (mNsdManager != null) mNsdManager!!.unregisterService(mRegistrationListener)
    }

    companion object {
        private val TAG = WifiNsdServer::class.java.name
    }
}
