/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $camelize, $typeof, $hyphenate, isPlainObject } from 'src/shared/util'

import { $bind, $proxy, getType, assertType, getTypeIndex, isReservedAttr } from '../util'

import { defineReactive } from './observer'
import XWatcher from './watcher'
import context from '../context'

import { nodeAddEventListener, updateNodeProperties } from './dom'

import { destroyVm } from '../page/misc'

/**
 * 应用原生组件的option
 * @param template
 */
function applyNativeComponentOptions(template) {
  // 根据模板类型，找到原生组件的option
  const { type } = template
  const options = context.quickapp.runtime.helper.getComponentDefaultOptions(type)

  // 如果option是对象，则提取option属性加入template中
  // 如果template中属性已经存在则跳过
  if (typeof options === 'object') {
    for (const key in options) {
      if (template[key] == null) {
        template[key] = options[key]
      }
      // 如果属性值是对象，则继续
      // TODO： 改为递归处理（目前暂时支持2层）
      else if ($typeof(template[key]) === 'object' && $typeof(options[key]) === 'object') {
        for (const subkey in options[key]) {
          if (template[key][subkey] == null) {
            template[key][subkey] = options[key][subkey]
          }
        }
      }
    }
  }
}

/**
 * 绑定属性、事件等给元素节点
 * @param vm
 * @param el
 * @param template
 */
function bindElement(vm, el, template) {
  // 如果template节点有id的属性的话，在vm._ids中建立映射表
  setVmId(vm, el, template.attr.id, vm)
  setAttr(vm, el, template.attr)
  setAttrClass(vm, el, template.classList)
  setExternalClasses(vm, el, template.classList)
  setAttrStyle(vm, el, template.style)
  setCustomDirectives(vm, el, template)

  bindEvents(vm, el, template.events) // 绑定事件
}

/**
 *
 * @param vm
 * @param subVm
 * @param template
 * @param repeatItem for循环定义
 */
function bindSubVm(vm, subVm, template, repeatItem) {
  console.trace(`### App Framework ### 处理自定义组件 ----`, JSON.stringify(template))

  subVm = subVm || {}
  template = template || {}

  // 组件的定义信息，包含<template><style><script>部分
  const options = subVm._options || {}

  mergeEvents(template.events, vm, subVm)

  if (template.attr.hasOwnProperty('$listeners')) {
    vm._$listeners.forEach(_listener => {
      mergeListeners(_listener, vm, subVm)
    })
    // delete template.attr.$listeners
  }

  mergeExternalClasses(options, template, subVm, vm)

  const repeatItemAttr = isPlainObject(repeatItem) ? repeatItem : {}
  Object.assign(subVm._attrs, template.attr || {}, repeatItemAttr)

  if (template.attr.hasOwnProperty('$attrs')) {
    // 各层级组件有重名变量时
    for (const key in subVm._attrs) {
      if (vm.__$attrs__[key]) {
        const targetAttr = {
          targetFunction: subVm._attrs[key],
          targetVm: vm
        }
        vm.__$attrs__[key] = targetAttr
      }
    }
    Object.assign(subVm._attrs, vm.__$attrs__)
    delete subVm._attrs.$attrs
  }

  mergeProps(subVm._attrs, options.props, vm, subVm)
  mergeAttrs(subVm._attrs, options.props, vm, subVm)
}

/**
 * 在_externalClasses中保存父组件中样式定义
 * @param {Object} options
 * @param {Object} template
 * @param {Object} subVm
 * @param {Object} vm
 * @returns
 */
