/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * 事件对象
 */
class XEvent {
  constructor(type, detail) {
    if (detail instanceof XEvent) {
      return detail
    }

    this.timestamp = Date.now() // 时戳
    this.detail = detail // 来源
    this.type = type // 类型

    let shouldStop = false
    // 停止发送/广播
    this.stop = function() {
      shouldStop = true
    }

    // 检查是否已经停止
    this.hasStopped = function() {
      return shouldStop
    }
  }
}

// 保留的生命周期接口函数
const lifeCycleEvents = [
  'onCreate',
  'onInit',
  'onReady',
  'onShow',
  'onHide',
  'onDestroy',
  'onBackPress',
  'onMenuButtonPress',
  'onKey',
  'onMenuPress',
  'onConfigurationChanged',
  'onOrientationChange',
  'onRefresh',
  'onReachTop',
  'onReachBottom',
  'onPageScroll',
  // TODO 待拆分app与page
  'onError',
  'onPageNotFound'
]
XEvent.reserveEvents = lifeCycleEvents
// 是否为保留函数
XEvent.isReservedEvent = function(evt) {
  return lifeCycleEvents.indexOf(evt) >= 0
}

export default XEvent
