/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { requireByRepo } from '../common'

const proxySendActions = requireByRepo(
  'dist/release/output/styling',
  'proxySendActions',
  'src/infras/styling/index',
  'proxySendActions'
)

const { Node, Event, TouchEvent, DomDocument, freeze } = requireByRepo(
  'dist/release/output/infras-ext',
  'dom',
  'src/infras/runtime/dom/index'
)

const Listener = requireByRepo(
  'dist/release/output/infras-ext',
  'runtime.Listener',
  'src/infras/runtime/listener',
  'default'
)
const Streamer = requireByRepo(
  'dist/release/output/infras-ext',
  'runtime.Streamer',
  'src/infras/runtime/streamer',
  'default'
)

const helper = requireByRepo(
  'dist/release/output/infras-ext',
  'runtime.helper',
  'src/infras/runtime/helper'
)
const misc = requireByRepo(
  'dist/release/output/infras-ext',
  'runtime.misc',
  'src/infras/runtime/dom/misc'
)

// DOM相关
freeze()

global.config = {
  Node,
  Event,
  TouchEvent,
  helper,
  misc
}

// 创建Document实例
config.helper.createDocument = function(docId) {
  const streamer = new Streamer(global.sendActions, 1)
  const listener = new Listener(docId, streamer)
  const document = new DomDocument(docId, listener)
  return document
}

// 收集消息
global.callNativeMessageList = []
global.sendActions = function(instId, actionList) {
  for (let i = 0, len = actionList.length; i < len; i++) {
    const actionItem = actionList[i]
    const msg = `${JSON.stringify(actionItem)}`

    callNativeMessageList.push(msg)
  }
}

// TODO 通过开关控制
const unproxySendActions = proxySendActions()

export { proxySendActions, unproxySendActions }