function mergeExternalClasses(options, template, subVm, vm) {
  const subExternalClassesDefine = options.externalClasses
  const styleDefine = vm._options && vm._options.style
  if (!subExternalClassesDefine || !styleDefine) {
    return
  }

  if (!Array.isArray(subExternalClassesDefine)) {
    console.error(`### App Framework ### 组件 ${subVm._type} 选项 externalClasses 必须为数组类型`)
    return
  }

  subVm._externalClasses = {}
  subExternalClassesDefine.forEach(className => {
    const attrName = $camelize(className)
    const isProps = options.props && options.props[attrName]
    const value = template.attr && template.attr[attrName]
    // props中接收的值，此处不能再使用
    if (!isProps && value) {
      const classSelector = '.' + className
      if (typeof value === 'function') {
        // 传递样式为变量，响应式更新
        ;(function() {
          const watcher = watch(vm, value, function(v) {
            assignExternalClasses(v, classSelector, subVm, vm)
          })
          subVm._parentWatchers.push(watcher)
          assignExternalClasses(watcher.value, classSelector, subVm, vm)
        })()
      } else {
        assignExternalClasses(value, classSelector, subVm, vm)
      }
      defineReactive(subVm._externalClasses, classSelector, subVm._externalClasses[classSelector])
    }
  })

  /**
   * 将父组件中样式定义传递给自定义组件，样式重复则后定义的优先
   * @param {String} value 自定义组件<comp xxx="yyy">中yyy部分
   * @param {String} classSelector 自定义组件<comp xxx="yyy">中xxx部分
   * @param {Object} subExtClassesVal 自定义组件_externalClasses样式记录
   * @param {Object} vm 父组件vm
   */
  function assignExternalClasses(value, classSelector, subVm, vm) {
    // 拿到父组件中样式定义顺序，样式重复则以后定义的为准
    const classArr = value.trim().split(' ')
    const styleArr = Object.getOwnPropertyNames(styleDefine)
    let idx = -1
    let curIdx
    const subExtClassesVal = {}
    classArr.forEach(val => {
      const selector = '.' + val
      const styleVal = styleDefine[selector]
      if (vm._externalClasses && vm._externalClasses[selector]) {
        // 高阶组件定义了externalClasses则一直往下传，并响应式更新
        const calc = () => {
          return vm._externalClasses[selector]
        }
        watch(vm, calc, function(v) {
          const newSubExtClassesVal = {}
          Object.assign(newSubExtClassesVal, vm._externalClasses[selector])
          subVm._externalClasses[classSelector] = newSubExtClassesVal
        })
        Object.assign(subExtClassesVal, vm._externalClasses[selector])
      } else if (styleVal) {
        curIdx = styleArr.indexOf(selector)
        Object.keys(styleVal).forEach(classObj => {
          if (!subExtClassesVal[classObj] || curIdx > idx) {
            subExtClassesVal[classObj] = styleVal[classObj]
          }
        })
        idx = curIdx
      } else {
        console.warn(
          `### App Framework ### 组件 ${subVm._type} 选项 externalClasses 中传递的 ${classSelector}: ${val} 样式在父组件 ${vm._type} 中未找到定义`
        )
      }
    })
    subVm._externalClasses[classSelector] = subExtClassesVal
  }
}

/**
 * 合并样式从vm到subVm
 * @param vm
 * @param subVm
 * @param template
 * @param target
 */
function postBindSubVm(vm, subVm, template, dest = {}) {
  // 将组件实例中定义的class和style属性添加到subvm中
  mergeStyle(template, vm, subVm)

  // 绑定show指令
  if (template.attr && template.attr.hasOwnProperty('show') && subVm._rootElement) {
    bindDir(vm, subVm._rootElement, 'attr', { show: template.attr.show })
  }

  // dest是subVm的父doc节点
  if (dest.childNodes) {
    // 如果dest有子节点，则subVm对应的doc节点是队列的末尾
    dest.childNodes[dest.childNodes.length - 1]._vm = subVm
  } else {
    dest._vm = subVm
  }
}

/**
 * 将<template>中定义的属性绑定到subvm上
 * @param attrs
 * @param props
 * @param vm
 * @param subVm
 */
function mergeProps(attrs, props, vm, subVm) {
  if (!attrs) {
    return
  }
  if (!props) {
    if (Object.keys(attrs).length !== 0) {
      console.warn(
        `### App Framework ### 组件${subVm._type}中无props属性，放弃属性校验；推荐增加props属性`
      )
    }
    for (const key in attrs) {
      let targetFunction
      if (attrs[key] && attrs[key].targetFunction) {
        targetFunction =
          typeof attrs[key].targetFunction === 'function'
            ? attrs[key].targetFunction.bind(attrs[key].targetVm)
            : attrs[key].targetFunction
      } else {
        targetFunction = attrs[key]
      }
      initProps(key, targetFunction, vm, subVm, false)
    }
  } else {
    for (const key in props) {
      const absent = !attrs.hasOwnProperty(key)
      let targetFunction
      if (attrs[key] && attrs[key].targetFunction) {
        targetFunction =
          typeof attrs[key].targetFunction === 'function'
            ? attrs[key].targetFunction.bind(attrs[key].targetVm)
            : attrs[key].targetFunction
      } else {
        targetFunction = attrs[key]
      }
      initProps(key, targetFunction, vm, subVm, absent, props[key])
    }
  }
}

