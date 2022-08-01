package com.netease.filetrans.socket.nsd

import java.net.InetAddress

/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/27
 *   description :
 */
open interface ClientCallBack {
    /**
     * 开始搜索
     */
    fun startDiscover()

    /**
     * 自定义连接到服务端的条件
     * @param map
     * @return
     */
    fun connectCondition(map: HashMap<String, String>): Boolean

    /**
     * 连接成功
     * @param remoteAddress 返回对方的IP
     * @param port 返回对方的端口号
     * @param map  获取对方暴露的参数
     */
    fun onSuccess(remoteAddress: InetAddress?, port: Int, map: HashMap<String, String>)

    /**
     * 搜索、连接、解析失败的回调
     * @param error
     * @param code
     */
    fun onFailed(error: String?, code: Int)
}
