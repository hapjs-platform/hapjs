/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * @file DOM规范的事件API汇总
 * @desc 只能被外部文件引用，不能被内部引用
 */

import Event from './event'
import TouchEvent from './touch'

// 是否属于Touch类
const regEventTouch = /^(touchstart|touchmove|touchcancel|touchend)$/
// 是否支持冒泡
const regEventBubbles = /^(touchstart|touchmove|touchcancel|touchend|click|longpress)$/

/**
 * 根据事件名称创建不同的类实例
 * @param {object} evtName
 * @param {object} options
 * @return {*}
 */
function createEvent(evtName, options) {
  let evt

  if (!regEventTouch.test(evtName)) {
    evt = new Event(evtName, options)
    evt._supportW3C = false
  } else {
    evt = new TouchEvent(evtName, options)
    evt._supportW3C = false
  }

  // mPV为1040时,部分事件开启冒泡功能
  if (regEventBubbles.test(evtName) && global.isRpkMinPlatformVersionGEQ(1040)) {
    evt._supportW3C = true
    evt._bubbles = true
  }

  return evt
}

export { createEvent }
