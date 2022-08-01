package com.netease.filetrans.socket.nsd

import android.content.Context
import java.util.HashMap

/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/27
 *   description :
 */
abstract class WifiServer(callBack: ServerCallBack?) {
    var callBack: ServerCallBack?

    /**
     * 注册连接
     *
     * @param app
     * @param serviceName
     * @param serviceType
     * @param map
     */
    abstract fun register(
        app: Context,
        serviceName: String?,
        serviceType: String?,
        map: HashMap<String?, String?>?,
        port: Int
    )

    /**
     * 关闭注册连接
     */
    abstract fun destroy()

    init {
        this.callBack = callBack
    }
}
