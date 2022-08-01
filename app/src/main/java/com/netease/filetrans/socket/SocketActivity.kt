package com.netease.filetrans.socket

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.netease.filetrans.FileUtil
import com.netease.filetrans.MD5Util
import com.netease.filetrans.databinding.ActivitySocketBinding
import com.netease.filetrans.socket.nsd.*
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern


/**
 *   author : songsiting
 *   e-mail : songsiting@corp.netease.com
 *   date : 2022/7/26
 *   description :
 */
class SocketActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivitySocketBinding
    private var socketServer: SocketServer? = null
    private var socket: Socket? = null
    private var filePath: String? = null
    private val threadPool: ExecutorService = Executors.newCachedThreadPool()

    companion object {
        val CHOOSE_FILE_RESULT_CODE = 20

        val SERVICE_NAME = "godlike_file_trans"
        val SERVICE_TYPE = "_godlike._tcp"

        fun start(context: Context) {
            context.startActivity(Intent(context, SocketActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivitySocketBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mBinding.btLocalIp.setOnClickListener {
            mBinding.tvLocalIp.text = getLocalIPAddress()
        }

        mBinding.btLaunchDir.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE)
        }

        mBinding.btSend.setOnClickListener {
            Toast.makeText(this, "todo", Toast.LENGTH_LONG).show()
        }

        mBinding.btMd5.setOnClickListener {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    mBinding.tvMd5.text = MD5Util.getFileMD5(file)
                } else {
                    Toast.makeText(this, "文件不存在", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        mBinding.btNsdServer.setOnClickListener {
            startNSDServer()
        }

        mBinding.btNsdConsumer.setOnClickListener {
            startNSDConsumer()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        socketServer?.stopServerAsync()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CHOOSE_FILE_RESULT_CODE && resultCode == RESULT_OK) {
            data?.data?.let {
                filePath = FileUtil.getPath(this, it)
                mBinding.tvFilePath.text = filePath
                addLog("filePath: " + filePath)
            }
        }
    }

    //--------- server ----------

    fun startNSDServer() {
        if (TextUtils.isEmpty(filePath) || !FileUtil.isValid(filePath)) {
            Toast.makeText(this, "请选择文件", Toast.LENGTH_LONG).show()
            return
        }
        startServer()
    }

    fun startServer() {
        val webConfig = WebConfig()
        webConfig.setMaxParallels(10)
        socketServer = SocketServer({ a: String ->
            addLog(a)
        }, {
            val wifiManager = WifiManager.Builder(this)
                .withServiceName(SERVICE_NAME)
                .withServiceType(SERVICE_TYPE)
                .withPort(socketServer?.mLocalPort ?: -1)
                .build()

            //注册nsd服务
            wifiManager.registerWifiServer(WifiNsdServer(object : ServerCallBack {
                override fun onSuccess() {
                    addLog("startNSDServer onSuccess")
                }

                override fun onError(error: String?) {
                    addLog("startNSDServer onError: " + error)
                }
            }))
        }, { inputStream: InputStream? ->
            getFileFromStream(inputStream)
        }, { outputStream: OutputStream? ->
            sendFile(outputStream)
        })
        socketServer?.filePath = filePath
        socketServer?.startServerAsync()
        addLog("server start")
    }


    //--------- consumer ----------

    fun startNSDConsumer() {
        val wifiManager: WifiManager = WifiManager.Builder(this)
            .withServiceName(SERVICE_NAME)
            .withServiceType(SERVICE_TYPE)
            .build()

        //发现服务
        wifiManager.discoverServer(WifiNsdClient(object : ClientCallBack {
            override fun startDiscover() {
                addLog("startNSDConsumer startDiscover")
            }

            override fun connectCondition(map: HashMap<String, String>): Boolean {
                addLog("startNSDConsumer connectCondition: " + map)
                return true
            }

            override fun onSuccess(remoteAddress: InetAddress?, port: Int, map: HashMap<String, String>) {
                addLog("startNSDConsumer onSuccess: " + remoteAddress?.hostAddress + " " + port + " " + map)

                remoteAddress?.hostAddress?.let {
                    startConsumer(it, port)
                }
            }

            override fun onFailed(error: String?, code: Int) {
                addLog("startNSDConsumer onFailed: " + error + " " + code)
            }
        }))
    }

    fun startConsumer(ip: String, port: Int) = Thread {
        try {
            socket = Socket(ip, port)
            // socket.setSoTimeout(10000);//设置10秒超时
            addLog("与服务器建立连接：$socket")

            threadPool.submit {
                try {
                    getFileFromStream(socket?.getInputStream()) // 发文件
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            threadPool.submit {
                try {
                    sendFile(socket?.getOutputStream()) // 发文件
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()


    // ------------ util -------------

    fun getFileFromStream(inputStream: InputStream?) {
        //解析流中的文件header,也就是开头的流
        var content: Int
        val header = ByteArray(1024)
        var i = 0
        while (inputStream?.read().also { content = it ?: -1 } != -1) {
            if (content == '\n'.code && i >= 1 && header[i - 1] == '\r'.code.toByte()) {
                break //表示文件名已经读取完毕
            }
            header[i] = content.toByte()
            i++
        }
        addLog("receive header: " + String(header).trim())
        var fileName: String? = null

        try {
            val json = JSONObject(String(header).trim())
            fileName = json.getString("File-Name")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val buffer = ByteArray(1024 * 4)
        var temp = 0
        val fileP =  this@SocketActivity.externalCacheDir?.path + "/" + fileName
        val oldFile = File(fileP)
        if (oldFile.exists()) {
            oldFile.delete()
        }
        val fileOutput = FileOutputStream(fileP, false)
        while (inputStream?.read(buffer).also { temp = it ?: -1 } != -1) {
            addLog("receive：" +  String(buffer, 0, temp))
            fileOutput.write(buffer, 0, temp);
        }
        fileOutput.close()
        addLog("接收完成: " + fileP)
    }

    fun sendFile(outputStream: OutputStream?) {
        if (TextUtils.isEmpty(filePath)) {
            addLog("请选择文件")
            return
        }

        val header = getFileInstruction(filePath!!)
        addLog("send header: " + header)
        outputStream?.write(header.toByteArray())

        val fileInput = FileInputStream(filePath)
        var size = -1
        val bufferFile = ByteArray(1024)
        while (fileInput.read(bufferFile, 0, 1024).also { size = it } != -1) {
            outputStream?.write(bufferFile, 0, size)
        }
        fileInput.close()

        addLog("发送完成: " + filePath)
    }

    /**
     * 返回以下格式：{
    "File-Name": "",
    "File-MD5": "",
    "Content-Type":"",
    "Total-Length": 1111, // 文件总长
    "Content-Length": 1111 // 每个包字长
    }
     */
    private fun getFileInstruction(path: String): String {
        val file = File(path)
        val json = JSONObject()
        json.put("File-Name", file.name)
        json.put("File-MD5", MD5Util.getFileMD5(file))
        json.put("Content-Type", FileUtil.getMimeType(path))
        json.put("Total-Length", file.length())
        json.put("Content-Length", file.length())
        return json.toString() + "\r\n"
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
}