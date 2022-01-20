/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Node from './node'
import EventTarget from './target'
import {
  getListener,
  removeNodeInDocument,
  setParent,
  insertIndex,
  moveIndex,
  removeIndex
} from '../model'
import {
  getNodeAsJSON,
  supportLayout,
  supportRender,
  renderParent,
  renderIndexInRenderParent,
  renderIndexInParent,
  resetRenderCount
} from '../misc'

/**
 * 插入到文档中时的操作
 * @param node
 * @private
 */
export function applyWhenInDocument(node) {
  // 样式计算
  if (node.nodeType === Node.NodeType.ELEMENT && node._docId) {
    // 递归
    for (let i = 0, len = node.layoutChildren.length; i < len; i++) {
      applyWhenInDocument(node.layoutChildren[i])
    }
  }
}

function nodeInsertBefore(parentNode, node, before) {
  // 处理Fragment节点
  if (node.nodeType === Node.NodeType.DOCUMENT_FRAGMENT) {
    // 添加子节点
    const childNodeList = node.childNodes.slice()
    for (let i = 0, len = childNodeList.length; i < len; i++) {
      node.removeChild(childNodeList[i])
      nodeInsertBefore(parentNode, childNodeList[i], before)
    }
    // 清空当前节点
    return node
  }

  // 渲染父节点
  const renderParentNode = renderParent(parentNode, true)

  const childIndex = !before ? parentNode.childNodes.length : parentNode.childNodes.indexOf(before)
  const listener = getListener(parentNode._docId)

  // 处理文本节点
  if (node.nodeType === Node.NodeType.TEXT) {
    throw new Error(`### App Runtime ### 不支持在非渲染节点中添加文本节点：${node.textContent}`)
  }

  if (!node.parentNode) {
    // 新增节点
    setParent(node, parentNode)
    insertIndex(node, parentNode.childNodes, childIndex, true)

    applyWhenInDocument(node)

    if (supportLayout(node)) {
      if (!before) {
        // appendChild操作
        insertIndex(node, parentNode.layoutChildren, parentNode.layoutChildren.length)
        resetRenderCount(parentNode)
        const nodeRenderIndex =
          parentNode === renderParentNode ? -1 : renderIndexInRenderParent(node)
        if (listener) {
          return listener.addNode(node, renderParentNode, nodeRenderIndex)
        }
      } else {
        // insertBefore操作
        insertIndex(node, parentNode.layoutChildren, parentNode.layoutChildren.indexOf(before))
        resetRenderCount(parentNode)
        const nodeRenderIndex = renderIndexInRenderParent(node)
        if (listener) {
          return listener.addNode(node, renderParentNode, nodeRenderIndex)
        }
      }
    }
  } else {
    // 移动节点
    moveIndex(node, parentNode.childNodes, childIndex, true)

    if (supportLayout(node)) {
      if (!before) {
        const index = moveIndex(node, parentNode.layoutChildren, parentNode.layoutChildren.length)
        resetRenderCount(parentNode)
        const nodeRenderIndex = renderIndexInRenderParent(node)
        if (listener && index >= 0) {
          return listener.moveNode(node, renderParentNode, nodeRenderIndex)
        }
      } else {
        const index = moveIndex(
          node,
          parentNode.layoutChildren,
          parentNode.layoutChildren.indexOf(before)
        )
        resetRenderCount(parentNode)
        const nodeRenderIndex = renderIndexInRenderParent(node)
        if (listener && index >= 0) {
          return listener.moveNode(node, renderParentNode, nodeRenderIndex)
        }
      }
    }
  }
}

/**
 * 清空孩子节点，记录关键信息，待重新插入
 * @param parent
 */
export function resetNodeChildren(parent) {
  for (let i = 0, len = parent.layoutChildren.length; i < len; i++) {
    const childNode = parent.layoutChildren[i]
    childNode._tmpRenderIndexInParent = renderIndexInParent(childNode, parent)
  }

  parent.childNodes.length = 0
  parent.layoutChildren.length = 0
}

/**
 * 重新插入
 * @param parent
 * @param node
 */
export function restoreNodeChildren(parent, node) {
  // 新插入节点的父节点必须是当前节点
  if (!node.parentNode || node.parentNode !== parent) {
    return
  }

  parent.childNodes.push(node)
  parent.layoutChildren.push(node)

  // 实际父节点
  const renderParentNode = renderParent(parent, true)

  if (supportLayout(node)) {
    const listener = getListener(parent._docId)
    const nodeRenderIndex = renderIndexInRenderParent(node)
    const needMove = nodeRenderIndex !== node._tmpRenderIndexInParent

    node._tmpRenderIndexInParent = null
    if (listener && needMove) {
      return listener.moveNode(node, renderParentNode, nodeRenderIndex)
    }
  }
}

/**
 * DOM节点基类
 */
class DomTag extends EventTarget {
  constructor() {
    super(...arguments)
    this._depth = null
    // 临时的记录
    this._tmpRenderIndexInParent = null
  }

  appendChild(node) {
    if (!node || !(node instanceof Node)) {
      throw new Error(`### App Runtime ### appendChild() 函数的node参数无效`)
    }

    if (node.parentNode && node.parentNode !== this) {
      throw new Error(`### App Runtime ### appendChild() 参数node的父节点不匹配`)
    }

    return nodeInsertBefore(this, node, null)
  }

  insertBefore(node, before) {
    if (!node || arguments.length !== 2 || !(node instanceof Node)) {
      throw new Error(`### App Runtime ### insertBefore() 函数的node/before参数无效`)
    }

    if (node.parentNode && node.parentNode !== this) {
      throw new Error(`### App Runtime ### insertBefore() 参数node的父节点不匹配`)
    }

    // 检查是否需要移动
    if (node === before || (node.nextSibling && node.nextSibling === before)) {
      return node
    }

    return nodeInsertBefore(this, node, before)
  }

  removeChild(node) {
    if (!node || !(node instanceof Node)) {
      throw new Error(`### App Runtime ### removeChild() node参数无效`)
    }
    if (node.parentNode !== this) {
      throw new Error(`### App Runtime ### removeChild() 参数node的父节点不匹配`)
    }
    console.trace(
      `### App Runtime ### removeChild() 移除节点：${JSON.stringify(
        getNodeAsJSON(node, false, false, false)
      )}`
    )

    resetRenderCount(node)
    node.parentNode = null

    if (supportLayout(node)) {
      if (supportRender(node)) {
        const listener = getListener(node._docId)
        if (listener) {
          listener.removeElement(node.ref)
        }
      } else {
        const list = node.childNodes.slice()
        for (let i = 0; i < list.length; i++) {
          node.removeChild(list[i])
        }
      }
      removeIndex(node, this.layoutChildren)
    }
    removeIndex(node, this.childNodes, true)

    // 文档中移除
    removeNodeInDocument(node, node._docId)

    return node
  }

  toJSON() {
    return getNodeAsJSON(this, true, false, false)
  }
}

export default DomTag
