/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { isPlainObject, isApplication, isComponent, removeAppPrefix } from 'src/shared/util'

import context from '../context'

import XVm from '../vm/index'
import XEvent from './event'
import VmI18n from '../plugin-i18n/index'

import { defineCustomComponent } from './custom'

/**
 * 创建App实例
 * @param app
 * @param name
 * @param config
 * @returns {Error}
 */
const $bootstrap = function(app, name, config) {
  console.trace(`### App Framework ### 启动App---- ${name}`)

  // 验证页面‘@app-application/xxxx’
  let appName
  if (isApplication(name)) {
    appName = removeAppPrefix(name)
  } else {
    return new Error(`bootstrap: 错误App名: ${name}`)
  }

  // 验证配置信息（确保config是对象）,确保文件打包版本与Runtime版本保持兼容
  config = isPlainObject(config) ? config : {}

  // 混合到app中
  XVm.mixinApp = function(options) {
    for (const key in options) {
      // 绑定保留的生命周期
      if (XEvent.isReservedEvent(key)) {
        const keyName = `applc:${key}`
        const handler = options[key]
        if (typeof handler === 'function') {
          app.$on(keyName, handler)
        } else {
          console.warn(`### App Framework ### ${key} must be a function`)
        }
      } else if (key === 'onErrorHandler') {
        console.warn(
          `### App Framework ### ${key} 生命周期不支持以插件的方式定义，请在 app.ux 导出的对象中定义`
        )
      } else {
        console.warn(`### App Framework ### 插件定义的函数: ${key}，不属于 APP 生命周期函数`)
      }
    }
  }

  // 安装插件
  app._plugins.push(VmI18n)
  app._plugins.push(...app.$def.plugins)
  if (!app._pluginInstalled) {
    for (let i = 0, len = app._plugins.length; i < len; i++) {
      const pluginPass = app._plugins[i]
      let pluginItem, pluginArgs

      if (pluginPass instanceof Array) {
        // 数组形式
        pluginItem = pluginPass[0]
        pluginArgs = pluginPass.slice(1)
      } else {
        // 对象形式
        pluginItem = pluginPass
        pluginArgs = []
      }

      if (!pluginItem._installed) {
        pluginItem.install(XVm, ...pluginArgs)
        pluginItem._installed = true
      } else {
        console.warn(`### App Framework ### 插件已安装，不再安装：`, JSON.stringify(pluginItem))
      }
    }
    app._pluginInstalled = true
  }

  // 当前页面名
  app.name = appName
  profiler.record(
    `### App Performance ### 触发APP创建事件[PERF:appOnCreate]开始：${new Date().toJSON()}`
  )
  profiler.time(`PERF:appOnCreate`)
  console.trace(`### App Framework ### 调用App(${appName})生命周期 onCreate`)
  app._emit('applc:onCreate')
  profiler.timeEnd(`PERF:appOnCreate`)
}

/**
 * 执行函数，生成对外的模块
 * @param {*} inst 模块所属的实例对象inst
 * @param {string} name 模块的名称
 * @param {*} args 生成模块所依赖的参数
 */
function defineModule(inst, name, args) {
  // 支持2种参数形式：
  // 1. name, factory()
  // 2. name, definition{}
  let factory, definition
  if (args.length > 1) {
    definition = args[1]
  } else {
    definition = args[0]
  }
  if (typeof definition === 'function') {
    factory = definition
    definition = null
  }

  // 如果有组件的工厂函数
  if (factory) {
    // app_require 函数
    const r = name => {
      return context.quickapp.platform.requireModule(inst, removeAppPrefix(name))
    }
    const m = { exports: {} }
    profiler.record(
      `### App Performance ### 执行自定义组件函数[PERF:executeCustomCompFn:${name}]开始：${new Date().toJSON()}`
    )
    profiler.time(`PERF:executeCustomCompFn:${name}`)

    factory(r, m.exports, m, m.exports)
    definition = m.exports // 执行函数得到的定义对象
    console.trace(`### App Framework ### 初始化组件配置`, JSON.stringify(definition))
    profiler.timeEnd(`PERF:executeCustomCompFn:${name}`)
    profiler.record(
      `### App Performance ### 执行自定义组件函数[PERF:executeCustomCompFn:${name}]结束：${new Date().toJSON()}`
    )
  }
  return definition
}

/**
 * 定义应用
 * @param app
 * @param name
 * @param args
 */
const $define = function(app, name, ...args) {
  const definition = defineModule(app, name, args)

  if (isComponent(name)) {
    // 应用公共组件
    const cleanName = removeAppPrefix(name)
    // 将组件定义放入app.customComponentMap中
    console.trace(`### App Framework ### 注册公共自定义组件 ${cleanName}`)
    defineCustomComponent(app, app, cleanName, definition)
  }

  if (isApplication(name)) {
    // 应用App数据
    console.trace(`### App Framework ### 初始化App配置`, JSON.stringify(definition || {}))
    app.$def = definition
  }
}

/**
 * 封装定义函数
 * @param app app实例
 * @param inst page或者app实例
 * @param name
 * @param definition 组件函数定义
 */
const $defineWrap = function(app, inst, name, definition) {
  // 应用定义
  const cleanName = removeAppPrefix(name)
  // 将组件定义放入app.customComponentMap中
  console.trace(`### App Framework ### 注册自定义组件的包装函数 ${cleanName}`)
  defineCustomComponent(app, inst, cleanName, definition)
}

export { $bootstrap, $define, $defineWrap, defineModule }
