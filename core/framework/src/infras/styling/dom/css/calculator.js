/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $unique } from 'src/shared/util'

import { CSSStyleRuleType, createCssRuleInlineInfo } from './definition'
import { isNodeMatchCssRule } from './selector'

import { getDocument, getNodeStyleSheetList } from '../model'
import {
  getNodeAttributesAsObject,
  getNodeInlineStyleAsObject,
  getNodeAttrClassList,
  supportRender
} from '../misc'

/**
 * 计算节点的样式
 * @param node
 * @param calcType {CSSStyleRuleType}
 */
function calcNodeStyle(node, calcType) {
  const doc = getDocument(node._docId)
  if (!doc) {
    console.trace(
      `### App Runtime ### calcNodeStyle() 节点(${node.ref}:${node._type})暂无关联document`
    )
    return
  } else {
    console.trace(
      `### App Runtime ### calcNodeStyle() 节点(${node.ref}:${node._type})计算类型：${calcType}`
    )
  }

  // 置空之前计算的结果缓存
  node._mergedStyleCache = null
  // 清空之前计算的分类缓存
  if (!calcType || calcType === CSSStyleRuleType.INLINE) {
    const existedCache = node._matchedCssRuleCache[CSSStyleRuleType.INLINE]
    node._matchedCssRuleCache[CSSStyleRuleType.INLINE] = existedCache || []
  }
  if (!calcType || calcType === CSSStyleRuleType.TAG) {
    node._matchedCssRuleCache[CSSStyleRuleType.TAG] = []
  }
  if (!calcType || calcType === CSSStyleRuleType.CLASS) {
    node._matchedCssRuleCache[CSSStyleRuleType.CLASS] = []
  }
  if (!calcType || calcType === CSSStyleRuleType.ID) {
    node._matchedCssRuleCache[CSSStyleRuleType.ID] = []
  }

  let cssRuleDefList,
    matchedListSimple,
    matchedListDesc,
    matchedListAll,
    cssRuleDefListCacheInParent

  const nodeStyle = getNodeInlineStyleAsObject(node)
  // 先计算内联样式：它不依赖于样式表
  if (!calcType || calcType === CSSStyleRuleType.INLINE) {
    // inline样式
    cssRuleDefList = node._matchedCssRuleCache[CSSStyleRuleType.INLINE]
    // 新增或复用
    if (!cssRuleDefList.length) {
      cssRuleDefList.push(
        createCssRuleInlineInfo({
          style: nodeStyle
        })
      )
    } else {
      cssRuleDefList[0].style = nodeStyle
    }

    // 仅计算内联
    if (calcType === CSSStyleRuleType.INLINE) {
      return
    }
  }

  // 样式节点列表
  const styleSheetInstList = getNodeStyleSheetList(node)

  for (let i = 0, len = styleSheetInstList.length; i < len; i++) {
    const styleSheetInst = styleSheetInstList[i]
    const styleSheetInstId = styleSheetInst.id

    const nameHash = styleSheetInst.nameHash
    const descLast = styleSheetInst.descLast

    if (!calcType || calcType === CSSStyleRuleType.TAG) {
      cssRuleDefList = node._matchedCssRuleCache[CSSStyleRuleType.TAG]
      const nodeType = node._type
      // 缓存获取
      cssRuleDefListCacheInParent = getCssRuleInNodeParentCache(node, styleSheetInstId, nodeType)
      if (cssRuleDefListCacheInParent) {
        cssRuleDefList.push.apply(cssRuleDefList, cssRuleDefListCacheInParent)
      } else {
        matchedListSimple = matchedListDesc = []
        // 简单
        if (nameHash.hasOwnProperty(nodeType)) {
          matchedListSimple = _getMatchedStyleForSimple([nameHash[nodeType]], node)
        }
        // 后代
        const typeDefList = (descLast[nodeType] && descLast[nodeType].list) || []
        if (typeDefList.length > 0) {
          matchedListDesc = _getMatchedStyleForDesc(typeDefList, node)
        }
        matchedListAll = matchedListSimple.concat(matchedListDesc)
        cssRuleDefList.push.apply(cssRuleDefList, matchedListAll)
        // 更新缓存
        // saveCssRuleInNodeParentCache(node, styleSheetInstId, nodeType, matchedListAll)
      }
    }

    const nodeClassList = getNodeAttrClassList(node)
    if (nodeClassList.length > 0 && (!calcType || calcType === CSSStyleRuleType.CLASS)) {
      cssRuleDefList = node._matchedCssRuleCache[CSSStyleRuleType.CLASS]
      // 转换
      const wrapClassList = nodeClassList.map(name => `.${name}`)
      for (let i = 0, len = wrapClassList.length; i < len; i++) {
        const wrapClassName = wrapClassList[i]
        // 缓存获取
        cssRuleDefListCacheInParent = getCssRuleInNodeParentCache(
          node,
          styleSheetInstId,
          wrapClassName
        )
        if (cssRuleDefListCacheInParent) {
          cssRuleDefList.push.apply(cssRuleDefList, cssRuleDefListCacheInParent)
        } else {
          matchedListSimple = matchedListDesc = []
          // 简单
          if (nameHash.hasOwnProperty(wrapClassName)) {
            matchedListSimple = _getMatchedStyleForSimple([nameHash[wrapClassName]], node)
          }
          // 后代
          if (descLast[wrapClassName]) {
            const typeDefList = descLast[wrapClassName].list || []
            matchedListDesc = _getMatchedStyleForDesc(typeDefList, node)
          }
          matchedListAll = matchedListSimple.concat(matchedListDesc)
          cssRuleDefList.push.apply(cssRuleDefList, matchedListAll)
          // 更新缓存
          // saveCssRuleInNodeParentCache(node, styleSheetInstId, wrapClassName, matchedListAll)
        }
      }
    }

    if (getNodeAttributesAsObject(node).id && (!calcType || calcType === CSSStyleRuleType.ID)) {
      cssRuleDefList = node._matchedCssRuleCache[CSSStyleRuleType.ID]
      const attrId = getNodeAttributesAsObject(node).id
      const attrIdSelector = `#${attrId}`
      // 缓存获取
      cssRuleDefListCacheInParent = getCssRuleInNodeParentCache(
        node,
        styleSheetInstId,
        attrIdSelector
      )
      if (cssRuleDefListCacheInParent) {
        cssRuleDefList.push.apply(cssRuleDefList, cssRuleDefListCacheInParent)
      } else {
        matchedListSimple = matchedListDesc = []
        // 简单
        if (attrId && nameHash.hasOwnProperty(attrIdSelector)) {
          matchedListSimple = _getMatchedStyleForSimple([nameHash[attrIdSelector]], node)
        }
        // 后代
        const typeDefList = (descLast[attrIdSelector] && descLast[attrIdSelector].list) || []
        if (attrId && typeDefList.length > 0) {
          matchedListDesc = _getMatchedStyleForDesc(typeDefList, node)
        }
        matchedListAll = matchedListSimple.concat(matchedListDesc)
        cssRuleDefList.push.apply(cssRuleDefList, matchedListAll)
        // 更新缓存
        // saveCssRuleInNodeParentCache(node, styleSheetInstId, attrIdSelector, matchedListAll)
      }
    }
  }
}

