/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $remove } from '../util'

// 唯一Id
let _uid = 0
// 被观察的目标对象栈
let _targetStack = []

/**
 * 依赖管理
 */
class XLinker {
  constructor() {
    this.id = _uid++ // 唯一id
    this.subs = [] // 订阅者watcher
    // console.trace(`### App Framework ### 创建 XLinker ${this.id}`)
  }

  /**
   * 添加订阅者
   * @param sub
   */
  addSub(sub) {
    this.subs.push(sub)
  }

  /**
   * 移除订阅者
   * @param sub
   */
  removeSub(sub) {
    $remove(this.subs, sub)
  }

  /**
   * 建立watcher与dep的关联
   */
  depend() {
    if (XLinker.target) {
      // target是watcher
      XLinker.target.addLink(this)
    }
  }

  /**
   * 通知订阅者新值
   */
  notify() {
    // 顺序通知
    const subs = this.subs.slice()
    for (let i = 0, l = subs.length; i < l; i++) {
      subs[i].update()
    }
  }
}

// 当前被观察的目标对象
XLinker.target = null

XLinker.pushTarget = function(target) {
  _targetStack.push(target)
  XLinker.target = target
}

XLinker.popTarget = function() {
  _targetStack.pop()
  XLinker.target = _targetStack[_targetStack.length - 1]
}

XLinker.resetTarget = function() {
  XLinker.target = null
  _targetStack = []
}

export default XLinker
