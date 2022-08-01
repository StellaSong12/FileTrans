package com.netease.filetrans.socket.nsd

/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/27
 *   description :
 */
open interface ServerCallBack {
    fun onSuccess()
    fun onError(error: String?)
}
