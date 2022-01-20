/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import base from './base'

// 对操作指令的解释
// 0x20000001 31位 1是webgl，0是2d 30位1 0是异步，1位同步 1-13位是操作，其他为保留位
const OPERATION = {
  RENDER_2D_SYNC: 0x20000001,
  BUFFER_2D_SYNC: 0x20000000
}
export const config = {
  name: 'system.canvas',
  methods: [
    {
      name: 'enable',
      type: 0,
      mode: 0
    },
    {
      name: 'getContext',
      type: 0,
      mode: 0
    },
    {
      name: 'canvasNative2D',
      type: 0,
      mode: 1
    },
    {
      name: 'canvasNative2DSync',
      type: 0,
      mode: 0
    }
  ]
}

const moduleOwn = Object.assign(
  {
    name: 'system.canvas',

    enable(options = {}) {
      return this.mockSync(options._data, options._code)
    },

    getContext(options = {}) {
      return this.mockSync(options._data, options._code)
    },

    canvasNative2DSync(options = {}) {
      return global.extendCallNative(
        options.componentId,
        OPERATION.BUFFER_2D_SYNC,
        options.commands
      )
    },

    canvasNative2D(options = {}) {
      global.extendCallNative(options.componentId, OPERATION.RENDER_2D_SYNC, options.commands)
      return this.mockSync(options._data, options._code)
    }
  },
  base
)

export default moduleOwn
