/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * 判断对象是否为空（没有任何属性）
 * @param e
 * @returns {boolean}
 */
function isEmptyObject(obj) {
  if (!obj) {
    return !0
  }
  let t
  for (t in obj) {
    return !1
  }
  return !0
}

/**
 * 时间格式化
 * @param date
 * @returns {string}
 */
function formatTime(date) {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()
  return (
    [year, month, day].map(formatNumber).join('/') +
    ' ' +
    [hour, minute, second].map(formatNumber).join(':')
  )
}

/**
 * 数字格式化
 * @param n
 * @returns {string}
 */
function formatNumber(n) {
  n = n.toString()
  return n[1] ? n : '0' + n
}

/**
 * 时间格式化
 * @param timestamp
 * @returns {string}
 */
function formatTimeString(timestamp) {
  const time = arguments[0] || 0
  const t = time ? new Date(time * 1000) : new Date()
  const y = t.getFullYear() // 年
  const m = t.getMonth() + 1 // 月
  const d = t.getDate() // 日
  const h = t.getHours() // 时
  const i = t.getMinutes() // 分
  const s = t.getSeconds() // 秒
  return [y, m, d].map(formatNumber).join('-') + ' ' + [h, i, s].map(formatNumber).join(':')
}

/**
 * 日期格式化
 * @param str
 * @returns {{dateDay: string}}
 */
function formatDate(str) {
  // 拆分日期为年 月 日
  const YEAR = str.substring(0, 4)
  const MONTH = str.substring(4, 6)
  const DATE = str.slice(-2)
  // 拼接为 2016/10/02 可用于请求日期格式
  const dateDay = YEAR + '/' + MONTH + '/' + DATE
  // 获取星期几
  const week = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const day = new Date(dateDay).getDay()
  return { dateDay: MONTH + '月' + DATE + '日 ' + week[day] }
}

/**
 * 字符串转map
 * 不要重构成Set，Map实现比Object快2倍，Set三倍
 * @param str
 * @returns {Map<any, any>}
 */
export function $newMap(str) {
  const obj = new Map()
  const items = str.split(',')
  for (let i = 0; i < items.length; i++) {
    obj.set(items[i], true)
  }
  return obj
}

/**
 * 字符串加双引号
 * @param v
 * @returns {string}
 */
function $quote(v) {
  return '"' + v + '"'
}

function $camelize(value) {
  return value.replace(/-([a-z])/g, function(s, m) {
    return m.toUpperCase()
  })
}

/**
 * XxxXxx转换为-xxx-xxx
 * @param value
 * @returns {void|string|XML|*}
 */
function $hyphenate(value) {
  return value.replace(/([A-Z])/g, function(s, m) {
    return '-' + m.toLowerCase()
  })
}

/**
 * 拆分上下左右类型的简写属性
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
 * 值的有效性检验
 * @param value  值
 */
function isValidValue(value) {
  return typeof value === 'number' || typeof value === 'string'
}

export {
  isEmptyObject,
  formatTime,
  formatTimeString,
  formatDate,
  $quote,
  $camelize,
  $hyphenate,
  splitAttr,
  isValidValue
}
