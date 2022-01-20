/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { initModulesDef, requireModule, mapInvokeCallback, getModuleHostMap } from './misc'

/**
 * 注册Api模块
 * @param modulesConf
 * @param type feature | module  默认为feature
 */
function registerModules(modulesConf, type = 'feature') {
  profiler.record(`### App Performance ### 注册模块[PERF:registerMod]开始：${new Date().toJSON()}`)
  // 解析字符串
  if (typeof modulesConf === 'string') {
    modulesConf = JSON.parse(modulesConf)
  }

  console.trace(`### App Framework ### registerModules---- `, JSON.stringify(modulesConf))

  modulesConf = modulesConf.map(function(module) {
    module.__type__ = type
    return module
  })

  if (typeof modulesConf === 'object') {
    initModulesDef(modulesConf)
  }
  profiler.record(`### App Performance ### 注册模块[PERF:registerMod]结束：${new Date().toJSON()}`)
}

/**
 * 接受来自原生的事件或回调
 * @param event 返回结果
 * @returns {*} 结果数组,无返回值, 'invalid'表示无效事件
 */
function execInvokeCallback(event) {
  if (typeof event === 'string') {
    event = JSON.parse(event)
  }
  // 根据回调Id找到对应的实例映射
  const id = mapInvokeCallback(event.callback)
  console.trace(`### App Framework ### 处理invoke回调----`, JSON.stringify(id))
  if (id) {
    // 判断回调来源
    const instance = getModuleHostMap(id.instance)

    if (instance) {
      const args = [instance, event.callback, event.data, id.preserved]

      if (!instance._callbacks) {
        return new Error(`execInvokeCallback: 回调函数所属对象已经无效 "${instance.id}"`)
      }

      return instance.invoke(...args)
    }
  }
  return new Error(`execInvokeCallback: 无效invoke回调Id "${id && id.instance}"`)
}

export { registerModules, execInvokeCallback, requireModule }