/**
 * 初始化props值
 * @param key
 * @param value
 * @param vm
 * @param subVm
 */
function initProps(key, value, vm, subVm, absent, prop) {
  const _props = subVm._props

  if (isReservedAttr(key)) {
    console.warn(
      `### App Framework ### 组件${subVm._type}中属性 '${key}' 是保留字, 可能会导致应用运行异常`
    )
  }
  if (typeof value === 'function') {
    ;(function() {
      // 标识：更新子组件
      let isUpdatingChildComponent = false
      // 如果属性是函数，则添加watch，当属性改变时，修改当前vm的属性值
      const watcher = watch(vm, value, function(v) {
        isUpdatingChildComponent = true
        _props[key] = validateProp(key, v, absent, prop, subVm)
        isUpdatingChildComponent = false
      })
      subVm._parentWatchers.push(watcher)

      const watcherValue = validateProp(key, watcher.value, absent, prop, subVm)
      defineReactive(_props, key, watcherValue, () => {
        if (!isUpdatingChildComponent) {
          console.error(
            `### App Framework ### 组件${subVm._type}禁止修改props中的：${key}!如需改变，请额外在data中使用另一个名称声明`
          )
        }
      })
    })()
  } else {
    value = validateProp(key, value, absent, prop, subVm)
    defineReactive(_props, key, value, () => {
      console.error(
        `### App Framework ### 组件${subVm._type}禁止修改props中的：${key}!如需改变，请额外在data中使用另一个名称声明`
      )
    })
  }
  // 代理props
  $proxy(subVm, '_props', key)
}

/**
 * 验证prop
 * @param key
 * @param value
 * @param absent
 * @param prop
 * @returns {*}
 */
function validateProp(key, value, absent, prop, subVm) {
  if (prop === undefined) {
    return value
  }
  // boolean casting
  const booleanIndex = getTypeIndex(Boolean, prop.type)
  if (booleanIndex > -1) {
    if (absent && !prop.hasOwnProperty('default')) {
      value = false
    } else if (value === '' || value === $hyphenate(key)) {
      // only cast empty string / same name to boolean if
      // boolean has higher priority
      const stringIndex = getTypeIndex(String, prop.type)
      if (stringIndex < 0 || booleanIndex < stringIndex) {
        value = true
      }
    }
  }
  // check default value
  if (value === undefined && prop.hasOwnProperty('default')) {
    if (typeof prop.default === 'function' && getType(prop.type) !== 'Function') {
      value = prop.default.call(subVm)
    } else {
      value = prop.default
    }
  }

  assertProp(key, value, absent, prop, subVm)
  return value
}

/**
 * 验证prop是否有效
 * @param value
 * @param prop
 */
function assertProp(key, value, absent, prop, subVm) {
  if (!prop.required && value == null) {
    return
  }
  if (prop.required && absent) {
    console.error(`### App Framework ### 组件${subVm._type} props中的：${key} 是必填字段`)
    return
  }

  let type = prop.type
  let valid = !type
  const expectedTypes = []

  if (type) {
    if (!Array.isArray(type)) {
      type = [type]
    }
    for (let i = 0; i < type.length && !valid; i++) {
      const assertedType = assertType(value, type[i])
      expectedTypes.push(assertedType.expectedType)
      valid = assertedType.valid
    }
  }
  if (!valid) {
    console.error(
      `### App Framework ### 组件${
        subVm._type
      } props中的：${key} type类型验证失败，期望类型为${expectedTypes.join(', ')}`
    )
    return
  }

  const validator = prop.validator
  if (validator && typeof validator === 'function' && !validator(value)) {
    console.error(`### App Framework ### 组件${subVm._type} props中的：${key} validator验证失败`)
  }
}

