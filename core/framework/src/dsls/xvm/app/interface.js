/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { removeAppPrefix } from 'src/shared/util'
import { invokeScript } from 'src/shared/function'

import { $define, $bootstrap, $defineWrap } from './bundle'

import context from '../context'

/**
 * 初始化App
 * @param inst
 * @param code
 * @returns {*}
 */
function initApp(inst, code) {
  profiler.record(`### App Performance ### 初始化APP[PERF:initApp]开始：${new Date().toJSON()}`)
  profiler.time(`PERF:initApp`)
  console.trace(`### App Framework ### 开始初始化App(${inst.id})`)

  let result

  // 准备App全局方法
  const instDefine = (...args) => {
    $define(inst, ...args)
  }
  const instBootstrap = (name, config) => {
    profiler.time(`PERF:bootstrapApp`)
    result = $bootstrap(inst, name, config)
    console.trace(`### App Framework ### 完成App初始化(${inst.id})----`)
    profiler.timeEnd(`PERF:bootstrapApp`)
    profiler.record(
      `### App Performance ### 启动APP[PERF:bootstrapApp]结束：${new Date().toJSON()}`
    )
  }
  const instRequire = name => _data => {
    result = $bootstrap(inst, name, {}, _data)
  }

  const instDefineWrap = (name, definition) => $defineWrap(inst, inst, name, definition)

  const instRequireModule = name => {
    return context.quickapp.platform.requireModule(inst, removeAppPrefix(name))
  }

  const instEvaluate = context.quickapp.dock.makeEvaluateBuildScript(null)

  // 处理代码
  let functionBody
  if (typeof code === 'function') {
    functionBody = code
  } else if (code) {
    functionBody = code.toString()
    // 重新封装代码
    functionBody = `(function(global){"use strict"; ${functionBody} \n })(Object.create(this))`
  }
  console.trace(`### App Framework ### 开始编译代码----`)

  if (ENV_PLATFORM === 'na') {
    // 如果是原生渲染
    const globalObjects = {
      define: instDefine,
      require: instRequire,
      bootstrap: instBootstrap,
      $app_define$: instDefine,
      $app_bootstrap$: instBootstrap,
      $app_require$: instRequireModule,
      $app_define_wrap$: instDefineWrap,
      $app_evaluate$: instEvaluate
    }

    invokeScript(globalObjects, functionBody, 'app.js')
  } else {
    // H5 渲染
    global.$app_define$ = instDefine
    global.$app_bootstrap$ = instBootstrap
    global.$app_require$ = instRequireModule
    global.$app_evaluate$ = instEvaluate

    functionBody()
  }

  profiler.timeEnd(`PERF:initApp`)
  profiler.record(`### App Performance ### 初始化APP[PERF:initApp]结束：${new Date().toJSON()}`)
  return result
}

export { initApp }
