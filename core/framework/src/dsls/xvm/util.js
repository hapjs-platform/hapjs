/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { isFunction, isPlainObject, isReserved } from 'src/shared/util'

/**
 * 定义属性
 * @param obj
 * @param key
 * @param val
 * @param enumerable
 */
function $def(obj, key, val, enumerable) {
  Object.defineProperty(obj, key, {
    value: val,
    enumerable: !!enumerable,
    writable: true,
    configurable: true
  })
}

/**
 *
 * @param arr
 * @param item
 * @returns {*|Array.<T>}
 */
function $remove(arr, item) {
  if (arr.length) {
    const index = arr.indexOf(item)
    if (index > -1) {
      return arr.splice(index, 1)
    }
  }
}

/**
 * 是否拥有属性
 */
const _hasOwnProperty = Object.prototype.hasOwnProperty
function $own(obj, key) {
  return _hasOwnProperty.call(obj, key)
}

/**
 * 绑定函数执行上下文
 * @param fn
 * @param ctx
 * @returns {Function}
 */
function $bind(fn, ctx) {
  return function(a) {
    const l = arguments.length
    return l
      ? l > 1
        ? fn.apply(ctx, arguments) // 大于1个参数
        : fn.call(ctx, a) // 一个参数
      : fn.call(ctx) // 无参数
  }
}

/**
 * 转换为数字
 * @param val  字符串
 * @param def  转换失败的缺省值
 * @returns {*}
 */
function $number(val, def) {
  if (typeof val === 'number') {
    return val
  }

  if (def && typeof def !== 'number') {
    def = parseFloat(def)
  } else if (!def) {
    def = NaN
  }

  const n = val ? parseFloat(val) : NaN
  return isNaN(n) ? def : n
}

/**
 * 区间取值
 * @param v
 * @param min
 * @param max
 */
function $clamp(v, min, max) {
  let val = $number(v)
  const minV = $number(min)
  const maxV = $number(max)
  // 无效值对应最小值
  if (!isNaN(minV) && (val < minV || isNaN(val))) {
    val = minV
  }

  if (!isNaN(maxV) && val > maxV) {
    val = maxV
  }
  return val
}

/**
 * 转换为数组
 * @param list
 * @param start
 * @returns {Array}
 */
function $array(list, start) {
  start = start || 0
  let i = list.length - start
  const ret = new Array(i)
  while (i--) {
    ret[i] = list[i + start]
  }
  return ret
}

/**
 * 添加data代理, 将data属性添加到vm中
 * @type {[*]}
 */
const keyWords = ['$idx', '$item', '$evt']
function $proxy(vm, sourceKey, key) {
  if (keyWords.indexOf(key) > -1 || !isReserved(key)) {
    // 不能以$或_开头
    Object.defineProperty(vm, key, {
      configurable: true,
      enumerable: true,
      get: function proxyGetter() {
        return vm[sourceKey] ? vm[sourceKey][key] : null
      },
      set: function proxySetter(val) {
        if (vm[sourceKey]) {
          vm[sourceKey][key] = val
        }
      }
    })
  } else {
    console.error(`### App Framework ### 页面数据属性名 '${key}' 非法, 属性名不能以$或_开头`)
  }
}

/**
 * 取消代理
 */
function $unproxy(vm, key) {
  if (!isReserved(key) && vm) {
    delete vm[key]
  }
}

// Zepto.param
const escape = encodeURIComponent
function serialize(params, obj, traditional) {
  const array = Array.isArray(obj)
  Object.keys(obj).forEach(function(key) {
    const value = obj[key]
    // handle data in serializeArray() format
    if (array) params.add(value.name, value.value)
    // recurse into nested objects
    else if (!traditional && isPlainObject(value)) {
      serialize(params, value, traditional, key)
    } else params.add(key, value)
  })
}
const param = function(obj, traditional) {
  const params = []
  params.add = function(key, value) {
    if (isFunction(value)) value = value()
    if (value == null) value = ''
    this.push(escape(key) + '=' + escape(value))
  }
  serialize(params, obj, traditional)
  return params.join('&').replace(/%20/g, '+')
}