/**
 * 将<template>中定义的未被props接收的属性绑定到$attrs上
 * @param attrs
 * @param props
 * @param vm
 * @param subVm
 */
function mergeAttrs(attrs, props, vm, subVm) {
  if (!attrs) {
    return
  }
  const reservedAttr = ['id', 'tid']
  for (const key in attrs) {
    // props 未定义或 props 已定义但不包含该变量
    if ((!props || !props.hasOwnProperty(key)) && reservedAttr.indexOf(key) === -1) {
      // targetVm 和 targetFunction 用来确保响应式数据更新正确
      let targetFunction
      if (attrs[key] && attrs[key].targetFunction) {
        targetFunction =
          typeof attrs[key].targetFunction === 'function'
            ? attrs[key].targetFunction.bind(attrs[key].targetVm)
            : attrs[key].targetFunction
      } else {
        targetFunction = attrs[key]
      }
      initAttrs(key, targetFunction, vm, subVm, props)
    }
  }
}

/**
 * 初始化$attrs值
 * @param key
 * @param value
 * @param vm
 * @param subVm
 */
function initAttrs(key, value, vm, subVm, props) {
  const targetAttr = {
    targetFunction: value,
    targetVm: vm
  }
  subVm.__$attrs__[key] = targetAttr
  subVm._$attrs[key] = value
  const _$attrs = subVm._$attrs

  if (isReservedAttr(key)) {
    console.warn(
      `### App Framework ### 组件 ${subVm._type} 中属性 '${key}' 是保留字, 可能会导致应用运行异常`
    )
  }
  if (typeof value === 'function') {
    try {
      ;(function() {
        const watcher = watch(vm, value, function(v) {
          _$attrs[key] = v
        })
        subVm._parentWatchers.push(watcher)
        defineReactive(_$attrs, key, watcher.value)
      })()
    } catch (err) {
      console.error(`### App Framework ### ${err}`)
    }
  } else {
    defineReactive(_$attrs, key, value)
  }
}

/**
 * 将<template>组件实例的內联样式付给subVm对应的doc节点(覆盖旧值)
 * @param template
 * @param vm
 * @param subVm
 */
function mergeStyle(template, vm, subVm) {
  mergeAttrClass(template.classList, vm, subVm)
  mergeInlineStyle(template.style, vm, subVm)
}

/**
 * 绑定事件：响应父组件方法
 * @param events
 * @param vm
 * @param subVm
 */
function mergeEvents(events, vm, subVm) {
  if (events) {
    subVm._$listeners.push({
      id: vm.__id__,
      events: events
    })
  }

  for (const key in events) {
    const parentMethodName = events[key]
    subVm.$on($camelize(key), function(...args) {
      let res
      if (vm && vm[parentMethodName] && typeof vm[parentMethodName] === 'function') {
        res = vm[parentMethodName](...args)
      } else if (typeof parentMethodName === 'function') {
        res = parentMethodName.apply(vm, args)
      } else {
        console.warn(
          `### App Framework ### 子组件: ${subVm._type} 绑定了父组件不存在的方法：'${parentMethodName}'`
        )
      }
      return res
    })
  }
}

/**
 * 绑定事件：响应除父组件外的高阶组件方法
 * @param _listener 对应vm.__id__的组件中绑定的事件
 * @param vm
 * @param subVm
 */
function mergeListeners(_listener, vm, subVm) {
  subVm._$listeners.push(_listener)

  const events = _listener.events
  const vmId = _listener.id
  for (const key in events) {
    const parentMethodName = events[key]
    subVm.$on($camelize(key), function(...args) {
      let curVm = vm
      let hasMethod = false
      // 保证冒泡向上触发各vm的同名事件。使用__id__保证精准触发对应vm的事件
      while ((curVm = curVm._parent)) {
        if (
          curVm[parentMethodName] &&
          typeof curVm[parentMethodName] === 'function' &&
          vmId === curVm.__id__
        ) {
          curVm[parentMethodName](...args)
          hasMethod = true
        } else if (typeof parentMethodName === 'function' && vmId === curVm.__id__) {
          parentMethodName.apply(curVm, args)
          hasMethod = true
        }
      }
      // 冒泡至最上层节点仍未触发对应方法
      if (!hasMethod) {
        console.warn(
          `### App Framework ### 子组件: ${subVm._type} 绑定了高阶组件不存在的方法：'${parentMethodName}'`
        )
      }
    })
  }
}

