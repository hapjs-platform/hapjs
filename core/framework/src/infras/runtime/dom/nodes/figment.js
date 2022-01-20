/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Node from './node'
import DomTag from './tag'

/**
 * 可布局但不可渲染的节点；用于if, for等逻辑，不对外开放
 */
class DomFigment extends DomTag {
  constructor() {
    super(...arguments)
    this._nodeType = Node.NodeType.FIGMENT
    this._nodeName = '#figment'

    this._layout = true
    this._render = false
  }

  /**
   * 转换为HTML.
   * @return {stirng} html
   */
  toString() {
    let str = ''
    if (this.childNodes.length) {
      str = this.childNodes.map(child => child.toString()).join('')
    }
    return str
  }
}

export default DomFigment
