/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import NodeView from './node'
import ElementView from './element'

import { installCssFiles, setDocument, defineNodeInDocument } from '../model'

function createDocumentElement(doc) {
  const node = new ElementView('html')
  node.ref = NodeView.NodeRef.HTML_ID
  node._depth = 0
  node._ownerDocument = doc

  defineNodeInDocument(node, doc._docId)
  node.parentNode = doc
  doc.childNodes.push(node)

  return node
}

class DocumentView extends ElementView {
  constructor(docId, listener) {
    super('#document')
    this._nodeType = NodeView.NodeType.DOCUMENT
    this._nodeName = '#document'

    this.body = null

    docId = docId ? docId.toString() : ''
    // 文档实例id
    this._docId = docId
    // 所有节点映射表，key值为node.ref
    this._nodeMap = {}
    // 监听DOM操作并转换为消息
    this.listener = listener
    // 样式表
    this._styleSheetHash = {}
    // 公共样式表
    this._styleSheetHash[0] = []
    // 添加到文档列表中
    setDocument(this._docId, this)
    // 创建HTML节点
    Object.defineProperty(this, 'documentElement', {
      configurable: true,
      enumerable: false,
      writable: false,
      value: createDocumentElement(this)
    })

    installCssFiles(this.documentElement)
  }

  /**
   * 创建子元素
   * @param name
   * @return {ElementView}
   */
  createElement(name) {
    const node = new ElementView(name)
    node._ownerDocument = this
    return node
  }
}

export default DocumentView
