package com.netease.filetrans.socket.nsd

import android.content.Context

/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/27
 *   description :
 */
abstract class WifiClient(val clientCallBack: ClientCallBack?) {
    abstract fun discover(context: Context, serviceName: String?, serviceType: String?)
    abstract fun unDiscover()
}

