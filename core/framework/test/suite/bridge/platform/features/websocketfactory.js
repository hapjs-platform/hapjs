/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import base from './base'

export const config = {
  name: 'system.websocketfactory',
  methods: [
    // Method
    {
      name: 'create',
      type: 0,
      mode: 0
    }
  ]
}

let instId = 0

const moduleOwn = Object.assign(
  {
    name: 'system.websocketfactory',

    create(options = {}) {
      options._data = options._data || {}
      options._data.instId = ++instId
      options._data.name = 'system.websocket'
      options._data._nativeType = 0

      return this.mockSync(options._data, options._code)
    }
  },
  base
)

export default moduleOwn
