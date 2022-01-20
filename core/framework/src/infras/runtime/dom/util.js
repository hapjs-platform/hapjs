/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $hyphenate } from 'src/shared/util'

// 是否属于data属性
const REG_IS_DATA_ATTR = /^data.+/

/**
 * 返回标准dataset DOMStringMap的Key值
 * @param {string} nameWithData 以data开头的需要返回标准datasetKey值的属性
 * @return {string}
 */
function $dataAttr(nameWithData) {
  const originAttr = nameWithData.replace(/[A-Z]/g, $0 => '-' + $0.toLowerCase())
  return originAttr.replace(/^data-/, '').replace(/-([a-z])/g, ($0, $1) => $1.toUpperCase())
}

/**
 * 转换样式信息对象
 * @param styleListAll
 * @param styleHashYes
 */
function $hyphenateStyle(styleListAll, styleHashYes) {
  const styleListNew = []
  const styleHashLeft = Object.assign({}, styleHashYes)

  if (styleListAll) {
    for (let i = 0, len = styleListAll.length; i < len; i++) {
      const styleItem = styleListAll[i]

      if (styleHashLeft.hasOwnProperty(styleItem.name)) {
        styleListNew.push({
          name: $hyphenate(styleItem.name),
          value: styleHashLeft[styleItem.name],
          disabled: false
        })
        delete styleHashLeft[styleItem.name]
      } else if (styleItem.disabled) {
        styleListNew.push({
          name: $hyphenate(styleItem.name),
          value: styleItem.value,
          disabled: styleItem.disabled
        })
      }
    }
  }

  for (const propName in styleHashLeft) {
    styleListNew.push({
      name: $hyphenate(propName),
      value: styleHashLeft[propName],
      disabled: false
    })
  }

  return styleListNew
}

/**
 * 解析class名称，转换为数组
 * @param className
 * @return {Array}
 */
function classAttrToClassList(className) {
  return (className || '')
    .split(/\s+/)
    .filter(s => s !== '')
    .filter((s, i, l) => {
      return l.indexOf(s) === i
    })
}

/**
 * 获取数组中不一样的元素
 */
function diffClassNameList(arg1, arg2) {
  const hash = {}
  arg1.forEach(k => (hash[k] = true))
  arg2.forEach(k => (hash[k] ? delete hash[k] : (hash[k] = true)))
  return Object.keys(hash)
}

export { REG_IS_DATA_ATTR, $dataAttr, $hyphenateStyle, classAttrToClassList, diffClassNameList }
