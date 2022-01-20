/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// 定义在global上
let _onmessage = null
let _onmessageerror = null

Object.defineProperty(global, 'onmessage', {
  configurable: true,
  enumberable: true,
  get() {
    return _onmessage
  },
  set(v) {
    _onmessage = v
  }
})

Object.defineProperty(global, 'onmessageerror', {
  configurable: true,
  enumberable: true,
  get() {
    return _onmessageerror
  },
  set(v) {
    _onmessageerror = v
  }
})

/**
 * 发送消息到Native
 */
function postMessage(...args) {
  console.trace(`### App Worker ### 发送消息：${JSON.stringify(args)}`)
  global.postMessageInternal(JSON.stringify(args[0]))
}

/**
 * 接收Native的消息
 */
function onMessageInternal(event) {
  console.trace(`### App Worker ### 接收消息：${event}`)
  if (typeof event === 'string') {
    event = JSON.parse(event)
  }

  if (global.onmessage) {
    global.onmessage(event)
  }
}

/**
 * 接收Native的错误消息
 */
function onMessageErrorInternal(err) {
  console.trace(`### App Worker ### 接收错误消息：${err}`)
  if (typeof err === 'string') {
    err = JSON.parse(err)
  }

  if (global.onmessageerror) {
    global.onmessageerror(err)
  }
}

export { postMessage, onMessageInternal, onMessageErrorInternal }
