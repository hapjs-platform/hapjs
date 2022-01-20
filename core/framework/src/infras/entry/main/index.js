/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { version } from '../../../../package.json'
import { createSession } from '../../../shared/util'
import platform from 'src/infras/platform/interface'
import runtime from 'src/infras/runtime'

import dock from 'src/infras/dock/interface'

function initInfras() {
  // 框架版本
  global.frameworkVersion = version

  // 将各层分别连接起来
  const glue = {}

  // 基本能力
  glue.platform = platform
  platform.init()
  glue.platform.exposeToNative(platform.exposure)

  // [REQPOOL-392]implement inspector session
  global.Session = platform.Session

  // 启用session记录
  if (global.profiler._isEnabled) {
    const session = createSession()
    session.connect()
    session.post('Profiler.enable', () => {
      session.post('Profiler.start', () => {})
    })
    console.trace(`### App Framework ### session.post('Profiler.enable') 启用`)
  }

  // 对外提供一种消息的通讯机制
  global.BroadcastChannel = platform.BroadcastChannel
  // 放置全局：方便判断
  global.ENGINE_TYPE = platform.ENGINE_TYPE

  // DOM能力
  glue.runtime = runtime
  runtime.init()
  glue.platform.exposeToNative(runtime.exposure)

  // 初始化dock并将dock的API绑定到全局
  glue.dock = dock
  dock.init(glue)
  glue.platform.exposeToNative(dock.exposure)

  // HTML解析
  glue.platform.defineBundle('parser')
  // Canvas解析
  glue.platform.defineBundle('canvas')
  // animation
  glue.platform.defineBundle('animation')

  profiler.record(`### App Performance ### 启动平台[PERF:infras]结束：${new Date().toJSON()}`)
}

global.initInfras = initInfras
