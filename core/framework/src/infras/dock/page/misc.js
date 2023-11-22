/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { APP_KEYS } from 'src/shared/events'

import { $typeof, uniqueCallbackId, $camelize } from 'src/shared/util'
import { updatePageActions } from '../../../dsls/xvm/page/misc'
import { xInvokeWithErrorHandling } from 'src/shared/error'

import config from '../config'

/**
 * 重新创建页面
 * @param page
 */
function recreatePage(page) {
  console.log(`### App Framework ### 重建页面(${page.id})----`)

  if (!page.$valid) {
    console.error(`### App Framework ### 页面(${page.id})缺少dom数据`)
    return new Error(`recreatePage: 无效数据`)
  }

  // 遍历doc, 生成Action
  config.runtime.helper.recreateDocument(page.doc)
  page.doc.listener.createFinish()
}

/**
 * 销毁页面
 * @param  {object} page
 */
function destroyPage(page) {
  console.log(`### App Framework ### 销毁页面 ${page.id} ----`)

  page.intent = null
  page.name = null
  config.runtime.helper.destroyTagNode(page.doc)
  page.doc = null

  page._valid = false
  page._visible = false
  // 父类
  page.destroy()

  console.trace(`### App Framework ### 成功销毁页面(${page.id})----`)
}

/**
 * 获取Body的JSON对象
 * @param page
 * @returns {{}}
 */
function getRootElement(page) {
  const doc = page.doc || {}
  const body = doc.body || {}
  return config.runtime.helper.getNodeAsJSON(body)
}

/**
 * Dom操作
 * @param page 页面对象
 * @param ref
 * @param type
 * @param e
 * @param domChanges 当前仅有attr的更新
 * @returns {*}
 */
function fireEvent(page, ref, type, e, domChanges) {
  if (!page.$valid) {
    return new Error(`fireEvent: 只有Page才能发送Dom事件`)
  }

  console.trace(
    `### App Framework ### 向页面(${page.id}) 的节点(${ref})发送事件 "${type}"---- ${JSON.stringify(
      domChanges
    )}`
  )

  if (Array.isArray(ref)) {
    ref.some(ref => {
      return fireEvent(page, ref, type, e) !== false
    })
    return
  }

  const result = config.publish(APP_KEYS.fireEvent, [page, ref, type, e, domChanges])

  return result
}

/**
 * 根据函数id执行回调函数
 * @param inst
 * @param callbackId
 * @param data
 * @param preserved 是否保留
 * @returns {Error}
 */
function callback(inst, callbackId, args = [], preserved) {
  console.trace(
    `### App Framework ### 调用页面(${inst.id})的回调(${callbackId}) 参数：`,
    JSON.stringify(args)
  )

  // 当页面切换可能导致无法找到回调函数
  if (!inst._callbacks) {
    return new Error(`invoke: 回调函数 "${callbackId}" 没有注册`)
  }

  if (!inst.$valid) {
    console.error(
      `### App Framework ### invoke: 回调函数所属对象(${inst.id})已经无效, 终止回调执行`
    )
    return
  }

  const callback = inst._callbacks[callbackId]
  if (typeof callback === 'function') {
    const { vm, info } = callback.cbErrorData || {}
    const errInfo = `${info || 'callback error:'} "${callback}"`
    const _app = inst.app || {}

    // 执行回调
    const ret = xInvokeWithErrorHandling(callback, undefined, [...args], vm, errInfo, _app, inst)

    // 如果是定时器函数，则保留；否则清除（只使用一次）
    if (typeof preserved === 'undefined' || preserved === false) {
      delete inst._callbacks[callbackId]
    }

    // 如果是页面对象
    if (inst.doc) {
      config.publish(APP_KEYS.callbackDone, [inst])
    }
    return ret
  } else {
    return new Error(`callback: 无效回调函数Id "${callbackId}"`)
  }
}

/**
 * 参数转换，全部转为基础类型
 * @param v
 * @param app
 * @param cbErrorData 捕获回调错误时所需数据
 * @returns {*}
 */
