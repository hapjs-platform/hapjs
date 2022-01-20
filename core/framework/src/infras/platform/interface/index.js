/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import initTextDecoder from './textdecoder'

function initInterface(app) {
  if (ENV_PLATFORM === 'na') {
    initTextDecoder(app)
  }
}

export default initInterface