/**
 * 将<template>组件实例的內联样式付给subVm对应的doc节点(覆盖旧值)
 * @param target
 * @param vm
 * @param subVm
 */
function mergeInlineStyle(target, vm, subVm) {
  if (subVm._rootElement) {
    return
  }

  for (const key in target) {
    const value = target[key]
    if (typeof value === 'function') {
      // 如果是函数
      const watcher = watch(vm, value, function(v) {
        if (subVm._rootElement) {
          updateNodeProperties(subVm._rootElement, 'style', key, v)
        }
      })
      subVm._parentWatchers.push(watcher)
      const watcherValue = watcher.value
      updateNodeProperties(subVm._rootElement, 'style', key, watcherValue)
    } else {
      updateNodeProperties(subVm._rootElement, 'style', key, value)
    }
  }
}

/**
 * 将<template>组件实例的class样式付给subVm对应的doc节点(覆盖旧值)
 * @param classList
 * @param vm
 * @param subVm
 */
function mergeAttrClass(classList, vm, subVm) {
  if (!subVm._rootElement) {
    // 确保subVm对应的doc节点已经创建
    return
  }

  if (!classList) {
    return
  }

  const el = subVm._rootElement

  // 必须是数组或函数
  if (typeof classList === 'function') {
    const watcher = watch(vm, classList, v => {
      updateNodeProperties(el, 'attr', 'class', v.join(' '))
    })
    subVm._parentWatchers.push(watcher)
    const watcherValue = watcher.value

    console.warn(
      `### App Framework ### 自定义组件，设置了class属性：${JSON.stringify(
        classList
      )}，使用父组件样式`
    )
    updateNodeProperties(el, 'prop', '_useParentStyle', true)
    updateNodeProperties(el, 'attr', 'class', watcherValue.join(' '))
  } else if (classList != null) {
    if ($typeof(classList) !== 'array') {
      return new Error(`mergeClassStyle: classList的类型不是数组----`, classList)
    }

    console.warn(
      `### App Framework ### 自定义组件，设置了class属性：${JSON.stringify(
        classList
      )}，使用父组件样式`
    )
    updateNodeProperties(el, 'prop', '_useParentStyle', true)
    updateNodeProperties(el, 'attr', 'class', classList.join(' '))
  }
}

/**
 * 设置元素Id, 必须唯一
 * @param vm
 * @param el
 * @param id  字符串或者函数
 * @param target
 */
function setVmId(vm, el, id, target) {
  if (!id) {
    return
  }
  // 创建空对象，定义vm,el只读属性
  // 保存每个Id的对应的vm和element
  const map = Object.create(null)
  Object.defineProperties(map, {
    vm: {
      value: target,
      writable: false,
      configurable: false
    },
    element: {
      get: () => el || target._rootElement,
      configurable: false
    }
  })

  if (typeof id === 'function') {
    // 如果id是函数，则执行函数生成Id
    const handler = id
    id = handler.call(vm)
    if (id) {
      vm._ids[id] = map
    }
    // 修改元素Id不会引起渲染刷新
    const watcher = watch(
      vm,
      handler,
      newId => {
        // 旧id记录依然有效，除非被覆盖
        if (newId) {
          vm._ids[newId] = map
        }
      },
      el
    )

    // target是子组件时记录
    if (vm !== target && target._parentWatchers) {
      target._parentWatchers.push(watcher)
    }
  } else if (typeof id === 'string') {
    vm._ids[id] = map
  }
}

/**
 * 给element设置属性
 * @param vm
 * @param el
 * @param attr
 */
function setAttr(vm, el, attr) {
  bindDir(vm, el, 'attr', attr)
}

/**
 * 给element添加class属性
 * @param vm
 * @param el
 * @param classList
 */
function setAttrClass(vm, el, classList) {
  if (typeof classList !== 'function' && !Array.isArray(classList)) {
    // 必须是数组或函数
    return
  }

  // 类型为空
  if (Array.isArray(classList) && !classList.length) {
    return
  }

  if (typeof classList === 'function') {
    const watcher = watch(
      vm,
      classList,
      v => {
        updateNodeProperties(el, 'attr', 'class', v.join(' '))
      },
      el
    )
    const watcherValue = watcher.value
    updateNodeProperties(el, 'attr', 'class', watcherValue.join(' '))
  } else {
    updateNodeProperties(el, 'attr', 'class', classList.join(' '))
  }
}

