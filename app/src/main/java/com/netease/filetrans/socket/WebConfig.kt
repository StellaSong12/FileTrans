package com.netease.filetrans.socket

/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/26
 *   description :
 */
class WebConfig {
    private var port //端口
            = 0
    private var maxParallels //最大监听数
            = 0

    fun getPort(): Int {
        return port
    }

    fun setPort(port: Int) {
        this.port = port
    }

    fun getMaxParallels(): Int {
        return maxParallels
    }

    fun setMaxParallels(maxParallels: Int) {
        this.maxParallels = maxParallels
    }
}