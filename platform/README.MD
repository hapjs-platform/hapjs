# 快应用框架开发指南

## 开发快应用
开发者可以采用以下几种开发方式:
1. 基于mockup项目开发出一个自己的快应用框架
2. 新建一个vendor项目，参考mockup项目增加必要的模块构建自己的快应用框架
3. 在现有项目中集成部分模块而获得运行快应用的能力
如果要构建自己的快应用框架，方式2比方式1更优，因为采用方式2开发可以方便合入快应用项目的后续更新。另外，快应用联盟的手机厂商成员也是按方式2开发。

如果希望在现有项目中添加运行快应用的功能，以下是需要添加的最小依赖集:
```Groovy
dependencies {
	implementation(name:'runtime', ext:'aar')
	implementation(name:'widgets', ext:'aar')
	implementation(name:'features', ext:'aar')
}
```

## 开发卡片
如果希望在现有项目中添加运行快应用卡片的功能，以下是需要添加的最小依赖集:
```Groovy
dependencies {
	implementation(name:'card-api', ext:'jar')
	implementation(name:'card-sdk', ext:'jar')
	implementation(name:'card-common', ext:'jar')
}
```

上面这种集成快应用卡片功能的方式称为动态集成，集成卡片功能的应用称为卡片宿主。这种方式需要卡片宿主和框架配合运行，卡片宿主会动态装载框架的代码和资源到自己进程中运行，而框架则负责卡片资源的统一管理。与卡片宿主配合的框架通过card.json的"platform"字段指定。

另外一种集成快应用卡片功能的方式是静态集成，即同时集成框架sdk(runtime等)和卡片sdk(card-api等)。采用这种方式卡片宿主可独立运行卡片。

快应用卡片的技术标准和快应用基本一样，唯一区别是有部分接口或组件不支持在卡片模式下运行(技术上不支持，或我们认为卡片上用不到)。card.json记录了不支持的接口和组件列表。

## 缺失的标准
mockup项目支持快应用的大部分标准，但由于某些技术或其他方面的原因，仍有部分功能需要开发者自己实现。缺失部分包括:
1. 微信支付，支付宝支付，第三方分享，QQ账户，微信账户，微博账户
2. 地图组件(实现MapProvider)
3. 快应用分发(实现DistributionProvider)

## 自定义接口和组件
开发者可以方便地覆写现有接口或组件，也可以方便地添加新的接口或组件。快应用框架定义了一个注解系统，编译时被这些注解标注的类将被认为是接口或组件，按照预定的规则被添加到框架里，供快应用后续使用。如果覆写或添加了接口或组件，需要做下列配置:
1. 全局build.gradle文件:
```Groovy
dependencies {
	classpath files('app/libs/annotation-generator.jar')
}
```
2. app/build.gradle文件:
```Groovy
apply plugin: 'org.hapjs.gradle.plugin.annotation.generator'
dependencies {
	implementation(name:'annotation', ext:'jar')
}
```
