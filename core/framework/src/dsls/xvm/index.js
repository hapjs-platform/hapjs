/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $typeof } from 'src/shared/util'

import XVm from './vm'
import context from './context'

import framework from './interface'
import { isFunction } from '../../shared/util'

/**
 * 根据id查找元素(添加对应原生组件的接口函数)
 * @param  {string} id, 如果没有指定,则返回根节点
 * @return {Element}
 */
function $element(id) {
  if (id === undefined) {
    return typeof this.$rootElement === 'function' && this.$rootElement()
  }

  const info = this._ids[id]
  if (info) {
    context.quickapp.dock.bindComponentMethods(info.vm._page || {}, info.element)
    return info.element
  }
}

/**
 * 返回当前组件根元素(添加对应原生组件的接口函数)
 * @return {Element}
 */
function $rootElement() {
  if (this._rootElement) {
    context.quickapp.dock.bindComponentMethods(this._page || {}, this._rootElement)
    return this._rootElement
  }
}

/**
 * 根据Id获取vm
 * @param id
 * @returns {*}
 */
function $child(id) {
  const info = this._ids[id]
  if (info) {
    if (info.vm._destroyed) {
      console.warn(`### App Framework ### 组件 ${info.vm._type} 已销毁`)
    }
    return info.vm
  }
}

/**
 * 根据Id获取vm
 * @deprecated 请使用$child替代
 * @param id
 * @returns {*}
 */
function $vm(id) {
  const info = this._ids[id]
  if (info) {
    return info.vm
  }
}

/**
 * 获取App实例配置
 */
function $config() {
  if (this._destroyed) {
    console.error(`### App Framework ### 组件Vm '${this._type}' 已销毁`)
  }
  if (this._page && this._page.app && this._page.app.options) {
    return this._page.app.options
  } else {
    console.error(`### App Framework ### $config 获取配置失败`)
  }
}

/**
 * 扩展属性
 * @type {*}
 */
const $extend =
  Object.assign ||
  function(target) {
    for (let i = 1; i < arguments.length; i++) {
      const source = arguments[i]
      for (const key in source) {
        if (Object.prototype.hasOwnProperty.call(source, key)) {
          target[key] = source[key]
        }
      }
    }
    return target
  }

/**
 * 转换为字符串
 * @param v
 * @returns {*}
 */
function $stringify(v) {
  const type = $typeof(v)
  switch (type) {
    case 'undefined':
    case 'null':
      return '' // 空字符串
    case 'number':
    case 'boolean':
    case 'function':
    case 'regexp':
      return v.toString()
    case 'date':
      return v.toISOString() // 日期
    case 'string':
      return v
    default:
      return JSON.stringify(v) // 其余一律转换为字符串
  }
}

/**
 * 创建jsx元素
 * @param type
 * @param attrs
 * @param children
 */
function $createElement(type, attrs, children) {
  const parser = context.quickapp.platform.requireBundle('parser.js')
  const elem = {
    __jsx__: true,
    type: type,
    attr: {},
    events: {},
    style: {},
    classList: [],
    children: []
  }

  // 处理属性
  if (attrs) {
    // 属性分类
    Object.keys(attrs).forEach(key => {
      const value = attrs[key]
      if (key === 'style') {
        value.split(';').forEach(function(declarationText) {
          let k, v
          let pair = declarationText.trim().split(':')
          // 如果出现xxx:xxx:xxx的情况, 则将第一个:之后文本作为value
          if (pair.length > 2) {
            pair[1] = pair.slice(1).join(':')
            pair = pair.slice(0, 2)
          }
          if (pair.length === 2) {
            k = pair[0].trim()
            v = parser.validateStyle(k, pair[1].trim())
            if (v.value) {
              v.value.forEach(t => {
                elem.style[t.n] = t.v
              })
            }
            if (v.log) {
              console.error('### App Parser ### ', v.log)
            }
          }
        })
      } else if (key === 'class') {
        elem.classList = value.split(/\s+/)
      } else if (key === 'id') {
        elem.id = value
      } else if (key.match(/^(on|@)/)) {
        const eventName = key.replace(/^(on|@)/, '')
        if (eventName) {
          elem.events[eventName] = value
        }
      } else {
        elem.attr[key] = value
      }
    })
  }

  // 处理子节点（children数据扁平化处理）
  function textElement(parent, value) {
    return {
      type: parent === 'text' ? 'span' : 'text',
      attr: {
        value: $stringify(value)
      }
    }
  }

  // 扁平化
  function flatArray(arr, output) {
    arr.forEach(item => {
      if (Array.isArray(item)) {
        flatArray(item, output)
      } else {
        if (type === 'span' || type === 'a') {
          output.attr.value = item
        } else {
          output.children.push(isJsxNode(item) ? item : textElement(type, item))
        }
      }
    })
  }

  // 是否为jsx节点
  function isJsxNode(item) {
    return $typeof(item) === 'object' && item.__jsx__
  }

  if (children && children.length) {
    children.forEach(item => {
      if (Array.isArray(item)) {
        flatArray(item, elem)
      } else if (item != null) {
        // item可能为空
        if (type === 'span' || type === 'a') {
          elem.attr.value = $stringify(item)
        } else {
          elem.children.push(isJsxNode(item) ? item : textElement(type, item))
        }
      }
    })
  }

  return elem
}

/**
 * 将回调函数加入 vm 的 nextTickCallbacks中
 * @param  {function} cb, 回调函数
 */
function $nextTick(cb) {
  if (!isFunction(cb)) {
    console.warn('### App Framework ### $nextTick函数仅支持函数类型的参数')
  } else if (this._page && this._page.nextTickCallbacks) {
    // 缓存当前组件的 vm 实例
    cb._vm = this
    this._page.nextTickCallbacks.push(cb)
  }
}

const methods = {
  $child,
  $vm,
  $element,
  $rootElement,
  $config,
  $createElement,
  $extend,
  $stringify,
  $nextTick
}

// 給Vm原型注册方法
XVm.registerMethods(methods)

export default framework
