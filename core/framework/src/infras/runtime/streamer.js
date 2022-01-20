/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * @file 消息汇总
 * @desc 模式不同时发送策略不一样
 */
class Streamer {
  /**
   * 构造函数
   * @param call {Function} 交给Native处理的函数
   * @param batchThreshold
   */
  constructor(call, batchThreshold) {
    this.call = call
    this.batchThreshold = batchThreshold === undefined ? 50 : batchThreshold
    this._boolDefineNextTick = true

    this.list = []
  }

  push(id, list) {
    let result

    if (this.batchThreshold === 0) {
      this.list.push.apply(this.list, list)
    } else if (this.batchThreshold === 1) {
      // 合并之前
      this.list.push.apply(this.list, list)
      result = this.call(id, this.list)
      this.list.splice(0)
    } else if (this.batchThreshold > 1) {
      // 合并之前
      this.list.push.apply(this.list, list)
      // 批量截取发送
      while (this.list.length >= this.batchThreshold) {
        const segs = this.list.splice(0, this.batchThreshold)
        result = this.call(id, segs)
      }
      // 此处可能会有剩余
    } else {
      throw new Error(`### App Runtime ### push() 不支持的batchThreshold值：${this.batchThreshold}`)
    }

    // 记得下次发送
    this._boolDefineNextTick && this._defineNextTick(id)

    return result
  }

  /**
   * 发送结束标识
   */
  over(id, list) {
    if (this.list.length) {
      // 发送剩余消息
      this.call(id, this.list)
      this.list.splice(0)
    }
    const result = this.call(id, list)

    return result
  }

  _defineNextTick(id) {
    if (!this._nextTick) {
      this._nextTick = Promise.resolve().then(() => {
        if (this.list.length) {
          // 应该由框架保证及时发送
          console.warn(`### App Runtime ### _defineNextTick(${id}) 应该由框架保证及时发送`)
        }
        this._nextTick = null
      })
    }
  }
}

export default Streamer