/**
 * externalClasses赋予的样式值变化时，响应式更新节点
 * @param {Object} vm
 * @param {Object} el
 * @param {Array || String} classList
 * @returns
 */
function setExternalClasses(vm, el, classList) {
  if (
    !vm._externalClasses ||
    !Object.keys(vm._externalClasses).length ||
    typeof classList === 'function'
  ) {
    return
  }

  if (!Array.isArray(classList)) {
    bindExtClasses(vm, el, '.' + classList)
  } else {
    classList.forEach(className => {
      bindExtClasses(vm, el, '.' + className)
    })
  }
}

/**
 * vm._externalClasses中样式值变化时通知节点更新
 * @param {*} vm
 * @param {*} el
 * @param {String} className
 */
function bindExtClasses(vm, el, className) {
  if (vm._externalClasses[className]) {
    const calc = () => {
      return vm._externalClasses[className]
    }
    watch(
      vm,
      calc,
      value => {
        Object.keys(value).forEach(key => {
          updateNodeProperties(el, 'style', key, value[key])
        })
      },
      el
    )
  }
}

/**
 * 给element绑定內联样式
 * @param vm
 * @param el
 * @param style
 */
function setAttrStyle(vm, el, style) {
  if (typeof style === 'function') {
    bindDir(vm, el, 'styles', style)
  } else {
    bindDir(vm, el, 'style', style)
  }
}

/**
 * 给element绑定自定义指令
 * @param vm 节点所在的vm
 * @param el 触发指令的节点
 * @param data 节点描述信息
 */
function setCustomDirectives(vm, el, data) {
  // directives：节点自带的指令信息
  // appendDirectives：页面传递给自定义组件根节点的指令信息
  const { directives, appendDirectives } = data

  // 节点上不存在自定义指令信息则跳过
  if (!directives && !appendDirectives) return

  // 节点上存在自定义指令，则初始化_directives
  el._directives = []

  // 合并节点的directives和appendDirectives
  const allDirs = [].concat(directives, appendDirectives)
  let curVm = vm
  for (let i = 0, len = allDirs.length; i < len; i++) {
    const elDir = allDirs[i]
    if (!elDir) continue

    // 组件根节点处理：如果当前节点是组件的根节点，并且当前vm中没有定义节点的指令信息
    if (el === vm._rootElement && !vm._directives[elDir.name]) {
      // 则取父vm为自定义指令的context
      curVm = vm._parent
    }

    const vmDirs = curVm._directives
    // 节点自定义指令名称在vm上有定义
    if (vmDirs[elDir.name]) {
      const dirInfo = {
        name: elDir.name,
        callbacks: vmDirs[elDir.name]
      }
      if (typeof elDir.value === 'function') {
        // 指令绑定的值为动态变量时：标记useDynamic为true，创建函数观察器，返回当前计算值
        dirInfo.useDynamic = true
        dirInfo.value = watch(curVm, elDir.value, undefined, el)
      } else {
        dirInfo.useDynamic = false
        // 指令绑定的值非动态变量，直接返回
        dirInfo.value = elDir.value
      }
      // 将当前指令信息push到节点_directives中
      el._directives.push(dirInfo)

      const directivesContext = vm._root._directivesContext
      directivesContext[el.ref] = {}
      // 记录当前节点自定义指令的vm上下文
      directivesContext[el.ref][elDir.name] = curVm

      // 为节点绑定原生方法
      context.quickapp.dock.bindComponentMethods(curVm._page || {}, el)
    }
  }
}

/**
 * 绑定事件
 * @param vm
 * @param el
 * @param events
 */
