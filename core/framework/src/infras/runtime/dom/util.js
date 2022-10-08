/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
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

/**
 * 拆分上下左右类型简写属性
 * @param names   属性名数组
 * @param values  属性值数组
 * @returns {array}
 */
function splitAttr(names, values) {
  const resultArray = []
  if (values) {
    names.forEach((n, idx) => {
      resultArray[idx] = {}
      resultArray[idx].n = n
    })

    switch (values.length) {
      case 1:
        names.forEach((n, idx) => {
          resultArray[idx].v = values[0]
        })
        break
      case 2:
        names.forEach((n, idx) => {
          if (idx % 2) {
            resultArray[idx].v = values[1]
          } else {
            resultArray[idx].v = values[0]
          }
        })
        break
      case 3:
        names.forEach((n, idx) => {
          if (idx % 2) {
            resultArray[idx].v = values[1]
          } else {
            resultArray[idx].v = values[idx]
          }
        })
        break
      default:
        names.forEach((n, idx) => {
          resultArray[idx].v = values[idx]
        })
    }
  }
  return resultArray
}

/**
 * XxxXxx转换为-xxx-xxx
 * @param value
 * @returns {void|string|XML|*}
 */
function camelCaseToHyphened(value) {
  return value.replace(/([A-Z])/g, function(s, m) {
    return '-' + m.toLowerCase()
  })
}

/**
 * 值的有效性检验
 * @param value  值
 */
function isValidValue(value) {
  return typeof value === 'number' || typeof value === 'string'
}

/**
 * -xxx-xxx转换为XxxXxx
 * @param value
 * @returns {void|string|XML|*}
 */
function hyphenedToCamelCase(value) {
  return value.replace(/-([a-z])/g, function(s, m) {
    return m.toUpperCase()
  })
}

export {
  REG_IS_DATA_ATTR,
  $dataAttr,
  $hyphenateStyle,
  classAttrToClassList,
  diffClassNameList,
  splitAttr,
  camelCaseToHyphened,
  isValidValue,
  hyphenedToCamelCase
}
