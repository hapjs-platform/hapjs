/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Transfer from './transfer.js'
import validateStyle from './style'

/**
 * 编译器入口
 * @param data
 * @param type
 * @returns {{node: *, nodes: Array, images: Array, imageUrls: Array}}
 */
function compile(data, type) {
  data = data.trim()
  if (data === '') {
    data = '<div></div>'
  }
  return Transfer.compile(data, type)
}

export default {
  compile,
  validateStyle,
  parseHTML: Transfer.parseHTML
}
