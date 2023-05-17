/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueCallbackId, $typeof } from 'src/shared/util'

import { removeCustomComponent } from '../app/custom'

import { fireEventWrap, getNodeByRef } from '../vm/dom'

/**
 * 触发DOM事件
 * @param page 页面对象
 * @param ref
 * @param type
 * @param e
 * @param domChanges 当前仅有attr的更新
 * @returns {*}
 */
function fireEvent(page, ref, type, e, domChanges) {
  // 根据ref找到对应的element
  const el = getNodeByRef(page.doc, ref)
  if (el) {
    const result = fireEventWrap(el, type, e, { attr: domChanges })
    return result
  }
  return new Error(`fireEvent: 无效element索引 "${ref}"`)
}

function execPageTasks(page) {
  let hasActions = false

  if (page.doc && page.$valid && page.executor) {
    // 处理任务
    hasActions = page.executor.exec()
  }

  return hasActions
}

/**
 * 更新页面
 * @param page
 */
function updatePageActions(page) {
  // 更新动作队列
  const hasActions = execPageTasks(page)
  // 发送更新结束标识
  if (hasActions) {
    const hasCallbacks = page && page.nextTickCallbacks ? page.nextTickCallbacks.length > 0 : false
    page.doc.listener.updateFinish(null, hasCallbacks)
  }
}

/**
 * 参数转换，全部转为基础类型
 * @param v
 * @param app
 * @returns {*}
 */
function normalize(v, page) {
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
        newArgs[k] = normalize(v[k], page)
      }
      return newArgs
    case 'function':
      const cbId = uniqueCallbackId()
      if (!page._callbacks) {
        console.trace(`### App Framework ### normalize() 页面已经销毁，不再注册回调`)
      } else {
        page._callbacks[cbId] = v
      }
      return cbId.toString()
    default:
      return JSON.stringify(v) // 其余一律转换为字符串
  }
}

function destroyPage(page) {
  // 如果有VM，销毁VM
  if (page.vm) {
    page.vm._destroyVm()
  }

  // 删除自定义组件的定义
  removeCustomComponent(page.app, page)

  page.executor && page.executor.reset()
  page.executor = null
  page.vm = null
}

/**
 * 销毁VM
 * @param {object} vm
 */
function destroyVm(vm) {
  console.trace(`### App Framework ### 销毁VM(${vm.__id__}:${vm._type})`)
  if (vm._destroyed) {
    return
  }

  // 延迟 VM 部分属性的销毁
  vm._root.$on('xlc:onDestroy', function() {
    delete vm._props
    delete vm._data
    delete vm._ids
  })
  vm._emit('xlc:onDestroy')

  // 从父 VM 中删除引用
  if (vm._parent && vm._parent._childrenVms) {
    const childVms = vm._parent._childrenVms
    const childVmIndex = childVms.indexOf(vm)
    if (childVmIndex !== -1) {
      childVms.splice(childVmIndex, 1)
    }
  }

  delete vm._page
  delete vm._methods
  delete vm._options
  delete vm._parent
  delete vm._root
  delete vm._parentContext
  delete vm._attrs
  delete vm._parentElement
  delete vm._rootElement
  delete vm._slot

  // 移除parentWatchers
  if (vm._parentWatchers) {
    let watcherCount = vm._parentWatchers.length
    while (watcherCount--) {
      vm._parentWatchers[watcherCount].close()
    }
    delete vm._parentWatchers
  }

  // 移除watchers
  if (vm._watchers) {
    let watcherCount = vm._watchers.length
    while (watcherCount--) {
      vm._watchers[watcherCount].close()
    }
    delete vm._watchers
  }

  // 递归销毁所有子VM
  if (vm._childrenVms) {
    let vmCount = vm._childrenVms.length
    while (vmCount--) {
      destroyVm(vm._childrenVms[vmCount])
    }
  }

  delete vm._vmEvents
  vm._destroyed = true
}

export { fireEvent, execPageTasks, updatePageActions, normalize, destroyPage, destroyVm }
