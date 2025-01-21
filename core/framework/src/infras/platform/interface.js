/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import common from '../common'
import { invokeScript } from 'src/shared/function'

import ModuleHost from './module/index'
import { registerModules, execInvokeCallback, requireModule } from './module/interface'
import Session from './session'
import {
  registerBundleChunks,
  requireBundleChunk,
  registerComponentJson,
  requireJson
} from './chunk'

import {
  registerManifest,
  isRpkMinPlatformVersionGEQ,
  getManifestField,
  isRpkDebugMode
} from './manifest'

import initInterface from './interface/index'

import BroadcastChannel from './broadcastchannel/index'
import { setUpPromiseRejectionHook } from './promise'

// 宿主环境类型
const ENGINE_TYPE = {
  PAGE: 'page',
  CARD: 'card'
}

function init() {
  // [ISSUE-989] setPromiseRejectionCallback
  setUpPromiseRejectionHook()

  common.setNativeConsole()
  common.setNativeProfiler()
  common.setNativeTimer()
  common.setNativeRouter()
}

/**
 * 暴露供Native使用
 * @param methodNames
 */
function exposeToNative(methodNames) {
  for (const methodName in methodNames) {
    console.trace(`### App Framework ### 注册全局函数----`, methodName)
    global[methodName] = (...args) => {
      const ret = methodNames[methodName](...args)
      if (ret instanceof Error) {
        console.error(`### App Framework ### ${ret.toString()}`)
      }
      return ret
    }
  }
}

// JS模块缓存Map
const bundleMap = {}

/**
 * 缓存JS模块
 * @param {string} name 模块名称
 * @param {object} bundle 需要缓存的模块
 */
function defineBundle(name, bundle = null) {
  bundleMap[name] = bundle
}

/**
 * 获取JS模块，如果不存在则从安卓加载并缓存
 * @param {string} name 需要加载的模块名称
 * @return {object}
 */
function requireBundle(name) {
  name = name.replace(/\.js$/, '')
  if (!bundleMap[name]) {
    let code = global.readResource(`assets:///js/bundles/${name}.js`)
    if (typeof code === 'string') {
      code = invokeScript({}, `${code}; return ${name}`, `/bundles/${name}.js`)
    }
    defineBundle(name, code)
  }
  const module = bundleMap[name]
  return module
}

// 放在全局
// global.__dateReadresCost = global.__dateReadresCost || 0

/**
 * 调用安卓接口同步加载对应资源
 * @param {string} uri 带有协议的资源地址
 * @return {string} 文件字符串，如果文件不存在返回null
 */
function loadResource(uri) {
  // const dateReadresS = new Date()

  // profiler.time(`PERF:readResource(${uri})`)
  const ret = global.readResource(uri)
  // profiler.timeEnd(`PERF:readResource(${uri})`)

  // const dateReadresE = new Date()
  // global.__dateReadresCost += dateReadresE - dateReadresS
  // profiler.record(
  //   `### App Performance ### 读取资源文件[PERF:readResource]累积耗时：${global.__dateReadresCost}ms`
  // )

  if (!ret) {
    console.warn(`### App Framework ### 文件不存在，请确认其路径：${uri}`)
  }
  return ret
}

function requireScriptFile(filePath, globalObjects) {
  // 读取
  const content = requireBundleChunk(filePath)
  // 执行
  if (content && typeof content === 'string') {
    return invokeScript(
      globalObjects,
      `(function(global){"use strict"; ${content}  \n })(Object.create(this))`,
      filePath
    )
  }
}

export default {
  init,
  exposeToNative,
  defineBundle,
  Session,
  requireBundle,
  loadResource,
  BroadcastChannel,
  ENGINE_TYPE,
  // 模块
  ModuleHost,
  requireModule,
  requireScriptFile,
  requireJson,
  initInterface,
  exposure: {
    registerModules,
    registerBundleChunks,
    registerComponentJson,
    execInvokeCallback,
    registerManifest,
    getManifestField,
    isRpkMinPlatformVersionGEQ,
    isRpkDebugMode
  }
}
