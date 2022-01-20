/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import base from './base'

export const config = {
  name: 'system.Class1',
  instantiable: true,
  methods: [
    // 构造函数对应
    {
      name: '__init__',
      instanceMethod: false,
      type: 0,
      mode: 0
    },
    // Method
    {
      name: 'methodClass1',
      instanceMethod: false,
      type: 0,
      mode: 0
    },
    {
      name: 'methodInst1',
      instanceMethod: true,
      type: 0,
      mode: 0
    },
    // Property
    {
      name: '__getReadwrite',
      instanceMethod: true,
      mode: 0,
      type: 1,
      access: 1,
      alias: 'readwrite'
    },
    {
      name: '__setReadwrite',
      instanceMethod: true,
      mode: 0,
      type: 1,
      access: 2,
      alias: 'readwrite'
    },
    // Event
    {
      name: '__onCustomEvent1',
      instanceMethod: true,
      mode: 1,
      type: 2,
      alias: 'onCustomEvent1'
    }
  ]
}

let instId = 0

const moduleOwn = Object.assign(
  {
    name: 'system.Class1',
    readwrite: undefined,
    handlerCustomEvent1: null,

    __init__(options = {}) {
      options._data = options._data || {}
      options._data.instId = ++instId
      options._data.name = 'system.Class1'
      options._data._nativeType = 0

      return this.mockSync(options._data, options._code)
    },

    methodClass1(options = {}) {
      return this.mockSync(options._data, options._code)
    },

    methodInst1(options = {}) {
      return this.mockSync(options._data, options._code)
    },

    __getReadwrite() {
      return {
        content: this.readwrite
      }
    },

    __setReadwrite(options) {
      this.readwrite = options.value
    },

    __onCustomEvent1(options, callbackId) {
      if (callbackId <= 0) {
        this.clearTimeout(this.handlerCustomEvent1)
        this.handlerCustomEvent1 = null
      } else {
        this.handlerCustomEvent1 = this.setTimeout(callbackId, 'onCustomEvent1')
      }
    }
  },
  base
)

export default moduleOwn
