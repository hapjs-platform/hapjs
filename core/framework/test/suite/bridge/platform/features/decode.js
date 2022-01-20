/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import base from './base'

export const config = {
  name: 'system.decode',
  methods: [
    // Method
    {
      name: 'decode',
      type: 0,
      mode: 0
    }
  ]
}

const moduleOwn = Object.assign(
  {
    name: 'system.decode',

    decode(options = {}) {}
  },
  base
)

export default moduleOwn
