/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import base from './base'

export const config = {
  name: 'service.bannerAd',
  methods: [
    // Method
    {
      name: 'methodSyncSimple',
      type: 0,
      mode: 0
    },
    // 移除监听
    {
      name: 'removeEventSyncMulti',
      type: 0,
      mode: 0,
      multiple: 1
    },
    // Event Method
    {
      name: 'methodEventMulti',
      mode: 1,
      type: 0,
      multiple: 1
    },
    // Property
    {
      name: '__getReadwrite',
      mode: 0,
      type: 1,
      access: 1,
      alias: 'readwrite',
      subAttrs: ['subAttr1']
    },
    {
      name: '__setReadwrite',
      mode: 0,
      type: 1,
      access: 2,
      alias: 'readwrite',
      subAttrs: ['subAttr1']
    }
  ]
}

const moduleOwn = Object.assign(
  {
    name: 'service.bannerAd',
    readwrite: {},
    handlerCustomEvents: {}, // 回调列表

    methodSyncSimple(options = {}, callbackId, moduleInstId) {
      return this.mockSync(options._data, options._code)
    },

    methodEventMulti(options, callbackId, moduleInstId) {
      if (callbackId > -1) {
        this.setTimeoutMulti(this.handlerCustomEvents, callbackId, 'onCustomEvent1')
      }
    },

    removeEventSyncMulti(options = {}, callbackId, moduleInstId) {
      return this.clearTimeoutMulti(this.handlerCustomEvents, callbackId)
    },

    __getReadwrite(options = {}, callbackId, moduleInstId) {
      return {
        content: Object.assign({}, this.readwrite)
      }
    },

    __setReadwrite(options = {}, callbackId, moduleInstId) {
      this.readwrite = options.value
    }
  },
  base
)

export default moduleOwn
