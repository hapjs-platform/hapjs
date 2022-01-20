/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import base from './base'

export const config = {
  name: 'system.prompt',
  methods: [
    {
      name: 'showToast',
      type: 0,
      mode: 0
    },
    {
      name: 'showDialog',
      type: 0,
      mode: 1
    },
    {
      name: 'showContextMenu',
      type: 0,
      mode: 0
    }
  ]
}

const moduleOwn = Object.assign(
  {
    name: 'system.prompt',

    showToast(options = {}) {
      console.log('system.prompt.showToast: ' + options.message)
    },

    showDialog(options = {}) {
      return this.mockSync(options._data, options._code)
    },
    showContextMenu(options = {}) {
      return this.mockSync(options._data, options._code)
    }
  },
  base
)

export default moduleOwn
