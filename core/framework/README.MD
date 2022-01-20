# 快应用引擎前端框架开发指南

## 准备

执行命令`npm install`，安装依赖

接下来，我们会介绍部分`package.json`的常用命令，开发者可快速入门。

查看全部可执行命令，请查看`package.json`下的`scripts`列表

## 生成框架代码

### 热更新构建测试版框架

执行命令`npm run na`，测试版框架会在`quickapp/core/framework`目录下的`dist/debug`目录生成测试版框架

开发者在修改框架代码时，本地JS打包服务会自动生成最新代码

### 构建测试版框架

执行命令`npm run na:dv`，测试版框架会在`quickapp/core/framework`目录下的`dist/debug`目录生成测试版框架

### 构建正式版框架

执行命令`npm run na:ol`，测试版框架会在`quickapp/core/framework`目录下的`dist/release`目录生成正式版框架

## 如何在安卓手机上调试框架

1. 在Android Studio打开快应用安卓项目，并且通过gradle打包`debug`版本`快应用引擎`。

2. 在手机上安装`快应用调试器`和步骤1打包生成的`快应用引擎`

快应用调试器可在[此处](https://www.quickapp.cn/docCenter/post/69) 下载

注意：必须使用debug版`快应用引擎`,才可以使引擎在后面的步骤，读取到开发者自己打包的框架代码

3. 通过usb连接，将手机连接到电脑

4. 在`quickapp/core/framework`目录下，打开终端

5. 现在有两种方法可以将前端框架打包放到手机上

- 自动推送框架方法：运行`npm run na`命令打包，将会自动启动服务，并推送生成后代码到手机存储的`/sdcard/quickapp/assets/js/`目录。同时，修改前端框架代码后，也会自动触发重新构建，并再次推送到手机上
  
- 手动打包拷贝框架方法：运行`npm run na:dv`命令打包，生成后的代码在`dist/debug`，然后手动拷贝全部此目录文件到手机存储的`/sdcard/quickapp/assets/js/`目录即可

6. 此时前端框架代码已经传输到手机上了，可以开始调试。
   
注意：若在步骤5使用了自动推送方法，请在此时停止`npm run na`命令的执行

7. 在电脑上运行你的快应用项目JS服务，可通过IDE或终端hap-toolkit实现

8. 打开手机的`快应用调试器`，点击`在线更新`按钮

9. 成功运行起来快应用后，退出当前快应用，再点击"开始调试"

10. 电脑上会自动打开`chrome DevTools`，此时就可以调试项目代码或框架代码了

11. 如果需要在特定代码处进行调试，建议在前端代码处添加代码`debugger`以增加断点，实现断点调试

## 格式化代码

执行命令`npm run format` 格式化前端代码(JS、JSON)与文档(markdown)

执行命令`npm run format-c` 格式化C++代码

## 执行单元测试

执行命令`npm run test:suite`， 运行整个框架的单元测试。开发者可通过终端日志，了解测试结果

