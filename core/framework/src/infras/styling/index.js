/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { sendActionsWrap, getStylingNode, registerFromCssFile, registerStyleObject } from './action'

// 代理到Native的消息通知函数
let _originalSendActions = null

function proxySendActions() {
  _originalSendActions = global.sendActions
  global.sendActions = function(...args) {
    const ret = sendActionsWrap(...args)
    return _originalSendActions(...ret)
  }
  return function() {
    global.sendActions = _originalSendActions
  }
}

// 代理到Native的消息通知函数
let _originalCallNative = null

function proxyCallNative() {
  // 标识位：支持样式计算
  global.STYLING = true

  // 代理消息函数
  _originalCallNative = global.callNative
  global.callNative = function(...args) {
    if (typeof args[1] === 'string') {
      args[1] = JSON.parse(args[1])
    }
    const ret = sendActionsWrap(...args)

    if (ret[1] instanceof Array) {
      ret[1] = JSON.stringify(ret[1])
    }
    return _originalCallNative(...ret)
  }

  // 获取节点对应的样式合并的节点
  global.getStylingNode = getStylingNode

  // 暴露给模拟平台
  global.registerFromCssFile = registerFromCssFile
  // 暴露给该模块
  global.registerStyleObject = registerStyleObject

  return function() {
    global.callNative = _originalCallNative
    // 暴露给模拟平台
    global.registerFromCssFile = function() {}
    // 暴露给该模块
    global.registerStyleObject = function() {}
  }
}

export { proxySendActions, proxyCallNative }