function bindEvents(vm, el, events) {
  if (!events) {
    return
  }

  const typeList = Object.keys(events)
  let i = typeList.length

  while (i--) {
    const type = typeList[i]
    let handler = events[type]
    if (typeof handler === 'string') {
      // 如果hander为字符串，则从vm的属性中查找对应的handler函数
      // 之前initState函数已经将自定义函数添加到vm中
      handler = vm[handler]
      if (!handler) {
        console.warn(`### App Framework ### 没有找到回调事件 '${type}'`)
      }
    }

    if (typeof handler === 'function') {
      console.trace(`### App Framework ### 绑定回调事件---- ${type}`)
      nodeAddEventListener(el, type, $bind(handler, vm), false)
    } else {
      console.warn(`### App Framework ### 回调事件 '${type}' 必须是函数`)
    }
  }
}

/**
 *
 * @param vm
 * @param el
 * @param name  'attr' 或者 'style'
 * @param data  <template>中定义的属性或者style
 */
function bindDir(vm, el, name, data) {
  if (!data) {
    return
  }

  if (typeof data === 'function') {
    bindKeys(vm, el, name, data)
    return
  }
  // 遍历数据项
  const keys = Object.keys(data)
  let i = keys.length
  while (i--) {
    const key = keys[i]
    const value = data[key]
    // 如果属性值中包含{{}},则在loader时转换为function
    if (typeof value === 'function') {
      bindKey(vm, el, name, key, value)
    } else {
      updateNodeProperties(el, name, key, value)
    }
  }
}

/**
 *
 * @param vm
 * @param el
 * @param name  属性类型 attr/style
 * @param key
 * @param calc  计算key值的函数
 */
function bindKey(vm, el, name, key, calc) {
  // 创建函数观察器，返回当前计算值；回调函数只有计算结果变化时才被调用
  const watcher = watch(
    vm,
    calc,
    value => {
      updateNodeProperties(el, name, key, value)
    },
    el
  )
  const watcherValue = watcher.value

  // 设置初始化值
  updateNodeProperties(el, name, key, watcherValue)
}

/**
 *
 * @param vm
 * @param el
 * @param name  属性类型 styles
 * @param calc  计算key值的函数
 */
function bindKeys(vm, el, name, calc) {
  // 创建函数观察器，返回当前计算值；回调函数只有计算结果变化时才被调用
  const watcher = watch(
    vm,
    calc,
    value => {
      updateNodeProperties(el, name, value)
    },
    el
  )
  const watcherValue = watcher.value

  updateNodeProperties(el, name, watcherValue)
}

/**
 * 观测一个计算函数，如果函数值改变，则执行回调
 * @param vm
 * @param calc 表达式函数
 * @param callback 回调
 * @param node 关联watcher
 * @returns {*} 返回当前计算的结果
 */
function watch(vm, calc, callback, node) {
  const watcher = new XWatcher(vm, calc, function(value, oldValue) {
    // 如果函数计算结果是对象则始终认为值被改变，如果是基本类型则进行比较
    if (typeof value !== 'object' && value === oldValue) {
      // 如果值没改变则直接返回
      return
    }
    // 执行回调
    callback && callback(value)
  })
  bindNodeWatcher(node, watcher)
  // 返回当前calc函数的计算结果
  return watcher
}

/**
 * 节点绑定Watcher
 * @param node
 * @param watcher
 */
function bindNodeWatcher(node, watcher) {
  if (node) {
    node._bindWatcherList = node._bindWatcherList || []
    node._bindWatcherList.push(watcher)
  }
}

/**
 * 节点取消绑定Watcher
 * @param node
 */
function unbindNodeWatcher(node) {
  if (node._bindWatcherList) {
    const nodeWatcherList = node._bindWatcherList
    for (let i = 0, len = nodeWatcherList.length; i < len; i++) {
      const nodeWatcher = nodeWatcherList[i]
      nodeWatcher.close()
    }
    delete node._bindWatcherList
  }
}

/**
 * 节点取消绑定Watcher
 * @param node
 * @param preserved
 */
function unbindNode(node, preserved = false) {
  const childList = (node.childNodes || []).slice()
  for (let i = 0, len = childList.length; i < len; i++) {
    const childNode = childList[i]
    unbindNode(childNode, false)
  }

  if (!preserved) {
    unbindNodeWatcher(node)
    if (node._vm) {
      destroyVm(node._vm)
      delete node._vm
    }
  }
}

export {
  applyNativeComponentOptions,
  bindElement,
  bindSubVm,
  postBindSubVm,
  setVmId,
  watch,
  unbindNode
}
