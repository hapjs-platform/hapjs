/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Node from './node'
import DomElement from './element'
import { applyWhenInDocument } from './tag'
import { supportLayout, resetRenderCount } from '../misc'
import { getDocument, defineNodeInDocument, insertIndex, getListener, setParent } from '../model'

function nodeInsertBefore(parentNode, node, before) {
  // 如果body节点已经有父节点，则无法添加
  if (parentNode.layoutChildren.length > 0 || node.parentNode) {
    console.warn(`### App Runtime ### Document添加多个Body节点----`)
    return
  }

  const document = getDocument(parentNode._docId)
  const childNodeList = parentNode.childNodes
  const beforeIndex = childNodeList.indexOf(before)
  if (beforeIndex < 0) {
    childNodeList.push(node)
  } else {
    childNodeList.splice(beforeIndex, 0, node)
  }

  if (supportLayout(node)) {
    // 清理更新
    const docId = node._docId
    defineNodeInDocument(node, docId)
    // 向下传递doc信息
    setParent(node, parentNode)

    insertIndex(node, parentNode.layoutChildren, parentNode.layoutChildren.length)
    resetRenderCount(parentNode)

    applyWhenInDocument(node)

    // 设置body
    document.body = node

    const listener = getListener(parentNode._docId)
    listener.createBody(node)
  } else {
    // 不支持布局，则不会向原生端发送通知
    setParent(node, parentNode)
  }
}

class DomHtmlElement extends DomElement {
  constructor() {
    super(...arguments)
    this.ref = Node.NodeRef.HTML_ID
    this._depth = 0
  }

  appendChild(node) {
    return nodeInsertBefore(this, node)
  }

  insertBefore(node, before) {
    console.warn(`### App Runtime ### 暂不支持nodeHtml.insertBefore()`)
  }
}

export default DomHtmlElement
