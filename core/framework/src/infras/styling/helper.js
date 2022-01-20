/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import {
  DocumentView,
  getDocument,
  compileStyleObject,
  getListener,
  getDocumentNodeByRef,
  defineNodeStyleSheet,
  setElementProp,
  setElementAttr,
  setElementStyles,
  getElementMatchedCssRuleList,
  setElementMatchedCssRule
} from './dom/index'

import { saveNewActions } from './calls'

import Streamer from '../runtime/streamer'
import Listener from './listener'

const createDocument = function(docId) {
  const streamer = new Streamer(saveNewActions, 1)
  const listener = new Listener(docId, streamer)
  const document = new DocumentView(docId, listener)
  return document
}

function registerStyleObject(
  styleSheetName,
  styleObjectId,
  styleObject,
  isDocLevel,
  node,
  callListener
) {
  console.trace(`### App Runtime ### 基于节点(${node && node.ref})注册样式节点(${isDocLevel})`)
  if (!node._ownerDocument) {
    return
  }

  if (!isDocLevel) {
    node._styleObject = styleObject
    node._styleObjectId = styleObjectId
  }

  if (styleObject) {
    const document = node._ownerDocument
    const styleSheetInst = compileStyleObject(styleSheetName, styleObject)
    defineNodeStyleSheet(document, styleObjectId, styleSheetInst, isDocLevel, node)
  }

  if (callListener) {
    const listener = getListener(node._docId)
    if (listener) {
      listener.setStyleObject(node.ref, isDocLevel, styleSheetName, styleObjectId, styleObject)
    }
  }
}

function parseNodeJSON(document, nodeHash) {
  // 创建
  const node = document.createElement(nodeHash.type)
  node.ref = nodeHash.ref

  // 内置属性
  const propKeys = Object.keys(nodeHash.prop || {})
  for (let i = 0, len = propKeys.length; i < len; i++) {
    setElementProp(node, propKeys[i], nodeHash.prop[propKeys[i]])
  }

  // 样式对象
  if (nodeHash.styleObject) {
    registerStyleObject('', node._styleObjectId, nodeHash.styleObject, false, node, true)
  }

  // 属性
  const attrKeys = Object.keys(nodeHash.attr || {})
  for (let i = 0, len = attrKeys.length; i < len; i++) {
    setElementAttr(node, attrKeys[i], nodeHash.attr[attrKeys[i]])
  }

  // 内联样式
  if (nodeHash.inlineStyle) {
    setElementStyles(node, nodeHash.inlineStyle)
  }

  // 合并后的样式：JS线程不再合并

  // 事件
  if (nodeHash.event) {
    for (let i = 0, len = nodeHash.event.length; i < len; i++) {
      node._eventTargetListeners[nodeHash.event[i]] = ''
    }
  }

  // 子节点
  if (nodeHash.children) {
    for (let i = 0, len = nodeHash.children.length; i < len; i++) {
      const childNode = parseNodeJSON(document, nodeHash.children[i])
      node.appendChild(childNode)
    }
  }

  return node
}

export {
  getDocument,
  getDocumentNodeByRef,
  setElementProp,
  setElementAttr,
  setElementStyles,
  getElementMatchedCssRuleList,
  setElementMatchedCssRule,
  createDocument,
  registerStyleObject,
  parseNodeJSON
}
