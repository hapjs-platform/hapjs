/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { APP_KEYS } from 'src/shared/events'
import config from '../config'

import ModuleHost from '../../platform/module/index'

class XPage extends ModuleHost {
  /**
   * page 的构造函数, 初始化页面 meta 信息
   * @param {number} id - 唯一id, 由原生端生成
   * @param {object} app - 所属App
   * @param {object} intent - 系统传递到页面的数据
   * @param {boolean} intent.fromExternal - 请求是否来源于外部
   * @param {string} intent.action - 请求动作
   * @param {string} intent.orientation - 屏幕方向，为枚举值：portrait,landscape
   * @param {string} intent.uri - 匹配规则
   * @param {object} meta - 页面路径元信息，对应router对象
   * @param {string} meta.name - 对应router对象中的key
   * @param {string} meta.component - 对应router对象中的文件名(无后缀)
   * @param {string} meta.path - 对应router对象中的路径
   * @param {object} query - 页面的参数
   */
  constructor(id, app, intent, meta, query) {
    super(...arguments)

    this._isPage = true
    this.app = app // 所属App
    this.name = null // 页面名称
    this.vm = null // 顶层Vm
    this.intent = intent // 系统传递到页面的数据
    this.doc = config.runtime.helper.createDocument(id) // vdom模型
    this._valid = true
    this._visible = false
    this._meta = Object.assign({ query }, intent, meta)
    this._orientation = intent.orientation
    this.nextTickCallbacks = [] // nextTick的回调函数数组
  }

  get $valid() {
    return this._valid
  }

  get $visible() {
    return this._valid && this._visible
  }

  get orientation() {
    return this._orientation
  }

  get pageName() {
    return this._meta.name
  }

  get pageComponent() {
    return this._meta.component
  }

  get query() {
    return this._meta.query
  }

  /**
   * @override
   * @param args
   */
  invoke(...args) {
    super.invoke(...args)

    if (this.$valid) {
      config.publish(APP_KEYS.callbackDone, [this])
    }
  }
}

export default XPage