/**
 * 获取节点在各个Selector下的样式列表
 * @param cssRuleList
 * @param node
 * @return {Array}
 */
function _getMatchedStyleForSimple(cssRuleList, node) {
  const matchedList = []
  // 非后代
  for (let i = 0, len = cssRuleList.length; i < len; i++) {
    const cssRuleItem = cssRuleList[i]
    if (isNodeMatchCssRule(cssRuleItem, node).match) {
      matchedList.push(cssRuleItem)
    }
  }
  return matchedList
}

/**
 * 获取节点在各个Selector下的样式列表
 * @param nameHash
 * @param cssRuleList
 * @param node
 * @return {Array}
 */
function _getMatchedStyleForDesc(cssRuleList, node) {
  const matchedList = []
  // 后代
  for (let i = 0; i < cssRuleList.length; i++) {
    const cssRuleItem = cssRuleList[i]
    if (isNodeMatchCssRule(cssRuleItem, node).match) {
      matchedList.push(cssRuleItem)
    }
  }
  return matchedList
}

/**
 * 收集匹配变化的节点列表
 * @param node
 * @param ruleTypeIdtt
 * @return {Array}
 */
function collectMatchChangedNodeList(node, ruleTypeIdtt) {
  const cssRuleList = getCssRuleListByNotLastRuleType(node, ruleTypeIdtt)
  const nodeRefList = []

  for (let i = 0, len = cssRuleList.length; i < len; i++) {
    const cssRuleItem = cssRuleList[i]
    const tmpList = collectChangedNodeListByCssRule(cssRuleItem, node)
    nodeRefList.push(...tmpList)
  }

  return $unique(nodeRefList)
}

/**
 * 获取某项规则类型作为非末尾类型的CSS规则列表；
 * 因为不同的DOM节点有不同的样式表，因此依赖节点
 * @param node
 * @param ruleTypeIdtt
 * @return {Array}
 */
function getCssRuleListByNotLastRuleType(node, ruleTypeIdtt) {
  const styleSheetInstList = getNodeStyleSheetList(node)
  const cssRuleList = []

  for (let i = 0, len = styleSheetInstList.length; i < len; i++) {
    const styleSheetInst = styleSheetInstList[i]
    const descNotLast = styleSheetInst.descNotLast

    if (descNotLast[ruleTypeIdtt]) {
      const tmpList = descNotLast[ruleTypeIdtt].list
      cssRuleList.push(...tmpList)
    }
  }

  return cssRuleList
}

/**
 * 收集所有孩子节点中，对于某项CSS规则匹配变化的节点列表
 * @param cssRule
 * @param node
 * @return {Array}
 */
function collectChangedNodeListByCssRule(cssRule, node) {
  const impactedList = []
  for (let i = 0, len = node.childNodes.length; i < len; i++) {
    const childNode = node.childNodes[i]
    // 先深度再广度
    if (supportRender(childNode)) {
      const result = isNodeMatchCssRule(cssRule, childNode)
      if (result.matchChanged) {
        impactedList.push(childNode.ref)
      }
    }
    const listFromChild = collectChangedNodeListByCssRule(cssRule, childNode)
    impactedList.push(...listFromChild)
  }
  return impactedList
}

function getCssRuleInNodeParentCache() {
  return false
}

export { calcNodeStyle, isNodeMatchCssRule, collectMatchChangedNodeList }
