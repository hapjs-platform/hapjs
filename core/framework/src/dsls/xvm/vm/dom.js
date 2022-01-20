/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import context from '../context'

/**
 * 创建指定类型的Element
 * @param vm
 * @param type
 * @returns {*|Object|Element}
 */
function createElement(vm, type) {
  const doc = vm._page.doc
  return doc.createElement(type)
}

/**
 * 创建逻辑虚拟节点
 * @param vm
 * @param element
 * @returns {Object}
 */
function createFragment(vm, element) {
  const doc = vm._page.doc
  const node = context.quickapp.runtime.helper.createFigment(doc)
  element.appendChild(node)
  return node
}

/**
 * 节点是否在文档中
 * @param node
 */
function isNodeInDocument(node) {
  return context.quickapp.runtime.helper.isNodeInDocument(node)
}

/**
 * 添加节点
 * @param parent
 * @param child
 */
function nodeAppendChild(parent, child) {
  return parent.appendChild(child)
}

/**
 * 从Dom中移除节点
 * @param vm
 * @param node 待移除的节点
 * @param preserved 是否保留（fragment）
 */
function removeNode(node, preserved = false) {
  const parent = node.parentNode

  if (parent) {
    if (preserved) {
      const childNodeList = (node.childNodes || []).slice()
      for (let i = 0, len = childNodeList.length; i < len; i++) {
        node.removeChild(childNodeList[i])
      }
    } else {
      parent.removeChild(node)
    }
  }
}

/**
 * 绑定事件
 * @param node
 * @param args
 */
function nodeAddEventListener(node, ...args) {
  node.addEventListener(...args)
}

/**
 * 触发事件
 */
function fireEventWrap(el, evtName, evtHash, domChanges) {
  if (!el) {
    return
  }

  const evt = context.quickapp.runtime.helper.createEvent(evtName)
  Object.assign(evt, evtHash)
  console.trace(
    `### App Framework ### fireEventWrap():事件(${evtName})的参数：${evtHash &&
      JSON.stringify(evtHash)}, ${domChanges && JSON.stringify(domChanges)}`
  )

  // 如果修改dom
  if (domChanges) {
    const attr = domChanges.attr || {}
    for (const name in attr) {
      context.quickapp.runtime.helper.setElementAttr(el, name, attr[name], true)
    }
    const style = domChanges.style || {}
    for (const name in style) {
      context.quickapp.runtime.helper.setElementStyle(el, name, style[name], true)
    }
  }

  return el.dispatchEvent(evt)
}

/**
 * 获取Element
 * @param document
 * @param ref
 */
function getNodeByRef(document, ref) {
  return context.quickapp.runtime.helper.getDocumentNodeByRef(document, ref)
}

/**
 * 更新节点的属性，比如：attribute, style
 * @param node
 * @param name
 * @param args
 */
function updateNodeProperties(node, name, ...args) {
  switch (name) {
    case 'prop':
      context.quickapp.runtime.helper.setElementProp(node, ...args)
      break
    case 'attr':
      context.quickapp.runtime.helper.setElementAttr(node, ...args)
      break
    case 'style':
      context.quickapp.runtime.helper.setElementStyle(node, ...args)
      break
    case 'styles':
      // 直接一次性更新style属性
      context.quickapp.runtime.helper.setElementStyles(node, ...args)
      break
    default:
      console.warn(`### App Framework ### updateNodeProperties() 未知的更新项：${name}`)
  }
}

/**
 * 获取节点在DOM树中的层级深度
 * @param node
 * @return {null|*|number}
 */
function getNodeDepth(node) {
  return context.quickapp.runtime.helper.getNodeDepth(node) || 1
}

export {
  createElement,
  createFragment,
  isNodeInDocument,
  nodeAppendChild,
  removeNode,
  nodeAddEventListener,
  fireEventWrap,
  getNodeByRef,
  updateNodeProperties,
  getNodeDepth
}
