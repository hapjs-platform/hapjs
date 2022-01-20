/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Node from './node'
import DomTag from './tag'

class DomText extends DomTag {
  constructor(text) {
    super(...arguments)
    this._nodeType = Node.NodeType.TEXT
    this._nodeName = '#text'

    this._textContent = text
    this._wholeText = text

    this._layout = false
    this._render = false
  }

  /**
   * 相邻节点的文本连接，与CharacterData中的data相同
   * @return {null}
   */
  get wholeText() {
    return this._wholeText
  }

  appendChild(node) {
    throw new Error(`### App Runtime ### appendChild() 文本节点不支持插入子节点`)
  }

  insertBefore(node, before) {
    throw new Error(`### App Runtime ### insertBefore() 文本节点不支持插入子节点`)
  }
}

export default DomText
