/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { isObject, isReserved, removeLifecyclePrefix } from 'src/shared/util'

import XEvent from './event'

import config from '../config'

import ModuleHost from '../../platform/module/index'

import { getLocaleConfig } from './locale'

import { xInvokeWithErrorHandling } from 'src/shared/error'

// app module
let appModule = null
// app 内部使用的函数名
const appInnerFuncs = ['onErrorHandler']

/**
 * 是否为 app 内部保留函数
 * @param {*} name
 * @returns {boolean}
 */
function isAppReservedFunc(name) {
  return appInnerFuncs.indexOf(name) >= 0
}

class XApp extends ModuleHost {
  constructor(id, options) {
    super(...arguments)

    this._isApp = true
    this.options = options || {}
    this.name = '' // App名称
    this.entry = '' // 入口页面名
    this.customComponentMap = {} // 自定义组件表
    this._def = null // App 定义信息（来自app文件）
    this._data = {} // 全局数据定义
    this._events = {} // 绑定事件
    this._valid = true
    this._plugins = []
    this._pluginInstalled = false // 页面是否已安装
    this._errorHandler = null // 全局错误回调

    this._shareDocStyle = false
  }

  /**
   *  清空数据
   */
  $clear() {
    this.options = {}
    this.name = ''
    this.customComponentMap = {}
    this._def = null
    this._data = {}
    this._events = {}
    // 父类
    this.destroy()
  }

  /**
   * 全局数据, 通过app.$data来访问; $data中数据只支持常量
   * data数据必须是对象
   */
  get $data() {
    if (!isObject(this._data)) {
      this._data = {}
    }
    return this._data
  }

  set $data(data) {
    const newdata = typeof data === 'function' ? data() : data
    if (newdata) {
      this._data = this._data || {}
      Object.keys(newdata).forEach(key => {
        if (isReserved(key)) {
          // 保留字
          console.error(`### App Framework ### 页面数据属性名 '${key}' 非法, 属性名不能以$或_开头`)
        } else {
          if (key in this._data) {
            console.warn(`### App Framework ### App 全局数据 (${key}) 被覆盖`)
          }
          this._data[key] = newdata[key]
        }
      })
    } else {
      this._data = {}
    }
  }

  /**
   * app 元数据
   */
  get $def() {
    return this._def
  }

  set $def(data) {
    console.trace(`### App Framework ### App 元数据初始化----`)
    this._def = data

    if (!data.plugins) {
      data.plugins = []
    }
    const manifest = data.manifest
    console.trace(`### App Framework ### App 元数据manifest----`, JSON.stringify(manifest))
    if (manifest) {
      // 应用名
      this.name = manifest.name

      const config = manifest.config
      console.trace(`### App Framework ### App 元数据config----`, JSON.stringify(config))
      if (config) {
        // 提取数据
        this.$data = config.data
        // 是否共享页面样式
        this._shareDocStyle = !!config.shareDocStyle
      }

      const router = manifest.router
      console.trace(`### App Framework ### App 元数据router----`, JSON.stringify(router))
      if (router) {
        // 入口
        this.entry = router.entry || ''
      }
    }

    // 将自定义函数属性添加到App对象上
    for (const key in data) {
      const item = data[key]
      if (typeof item === 'function' && !XEvent.isReservedEvent(key) && !isAppReservedFunc(key)) {
        this[key] = data[key]
      }
    }

    const errorHandler = data.onErrorHandler
    // 绑定全局错误回调
    if (errorHandler) {
      if (!global.isRpkMinPlatformVersionGEQ(1300)) {
        console.warn(
          `### App Framework ### onErrorHandler() 为1300版本中新增的app生命周期，不再当做app方法，如果用于方法调用或事件响应，请使用其它名称，后续版本不再兼容`
        )
      }
      if (typeof errorHandler === 'function') {
        this._errorHandler = errorHandler
      } else {
        console.warn('### App Framework ### onErrorHandler must be a function')
      }
    }

    // 绑定状态
    // this.$on('applc:onCreate', () => {})
    this.$on('applc:onDestroy', () => {
      this._valid = false
    })

    // 绑定生命周期接口函数(LC:lifecycle)
    XEvent.reserveEvents.forEach(type => {
      this.$on(`applc:${type}`, data[type])
    })

    this._defined = true
  }

  /**
   * 当前状态是否完成初始化
   * @desc 是否适合做一些Native引入的工作
   * @return {boolean}
   */
  get $valid() {
    return this._valid
  }

  getLocaleConfig() {
    return getLocaleConfig()
  }

  /**
   * 发送事件
   * @param type
   * @param detail
   */
  $emit(type, detail) {
    const events = this._events
    const handlerList = events[type]
    if (handlerList) {
      const info = `app: event handler for "${type}"`
      for (let i = 0; i < handlerList.length; i++) {
        const evt = new XEvent(type, detail)
        xInvokeWithErrorHandling(handlerList[i], this, [evt], undefined, info, this)
      }
    }
  }

  /**
   * 无事件对象，用于 app 生命周期的发布
   * @param type
   * @param param
   * @private
   */
  _emit(type, param) {
    const events = this._events
    const handlerList = events[type]
    if (handlerList) {
      const info = `app: lifecycle for "${removeLifecyclePrefix(type)}"`
      for (let i = 0; i < handlerList.length; i++) {
        xInvokeWithErrorHandling(handlerList[i], this, [param], undefined, info, this)
      }
    }
  }

  /**
   * 添加事件listener
   * @param  {string}
   * @param  {function}
   */
  $on(type, handler) {
    if (!type || !handler) {
      return
    }
    if (typeof handler !== 'function') {
      console.warn(`### App Framework ### ${removeLifecyclePrefix(type)} must be a function`)
      return
    }
    // 每个类型事件只能对应一个回调函数
    const events = this._events
    events[type] = events[type] || []
    events[type].push(handler)
  }

  /**
   * 移除事件句柄
   * @param  {string}
   */
  $off(type) {
    if (!type) {
      return
    }
    const events = this._events
    delete events[type]
  }

  /**
   * 退出应用
   */
  exit() {
    if (appModule === null) {
      appModule = config.platform.requireModule(this, 'system.app')
    }
    appModule.exit(this.id)
  }
}

export default XApp
