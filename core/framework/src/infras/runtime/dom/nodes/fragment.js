/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Node from './node'
import DomTag from './tag'

/**
 * DOM片段
 */
class DomFragment extends DomTag {
  constructor() {
    super(...arguments)
    this._nodeType = Node.NodeType.DOCUMENT_FRAGMENT
    this._nodeName = '#document-fragment'

    this._layout = true
    this._render = false
  }
}

export default DomFragment
