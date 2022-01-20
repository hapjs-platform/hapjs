/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import _console from './console'
import _router from './router'

let _oriSetTimeout = null
let _oriSetInterval = null
let _oriRequestAnimationFrame = null

let _oriClearTimeout = null
let _setTimeoutNative = null

let _oriClearInterval = null
let _setIntervalNative = null

let _oriCancelAnimationFrame = null
let _requestAnimationFrameNative = null

function setNativeProfiler() {
  if (!global.profiler) {
    global.profiler = {
      // 是否启用
      isEnabled() {
        return false
      },
      saveProfilerData: () => {},
      record: global.console.record,
      time: global.console.time,
      timeEnd: global.console.timeEnd
    }
  }
  // 属性形式，底层方法只调用一次，避免性能干扰
  global.profiler._isEnabled = global.profiler.isEnabled()
}

/**
 * 设置原生定时器
 */
function setNativeTimer() {
  if (ENV_PLATFORM === 'h5') {
    // H5环境下：重新封装，在setTimeout环境下保证updateFinish事件的产生
    global.setTimeoutNative = function(iid, timerId, time) {
      _oriSetTimeout(function() {
        global.setTimeoutCallback(timerId)
      }, time)
    }
    global.setIntervalNative = function(iid, timerId, time) {
      _oriSetInterval(function() {
        global.setIntervalCallback(timerId)
      }, time)
    }
    global.requestAnimationFrameNative = function(iid, timerId, time) {
      _oriRequestAnimationFrame(function() {
        global.requestAnimationFrameCallback(timerId)
      }, time)
    }
  }

  // 缓存原有
  _oriSetTimeout = global.setTimeout
  _oriSetInterval = global.setInterval
  _oriRequestAnimationFrame = global.requestAnimationFrame

  _oriClearTimeout = global.clearTimeoutNative
  _setTimeoutNative = global.setTimeoutNative

  _oriClearInterval = global.clearIntervalNative
  _setIntervalNative = global.setIntervalNative

  _oriCancelAnimationFrame = global.cancelAnimationFrameNative
  _requestAnimationFrameNative = global.requestAnimationFrameNative

  // 缓存计时器
  const timerMap = {}
  let timerId = 0 // Timer回调句柄

  if (typeof _setTimeoutNative === 'function') {
    global.setTimeoutWrap = function(iid, cb, time) {
      timerMap[++timerId] = cb
      console.trace(`### App Framework ### setTimeoutWrap ${timerId}----`)
      _setTimeoutNative(iid, timerId, time || 4)
      return timerId
    }

    global.setTimeout = function(cb, time) {
      return global.setTimeoutWrap(-1, cb, time)
    }

    global.setTimeoutCallback = function(id) {
      console.trace(`### App Framework ### setTimeout 执行回调 ${id}----`)
      // 执行定时器回调，并删除记录
      if (typeof timerMap[id] === 'function') {
        timerMap[id]()
        delete timerMap[id]
      }
    }

    global.clearTimeout = global.clearTimeoutWrap = function(id) {
      if (typeof _oriClearTimeout === 'function') {
        const idInt = parseInt(id)
        !isNaN(idInt) && _oriClearTimeout(idInt)
      }

      if (typeof timerMap[id] === 'function') {
        delete timerMap[id]
      } else {
        timerMap[id] = undefined
      }
    }
  }

  if (typeof _setIntervalNative === 'function') {
    global.setIntervalWrap = function(iid, cb, time) {
      timerMap[++timerId] = cb
      console.trace(`### App Framework ### setIntervalWrap ${timerId}----`)
      _setIntervalNative(iid, timerId, time || 4)
      return timerId
    }

    global.setInterval = function(cb, time) {
      return global.setIntervalWrap(-1, cb, time)
    }

    global.setIntervalCallback = function(id) {
      console.trace(`### App Framework ### setInterval 执行回调 ${id}----`)
      // 执行定时器回调
      if (typeof timerMap[id] === 'function') {
        timerMap[id]()
      }
    }

    global.clearInterval = global.clearIntervalWrap = function(id) {
      if (typeof _oriClearInterval === 'function') {
        const idInt = parseInt(id)
        !isNaN(idInt) && _oriClearInterval(idInt)
      }

      if (typeof timerMap[id] === 'function') {
        delete timerMap[id]
      } else {
        timerMap[id] = undefined
      }
    }
  }

  if (typeof _requestAnimationFrameNative === 'function') {
    global.requestAnimationFrameWrap = function(iid, cb) {
      timerMap[++timerId] = cb
      console.trace(`### App Framework ### requestAnimationFrame ${timerId}----`)
      _requestAnimationFrameNative(iid, timerId)
      return timerId
    }

    global.requestAnimationFrame = function(cb) {
      return global.requestAnimationFrameWrap(-1, cb)
    }

    global.requestAnimationFrameCallback = function(id) {
      console.trace(`### App Framework ### requestAnimationFrame 执行回调 ${timerId}----`)
      // 执行定时器回调
      if (typeof timerMap[id] === 'function') {
        timerMap[id]()
      }
    }

    global.cancelAnimationFrame = global.cancelAnimationFrameWrap = function(id) {
      if (typeof _oriCancelAnimationFrame === 'function') {
        const idInt = parseInt(id)
        !isNaN(idInt) && _oriCancelAnimationFrame(idInt)
      }
      if (typeof timerMap[id] === 'function') {
        delete timerMap[id]
      } else {
        timerMap[id] = undefined
      }
    }
  }
}

/**
 * 恢复原始定时器
 */
function resetNativeTimer() {
  global.setTimeout = _oriSetTimeout
  global.clearTimeout = _oriClearTimeout
  global.clearTimeoutWrap = null
  global.setTimeoutCallback = null
  global.setTimeoutWrap = null

  global.setInterval = _oriSetInterval
  global.clearInterval = _oriClearInterval
  global.clearIntervalWrap = null
  global.setIntervalCallback = null
  global.setIntervalWrap = null

  global.requestAnimationFrame = _oriRequestAnimationFrame
  global.cancelAnimationFrame = _oriCancelAnimationFrame
  global.cancelAnimationFrameWrap = null
  global.requestAnimationFrameCallback = null
  global.requestAnimationFrameWrap = null
}

function freezePrototype() {
  Object.freeze(Object)
  Object.freeze(Array)
  Object.freeze(Object.prototype)
  Object.freeze(Array.prototype)
  Object.freeze(String.prototype)
  Object.freeze(Number.prototype)
  Object.freeze(Boolean.prototype)
  Object.freeze(Error.prototype)
  Object.freeze(Date.prototype)
  Object.freeze(RegExp.prototype)
}

export default {
  setNativeConsole: _console.setNativeConsole,
  resetNativeConsole: _console.resetNativeConsole,
  setNativeProfiler,
  setNativeTimer,
  resetNativeTimer,
  setNativeRouter: _router.setNativeRouter,
  resetNativeRouter: _router.resetNativeRouter,
  freezePrototype
}