function normalize(v, page, cbErrorData = {}) {
  const type = $typeof(v)

  switch (type) {
    case 'undefined':
    case 'null':
      return '' // 空字符串
    case 'regexp':
      return v.toString() // 正则表达式
    case 'date':
      return v.toISOString() // 日期
    case 'number':
    case 'string':
    case 'boolean':
    case 'array':
      return v
    case 'object':
      if (v.nodeType) {
        // 如果是节点则返回索引
        return v.ref
      }
      const newArgs = {}
      for (const k in v) {
        newArgs[k] = normalize(v[k], page, cbErrorData)
      }
      return newArgs
    case 'function':
    case 'asyncfunction':
      const cbId = uniqueCallbackId()
      if (!page._callbacks) {
        console.trace(`### App Framework ### normalize() 页面已经销毁，不再注册回调`)
      } else {
        v.cbErrorData = cbErrorData
        page._callbacks[cbId] = v
      }
      return cbId.toString()
    default:
      return JSON.stringify(v) // 其余一律转换为字符串
  }
}

/**
 * 获取节点的匹配到的样式定义
 * @param page
 * @param elementRef
 * @return {*}
 */
function getElementStyles(page, elementRef) {
  let result
  const element = config.runtime.helper.getDocumentNodeByRef(page.doc, elementRef)
  if (element) {
    // 复制一份
    const cssRuleList = config.runtime.helper.getElementMatchedCssRuleList(element)
    // 区分：内联，其它
    result = {
      inlineStyle: cssRuleList.shift(),
      matchedCSSRules: cssRuleList
    }
    return result
  } else {
    result = new Error(`getElementStyles: 无效节点ref "${elementRef}"`)
  }
  return result
}

/**
 * 更新节点的样式
 * @param page
 * @param elementRef
 * @param ruleName
 * @param editCssPropertyList {Array} 数组中每个元素格式：{name: '', value: '', disabled: true}
 * @return {*}
 */
function setElementStyles(page, elementRef, ruleName, editCssPropertyList) {
  let result
  const element = config.runtime.helper.getDocumentNodeByRef(page.doc, elementRef)
  if (element) {
    // 转换
    for (let i = 0, len = editCssPropertyList.length; i < len; i++) {
      const editCssPropItem = editCssPropertyList[i]
      editCssPropItem.name = $camelize(editCssPropItem.name)
    }

    result = config.runtime.helper.setElementMatchedCssRule(element, ruleName, editCssPropertyList)
    page.doc.listener.updateFinish()
  } else {
    result = new Error(`setPageElementStyles: 无效节点ref "${elementRef}"`)
  }
  return result
}

/**
 * 更新节点的属性
 * @param page
 * @param elementRef
 * @param actionList {Array} 数组中每个元素格式：{action: '', name: '', value: ''}
 * @return {*}
 */
function setElementAttrs(page, elementRef, actionList) {
  let result
  const element = config.runtime.helper.getDocumentNodeByRef(page.doc, elementRef)
  if (element) {
    // 转换
    for (let i = 0, len = actionList.length; i < len; i++) {
      const actionItem = actionList[i]
      actionItem.name = $camelize(actionItem.name.trim())

      if (actionItem.name === '') {
        // 空串跳过
      } else if (actionItem.name === 'style') {
        // 解析字符串
        const editList = actionItem.value
          .split(';')
          .map(str => {
            const kv = str.split(':')
            return {
              name: $camelize(kv[0].trim()),
              value: kv[1]
            }
          })
          .filter(obj => obj.name !== '')

        // 提取禁用的样式
        const styleHashNew = {}
        for (let i = 0, len = editList.length; i < len; i++) {
          const editItem = editList[i]

          if (!editItem.disabled) {
            styleHashNew[editItem.name] = editItem.value
          }
        }
        config.runtime.helper.setElementStyles(element, styleHashNew)
      } else {
        config.runtime.helper.setElementAttr(element, actionItem.name, actionItem.value)
      }
    }

    page.doc.listener.updateFinish()
  } else {
    result = new Error(`setElementAttrs: 无效节点ref "${elementRef}"`)
  }
  return result
}

/**
 * 根据原始数据生成节点片段
 * @param rawData
 * @param rawData.nodeType {number}
 * @param rawData.tagName {string}
 * @param rawData.attr {object}
 * @param rawData.style {object}
 * @param rawData.children {Array}
 * @param document
 */