// 是否有proto属性
const hasProto = '__proto__' in {}

const arrayProto = Array.prototype
const arrayMethods = Object.create(arrayProto)

// 拦截数组的修改动作，发送事件通知
;['push', 'pop', 'shift', 'unshift', 'splice', 'sort', 'reverse'].forEach(function(method) {
  // 缓存原始函数实现
  const original = arrayProto[method]
  // 重新定义函数
  $def(arrayMethods, method, function mutator() {
    let i = arguments.length
    const args = new Array(i)
    while (i--) {
      args[i] = arguments[i]
    }
    const result = original.apply(this, args)
    const ob = this.__ob__
    let inserted
    switch (method) {
      case 'push':
        inserted = args
        break
      case 'unshift':
        inserted = args
        break
      case 'splice':
        inserted = args.slice(2)
        break
    }

    if (inserted) {
      ob.observeArray(inserted)
    }
    // 通知改变
    ob.dep.notify()
    return result
  })
})

/**
 * 修改指定位置的元素，返回被替换的元素
 */
$def(arrayProto, '$set', function $set(index, val) {
  if (index >= this.length) {
    this.length = index + 1
  }
  return this.splice(index, 1, val)[0]
})

/**
 * 删除指定位置的元素
 */
$def(arrayProto, '$remove', function $remove(index) {
  if (!this.length) return
  if (typeof index !== 'number') {
    index = this.indexOf(index)
  }
  if (index > -1) {
    this.splice(index, 1)
  }
})

// 检查是否是保留key值
const reservedKeys = [
  'manifest',
  'config',
  'router',
  'data',
  'props',
  'style',
  'template',
  'computed'
]
function isReservedKey(key) {
  return reservedKeys.indexOf(key) >= 0
}

// 检查是否是保留attr值
const reservedAttrs = ['if', 'for', 'show', 'tid']
function isReservedAttr(key) {
  return reservedAttrs.indexOf(key) >= 0
}

function getType(fn) {
  const match = fn && fn.toString().match(/^\s*function (\w+)/)
  return match ? match[1] : ''
}

function isSameType(a, b) {
  return getType(a) === getType(b)
}

function getTypeIndex(type, expectedTypes) {
  if (!Array.isArray(expectedTypes)) {
    return isSameType(expectedTypes, type) ? 0 : -1
  }
  for (let i = 0, len = expectedTypes.length; i < len; i++) {
    if (isSameType(expectedTypes[i], type)) {
      return i
    }
  }
  return -1
}

const simpleCheckRE = /^(String|Number|Boolean|Function|Symbol)$/
function assertType(value, type) {
  console.trace(`### App Framework ### 参数类型验证 ----`, value, type)

  let valid
  const expectedType = getType(type)

  if (simpleCheckRE.test(expectedType)) {
    const t = typeof value
    valid = t === expectedType.toLowerCase()
    // for primitive wrapper objects
    if (!valid && t === 'object') {
      valid = value instanceof type
    }
  } else if (expectedType === 'Object') {
    valid = isPlainObject(value)
  } else if (expectedType === 'Array') {
    valid = Array.isArray(value)
  } else {
    valid = value instanceof type
  }
  return {
    valid,
    expectedType
  }
}

/**
 * 按添加key的顺序复制对象
 */
function assignObjectInOrder(target, source) {
  if (!source) {
    return
  }

  Object.keys(source).forEach(key => {
    if (target[key]) delete target[key]
    target[key] = source[key]
  })
}

export {
  $def,
  $remove,
  $own,
  $bind,
  $number,
  $clamp,
  $array,
  $proxy,
  $unproxy,
  hasProto,
  param,
  isReservedKey,
  isReservedAttr,
  getType,
  assertType,
  getTypeIndex,
  assignObjectInOrder,
  arrayMethods
}
