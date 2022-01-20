/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const PluginDemo = {
  // 安装入口
  install(VmClass, options) {
    // this指向插件对象自身

    // 绑定到类
    VmClass[`plugin.install`] = true

    // 保存插件options到随意的某个地方
    VmClass.demoPluginOptions = options

    // 绑定到原型
    VmClass.prototype.defMethod01 = function() {
      return true
    }

    // 拦截Page的生命周期：自定义组件ViewModel的生命周期也会被调用
    VmClass.mixin({
      onInit() {
        // this指向每个ViewModel
        this[`plugin.onInit`] = true
      },
      onReady() {
        this[`plugin.onReady`] = true
      },
      onShow() {
        this[`plugin.onShow`] = true
      },
      onHide() {
        this[`plugin.onHide`] = true
      },
      onBackPress() {
        this[`plugin.onBackPress`] = true
      },
      onMenuButtonPress() {
        this[`plugin.onMenuButtonPress`] = true
      },
      onMenuPress() {
        this[`plugin.onMenuPress`] = true
      },
      onKey() {
        this[`plugin.onKey`] = true
      },
      onDestroy() {
        this[`plugin.onDestroy`] = true
      }
    })

    // 其它参考：https://doc.quickapp.cn/framework/script.html
    // 1. VM的页面级别VM：this.$root
    // 2. VM的所属页面：this.$page
    // 3. VM的页面有效性：this.$valid
    // 4. VM的页面可见性：this.$visible

    // 拦截App的生命周期
    VmClass.mixinApp({
      onCreate() {
        // this指向页面VM的$app
        this[`plugin.onCreate`] = true
      },
      onError() {
        this[`plugin.onError`] = true
      },
      onDestroy() {
        this[`plugin.onDestroy`] = true
      },
      onPageNotFound() {
        this[`plugin.onPageNotFound`] = true
      }
    })
  }
}

export default PluginDemo
