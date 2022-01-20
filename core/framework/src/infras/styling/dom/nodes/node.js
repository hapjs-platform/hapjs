/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { NodeRef, NodeType } from '../../dom/nodes/definition'

class NodeView {
  constructor() {
    this._nodeType = NodeView.NodeType.UNKNOWN
    this._nodeName = null
    // 节点创建时所属的Document
    this._ownerDocument = null
    // 区别于运行时
    this._real = true

    this.ref = null
    this._docId = null
    this._depth = null

    this.parentNode = null
    this.childNodes = []

    this.nextSibling = null
    this.previousSibling = null
  }

  get nodeType() {
    return this._nodeType
  }

  get nodeName() {
    return this._nodeName
  }

  get ownerDocument() {
    return this._ownerDocument
  }
}

NodeView.NodeRef = NodeRef
NodeView.NodeType = NodeType

export default NodeView
