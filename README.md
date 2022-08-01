# FileTrans
安卓和iOS文件互传

通过网络服务发现 NSD 和 Websocket 实现安卓和 iOS 设备间文件传输。

服务端:

- 创建 ServerSocket 开启 socket 服务，获取端口号；
- 通过 NSD_SERVICE 注册nsd服务，在 NsdServiceInfo 设置双端定义的服务名称和类型，设置 socket 端口号；
- 客户端连接后，发送包含文件信息的消息头和文件流；
- 支持解析客户端发送的消息头和文件流，并将文件保存到本地。

客户端:

- 在同一网络环境下，通过 NsdManager 发现nsd服务，解析得到 socket 服务器的ip地址和端口号；
- 通过ip和端口连接到 socket 服务器；
- 解析服务端发送的消息头和文件流，并将文件保存到本地；
- 支持选择本地文件并发送包含文件信息的消息头和文件流到服务端。
