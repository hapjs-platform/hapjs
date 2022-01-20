/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import base from './base'

export const config = {
  name: 'system.websocket',
  methods: [
    // Method
    {
      name: 'methodInstance',
      type: 0,
      mode: 0
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
    name: 'system.websocket',
    readwrite: undefined,
    handlerCustomEvent1: null,

    methodInstance(options = {}, callbackId, moduleInstId) {
      return this.mockSync(options._data, options._code)
    },

    __getReadwrite(options = {}, callbackId, moduleInstId) {
      return {
        content: this.readwrite
      }
    },

    __setReadwrite(options = {}, callbackId, moduleInstId) {
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
