/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $unique, isObject } from 'src/shared/util'

import NodeView from './node'

import {
  getDocument,
  removeNodeInDocument,
  getDocumentStyleSheetById,
  setParent,
  getListener,
  getDocumentNodeByRef,
  insertIndex,
  moveIndex,
  removeIndex
} from '../model'
import {
  getNodeAsJSON,
  getNodeAttributesAsObject,
  getNodeInlineStyleAsObject,
  getNodeMergedStyleAsObject,
  setNodeInlineStyle,
  sortNodeStyleCache,
  mergeNodeStyle,
  cssText2StyleObject
} from '../misc'
import {
  $hyphenateStyle,
  classAttrToClassList,
  diffClassNameList,
  REG_IS_DATA_ATTR,
  $dataAttr
} from '../util'

import { CSSStyleRuleType, getNodeListOfCssRule } from '../css/definition'

import { calcNodeStyle, isNodeMatchCssRule, collectMatchChangedNodeList } from '../css/calculator'

export function applyWhenInDocument(node) {
  // 样式计算
  if (node.nodeType === NodeView.NodeType.ELEMENT && node._docId) {
    console.trace(`### App Runtime ### 添加到文档时计算节点(${node.ref}:${node._type})的样式`)
    // 计算
    calcNodeStyle(node)
    // 递归
    for (let i = 0, len = node.childNodes.length; i < len; i++) {
      applyWhenInDocument(node.childNodes[i])
    }
  }
}

function nodeInsertBefore(parentNode, node, before) {
  const childIndex = !before ? parentNode.childNodes.length : parentNode.childNodes.indexOf(before)
  const listener = getListener(parentNode._docId)
  const document = getDocument(parentNode._docId)

  if (!node.parentNode) {
    // 新增节点
    setParent(node, parentNode)
    insertIndex(node, parentNode.childNodes, childIndex, true)

    applyWhenInDocument(node)

    if (listener) {
      if (parentNode === document.documentElement) {
        document.body = node
        listener.createBody(node)
      } else {
        listener.addElement(node, parentNode.ref, childIndex)
      }
    }
  } else {
    // 移动节点
    moveIndex(node, parentNode.childNodes, childIndex, true)

    if (listener) {
      listener.moveElement(node.ref, parentNode.ref, childIndex)
    }
  }
}

