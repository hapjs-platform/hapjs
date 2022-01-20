/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Node from './node'
import DomTag from './tag'
import DomElement from './element'
import DomFragment from './fragment'
import DomFigment from './figment'
import DomText from './text'
import DomHtmlElement from './html'
import DomComment from './comment'
import DomFormerElement from './former'
import { createEvent } from './event/index'
import { setDocument, defineNodeInDocument, getListener } from '../model'

/**
 * 创建HTML节点
 * @param doc
 * @return {DomHtmlElement}
 */
function createDocumentElement(doc) {
  const node = new DomHtmlElement('html')
  node._ownerDocument = doc

  defineNodeInDocument(node, doc._docId)
  node.parentNode = doc
  doc.childNodes.push(node)
  doc.layoutChildren.push(node)

  return node
}

/**
 * 创建逻辑的虚拟节点
 * @param document
 * @return {DomFigment}
 */
export function createFigment(document) {
  const node = new DomFigment()
  node._ownerDocument = document
  return node
}

/**
 * 更新文档的标题栏
 * @param document
 * @param attr { object }
 */
export function updatePageTitleBar(document, attr) {
  const listener = getListener(document._docId)
  if (listener) {
    listener.updatePageTitleBar(attr)
  }
}

/**
 * 更新文档的状态栏
 * @param document
 * @param attr { object }
 */
export function updatePageStatusBar(document, attr) {
  const listener = getListener(document._docId)
  if (listener) {
    listener.updatePageStatusBar(attr)
  }
}

/**
 * 设置页面元信息
 * @param document
 * @param attr { object }
 */
export function setMeta(document, attr) {
  const listener = getListener(document._docId)
  if (listener) {
    listener.setMeta(attr)
  }
}

/**
 * 页面滚动至指定位置
 * @param document
 * @param attr { object }
 */
export function scrollTo(document, attr) {
  const listener = getListener(document._docId)
  if (listener) {
    listener.scrollTo(attr)
  }
}

/**
 * 页面按指定的偏移量滚动文档
 * @param document
 * @param attr { object }
 */
export function scrollBy(document, attr) {
  const listener = getListener(document._docId)
  if (listener) {
    listener.scrollBy(attr)
  }
}

/**
 * 退出全屏模式
 * @param document
 */
export function exitFullscreen(document) {
  const listener = getListener(document._docId)
  if (listener) {
    listener.exitFullscreen()
  }
}

/**
 * 隐藏骨架屏
 * @param document
 */
export function hideSkeleton(document, pageId) {
  const listener = getListener(document._docId)
  if (listener) {
    listener.hideSkeleton(pageId)
  }
}

/**
 * 调用页面的方法
 */
export function callHostFunction(document, name, args, extra) {
  const listener = getListener(document._docId)
  if (listener) {
    listener.callHostFunction(name, args, extra)
  }
}

/**
 * 文档节点
 */
class DomDocument extends DomTag {
  /**
   * 构造函数
   * @param docId
   * @param listener {Listener} 监听DOM操作的类实例
   */
  constructor(docId, listener) {
    super(...arguments)
    this._nodeType = Node.NodeType.DOCUMENT
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
    // 样式映射缓存
    this._styleObjectMap = new Map()
    // 添加到文档列表中
    setDocument(this._docId, this)
    // 创建HTML节点
    Object.defineProperty(this, 'documentElement', {
      configurable: true,
      enumerable: false,
      writable: false,
      value: createDocumentElement(this)
    })
  }

  /**
   * 创建子元素
   * @param name
   * @return {DomElement}
   */
  createElement(name) {
    let node
    // 目前暂时支持input标签
    if (name === 'input' || name === 'textarea') {
      node = new DomFormerElement(name)
    } else {
      node = new DomElement(name)
    }
    node._ownerDocument = this
    return node
  }

  /**
   * 创建片段
   * @return {DomFragment}
   */
  createDocumentFragment() {
    const node = new DomFragment()
    node._ownerDocument = this
    return node
  }

  /**
   * 创建文本节点
   * @return {DomText}
   */
  createTextNode(text) {
    const node = new DomText(text)
    node._ownerDocument = this
    return node
  }

  /**
   * 创建注释
   * @param text
   * @return {DomComment}
   */
  createComment(text) {
    const node = new DomComment(text)
    node._ownerDocument = this
    return node
  }

  /**
   * 创建事件
   * @param {string} evtName 事件名
   * @param {object} options 配置
   */
  createEvent(evtName, options) {
    return createEvent(evtName, options)
  }
}

export default DomDocument
