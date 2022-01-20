/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import {
  CSSStyleRuleType,
  isSelectorRuleTypeClass,
  isSelectorRuleTypeId,
  saveNodeInCssRule,
  removeNodeInCssRule
} from './definition'

import {
  getNodeAttributesAsObject,
  getNodeAttrClassList,
  renderParent,
  renderParents
} from '../misc'

/**
 * 节点是否匹配某项CSS规则
 * @param cssRule
 * @param node
 */
function isNodeMatchCssRule(cssRule, node) {
  let result
  switch (cssRule.type) {
    case CSSStyleRuleType.TAG:
      const matchT = cssRule.name === node._type
      result = { match: matchT, matchChanged: false, pathChanged: false }
      break
    case CSSStyleRuleType.CLASS:
      const matchC = getNodeAttrClassList(node).indexOf(cssRule.name.substring(1)) !== -1
      result = { match: matchC, matchChanged: false, pathChanged: false }
      break
    case CSSStyleRuleType.ID:
      const matchI = getNodeAttributesAsObject(node).id === cssRule.name.substring(1)
      result = { match: matchI, matchChanged: false, pathChanged: false }
      break
    case CSSStyleRuleType.DESC:
      result = isNodeMatchDescSelector(cssRule, node)
      break
    default:
      throw new Error(`不支持的CSS规则类型：${cssRule.type}`)
  }

  // 记录下来，待cssRule更新时通知
  if (result.match) {
    saveNodeInCssRule(cssRule, node.ref)
  } else {
    removeNodeInCssRule(cssRule, node.ref)
  }

  return result
}

/**
 * 节点是否满足选择器规则
 * @desc 当前的查询匹配仅限于DomElement
 * @param descCssRule {CssRule}
 * @param node {DomElement}
 */
function isNodeMatchDescSelector(descCssRule, node) {
  const ruleTypeList = descCssRule.meta.ruleDef
  const nowMatchPathList = []
  let nodeList = [node]
  for (let i = ruleTypeList.length - 1; i >= 0; i--) {
    const ruleTypeItem = ruleTypeList[i]
    const isLast = i === ruleTypeList.length - 1
    nodeList = queryByRuleTypeItem(ruleTypeItem, nodeList)
    if (nodeList.length === 0 || node._depth < descCssRule.score.depth) {
      return _genMatchResult(descCssRule, node, false, [])
    } else {
      if (!isLast && ['descendant', 'child'].indexOf(ruleTypeItem.type) === -1) {
        nowMatchPathList.push(nodeList[0].ref)
      }
    }
  }
  return _genMatchResult(descCssRule, node, true, nowMatchPathList)
}

/**
 * 获取匹配的结果信息
 * @param cssRule
 * @param node
 * @param match
 * @param nowMatchPathList
 * @private
 */
function _genMatchResult(cssRule, node, match, nowMatchPathList) {
  const result = {
    // 是否匹配
    match: match,
    // 匹配与之前的是否变化
    matchChanged: null,
    // 匹配路径是否变化
    pathChanged: null
  }

  if (match) {
    // 匹配成功时
    const preMatchPathList = node._ownCssRuleCache[cssRule.order]
    if (preMatchPathList) {
      if (preMatchPathList.join(',') === nowMatchPathList.join(',')) {
        result.matchChanged = false
        result.pathChanged = false
      } else {
        result.matchChanged = false
        result.pathChanged = true
      }
    } else {
      result.matchChanged = true
      result.pathChanged = true
    }
    node._ownCssRuleCache[cssRule.order] = nowMatchPathList
  } else {
    // 匹配失败时
    const preMatchPathList = node._ownCssRuleCache[cssRule.order]
    if (preMatchPathList) {
      result.matchChanged = true
      result.pathChanged = true
    } else {
      result.matchChanged = false
      result.pathChanged = false
    }
    node._ownCssRuleCache[cssRule.order] = null
  }

  console.trace(
    `### App Runtime ### 节点(${node._type}:${node.ref})与后代CSS规则(${
      cssRule.name
    })的匹配计算：${JSON.stringify(result)}`
  )
  return result
}

/**
 * 获取满足某一项规则的节点列表
 * @param ruleTypeItem
 * @param nodeList {Array}
 * @return {Array}
 */
function queryByRuleTypeItem(ruleTypeItem, nodeList) {
  const ruleTypeName = ruleTypeItem.type
  switch (ruleTypeName) {
    case 'attribute':
      // id, class
      return queryByRuleTypeAttribute(ruleTypeItem, nodeList)
    case 'tag':
      // 标签名
      return queryByRuleTypeTag(ruleTypeItem, nodeList)
    case 'descendant':
      // 所有后代
      return queryByRuleTypeDesc(ruleTypeItem, nodeList)
    case 'child':
      // 直接后代
      return queryByRuleTypeChild(ruleTypeItem, nodeList)
    default:
      console.warn(
        `### App Runtime ### 未知的CSS Selector规则：${ruleTypeName}，当前支持：tag, class, id, 后代, '>'`
      )
      return []
  }
}

/**
 * 是否满足class, id
 * @param ruleTypeItem
 * @param nodeList {Array}nodeList
 * @return {Array}
 */
function queryByRuleTypeAttribute(ruleTypeItem, nodeList) {
  if (isSelectorRuleTypeClass(ruleTypeItem)) {
    const item = nodeList.find(
      node => getNodeAttrClassList(node).indexOf(ruleTypeItem.value) !== -1
    )
    return item ? [item] : []
  } else if (isSelectorRuleTypeId(ruleTypeItem)) {
    const item = nodeList.find(node => getNodeAttributesAsObject(node).id === ruleTypeItem.value)
    return item ? [item] : []
  } else {
    console.warn(
      `### App Runtime ### 未知的CSS Selector规则：${ruleTypeItem.name}，当前支持：class, id`
    )
    return []
  }
}

/**
 * 获取满足标签名称为指定名称的节点列表
 * @param ruleTypeItem
 * @param nodeList {Array}
 * @return {Array}
 */
function queryByRuleTypeTag(ruleTypeItem, nodeList) {
  const item = nodeList.find(node => node._type === ruleTypeItem.name)
  return item ? [item] : []
}

/**
 * 获取满足所有后代的节点列表
 * @param ruleTypeItem
 * @param nodeList {Array}
 * @return {Array}
 */
function queryByRuleTypeDesc(ruleTypeItem, nodeList) {
  const ret = []
  for (let i = 0, len = nodeList.length; i < len; i++) {
    const node = nodeList[i]
    ret.push.apply(ret, renderParents(node))
  }
  return ret
}

/**
 * 获取满足直接后代的节点列表
 * @param ruleTypeItem
 * @param nodeList {Array}
 * @return {Array}
 */
function queryByRuleTypeChild(ruleTypeItem, nodeList) {
  const ret = []
  for (let i = 0, len = nodeList.length; i < len; i++) {
    const node = nodeList[i]
    const parent = renderParent(node)
    parent && ret.push(parent)
  }
  return ret
}

export { isNodeMatchCssRule }
