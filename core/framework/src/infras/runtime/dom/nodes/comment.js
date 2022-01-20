/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Node from './node'
import DomTag from './tag'

/**
 * 注释节点：可布局但不可渲染的节点
 */
class DomComment extends DomTag {
  constructor(data) {
    super(...arguments)
    this._nodeType = Node.NodeType.COMMENT
    this._nodeName = '#comment'
    this._nodeValue = data

    this._layout = true
    this._render = false

    this._data = data
  }

  get data() {
    return this._data
  }

  set data(str) {
    const final = '' + str
    this._nodeValue = final
    this._data = final
    return this._data
  }

  appendChild(node) {
    throw new Error(`### App Runtime ### appendChild() 注释节点不支持插入子节点`)
  }

  insertBefore(node, before) {
    throw new Error(`### App Runtime ### insertBefore() 注释节点不支持插入子节点`)
  }

  /**
   * 转换为HTML注释.
   * @return {string} html
   */
  toString() {
    return '<!-- ' + this._data + ' -->'
  }
}

export default DomComment
