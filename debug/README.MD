# 快应用调试器介绍

## 快应用调试器使用方法
调试器的使用方法可以参考快应用联盟官网: https://doc.quickapp.cn/ide/debug.html。

## 调试器运行过程简介
调试器与hap-toolkit，ide以及快应用框架交互以完成调试功能。几个关联模块的介绍:
- hap-toolkit: 用于开发编译快应用的命令行工具，详细介绍可以参考快应用联盟官网: https://doc.quickapp.cn/ide/cli.html?h=toolkit。
- ide: 用于开发编译快应用的集成开发环境，详细介绍可以参考快应用联盟官网: https://doc.quickapp.cn/ide/new.html。
- 快应用框架: 任何具备分发运行快应用能力的应用都可视为快应用框架，比如各厂商发布的快应用框架，或快应用联盟发布的快应用预览版。

hap-toolkit会启动http调试服务器，调试器连接到此服务器获取rpk包; 调试器也会启动一个http服务器，给ide提供获取平台信息，更新rpk包，启动调试器等接口。

调试器通过查询注册了监听BIND_DEBUG_SERVICE action的service(DebugService)来识别系统中的可运行快应用的框架，用户可选择在哪个框架上运行调试快应用。调试器通过绑定框架的DebugService来与框架通信，通过此通道完成安装，卸载，启动，调试快应用等功能。为安全起见，消息的发送方跟框架签名一致时，DebugService才会处理(此逻辑在DebugHandler.verifySignature()中)。以调试模式启动快应用时，调试器与框架的通信会携带额外的参数，框架根据这些参数来启动调试环境。

## 调试卡片
调试器也支持调试卡片。点击设置页面标题栏5次并且允许调试器自启动即可开启卡片运行模式。卡片宿主需要调用CardClient.setCardDebugHost()设置好CardDebugHost，并且在launch()方法中加载指定卡片，这样调试器在安装好卡片包后便可以触发卡片宿主运行卡片进行调试。

