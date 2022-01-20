/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import base from './base'

export const config = {
  name: 'system.worker',
  methods: [
    // Method
    {
      name: 'createWorker',
      type: 0,
      mode: 0
    },
    {
      name: 'postMessage',
      type: 0,
      mode: 0
    },
    {
      name: 'terminate',
      type: 0,
      mode: 0
    },
    // Event
    {
      name: '__onmessage',
      mode: 1,
      type: 2,
      alias: 'onmessage'
    },
    {
      name: '__onmessageerror',
      mode: 1,
      type: 2,
      alias: 'onmessageerror'
    }
  ]
}

const moduleOwn = Object.assign(
  {
    name: 'system.worker',

    handlermessage: null,
    handlermessageerror: null,

    /**
     * @param options
     * @param options.path 路径名称
     * @return {*}
     */
    createWorker(options = {}) {
      throw new Error(`createWorker() 待实现!`)
    },

    postMessage(message) {
      throw new Error(`postMessage() 待实现!`)
    },

    terminate() {
      throw new Error(`terminate() 待实现!`)
    },

    __onmessage(options, callbackId) {
      if (callbackId <= 0) {
        this.clearTimeout(this.handlermessage)
        this.handlermessage = null
      } else {
        this.handlermessage = this.setTimeout(callbackId, 'onmessage')
      }
    },

    __onmessageerror(options, callbackId) {
      if (callbackId <= 0) {
        this.clearTimeout(this.handlermessageerror)
        this.handlermessageerror = null
      } else {
        this.handlermessageerror = this.setTimeout(callbackId, 'onmessageerror')
      }
    }
  },
  base
)

export default moduleOwn