function compileFragmentData(rawData, document) {
  console.trace(`### App Runtime ### 编译节点：${JSON.stringify(rawData)}`)

  if (rawData.hasOwnProperty('length')) {
    const nodeList = []
    for (let i = 0, len = rawData.length; i < len; i++) {
      const rawDataChild = rawData[i]
      const childNode = compileFragmentData(rawDataChild, document)
      childNode && nodeList.push(childNode)
    }
    return nodeList
  } else if (!rawData.nodeType || rawData.nodeType === document.NodeType.ELEMENT) {
    const node = document.createElement(rawData.type)
    // 属性
    for (const key in rawData.attr) {
      config.runtime.helper.setElementAttr(node, key, rawData.attr[key])
    }
    // class
    if (rawData.classList && rawData.classList.length) {
      config.runtime.helper.setElementAttr(node, 'class', rawData.classList.join(' '))
    }
    // 样式
    for (const key in rawData.style) {
      config.runtime.helper.setElementStyle(node, key, rawData.style[key])
    }
    // 孩子
    if (rawData.children) {
      for (let i = 0, len = rawData.children.length; i < len; i++) {
        const rawDataChild = rawData.children[i]
        const childNode = compileFragmentData(rawDataChild, document)
        childNode && node.appendChild(childNode)
      }
    }
    return node
  } else {
    console.warn(`### App Runtime ### 不支持插入的节点类型：${rawData.nodeType}`)
  }
}

/**
 * 处理页面 nextTickCallbacks
 * @param page 页面
 */
function processNextTickCallbacks(page) {
  if (!page || !page.nextTickCallbacks) return

  // 获取当前页面根Vm的回调数组
  const cbArr = page.nextTickCallbacks.slice(0)
  // 清空回调事件的数组
  page.nextTickCallbacks.length = 0
  // 执行回调函数
  for (const cb of cbArr) {
    const vm = cb._vm

    xInvokeWithErrorHandling(cb, vm, null, vm, `callback for nextTick "${cb}"`)

    console.trace(`### App Framework ### XExecutor 正在执行nextTick回调函数`)
    updatePageActions(page)
  }
}

/**
 * 处理节点自定义指令回调
 * @param page 页面实例
 * @param hookType 触发结构钩子类型
 * @param args 客户端回调的参数
 */
function processCustomDirectiveCallback(page, hookType, args) {
  const { ref, valueType, key, newValue, oldValue } = args

  let callback
  switch (hookType) {
    case 'nodeMounted':
      callback = 'mounted'
      break
    case 'nodeUpdate':
      callback = 'update'
      break
    case 'nodeDestroy':
      callback = 'destroy'
      break
  }
  // 只处理nodeMounted、nodeUpdate、nodeDestroy钩子
  if (!callback) return

  const directivesContext = page.vm._directivesContext
  // 从页面自定义指令上下文列表中得到对应节点的自定义指令上下文
  const nodeDirContext = directivesContext[ref]
  // 节点不存在指令上下文则跳过
  if (!nodeDirContext) return

  // 节点的自定义指令列表
  const nodeDirs = Object.keys(nodeDirContext)

  // 遍历触发节点自定义指令回调
  for (let i = 0; i < nodeDirs.length; i++) {
    // 指令名称
    const dirName = nodeDirs[i]
    // 节点指令所在的vm
    const nodeDirVm = nodeDirContext[dirName]
    // 取出节点在vm上对应的指令信息
    const nodeDir = nodeDirVm._directives[dirName]
    const curCb = nodeDir[callback]
    // 不存在对应指令 或 不存在对应指令回调则跳过
    if (!nodeDir || !curCb) continue

    let binding
    const element = config.runtime.helper.getDocumentNodeByRef(page.doc, ref)
    if (element) {
      const nodeDir = element._directives.find(dir => dir.name === dirName)
      // 指令名称
      binding = { name: nodeDir.name }

      // 为指令绑定的data添加getter
      Object.defineProperty(binding, 'data', {
        enumerable: true,
        configurable: false,
        get() {
          // 如果绑定的data为动态变量时，每次从watcher中取最新值，否则直接返回绑定值
          return nodeDir.useDynamic && nodeDir.value.get ? nodeDir.value.get() : nodeDir.value
        }
      })
      // 节点更新时增加更新相关参数
      if (hookType === 'nodeUpdate') {
        binding.key = key
        binding.type = valueType
        binding.newValue = newValue
        binding.oldValue = oldValue
      }
    }

    if (typeof curCb === 'function') {
      // 触发节点自定义指令回调
      xInvokeWithErrorHandling(
        curCb,
        nodeDirVm,
        [element, binding],
        nodeDirVm,
        `dir:${dirName} "${callback}" hook`
      )
      // 自定义指令回调执行结束后，发送更新结束标识
      page.doc.listener.updateFinish()
    } else {
      console.warn(`"${callback}" hook must be a function`)
    }
  }
}

export {
  recreatePage,
  getRootElement,
  fireEvent,
  callback,
  destroyPage,
  normalize,
  getElementStyles,
  setElementStyles,
  setElementAttrs,
  compileFragmentData,
  processNextTickCallbacks,
  processCustomDirectiveCallback
}
