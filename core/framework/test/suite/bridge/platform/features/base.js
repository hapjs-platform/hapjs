/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const waitTime = 5

const base = {
  mockResult(data, code) {
    return {
      code: code || 0,
      content: data || {
        flag: Math.random()
      }
    }
  },

  mockSync(data, code) {
    return this.mockResult(data, code)
  },

  mockOnce(callbackId, data, code) {
    global.setTimeout(() => {
      global.execInvokeCallback({
        callback: callbackId,
        data: this.mockResult(data, code)
      })
    }, waitTime)
  },

  mockSubscribe(callbackId, data, code) {
    return global.setInterval(() => {
      global.execInvokeCallback({
        callback: callbackId,
        data: this.mockResult(data, code)
      })
    }, waitTime)
  },

  mockUnsubscribe(handler) {
    handler && global.clearInterval(handler)
  },

  setTimeout(callbackId, data, code) {
    return global.setTimeout(() => {
      global.execInvokeCallback({
        callback: callbackId,
        data: this.mockResult(data, code)
      })
    }, waitTime)
  },

  setTimeoutMulti(actionCallbacks, callbackId, data, code) {
    const cb = global.setTimeout(() => {
      global.execInvokeCallback({
        callback: callbackId,
        data: this.mockResult(data, code)
      })
    }, waitTime)
    actionCallbacks[callbackId] = cb
  },

  clearTimeout(handler) {
    handler && global.clearTimeout(handler)
  },

  clearTimeoutMulti(actionCallbacks, callbackId) {
    let cbs = []
    if (callbackId > -1) {
      cbs.push(actionCallbacks[callbackId])
    } else {
      cbs = Object.values(actionCallbacks)
    }

    for (const i in cbs) {
      this.clearTimeout(cbs[i])
    }
  }
}

export default base
