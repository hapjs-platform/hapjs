/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * W3C DOM事件
 */
class Event {
  /**
   * 无参数2，则不冒泡
   * @param type
   * @param eventInitDict
   */
  constructor(type, eventInitDict = { bubbles: false, cancelable: false }) {
    if (arguments.length > 1 && typeof eventInitDict !== 'object') {
      throw new Error(
        `### App Runtime ### addEventListener() 参数2传递的话，必须是对象：${eventInitDict}`
      )
    }
    this._type = type

    // 是否冒泡
    this._bubbles = eventInitDict.bubbles
    // 是否可取消
    this._cancelable = eventInitDict.cancelable
    // 触发事件的最深层节点
    this._target = null
    // 当前捕获或冒泡到的节点
    this._currentTarget = null
    // 事件阶段
    this._eventPhase = Event.NONE
    // 阻止默认行为
    this._defaultPrevented = false
    // 时间戳
    this._timeStamp = Date.now()
    // 是否遵循W3C机制：否则只在当前节点上触发
    this._supportW3C = true

    // 标识：停止传播，停止立即传播
    this._flagStopPropagation = false
    this._flagStopImmediatePropagation = false

    // 如果回调报错，是否抛错
    this._throwError = true
    // 事件被处理的节点
    this._listenNodes = {}
  }

  get type() {
    return this._type
  }

  get bubbles() {
    return this._bubbles
  }

  get cancelable() {
    return this._cancelable
  }

  get target() {
    return this._target
  }

  get currentTarget() {
    return this._currentTarget
  }

  get eventPhase() {
    return this._eventPhase
  }

  get defaultPrevented() {
    return this._defaultPrevented
  }

  get timeStamp() {
    return this._timeStamp
  }

  stopPropagation() {
    this._flagStopPropagation = true
  }

  stopImmediatePropagation() {
    this._flagStopImmediatePropagation = true
    this.stopPropagation()
  }

  preventDefault() {
    throw new Error(`### App Runtime ### preventDefault() 暂不支持该方法`)
  }

  toJSON() {
    return {
      type: this._type,
      target: this._target,
      currentTarget: this._currentTarget,
      timeStamp: this._timeStamp
    }
  }
}

// 未分发
Event.NONE = 0
// 捕获期，Target之前
Event.CAPTURING_PHASE = 1
// 正处于Target
Event.AT_TARGET = 2
// 冒泡期，Target之后
Event.BUBBLING_PHASE = 3

export default Event
