/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { NodeRef, NodeType } from './definition'

/**
 * 生成唯一Id，每个节点具有唯一Id.
 */
let _uid = 1
function _uniqueId() {
  return (_uid++).toString()
}

/**
 * W3C规范基类：Node
 */
class Node {
  constructor() {
    this._nodeType = Node.NodeType.UNKNOWN
    this._nodeName = null
    this._nodeValue = null
    // 节点创建时所属的Document
    this._ownerDocument = null

    this._textContent = null

    this.ref = _uniqueId()

    this.childNodes = []
    this.layoutChildren = []
    this.parentNode = null

    this.nextSibling = null
    this.previousSibling = null

    // 非规范：标识已挂载到document
    this._docId = null
    // 是否支持布局：Element, Figment, Fragment
    this._layout = false
    // 是否支持渲染：Element
    this._render = false
    // 默认未计算
    this._renderCount = null
  }

  get nodeType() {
    return this._nodeType
  }

  get nodeName() {
    return this._nodeName
  }

  get nodeValue() {
    return this._nodeValue
  }

  get ownerDocument() {
    return this._ownerDocument
  }

  get textContent() {
    return this._textContent
  }

  set textContent(str) {
    if (this.nodeType !== Node.NodeType.TEXT) {
      const node = this.ownerDocument.createTextNode(str)
      this.appendChild(node)
    } else {
      this._textContent = str
    }
  }
}

Node.NodeRef = NodeRef
Node.NodeType = NodeType

export default Node
