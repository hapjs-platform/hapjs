/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { removeAppPrefix } from 'src/shared/util'

import { invokeScript } from 'src/shared/function'

import context from '../context'

import XExecutor from './executor'

import { $define, $bootstrap, $defineWrap } from './bundle'

import { fireEvent, destroyPage, updatePageActions } from './misc'

/**
 * 初始化页面
 * @param page
 * @param code
 * @param query
 * @returns {*}
 */
function initPage(page, code, query, globals) {
  profiler.record(`### App Performance ### 初始化Page[PERF:initPage]开始：${new Date().toJSON()}`)
  profiler.time(`PERF:initPage`)
  console.trace(`### App Framework ### 开始初始化页面(${page.id})---- `, JSON.stringify(query))
  page.executor = new XExecutor(page.id, page)

  let result
  // 准备App全局方法
  const instDefine = (...args) => {
    $define(page, ...args)
  }
  const instBootstrap = (name, configs) => {
    profiler.record(
      `### App Performance ### 启动Page[PERF:bootstrapPage]开始：${new Date().toJSON()}`
    )
    profiler.time(`PERF:bootstrapPage`)
    result = $bootstrap(page, name, configs, query)
    const hasCallbacks = page.nextTickCallbacks && page.nextTickCallbacks.length > 0
    page.doc.listener.createFinish(null, hasCallbacks)

    console.trace(`### App Framework ### 完成实例初始化(${page.id})----`)
    profiler.timeEnd(`PERF:bootstrapPage`)
    profiler.record(
      `### App Performance ### 启动Page[PERF:bootstrapPage]结束：${new Date().toJSON()}`
    )
  }
  const instDefineWrap = (name, definition) => $defineWrap(page.app, page, name, definition)

  const instRequire = name => _data => {
    result = $bootstrap(page, name, {}, _data)
  }
  const instDocument = page.doc
  const instRequireModule = name => {
    return context.quickapp.platform.requireModule(page, removeAppPrefix(name))
  }

  const instEvaluate = context.quickapp.dock.makeEvaluateBuildScript(globals)

  const jsonRequire = (jsonModuleName, options) => {
    return context.quickapp.platform.requireJson(jsonModuleName, options)
  }

  // 处理代码
  let functionBody
  if (typeof code === 'function') {
    functionBody = code
  } else if (code) {
    functionBody = code.toString()
    // 重新封装代码
    functionBody = `(function(global){"use strict"; ${functionBody}  \n })(Object.create(this))`
  }
  console.trace(`### App Framework ### 开始编译代码----`)

  if (ENV_PLATFORM === 'na') {
    // 如果是原生渲染
    const globalObjects = Object.assign(
      {
        define: instDefine,
        require: instRequire,
        document: instDocument,
        bootstrap: instBootstrap,
        $app_define$: instDefine,
        $app_bootstrap$: instBootstrap,
        $app_require$: instRequireModule,
        $app_define_wrap$: instDefineWrap,
        $app_evaluate$: instEvaluate,
        $json_require$: jsonRequire
      },
      globals
    )

    const bundleUrl = page.pageName ? `${page.pageName}/${page.pageComponent}.js` : '<anonymous>'
    console.trace(`### App Framework ### bundleUrl----`, bundleUrl)
    invokeScript(globalObjects, functionBody, bundleUrl)
  } else {
    // H5 渲染
    global.$app_define$ = instDefine
    global.$app_bootstrap$ = instBootstrap
    global.$app_require$ = instRequireModule
    global.$app_define_wrap$ = instDefineWrap
    global.$app_evaluate$ = instEvaluate
    global.$json_require$ = jsonRequire

    global.setTimeout = globals.setTimeout
    global.setInterval = globals.setInterval
    global.clearTimeout = globals.clearTimeout
    global.clearInterval = globals.clearInterval
    global.requestAnimationFrame = globals.requestAnimationFrame
    global.cancelAnimationFrame = globals.cancelAnimationFrame

    functionBody()
  }

  profiler.timeEnd(`PERF:initPage`)
  profiler.record(`### App Performance ### 初始化Page[PERF:initPage]结束：${new Date().toJSON()}`)
  return result
}

/**
 * 调用页面VM的生命周期事件
 * @param {object} page 对应的页面
 * @param {string} event 传入的事件名
 * @param {any} params 事件触发时的参数
 * @param {array} args 事件除了params额外携带的参数
 */
function invokePageEvent(event, page, params, ...args) {
  let result = false
  if (page.vm && page.vm._ready) {
    result = page.vm._emit(`xlc:${event}`, params, ...args)
    updatePageActions(page)
    console.trace(`### App Framework ### 页面(${page.id})触发事件(${event})完毕`)
  } else {
    console.trace(`### App Framework ### 页面(${page.id})触发事件(${event})失败`)
  }
  return result
}

/**
 * 处理特殊事件：点击菜单
 * @param {object} page 对应的页面
 */
function handleMenuPressEvent(page) {
  let result = false
  if (page.vm && page.vm._ready) {
    const events = page.vm._vmEvents
    const handlerList = events && events['xlc:onMenuPress']
    if (handlerList && handlerList.length) {
      result = true
    }
  }
  return result
}

export { initPage, fireEvent, destroyPage, invokePageEvent, handleMenuPressEvent }
