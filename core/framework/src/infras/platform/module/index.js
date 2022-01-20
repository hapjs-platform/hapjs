/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { setModuleHostMap } from './misc'

class ModuleHost {
  constructor(id) {
    this.id = id // 唯一id, 由原生端生成
    this._callbacks = {}
    this._nativeInstList = []

    setModuleHostMap(id, this)
  }

  /**
   * 接收接口回调的函数
   * @param inst
   * @param callbackId
   * @param data
   * @param ifKeepAlive
   * @return {Error}
   */
  invoke(inst, callbackId, data, ifKeepAlive) {
    console.trace(
      `### App Framework ### 调用对象(${inst.id})的回调(${callbackId}) 参数：`,
      JSON.stringify(data)
    )

    const callback = inst._callbacks[callbackId]
    if (typeof callback === 'function') {
      // 必须是函数
      // 执行回调
      const ret = callback(data)

      // 如果是定时器函数，则保留；否则清除（只使用一次）
      if (typeof ifKeepAlive === 'undefined' || ifKeepAlive === false) {
        inst._callbacks[callbackId] = undefined
      }

      return ret
    } else {
      return new Error(`invoke: 无效invoke回调函数Id "${callbackId}"`)
    }
  }

  destroy() {
    this._callbacks = null
    this._nativeInstList = null
    setModuleHostMap(this.id, null)
  }
}

export default ModuleHost
