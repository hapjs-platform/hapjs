/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// 各中选择器中规则的优先级分数
const WEIGHT = {
  TAG: 1,
  CLASS: 1e3,
  ID: 1e6,
  // 与order的间隔
  STEP: 1e6,
  // 内联最高
  INLINE: 1e9
}

// 选择器的类型
const CSSStyleRuleType = {
  TAG: 1,
  CLASS: 2,
  ID: 3,
  DESC: 4,
  INLINE: 5
}

// 类似CSSStyleRule：记录Selector的完整信息
const CSSStyleRuleDef = {
  // 选择器的声明名称
  name: null,
  // 选择器类型
  type: null,
  // 优先级分数对象: { self：仅权重之和, sum：self + order }
  score: null,
  // 在所有CSS声明中的先后顺序
  order: 0,
  // 样式信息
  style: null,
  // 所属样式表
  _sheetId: null,
  // 匹配的元素缓存
  _hitNodeMap: null,
  // 调试时的信息，仅调试用到，如：是否禁用
  _styleFullList: null
}

// CSS规则的映射表
const cssRuleMap = {}

/**
 * 记录到映射表中
 * TODO 需要考虑内存泄露
 * @param cssRule
 */
function addCssRule(cssRule) {
  cssRuleMap[cssRule.order] = cssRule
}

/**
 * 获取指定的CSS规则
 * @param cssRuleOrder
 * @return {*}
 */
function getCssRule(cssRuleOrder) {
  return cssRuleMap[cssRuleOrder]
}

/**
 * 实例化TAG选择器对象
 * @param info
 * @return {*}
 */
function createCssRuleTagInfo(info) {
  const item = Object.assign(
    {},
    CSSStyleRuleDef,
    {
      type: CSSStyleRuleType.TAG,
      score: { self: WEIGHT.TAG, depth: 1 },
      _hitNodeMap: {}
    },
    info
  )
  // 计算分数
  item.score.sum = item.score.self * WEIGHT.STEP + item.order
  return item
}

/**
 * 实例化Class选择器对象
 * @param info
 * @return {*}
 */
function createCssRuleClassInfo(info) {
  const item = Object.assign(
    {},
    CSSStyleRuleDef,
    {
      type: CSSStyleRuleType.CLASS,
      score: { self: WEIGHT.CLASS, depth: 1 },
      _hitNodeMap: {}
    },
    info
  )
  // 计算分数
  item.score.sum = item.score.self * WEIGHT.STEP + item.order
  return item
}

/**
 * 实例化ID选择器对象
 * @param info
 * @return {*}
 */
function createCssRuleIdInfo(info) {
  const item = Object.assign(
    {},
    CSSStyleRuleDef,
    {
      type: CSSStyleRuleType.ID,
      score: { self: WEIGHT.ID, depth: 1 },
      _hitNodeMap: {}
    },
    info
  )
  // 计算分数
  item.score.sum = item.score.self * WEIGHT.STEP + item.order
  return item
}

/**
 * 实例化后代选择器对象
 * @param info
 * @return {*}
 */
function createCssRuleDescInfo(info) {
  const item = Object.assign(
    {},
    CSSStyleRuleDef,
    {
      type: CSSStyleRuleType.DESC,
      _hitNodeMap: {}
    },
    info
  )
  // 计算分数
  item.score = _calculateDescScore(item.meta.ruleDef, item.order)
  item.score.sum = item.score.self * WEIGHT.STEP + item.order
  return item
}

/**
 * 实例化内联样式
 * @param info
 * @return {*}
 */
function createCssRuleInlineInfo(info) {
  const item = Object.assign(
    {},
    CSSStyleRuleDef,
    {
      name: 'INLINE',
      type: CSSStyleRuleType.INLINE,
      score: { self: WEIGHT.INLINE },
      _hitNodeMap: {}
    },
    info
  )
  // 计算分数
  item.score.sum = item.score.self * WEIGHT.STEP + item.order
  return item
}

/**
 * 计算后代选择器的分数
 * @param ruleTypeList
 * @param order
 */
function _calculateDescScore(ruleTypeList, order) {
  const hash = {
    id: 0,
    class: 0,
    tag: 0,
    self: 0,
    depth: 1
  }
  // 分别计算
  for (let i = 0, len = ruleTypeList.length; i < len; i++) {
    const ruleTypeItem = ruleTypeList[i]

    if (isSelectorRuleTypeTag(ruleTypeItem)) {
      hash.tag += 1
    } else if (isSelectorRuleTypeId(ruleTypeItem)) {
      hash.id += 1
    } else if (isSelectorRuleTypeClass(ruleTypeItem)) {
      hash.class += 1
    } else if (isSelectorRuleTypePointer(ruleTypeItem)) {
      hash.depth += 1
    }
  }
  // 求和，方便排序
  hash.self = hash.id * WEIGHT.ID + hash.class * WEIGHT.CLASS + hash.tag * WEIGHT.TAG

  return {
    self: hash.self,
    depth: hash.depth
  }
}

/**
 * CSS选择器中某个规则类型为tag匹配
 * @param ruleTypeItem
 * @return {boolean}
 */
function isSelectorRuleTypeTag(ruleTypeItem) {
  return ruleTypeItem.type === 'tag'
}

/**
 * CSS选择器中某个规则类型为class匹配
 * @param ruleTypeItem
 * @return {boolean}
 */
function isSelectorRuleTypeClass(ruleTypeItem) {
  return ruleTypeItem.type === 'attribute' && ruleTypeItem.name === 'class'
}

/**
 * CSS选择器中某个规则类型为id匹配
 * @param ruleTypeItem
 * @return {boolean}
 */
function isSelectorRuleTypeId(ruleTypeItem) {
  return ruleTypeItem.type === 'attribute' && ruleTypeItem.name === 'id'
}

/**
 * CSS选择器中某个规则类型为指向孩子或后代节点
 * @param ruleTypeItem
 * @return {boolean}
 */
function isSelectorRuleTypePointer(ruleTypeItem) {
  return ruleTypeItem.type === 'descendant' || ruleTypeItem.type === 'child'
}

function saveNodeInCssRule(cssRule, nodeRef) {
  cssRule._hitNodeMap[nodeRef] = true
}

function removeNodeInCssRule(cssRule, nodeRef) {
  delete cssRule._hitNodeMap[nodeRef]
}

function getNodeListOfCssRule(cssRule) {
  return Object.keys(cssRule._hitNodeMap)
}

export {
  CSSStyleRuleType,
  addCssRule,
  getCssRule,
  createCssRuleTagInfo,
  createCssRuleClassInfo,
  createCssRuleIdInfo,
  createCssRuleDescInfo,
  createCssRuleInlineInfo,
  isSelectorRuleTypeTag,
  isSelectorRuleTypeClass,
  isSelectorRuleTypeId,
  isSelectorRuleTypePointer,
  saveNodeInCssRule,
  removeNodeInCssRule,
  getNodeListOfCssRule
}
