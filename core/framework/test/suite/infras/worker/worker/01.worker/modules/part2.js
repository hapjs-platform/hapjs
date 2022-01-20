/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// 全局引用
const globalRef = Object.getPrototypeOf(global)

globalRef.msgObjectReq1 = { name: 'msgObject1' }
globalRef.msgObjectRes1 = null
globalRef.msgObjectErr1 = null

global.onmessage = function(evt) {
  globalRef.msgObjectRes1 = evt
}

global.onmessageerror = function(err) {
  globalRef.msgObjectErr1 = err
}

global.postMessage(global.msgObjectReq1)
