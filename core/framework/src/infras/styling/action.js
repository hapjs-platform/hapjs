/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { cleanNewActions, sliceNewActions } from './calls'

import {
  registerFromCssFile,
  getStylingDocumentId,
  setDocument,
  getDocument,
  getDocumentNodeByRef,
  setElementProp,
  setElementAttr,
  setElementStyle,
  setElementStyles
} from './dom/index'

import { createDocument, registerStyleObject, parseNodeJSON } from './helper'

function sendActionsWrap(instId, actionList) {
  // 消息处理
  const retActionList = []

  for (let i = 0, len = actionList.length; i < len; i++) {
    retActionList.push(...dispatchAction(instId, actionList[i]))
  }

  return [instId, retActionList]
}

/**
 * 获取对应的元素节点
 * @param node
 * @return {*}
 */
function getStylingNode(node) {
  const instId = getStylingDocumentId(node._docId)
  const document = getDocument(instId)
  const newNode = getDocumentNodeByRef(document, node.ref)
  return newNode
}

function dispatchAction(instId, actionItem) {
  // 区别于之前的构建
  const newInstId = getStylingDocumentId(instId)

  let newActionList = null
  if (actionItem.module === 'dom') {
    // 节点更新
    switch (actionItem.method) {
      case 'createFinish':
      case 'updateFinish':
        newActionList = [actionItem]
        break
      case 'createBody':
        dispatchActionForCreateBody(newInstId, ...actionItem.args)
        newActionList = sliceNewActions()
        cleanNewActions()
        break
      case 'addElement':
        dispatchActionForAddElement(newInstId, ...actionItem.args)
        newActionList = sliceNewActions()
        cleanNewActions()
        break
      case 'removeElement':
        dispatchActionForRemoveElement(newInstId, ...actionItem.args)
        newActionList = sliceNewActions()
        cleanNewActions()
        break
      case 'moveElement':
        dispatchActionForMoveElement(newInstId, ...actionItem.args)
        newActionList = sliceNewActions()
        cleanNewActions()
        break
      case 'updateProps':
        dispatchActionForUpdateProps(newInstId, ...actionItem.args)
        newActionList = sliceNewActions()
        cleanNewActions()
        break
      case 'updateAttrs':
        dispatchActionForUpdateAttrs(newInstId, ...actionItem.args)
        newActionList = sliceNewActions()
        cleanNewActions()
        break
      case 'updateStyle':
        dispatchActionForUpdateStyle(newInstId, ...actionItem.args)
        newActionList = sliceNewActions()
        cleanNewActions()
        break
      case 'updateStyles':
        dispatchActionForUpdateStyles(newInstId, ...actionItem.args)
        newActionList = sliceNewActions()
        cleanNewActions()
        break
      case 'updateStyleObject':
        dispatchActionForUpdateStyleObject(newInstId, ...actionItem.args)
        newActionList = sliceNewActions()
        cleanNewActions()
        break
      case 'addEvent':
      case 'removeEvent':
      case 'exitFullscreen':
      case 'updateTitleBar':
      case 'updateStatusBar':
      case 'statistics':
      case 'setSecure':
        newActionList = [actionItem]
        break
      default:
        throw new Error(`无法处理的DOM消息：${JSON.stringify(actionItem)}`)
    }
  } else if (actionItem.component) {
    // 节点方法
    newActionList = [actionItem]
  }

  if (!newActionList || newActionList.length === 0) {
    throw new Error(`无法处理的call消息：${JSON.stringify(actionItem)}`)
  }

  return newActionList
}

function dispatchActionForCreateBody(instId, nodeHash) {
  if (!getDocument(instId)) {
    const document = createDocument(instId)
    setDocument(instId, document)
  }
  const document = getDocument(instId)
  const node = parseNodeJSON(document, nodeHash)
  document.documentElement.appendChild(node)
}

function dispatchActionForAddElement(instId, parentNodeRef, nodeHash) {
  const document = getDocument(instId)
  const parentNode = getDocumentNodeByRef(document, parentNodeRef)
  if (parentNode) {
    const node = parseNodeJSON(document, nodeHash)
    parentNode.appendChild(node)
  }
}

function dispatchActionForRemoveElement(instId, nodeRef) {
  const document = getDocument(instId)
  const node = getDocumentNodeByRef(document, nodeRef)
  if (node && node.parentNode) {
    node.parentNode.removeChild(node)
  }
}

function dispatchActionForMoveElement(instId, nodeRef, parentNodeRef, moveIndex) {
  const document = getDocument(instId)
  const node = getDocumentNodeByRef(document, nodeRef)
  const parentNode = getDocumentNodeByRef(document, parentNodeRef)
  if (parentNode) {
    const moveNode = parentNode.childNodes[moveIndex]
    parentNode.insertBefore(node, moveNode)
  }
}

function dispatchActionForUpdateProps(instId, nodeRef, nodeHash) {
  const document = getDocument(instId)
  const node = getDocumentNodeByRef(document, nodeRef)
  for (const k in nodeHash.prop) {
    setElementProp(node, k, nodeHash.prop[k])
  }
}

function dispatchActionForUpdateAttrs(instId, nodeRef, nodeHash) {
  const document = getDocument(instId)
  const node = getDocumentNodeByRef(document, nodeRef)
  for (const k in nodeHash.attr) {
    setElementAttr(node, k, nodeHash.attr[k])
  }
}

function dispatchActionForUpdateStyle(instId, nodeRef, nodeHash) {
  const document = getDocument(instId)
  const node = getDocumentNodeByRef(document, nodeRef)
  for (const k in nodeHash.style) {
    setElementStyle(node, k, nodeHash.style[k])
  }
}

function dispatchActionForUpdateStyles(instId, nodeRef, nodeHash) {
  const document = getDocument(instId)
  const node = getDocumentNodeByRef(document, nodeRef)
  if (nodeHash.attr) {
    for (const k in nodeHash.attr) {
      setElementAttr(node, k, nodeHash.attr[k])
    }
  }
  if (nodeHash.style) {
    setElementStyles(node, nodeHash.style)
  }
}

function dispatchActionForUpdateStyleObject(
  instId,
  nodeRef,
  isDocLevel,
  name,
  styleObjectId,
  styleObject
) {
  if (!getDocument(instId) && isDocLevel) {
    const document = createDocument(instId)
    setDocument(instId, document)
  }
  const document = getDocument(instId)
  const node = getDocumentNodeByRef(document, nodeRef)
  registerStyleObject(name, styleObjectId, styleObject, isDocLevel, node, true)
}

export { sendActionsWrap, getStylingNode, registerFromCssFile, registerStyleObject }
