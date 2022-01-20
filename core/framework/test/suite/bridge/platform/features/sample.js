/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import base from './base'

export const config = {
  name: 'system.sample',
  methods: [
    // Method
    {
      name: 'methodSync1',
      type: 0,
      mode: 0
    },
    {
      name: 'methodSync2',
      type: 0,
      mode: 0,
      multiple: 1
    },
    {
      name: 'methodCallback1',
      type: 0,
      mode: 1
    },
    {
      name: 'methodCallback2',
      type: 0,
      mode: 1,
      normalize: 0
    },
    {
      name: 'methodSubscribe1',
      type: 0,
      mode: 2
    },
    {
      name: 'methodSubscribe2',
      type: 0,
      mode: 2,
      multiple: 1
    },
    {
      name: 'methodUnsubscribe1',
      type: 0,
      mode: 0
    },
    {
      name: 'methodUnsubscribe2',
      type: 0,
      mode: 0,
      multiple: 1
    },
    {
      name: 'getAttr',
      type: 0,
      mode: 0
    },
    {
      name: 'methodBindInstSync1',
      type: 0,
      mode: 0
    },
    {
      name: 'methodBindInstCallback1',
      type: 0,
      mode: 1
    },
    // Property
    {
      name: '__getReadwrite',
      mode: 0,
      type: 1,
      access: 1,
      alias: 'readwrite'
    },
    {
      name: '__setReadwrite',
      mode: 0,
      type: 1,
      access: 2,
      alias: 'readwrite'
    },
    {
      name: '__getReadonly',
      mode: 0,
      type: 1,
      access: 1,
      alias: 'readonly'
    },
    {
      name: '__setWriteonly',
      mode: 0,
      type: 1,
      access: 2,
      alias: 'writeonly'
    },
    // Event
    {
      name: '__onCustomEvent1',
      mode: 1,
      type: 2,
      alias: 'onCustomEvent1'
    }
  ]
}

const moduleOwn = Object.assign(
  {
    name: 'system.sample',

    handlerSubscribe1: null,
    handlerSubscribe2: null,

    readwrite: undefined,
    readonly: 'v1',
    writeonly: 'v1',

    handlerCustomEvent1: null,

    methodSync1(options = {}) {
      return this.mockSync(options._data, options._code)
    },

    // multiple 类型的调用
    methodSync2(options = {}) {
      return this.mockSync(options._data, options._code)
    },

    methodCallback1(options = {}, callbackId) {
      this.mockOnce(callbackId, options._data, options._code)
    },

    // 接收原始类型
    methodCallback2(options = {}, callbackId) {
      this.mockOnce(callbackId, options._data, options._code)
    },

    methodSubscribe1(options = {}, callbackId) {
      if (!this.handlerSubscribe1) {
        this.handlerSubscribe1 = this.mockSubscribe(callbackId, options._data, options._code)
      }
    },

    methodSubscribe2(options = {}, callbackId) {
      if (!this.handlerSubscribe2) {
        this.handlerSubscribe2 = this.mockSubscribe(callbackId, options._data, options._code)
      }
    },

    methodUnsubscribe1(options) {
      this.mockUnsubscribe(this.handlerSubscribe1)
      this.handlerSubscribe1 = null
    },

    methodUnsubscribe2() {
      this.mockUnsubscribe(this.handlerSubscribe2)
      this.handlerSubscribe1 = null
    },

    getAttr(attrName) {
      return {
        content: this[attrName]
      }
    },

    methodBindInstSync1(options = {}, callbackId, moduleInstId) {
      if (options._data.moduleInstId && moduleInstId !== options._data.moduleInstId) {
        throw new Error(`1.拥有instId时，moduleInstId不应该为null`)
      }
      return this.mockSync(options._data, options._code)
    },

    methodBindInstCallback1(options = {}, callbackId, moduleInstId) {
      if (options._data.moduleInstId && moduleInstId !== options._data.moduleInstId) {
        throw new Error(`2.拥有instId时，moduleInstId不应该为null`)
      }
      this.mockOnce(callbackId, options._data, options._code)
    },

    __getReadwrite() {
      return {
        content: this.readwrite
      }
    },

    __setReadwrite(options) {
      this.readwrite = options.value
    },

    __getReadonly() {
      return {
        content: this.readonly
      }
    },

    __setWriteonly(options) {
      this.writeonly = options.value
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