export function setElementProp(node, key, value) {
  if (!node._ownerDocument || key === 'ref') {
    return
  }

  if (node[key] === value) {
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

export function setElementAttr(node, key, value) {
  if (!node._ownerDocument || key === 'ref') {
    return
  }

  if (!isObject(value) && getNodeAttributesAsObject(node)[key] === value) {
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
  calcAndTellListener(node, key, value)
  // 通知后代节点
  calcAndTellListenerForDesc(node, key, value, oldValue)
}

export function setElementStyle(node, key, value) {
  if (!node._ownerDocument) {
    return
  }

  if (getNodeInlineStyleAsObject(node)[key] === value) {
    return
  }
  getNodeInlineStyleAsObject(node)[key] = value
  calcAndTellListener(node, 'style', null, [key, value])
}

export function setElementStyles(node, value) {
  if (!node._ownerDocument) {
    return
  }

  value = value || ''

  if (getNodeInlineStyleAsObject(node) === value) {
    return
  }

  const styleObj = typeof value === 'string' ? cssText2StyleObject(value) : value
  setNodeInlineStyle(node, styleObj)
  calcAndTellListener(node, 'style')
}

function calcAndTellListener(node, name, value, extra) {
  // 如果Document有Listener，则向Listener发送更新通知
  const listener = getListener(node._docId)

  if (!listener) {
    return
  }

  if (name === 'class') {
    // 更新样式
    calcNodeStyle(node, CSSStyleRuleType.CLASS)
    listener.setStyles(node.ref, getNodeMergedStyleAsObject(node), {
      class: value
    })
  } else if (name === 'id') {
    // 更新样式
    calcNodeStyle(node, CSSStyleRuleType.ID)
    listener.setStyles(node.ref, getNodeMergedStyleAsObject(node), {
      id: value
    })
  } else if (name === 'style') {
    // 更新样式
    calcNodeStyle(node, CSSStyleRuleType.INLINE)
    if (extra) {
      // 设置单个样式
      listener.setStyle(node.ref, ...extra)
    } else {
      // 设置整个style
      listener.setStyles(node.ref, getNodeMergedStyleAsObject(node))
    }
  } else {
    // 更新属性
    listener.setAttr(node.ref, name, value)
  }
}

function calcAndTellListenerForDesc(node, name, value, oldValue) {
  // 如果Document有Listener，则向Listener发送更新通知
  const listener = getListener(node._docId)
  const document = getDocument(node._docId)

  if (!listener) {
    return
  }

  if (!getNodeAttributesAsObject(node).hasOwnProperty('descRestyling')) {
    return
  }

  console.trace(`### App Runtime ### 元素的属性(${name})更新(${oldValue}=>${value})`)

  if (name === 'class') {
    // 收集并更新影响的节点
    const oldClassList = classAttrToClassList(oldValue)
    const newClassList = classAttrToClassList(value)
    const changedClassList = diffClassNameList(oldClassList, newClassList).map(k => `.${k}`)
    const impactList = changedClassList.map(name => collectMatchChangedNodeList(node, name))
    const nodeRefList = $unique(...impactList)

    for (let i = 0, len = nodeRefList.length; i < len; i++) {
      const nodeRefItem = nodeRefList[i]
      const nodeByRef = getDocumentNodeByRef(document, nodeRefItem)
      // 优化：仅更新其中变化的规则类型
      if (nodeByRef) {
        calcNodeStyle(nodeByRef)
        sendNodeStyle(nodeByRef)
      }
    }
  } else if (name === 'id') {
    // 收集并更新影响的节点
    const impactNodeRefListForOld = oldValue
      ? collectMatchChangedNodeList(node, `#${oldValue}`)
      : []
    const impactNodeRefListForNew = value ? collectMatchChangedNodeList(node, `#${value}`) : []
    const impactNodeRefList = $unique(impactNodeRefListForOld, impactNodeRefListForNew)
    const nodeRefList = impactNodeRefList

    for (let i = 0, len = nodeRefList.length; i < len; i++) {
      const nodeRefItem = nodeRefList[i]
      const nodeByRef = getDocumentNodeByRef(document, nodeRefItem)
      if (nodeByRef) {
        // 优化：仅更新其中变化的规则类型
        calcNodeStyle(nodeByRef)
        sendNodeStyle(nodeByRef)
      }
    }
  }
}

function sendNodeStyle(node) {
  const listener = getListener(node._docId)
  if (listener) {
    listener.setStyles(node.ref, getNodeMergedStyleAsObject(node))
  }
}

/**
 * 获取元素匹配到的样式对象
 * @param node
 * @param ruleName
 */
function getNodeMatchedCssRuleList(node, ruleName) {
  const cssRuleList = sortNodeStyleCache(node._matchedCssRuleCache)

  cssRuleList.reverse()

  if (ruleName) {
    return cssRuleList.filter(infoItem => infoItem.name === ruleName)
  } else {
    return cssRuleList
  }
}

/**
 * 调试器：获取元素匹配的样式规则列表
 * 因为有禁用的顺序恢复，同时复制一份，不修改原数据
 * @param node
 * @param ruleName 不传递时获取所有
 */
export function getElementMatchedCssRuleList(node, ruleName) {
  const cssRuleListFrom = getNodeMatchedCssRuleList(node, ruleName)
  const cssRuleListDest = JSON.parse(JSON.stringify(cssRuleListFrom))

  // 转换
  for (let i = 0, len = cssRuleListDest.length; i < len; i++) {
    const itemInfo = cssRuleListDest[i]
    itemInfo.editable = true
    itemInfo.style = $hyphenateStyle(itemInfo._styleFullList, itemInfo.style)
    delete itemInfo._styleFullList
    // 携带名称
    const styleSheetInst = getDocumentStyleSheetById(node._docId, itemInfo._sheetId)
    itemInfo.styleSheetName = styleSheetInst ? styleSheetInst.name : ''
  }

  return cssRuleListDest
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

  // 获取当前匹配到的CSS规则
  const cssRule = getNodeMatchedCssRuleList(node, ruleName)[0]
  if (cssRule) {
    if (cssRule.type === CSSStyleRuleType.INLINE) {
      // 内联
      cssRule._styleFullList = styleListAll
      setElementStyles(node, styleHashNew)
    } else {
      const doc = getDocument(node._docId)
      // 非内联
      cssRule._styleFullList = styleListAll
      // 更新样式
      cssRule.style = styleHashNew
      // 通知节点
      const nodeRefList = getNodeListOfCssRule(cssRule)
      for (let i = 0, len = nodeRefList.length; i < len; i++) {
        const nodeRefItem = nodeRefList[i]
        const nodeByRef = getDocumentNodeByRef(doc, nodeRefItem)
        const nodeMatch = isNodeMatchCssRule(cssRule, nodeByRef).match
        console.trace(
          `### App Runtime ### 更新CSS规则(${cssRule.name})的样式，元素(${nodeByRef.ref})的匹配结果：${nodeMatch}`
        )
        if (nodeMatch) {
          // 置空之前计算的结果缓存
          nodeByRef._mergedStyleCache = null
          // 触发更新
          sendNodeStyle(nodeByRef)
        }
      }
    }
    console.trace(
      `### App Runtime ### 更新样式:元素(${node.ref})的匹配样式：${ruleName}:${JSON.stringify(
        styleHashNew
      )}`
    )
  } else {
    // 不匹配
    console.trace(`### App Runtime ### 更新样式:元素(${node.ref})无匹配样式：${ruleName}`)
  }
}

class ElementView extends NodeView {
  constructor(name) {
    super(...arguments)

    this._nodeType = NodeView.NodeType.ELEMENT
    this._nodeName = name.toUpperCase()

    this._tagName = name.toUpperCase()

    // 事件名称
    this._eventTargetListeners = {}

    this._type = name
    this._attr = {}
    this._style = {}
    this._dataset = {}
    // 缓存类样式
    this._classList = null
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

  get tagName() {
    return this._tagName
  }

  get dataset() {
    return this._dataset
  }

  get mergedStyle() {
    if (!this._mergedStyleCache) {
      this._mergedStyleCache = mergeNodeStyle(this._usedCSSPropertyCache, this._matchedCssRuleCache)
    }
    return this._mergedStyleCache
  }

  appendChild(node) {
    if (!node || !(node instanceof NodeView)) {
      throw new Error(`### App Runtime ### appendChild() 函数的node参数无效`)
    }

    if (node.parentNode && node.parentNode !== this) {
      throw new Error(`### App Runtime ### appendChild() 参数node的父节点不匹配`)
    }

    return nodeInsertBefore(this, node, null)
  }

  insertBefore(node, before) {
    if (!node || arguments.length !== 2 || !(node instanceof NodeView)) {
      throw new Error(`### App Runtime ### insertBefore() 函数的node/before参数无效`)
    }

    if (node.parentNode && node.parentNode !== this) {
      throw new Error(`### App Runtime ### insertBefore() 参数node的父节点不匹配`)
    }

    // TODO 删除previousSibling,nextSibling检测

    return nodeInsertBefore(this, node, before)
  }

  removeChild(node) {
    if (!node || !(node instanceof NodeView)) {
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

    node.parentNode = null

    const listener = getListener(node._docId)
    if (listener) {
      listener.removeElement(node.ref)
    }
    removeIndex(node, this.childNodes, true)

    // 文档中移除
    removeNodeInDocument(node, node._docId)

    return node
  }

  toString() {
    // 属性转换
    const attr = getNodeAttributesAsObject(this)
    const style = getNodeMergedStyleAsObject(this)

    return (
      '<' +
      this._type +
      ' attr=' +
      JSON.stringify(attr) +
      ' style=' +
      JSON.stringify(style) +
      '>' +
      this.childNodes.map(child => child.toString()).join('') +
      '</' +
      this._type +
      '>'
    )
  }
}

export default ElementView
