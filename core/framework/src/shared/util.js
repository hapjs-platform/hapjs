/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * 获取对象类型，从[Object object]中提取object部分
 * @param v
 * @returns {string}
 */
function $typeof(v) {
  const s = Object.prototype.toString.call(v)
  return s.substring(8, s.length - 1).toLowerCase()
}

/**
 * 创建缓存版本的函数(函数值被缓存起来，下次调用时先从缓存中查找）
 * @param fn
 * @returns {cachedFn}
 */
function $cached(fn) {
  const cache = Object.create(null)
  return function cachedFn(str) {
    const hit = cache[str]
    return hit || (cache[str] = fn(str))
  }
}

/**
 * -xxx转换为Xxx
 * @type {RegExp}
 */
const camelizeReg = /-(\w)/g
const $camelize = $cached(str => {
  return str.replace(camelizeReg, toUpper)
})

function toUpper(_, c) {
  return c ? c.toUpperCase() : ''
}

/**
 * Xxx转换为-xxx
 * @type {RegExp}
 */
const hyphenateReg = /([a-z\d])([A-Z])/g
const $hyphenate = $cached(str => {
  return str.replace(hyphenateReg, '$1-$2').toLowerCase()
})

/**
 * 合并对象
 * @param {Object} target
 * @param {Object} src
 * @returns {*}
 */
function $extend(target, ...src) {
  if (typeof Object.assign === 'function') {
    Object.assign(target, ...src)
  } else {
    const first = src.shift()
    // 覆盖旧值
    for (const key in first) {
      target[key] = first[key]
    }
    if (src.length) {
      $extend(target, ...src)
    }
  }
  return target
}

/**
 * 获取数组中元素的并集
 * @param {array} args
 * @return {[*]}
 */
function $unique(...args) {
  const final = args.reduce((list, curr) => list.concat(curr), [])
  return Array.from(new Set(final))
}

/**
 * 是否为数字
 * @param {*} n
 * @returns {boolean}
 */
function isNumber(n) {
  return Object.prototype.toString.call(n) === '[object Number]'
}

/**
 * 是否为字符串
 * @param {*} n
 * @returns {boolean}
 */
function isString(n) {
  return Object.prototype.toString.call(n) === '[object String]'
}

/**
 * 是否为函数
 * @param {*} n
 * @returns {boolean}
 */
function isFunction(n) {
  return typeof n === 'function'
}

/**
 * 是否为对象
 * @param {*} obj
 * @returns {boolean}
 */
function isObject(obj) {
  return obj !== null && typeof obj === 'object'
}

/**
 * 是否为空对象
 * @param {*} obj
 * @returns {boolean}
 */
function isEmptyObject(obj) {
  for (const key in obj) {
    return false
  }
  return true
}

const toString = Object.prototype.toString
const OBJECT_STRING = '[object Object]'

/**
 * 对象是否为纯字典对象(仅有key,value)
 * @param {*} obj
 * @return {boolean}
 */
function isPlainObject(obj) {
  return toString.call(obj) === OBJECT_STRING
}

/**
 * @param {*} v
 * @returns {boolean}
 */
function isDef(v) {
  return v !== undefined && v !== null
}

/**
 * 是否为 promise 对象
 * @param {*} v
 * @returns {boolean}
 */
function isPromise(v) {
  return isDef(v) && typeof v.then === 'function' && typeof v.catch === 'function'
}

/**
 * 字符串是否以‘$’或‘_’开头（系统保留变量名）
 * @param {string} str
 * @returns {boolean}
 */
function isReserved(str) {
  const c = (str + '').charCodeAt(0)
  return c === 0x24 || c === 0x5f
}

const REGEXP_APPLICATION = /^@(app)-application\//
const REGEXP_COMPONENT = /^@(app)-component\//
const REGEXP_MODULE = /^@(app)-module\//

const isApplication = name => !!name.match(REGEXP_APPLICATION)
const isComponent = name => !!name.match(REGEXP_COMPONENT)
const isModule = name => !!name.match(REGEXP_MODULE)

/**
 * 删除其中约定的前缀
 * @param {string} str
 * @return {string}
 */
function removeAppPrefix(str) {
  const result = str
    .replace(REGEXP_APPLICATION, '')
    .replace(REGEXP_COMPONENT, '')
    .replace(REGEXP_MODULE, '')
  return result
}

const REGEXP_APP = /^applc:/
const REGEXP_XVM = /^xlc:/

/**
 * 去除 app 和 xvm 的生命周期前缀
 * @param {string} str
 * @return {string}
 */
function removeLifecyclePrefix(str) {
  const result = str.replace(REGEXP_APP, '').replace(REGEXP_XVM, '')
  return result
}

// 全局唯一回调Id
let invokeCallbackId = 0
function uniqueCallbackId() {
  return ++invokeCallbackId
}

// 全局唯一session，用于Framework性能火焰图统计
let _session

function createSession() {
  _session = new global.Session()
  return _session
}

function getSessionInstance() {
  return _session
}

let _isFirstOnShow = true
function changeIsFirstOnShowToFalse() {
  _isFirstOnShow = false
}

function getIsFirstOnShow() {
  return _isFirstOnShow
}

export {
  $typeof,
  $camelize,
  $hyphenate,
  $extend,
  $unique,
  isNumber,
  isString,
  isFunction,
  isObject,
  isEmptyObject,
  isPlainObject,
  isPromise,
  isReserved,
  // 应用相关
  isApplication,
  isComponent,
  isModule,
  removeAppPrefix,
  removeLifecyclePrefix,
  uniqueCallbackId,
  // 引擎性能统计火焰图相关
  createSession,
  getSessionInstance,
  changeIsFirstOnShowToFalse,
  getIsFirstOnShow
}
