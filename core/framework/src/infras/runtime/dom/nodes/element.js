/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { isObject } from 'src/shared/util'

import Node from './node'
import DomTag from './tag'
import { setParent, getListener } from '../model'
import {
  getNodeAttributesAsObject,
  getNodeInlineStyleAsObject,
  setNodeInlineStyle,
  cssText2StyleObject
} from '../misc'
import { REG_IS_DATA_ATTR, $dataAttr, $hyphenateStyle } from '../util'

/**
 * 设置元素属性值，判断是否发送给Native.
 * @param node {Element}
 * @param key {string}
 * @param value {string | number}
 * @param silent {boolean}
 */
export function setElementProp(node, key, value, silent) {
  if (!node._ownerDocument || key === 'ref') {
    return
  }

  if (node[key] === value && silent !== false) {
    return
  }

  const oldValue = node[key]
  node[key] = value

  console.trace(`### App Runtime ### 元素的内部属性(${key})更新(${oldValue}=>${value})`)
  const listener = getListener(node._docId)
  if (listener) {
    listener.setProp(node.ref, key, value)
  }
}

/**
 * 设置属性值，判断是否发送给Native.
 * @param node {Element}
 * @param key {string}
 * @param value {string | number}
 * @param silent {boolean}
 */
export function setElementAttr(node, key, value, silent) {
  if (!node._ownerDocument || key === 'ref') {
    return
  }

  if (!isObject(value) && getNodeAttributesAsObject(node)[key] === value && silent !== false) {
    return
  }

  if (key === 'class') {
    // 清空缓存
    node._classList = null
  } else if (REG_IS_DATA_ATTR.test(key)) {
    const dataKey = $dataAttr(key)
    node._dataset[dataKey] = value
  }

  // 更新属性
  const oldValue = getNodeAttributesAsObject(node)[key]
  getNodeAttributesAsObject(node)[key] = value
  console.trace(`### App Runtime ### 元素的属性(${key})更新(${oldValue}=>${value})`)
  // 通知属性
  calcAndTellListener(node, silent, key, value)
}

/**
 * 设置样式值，判断是否发送给Native.
 * @param key {string}
 * @param value {string | number}
 * @param silent {boolean}
 */
export function setElementStyle(node, key, value, silent) {
  if (!node._ownerDocument) {
    return
  }

  if (getNodeInlineStyleAsObject(node)[key] === value && silent !== false) {
    return
  }
  getNodeInlineStyleAsObject(node)[key] = value
  calcAndTellListener(node, silent, 'style', null, [key, value])
}

/**
 * 设置样式值，判断是否发送给Native.
 * @param key
 * @param value
 * @param silent
 */
export function setElementStyles(node, value, silent) {
  if (!node._ownerDocument) {
    return
  }

  value = value || ''

  if (getNodeInlineStyleAsObject(node) === value && silent !== false) {
    return
  }

  const styleObj = typeof value === 'string' ? cssText2StyleObject(value) : value
  setNodeInlineStyle(node, styleObj)
  calcAndTellListener(node, silent, 'style')
}

/**
 * 计算并更新节点
 * @param node
 * @param silent
 * @param name
 * @param value
 * @param extra {Array} 更新单个style时的数据
 */
function calcAndTellListener(node, silent, name, value, extra) {
  // 如果Document有Listener，则向Listener发送更新通知
  const listener = getListener(node._docId)

  if (!listener) {
    return
  }

  if (name === 'class') {
    // 更新样式
    !silent && listener.setStyles(node.ref, null, { class: value })
  } else if (name === 'id') {
    // 更新样式
    !silent && listener.setStyles(node.ref, null, { id: value })
  } else if (name === 'style') {
    // 更新样式
    if (extra) {
      // 设置单个样式
      !silent && listener.setStyle(node.ref, ...extra)
    } else {
      // 设置整个style
      !silent && listener.setStyles(node.ref, getNodeInlineStyleAsObject(node))
    }
  } else {
    // 更新属性
    !silent && listener.setAttr(node.ref, name, value)
  }
}

/**
 * 调试器：获取元素匹配的样式规则列表
 * 因为有禁用的顺序恢复，同时复制一份，不修改原数据
 * @param node
 * @param ruleName 不传递时获取所有
 */
export function getElementMatchedCssRuleList(node, ruleName) {
  const cssStyleInline = getNodeInlineStyleAsObject(node)
  const cssRuleInline = {
    editable: true,
    styleSheetName: null,
    style: $hyphenateStyle(node._inlineStyleFullList, cssStyleInline)
  }
  const cssRuleList = [cssRuleInline]
  const cssRuleCopy = JSON.parse(JSON.stringify(cssRuleList))

  console.warn(`### App Runtime ### 获取样式:该平台版本仅支持获取节点${node.ref}的内联样式`)
  return cssRuleCopy
}

