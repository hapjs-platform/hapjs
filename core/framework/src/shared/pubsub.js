/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * 提供Publish-Subscribe模型，作为dock与dsl的中间通讯层
 */
export default class Pubsub {
  constructor() {
    this.eventMap = {}
  }

  /**
   * 订阅事件
   * @param type {string} 事件名称
   * @param fn {function} 响应函数
   * @param options {object} 暂时保留
   * @return {*}
   */
  subscribe(type, fn, options) {
    if (options && options.once) {
      const fnOnce = args => {
        fn(args)
        this.remove(type, fnOnce)
      }
      return this.subscribe(type, fnOnce)
    }

    this.eventMap[type] = this.eventMap[type] || []

    if (typeof fn === 'function') {
      const list = this.eventMap[type]
      if (list.indexOf(fn) === -1) {
        list.push(fn)
      }
    }
  }

  /**
   * 发布事件
   * @param type {string} 事件名称
   * @param args {array} 事件触发时的参数
   * @param options {object} 暂时保留
   * @return {*}
   */
  publish(type, args, options) {
    let lastRet = null
    const list = this.eventMap[type] || []
    for (let i = 0, len = list.length; i < len; i++) {
      lastRet = list[i](args, lastRet)
    }
    return lastRet
  }

  /**
   * 删除事件订阅
   * @param type {string} 事件名称
   * @param fn {function} 响应函数
   */
  remove(type, fn) {
    if (!this.eventMap[type]) return
    const list = this.eventMap[type]
    const index = list.indexOf(fn)
    if (index > -1) {
      list.splice(index, 1)
    }
  }
}
