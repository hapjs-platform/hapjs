/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

function makeTimer(inst, callback, normalize) {
  const _inst = inst
  const _timerCallbackMap = {}

  return {
    setTimeout: function(cb, time) {
      const cid = normalize(cb, _inst, {
        vm: undefined,
        info: 'callback for setTimeout'
      })
      const handler = function() {
        callback(_inst, cid)
      }
      const tid = global.setTimeoutWrap(_inst.id, handler, time || 0)
      _timerCallbackMap[tid.toString()] = cid
      return tid.toString()
    },
    setInterval: function(cb, time) {
      const cid = normalize(cb, _inst, {
        vm: undefined,
        info: 'callback for setInterval'
      })
      const handler = function() {
        callback(_inst, cid, [], true)
      }
      const tid = global.setIntervalWrap(_inst.id, handler, time || 0)
      _timerCallbackMap[tid.toString()] = cid
      return tid.toString()
    },
    clearTimeout: function(n) {
      global.clearTimeoutWrap(n)
      const cid = _timerCallbackMap[n]
      delete _inst._callbacks[cid]
      delete _timerCallbackMap[n]
    },
    clearInterval: function(n) {
      global.clearIntervalWrap(n)
      const cid = _timerCallbackMap[n]
      delete _inst._callbacks[cid]
      delete _timerCallbackMap[n]
    },
    requestAnimationFrame: function(cb) {
      const cid = normalize(cb, _inst, {
        vm: undefined,
        info: 'callback for requestAnimationFrame'
      })
      const handler = function() {
        callback(_inst, cid)
      }
      const tid = global.requestAnimationFrameWrap(_inst.id, handler)
      _timerCallbackMap[tid.toString()] = cid
      return tid.toString()
    },
    cancelAnimationFrame: function(n) {
      global.cancelAnimationFrameWrap(n)
      const cid = _timerCallbackMap[n]
      delete _inst._callbacks[cid]
      delete _timerCallbackMap[n]
    }
  }
}

export { makeTimer }