/**
 * 调试器：更新元素匹配到的样式规则
 * @param node
 * @param ruleName
 * @param editCssPropertyList
 */
export function setElementMatchedCssRule(node, ruleName, editCssPropertyList) {
  // 提取禁用的样式
  const styleHashNew = {}
  const styleListAll = []
  for (let i = 0, len = editCssPropertyList.length; i < len; i++) {
    const editItem = editCssPropertyList[i]

    styleListAll.push({
      name: editItem.name,
      value: editItem.value,
      disabled: editItem.disabled
    })

    if (!editItem.disabled) {
      styleHashNew[editItem.name] = editItem.value
    }
  }

  if (ruleName === 'INLINE') {
    setElementStyles(node, styleHashNew)
    node._inlineStyleFullList = styleListAll
    console.trace(
      `### App Runtime ### 更新样式:元素(${node.ref})的内联样式：${ruleName}:${JSON.stringify(
        styleHashNew
      )}`
    )
  } else {
    console.warn(`### App Runtime ### 更新样式:该平台版本仅支持更新内联样式，不支持：${ruleName}`)
  }
}
const logOnceSet = new Set()
/**
 * UI节点
 */
class DomElement extends DomTag {
  constructor(name) {
    super(...arguments)
    this._nodeType = Node.NodeType.ELEMENT
    this._nodeName = name.toUpperCase()

    this._tagName = name.toUpperCase()

    this._type = name
    this._attr = {}
    this._style = {}
    this._dataset = {}

    this._layout = true
    this._render = true
    // 缓存类样式
    this._classList = null
    // 作为自定义组件的root而关联的样式对象
    this._styleObject = null
    // 关联的样式
    this._styleObjectId = null
    // 是否使用父节点的样式
    this._useParentStyle = null
    // 缓存：使用的CSS属性
    this._usedCSSPropertyCache = {}
    // 缓存：匹配的CSS规则：key为CSS规则类型，value为匹配数组
    this._matchedCssRuleCache = {}
    // 缓存：合并后的样式，计算并合并上面
    this._mergedStyleCache = null
    // 缓存：自身匹配的CSS规则：key为规则ID
    this._ownCssRuleCache = {}
  }

  get style() {
    return this._style
  }

  get type() {
    if (!logOnceSet.has('type')) {
      console.warn(`### App Runtime ### type属性将被废弃，不推荐使用`)
      logOnceSet.add('type')
    }
    return this._type
  }

  get id() {
    if (!logOnceSet.has('id')) {
      console.warn(`### App Runtime ### id属性将被废弃，不推荐使用`)
      logOnceSet.add('id')
    }
    const attr = getNodeAttributesAsObject(this)
    return attr && attr.id
  }

  get dataset() {
    return this._dataset
  }

  get attr() {
    if (!logOnceSet.has('attr')) {
      console.warn(`### App Runtime ### attr属性将被废弃，不推荐使用`)
      logOnceSet.add('attr')
    }
    return getNodeAttributesAsObject(this)
  }

  get tagName() {
    return this._tagName
  }

  get mergedStyle() {
    return null
  }

  appendChild(node) {
    // 处理文本节点
    if (node.nodeType === Node.NodeType.TEXT) {
      setElementAttr(this, 'value', node.textContent)
      setParent(node, this)
      return node
    }
    return super.appendChild(node)
  }

  insertBefore(node, before) {
    // 处理文本节点
    if (node.nodeType === Node.NodeType.TEXT) {
      setElementAttr(this, 'value', node.textContent)
      setParent(node, this)
      return node
    }
    return super.insertBefore(node, before)
  }

  hasAttribute(name) {
    const obj = getNodeAttributesAsObject(this)
    return obj && obj.hasOwnProperty(name)
  }

  /**
   * 转换为HTML元素.
   * @return {string}
   */
  toString() {
    // 属性转换
    const attr = getNodeAttributesAsObject(this)
    const style = getNodeInlineStyleAsObject(this)

    return (
      '<' +
      this._type +
      ' attr=' +
      JSON.stringify(attr) +
      ' style=' +
      JSON.stringify(style) +
      '>' +
      this.layoutChildren.map(child => child.toString()).join('') +
      '</' +
      this._type +
      '>'
    )
  }
}

export default DomElement
