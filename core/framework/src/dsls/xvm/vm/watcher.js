/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $extend, isObject } from 'src/shared/util'

import XLinker from './linker'

import { $remove } from '../util'

import { xHandleError, xInvokeWithErrorHandling } from 'src/shared/error'

let _uid = 0

/**
 * watcher解析表达式，收集依赖；当表达式的值改变时，触发回调；
 * @param vm
 * @param expOrFn
 * @param cb
 * @param options
 * @param options.sync {Boolean} 默认为异步
 * @param options.lazy {Boolean} 计算属性开启lazy模式，默认是false
 * @constructor
 */
class XWatcher {
  constructor(vm, target, cb, options) {
    // 合并option属性
    if (options) {
      $extend(this, options)
    }

    this.sync = !!this.sync
    this.lazy = !!this.lazy
    this.dirty = this.lazy

    this.vm = vm
    this.vmGetter = global.isRpkDebugMode() ? this.proxy(vm) : vm
    vm._watchers.push(this)

    this.expression = target
    this.cb = cb
    this.id = ++_uid // 唯一ID，用于批处理
    console.trace(`### App Framework ### 创建 XWatcher ${this.id} ${this.expression.toString()}`)

    this.active = true

    this.links = []
    this.linkIds = new Set()
    this.newLinks = [] // 新添加的依赖（当expression执行时，会把其中使用到的变量（通过封装的getter函数）的link添加进来）
    this.newLinkIds = new Set() // 新添加的依赖的Id

    // 设置get函数
    if (typeof target === 'function') {
      this.getter = target
    }
    this.value = this.lazy ? undefined : this.get() // 计算初始值
  }

  proxy(vm) {
    return new Proxy(vm, {
      get(target, key) {
        const has = key in target
        const descriptor = Object.getOwnPropertyDescriptor(target, key)
        const hasGetter = has && descriptor && descriptor.get
        if (!hasGetter) {
          if (descriptor && descriptor.value && typeof descriptor.value === 'function') {
            // 如果是methods，则不打印warn
          } else if (has && !Object.hasOwnProperty.call(target, key)) {
            // 如果属性在当前对象上不存在而在原型链上存在，如for指令生成的辅助Xvm，不打印warn
          } else if (typeof key !== 'symbol' && key[0] !== '_') {
            // 对象被Proxy代理后，原本访问[Symbol.toPrimitive]属性也会被劫持，即使该对象并未明确定义该属性
            // computed或者渲染watcher，则触发warn
            console.warn(
              `### App Framework ### 请确认VM的data/public/protected/private中定义了属性：${key}`
            )
          }
        }
        return target[key]
      }
    })
  }

  /**
   * 执行get函数，重新收集依赖
   * @returns {*}
   */
  get() {
    XLinker.pushTarget(this)
    let value
    const vm = this.vm
    // console.trace(`### App Framework ### XLinker pushTarget ${this.id}`)
    try {
      value = this.active ? this.getter.call(this.vmGetter, vm) : undefined
    } catch (e) {
      // 防止存在错误的 computed属性 不断被重新计算，无限触发错误回调
      this.dirty = false
      xHandleError(e, vm, `getter for watcher "${this.expression}"`)
    } finally {
      XLinker.popTarget()
      // console.trace(`### App Framework ### XLinker popTarget ${this.id}`)
      this.clearLink()
    }
    return value
  }

  /**
   * 添加依赖
   * @param link
   */
  addLink(link) {
    const id = link.id
    // console.trace(`### App Framework ### 观察器添加依赖 [${id}]`)
    if (!this.newLinkIds.has(id)) {
      this.newLinkIds.add(id)
      this.newLinks.push(link)
      // 如果之前没有依赖关系，则建立关系
      if (!this.linkIds.has(id)) {
        link.addSub(this) // 因为dep.subs是个数组，防止重复添加
      }
    }
  }

  /**
   * 清空依赖
   */
  clearLink() {
    let i = this.links.length
    // 解除双向依赖关系
    while (i--) {
      const link = this.links[i]
      if (!this.newLinkIds.has(link.id)) {
        link.removeSub(this)
      }
    }

    // 将新依赖Id列表体替换旧的
    let tmp = this.linkIds
    this.linkIds = this.newLinkIds
    this.newLinkIds = tmp
    this.newLinkIds.clear()

    tmp = this.links
    this.links = this.newLinks
    this.newLinks = tmp
    this.newLinks.length = 0
  }

  /**
   * 当依赖者改变时被调用
   */
  update() {
    if (this.lazy) {
      this.dirty = true
    } else if (this.sync) {
      this.run()
    } else {
      if (this.vm && this.vm._page && this.vm._page.$valid) {
        const executor = this.vm._page.executor
        executor.join(this)
      }
    }
  }

  evaluate() {
    this.value = this.get()
    this.dirty = false
  }

  run() {
    if (this.active) {
      // 重新计算calc
      const value = this.get()
      // 如果结果发生变化，或者结果是对象
      if (value !== this.value || isObject(value)) {
        // 设置新值
        const oldValue = this.value
        this.value = value
        const expression = this.expression ? this.expression.originExp || this.expression : ''
        const info = `callback for watcher "${expression}"`
        // 调用回调
        xInvokeWithErrorHandling(this.cb, this.vm, [value, oldValue], this.vm, info)
      }
    }
  }

  /**
   * 批量添加依赖
   */
  depend() {
    let i = this.links.length
    while (i--) {
      this.links[i].depend()
    }
  }

  /**
   *  从依赖项的订阅者列表中移除
   */
  close() {
    if (this.active) {
      $remove(this.vm._watchers, this)
      let i = this.links.length
      while (i--) {
        this.links[i].removeSub(this)
      }
      this.active = false
      this.vm = this.vmGetter = this.cb = this.value = null
    }
  }
}

export default XWatcher
