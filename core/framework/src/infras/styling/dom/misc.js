/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import {
  getNodeAttributesAsObject,
  getNodeInlineStyleAsObject,
  getNodeAttrClassList,
  getNodeMergedStyleAsObject,
  setNodeInlineStyle,
  getNodeAsJSON,
  getNodeDepth,
  cssText2StyleObject
} from '../../runtime/dom/misc'

function supportRender(node) {
  return true
}

function renderParent(node, includeSelf = false) {
  let nodeTmp = includeSelf ? node : node.parentNode
  while (nodeTmp && !supportRender(nodeTmp)) {
    nodeTmp = nodeTmp.parentNode
  }
  return nodeTmp
}

function renderParents(node, includeSelf = false) {
  const list = []
  let nodeTmp = renderParent(node, includeSelf)
  while (nodeTmp) {
    list.push(nodeTmp)
    nodeTmp = renderParent(nodeTmp)
  }
  return list
}

/**
 * 节点的样式缓存，进行排序
 * @param usedCSSPropertyMap
 * @param nodeStyleCache
 * @return {*}
 */
function sortNodeStyleCache(nodeStyleCache) {
  const cssRuleList = []

  // 收集：TAG, CLASS, ID, INLINE
  for (const k in nodeStyleCache) {
    const v = nodeStyleCache[k]
    if (v && v.length) {
      cssRuleList.push.apply(cssRuleList, v)
    }
  }

  // 排序
  cssRuleList.sort((prev, next) => {
    return prev.score.sum - next.score.sum
  })

  return cssRuleList
}

/**
 * 合并节点的样式Style
 * @param usedCSSPropertyMap
 * @param nodeStyleCache
 * @return {*}
 */
function mergeNodeStyle(usedCSSPropertyMap, nodeStyleCache) {
  if (!nodeStyleCache) {
    console.trace(`### App Runtime ### 计算节点CSS样式优先级 没有样式缓存对象，不再计算`)
    return {}
  }

  const cssRuleList = sortNodeStyleCache(nodeStyleCache)

  // 只有style
  const cssRuleStyleList = cssRuleList.map(info => info.style)

  console.trace(
    `### App Runtime ### 按照优先级合并节点样式：`,
    cssRuleList.map(info => `"${info.name}"`).join(' < ')
  )
  const styleHash = Object.assign({}, ...cssRuleStyleList)

  const propertyList = Object.keys(Object.assign({}, usedCSSPropertyMap, styleHash))
  for (let i = 0, len = propertyList.length; i < len; i++) {
    const propertyName = propertyList[i]
    if (styleHash[propertyName] !== undefined && usedCSSPropertyMap[propertyName] === undefined) {
      // 新增
      usedCSSPropertyMap[propertyName] = true
    } else if (
      styleHash[propertyName] === undefined &&
      usedCSSPropertyMap[propertyName] !== undefined
    ) {
      // 保留原有
      styleHash[propertyName] = ''
    }
  }

  return styleHash
}

export {
  getNodeAttributesAsObject,
  getNodeInlineStyleAsObject,
  getNodeAttrClassList,
  getNodeMergedStyleAsObject,
  setNodeInlineStyle,
  getNodeAsJSON,
  getNodeDepth,
  cssText2StyleObject,
  supportRender,
  renderParent,
  renderParents,
  sortNodeStyleCache,
  mergeNodeStyle
}
