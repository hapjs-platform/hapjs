/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// 缓存原始控制台
let _oriHistory = null

/**
 * 处理url参数
 * @param url
 * @returns {*}
 * @private
 */
function _processUrl(url) {
  if (typeof url === 'string') {
    return { uri: url }
  } else if (typeof url === 'object') {
    return url
  }
}

/**
 * 设置原生路由
 */
function setNativeRouter() {
  _oriHistory = global.history

  if (ENV_PLATFORM === 'na') {
    if (_oriHistory) {
      const { go, back, push, replace } = _oriHistory
      global.history._ori = { go, back, push, replace }
      global.history.go = (...args) => {
        global.history._ori.go.apply(global.history, args)
      }
      global.history.back = () => {
        global.history._ori.back.apply(global.history)
      }
      global.history.forward = () => {}
      global.history.push = (...args) => {
        const url = _processUrl(args)
        if (!url) {
          return
        }
        global.history._ori.push.apply(global.history, url)
      }
      global.history.replace = (...args) => {
        const url = _processUrl(args)
        if (!url) {
          return
        }
        global.history._ori.replace.apply(global.history, url)
      }
    } else {
      global.history = {
        go: function(...args) {},
        back: function(...args) {},
        forward: function(...args) {},
        push: function(...args) {},
        replace: function(...args) {}
      }
    }
  }
}

/**
 * 恢复原始控制台
 */
function resetNativeRouter() {
  global.history = _oriHistory
}

export default {
  setNativeRouter,
  resetNativeRouter
}
