/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $camelize, isPlainObject, isReserved, isObject } from 'src/shared/util'

import { $bind, assignObjectInOrder } from '../util'

import context from '../context'

import { requireCustomComponent } from '../app/custom'

import {
  bindElement,
  setVmId,
  watch,
  bindSubVm,
  postBindSubVm,
  applyNativeComponentOptions,
  unbindNode
} from './directive'

import {
  createElement,
  createFragment,
  isNodeInDocument,
  nodeAppendChild,
  nodeAddEventListener,
  removeNode as removeNodeImpl
} from './dom'

import { XObserver } from './observer'

import { xHandleError } from 'src/shared/error'

/**
 * 递归创建vm中的定义
 * @param vm
 */
function build(vm) {
  // _options保存组件的tempalte,style和script定义
  const options = vm._options || {}
  const template = options.template || {}
  try {
    compile(vm, template, vm._parentElement)
  } catch (e) {
    xHandleError(e, vm, 'render')
  }
  console.trace(`### App Framework ### 组件Vm (${vm._type}) UI准备就绪`)
  vm._emit('xlc:onReady')
  vm._ready = true
}

/**
 * 编译组件的template部分，创建对应的element节点
 * @param vm
 * @param target <template>定义
 * @param dest 父element
 * @param meta 上下文信息
 */
function compile(vm, target, dest, meta) {
  const page = vm._page || {}
  const doc = page.doc || {}

  // lastSignal用来记录编译结果，如果等于-1,说明发生错误，停止编译
  if (page.lastSignal === -1) {
    return
  }

  // 兼容原来的template.id写法
  if (target.id) {
    target.attr.id = target.attr.id || target.id
  }

  meta = meta || {}

  // 如果是片段
  if (isFragmentNode(target)) {
    console.trace(`### App Framework ### 编译 fragment 节点 ----`, JSON.stringify(target))
    compileFragment(vm, target, dest, meta)
    return
  }

  // 如果是slot
  if (isSlotNode(target)) {
    console.trace('### App Framework ### 编译 slot 节点----', JSON.stringify(target))
    compileSlot(vm, target, dest)
    return
  }

  // 如果是component
  if (isComponentNode(target)) {
    console.trace('### App Framework ### 编译 component 节点----', JSON.stringify(target))
    compileComponent(vm, target, dest)
    return
  }

  // 如果是block
  if (isBlockNode(target)) {
    console.trace(`### App Framework ### 编译 block 节点 ----`, JSON.stringify(target))
    compileBlock(vm, target, dest, meta)
    return
  }

  // 允许for, if同时出现, 编译时先处理for展开dom, 然后处理if
  if (isForNode(target, meta)) {
    console.trace('### App Framework ### 编译 for 节点----', JSON.stringify(target))
    if (dest === doc.documentElement) {
      console.trace('### App Framework ### 根节点不支持 `for` 指令!')
    } else {
      compileFor(vm, target, dest)
    }
    return
  }

  // 处理带if属性的节点
  if (isIfNode(target, meta)) {
    console.trace('### App Framework ### 编译 If 节点----', JSON.stringify(target))
    if (dest === doc.documentElement) {
      console.trace(`### App Framework ### 根节点不支持 'if' 指令!`)
    } else {
      compileIf(vm, target, dest, meta)
    }
    return
  }

  // 获取当前节点的组件类型
  const type = meta.type || target.type
  // 如果是自定义节点
  const component = isComposedNode(vm, type)
  if (component) {
    compileCustomComponent(vm, component, target, dest, type, meta)
    return
  }

  compileNativeComponent(vm, target, dest, type)
}

/**
 * 是否为block节点，用于for, if逻辑
 * @param target
 * @returns {boolean}
 */
function isBlockNode(target) {
  return target.type === 'block'
}

/**
 * 是否为片段
 * @param target
 * @returns {boolean}
 */
function isFragmentNode(target) {
  return Array.isArray(target)
}

/**
 * 是否为for节点
 * @param target
 * @param meta
 * @returns {boolean|*}
 */

function isForNode(target, meta) {
  return !meta.hasOwnProperty('repeat') && target.repeat
}

/**
 * 是否为If节点(可隐藏)
 * @param target  <template>定义
 * @param meta
 * @returns {boolean|*}
 */
function isIfNode(target, meta) {
  return !meta.hasOwnProperty('shown') && target.shown
}

/**
 * 是否为slot节点, 不能嵌套, 不支持if, for
 * @param target  <template>定义
 * @param meta
 * @returns {boolean|*}
 */
function isSlotNode(target) {
  return target.type === 'slot'
}

/**
 * 是否为component节点, 不能嵌套, 不支持if, for
 * @param {*} target
 */
function isComponentNode(target) {
  return target.type === 'component'
}

/**
 * 是否为richtext节点, 不能嵌套
 * @param target  <template>定义
 * @param meta
 * @returns {boolean|*}
 */
function isRichTextNode(target) {
  return target.type === 'richtext'
}

/**
 * 是否为组合节点
 * @param vm
 * @param type
 * @returns {*}
 */
function isComposedNode(vm, type) {
  let component
  // 所有自定义组件均放入到customComponentMap中
  if (vm._page && vm._page.app) {
    component = requireCustomComponent(vm._page.app, vm._page, type)

    if (typeof component === 'function') {
      component()
      component = requireCustomComponent(vm._page.app, vm._page, type)
    }
  }
  return component
}

/**
 * 编译component节点
 * @param {Object} vm
 * @param {Object} target
 * @param {Object} dest
 */
function compileComponent(vm, target, dest) {
  const fragment = createFragment(vm, dest)
  bindDynamicComponent(vm, target, fragment)
}

/**
 * 动态编译component对应的自定义组件
 * @param {*} vm
 * @param {*} target
 * @param {*} fragment
 */
function bindDynamicComponent(vm, target, fragment) {
  const componentName = watchFragment(vm, fragment, target.is, 'component', value => {
    if (!fragment) {
      return
    }
    removeNode(fragment, true)
    if (!value) {
      console.warn(`### App Framework ### component组件名称未定义`)
      return
    }
    const compTarget = Object.assign({}, target, {
      type: value
    })
    compile(vm, compTarget, fragment)
  })
  if (!componentName) {
    console.warn(`### App Framework ### component组件名称未定义`)
    return
  }
  const compTarget = Object.assign({}, target, {
    type: componentName
  })
  compile(vm, compTarget, fragment)
}

/**
 * 编译slot
 * @param {Object} vm - 组件vm
 * @param {Object} target - slot 节点的内容
 * @param {Object} dest - 父级element
 */
function compileSlot(vm, target, dest) {
  // TODO: 目前仅支持一个无名slot, 以后支持多个slot(通过name区分)
  const fragment = createFragment(vm, dest)
  // 检查slot是否包含content事件
  if (target.attr && target.attr.content) {
    const content = target.attr.content

    let render = null
    if (typeof content === 'string') {
      // 查找render函数
      render = vm[content]
      if (typeof render !== 'function') {
        console.trace(
          `### App Framework ### slot的 content属性 ${content} 有误， 找不到对应的render函数 `
        )
        render = null
      }
    } else if (typeof content === 'function') {
      render = content
    } else {
      console.trace(`### App Framework ### slot的 content属性 ${content} 无效`)
    }

    if (render) {
      // 绑定子节点函数
      target.children = []
      target.__render = render
      bindSlotRender(vm, target, fragment)
    }
  } else if (vm._slot) {
    // 父级Vm或者for指令上下文
    const parentContext = vm._parentContext
    const name = (target.attr && target.attr.name != null && target.attr.name) || 'default'

    const dynamicContent = Object.assign({}, vm._slot.template, {
      children: vm._slot.namedSlotCache[name] || []
    })

    const slotInfo = {
      fragment,
      defaultContent: target,
      dynamicContent
    }
    console.trace(`### App Framework ### 自定义组件${vm._type}, 校验 slot 的内容`)
    validateSlotContent(vm, parentContext, vm._type, slotInfo)
  }
}

/**
 * 校验自定义组件 slot 的内容：动态内容存在直接子节点时，编译动态内容；否则编译默认内容
 * @param {Object} vm - 组件vm
 * @param {Object} parentVm - 父级Vm
 * @param {string} name - 组件名称
 * @param {Object} slotInfo
 * @param {Object} slotInfo.fragment - slot 节点
 * @param {Object} slotInfo.dynamicContent - slot 动态内容
 * @param {Object} slotInfo.defaultContent - slot 默认内容
 */
function validateSlotContent(vm, parentVm, name, slotInfo) {
  const fragment = slotInfo.fragment
  const dynamicContent = slotInfo.dynamicContent
  const defaultContent = slotInfo.defaultContent
  // 动态内容的直接子节点列表
  const dynamicChildren = (dynamicContent && dynamicContent.children) || []

  // 检测动态内容的静态（不含 if 和 for 属性）直接子节点
  const hasStaticChild = dynamicChildren.some(child => {
    return !child.shown && !child.repeat
  })
  if (hasStaticChild) {
    // 动态内容含静态直接子节点，直接使用动态内容
    console.trace(
      `### App Framework ### 自定义组件 ${name} 编译动态内容 - ${JSON.stringify(dynamicContent)}`
    )
    if (parentVm && parentVm._rootElement) {
      // 动态节点使用父组件的样式
      fragment._styleObjectId = parentVm._rootElement._styleObjectId
    }
    compileChildren(parentVm, dynamicContent, fragment)
  } else if (dynamicChildren.length === 0) {
    // 动态内容无直接子节点，直接使用默认内容
    console.trace(
      `### App Framework ### 自定义组件 ${name} 编译默认内容 - ${JSON.stringify(defaultContent)}`
    )
    compileChildren(vm, defaultContent, fragment)
  } else {
    // 动态内容是否存在直接子节点，由"节点 if 和 for 属性绑定的表达式"决定
    const exps = []
    dynamicChildren.forEach((child, index) => {
      // 收集"节点 if 和 for 属性绑定的表达式"
      exps[index] = Object.create(null)
      if (child.repeat) {
        exps[index].for = child.repeat.exp || child.repeat
      }
      if (child.shown) {
        exps[index].if = child.shown
      }
    })
    // 定义观察"节点 if 和 for 属性绑定的表达式"的函数
    const calcExps = function() {
      const hasValidExp = exps.some(exp => {
        let ifResult = true
        let forResult = true
        if (exp.if) {
          ifResult = !!exp.if.call(parentVm)
        }
        if (exp.for) {
          forResult = exp.for.call(parentVm)
          forResult = !forResult || forResult.length > 0
        }
        return ifResult && forResult
      })
      return hasValidExp
    }
    // 通过"观察动态内容直接子节点的 if 和 for 属性绑定的表达式"，观察 slot 的动态内容
    bindSlotContent(vm, parentVm, calcExps, name, slotInfo)
  }
}

/**
 * 观察 slot 的动态内容，当动态内容更新时，更新 slot 内容
 * @param {Object} vm - 组件vm
 * @param {Object} parentVm - 页面vm
 * @param {Object} calc - 判断所有if,for子节点总状态的一个计算函数
 * @param {string} name - 组件名称
 * @param {Object} slotInfo
 * @param {Object} slotInfo.fragment - slot 节点
 * @param {Object} slotInfo.dynamicContent - slot 动态内容
 * @param {Object} slotInfo.defaultContent - slot 默认内容
 */
function bindSlotContent(vm, parentVm, calc, name, slotInfo) {
  const fragment = slotInfo.fragment
  let useDynamic = watchFragment(parentVm, fragment, calc, 'slot', v => {
    if (!fragment) {
      return
    }
    useDynamic = v
    // 更新 slot 内容
    compileSlotContent(vm, parentVm, name, useDynamic, slotInfo)
  })
  // 初始化 slot 内容
  compileSlotContent(vm, parentVm, name, useDynamic, slotInfo)
}

/**
 * 编译 slot 内容
 * @param {Object} vm - 组件vm
 * @param {Object} parentVm - 页面vm
 * @param {string} name - 组件名称
 * @param {boolean} useDynamic - 是否使用动态内容
 * @param {Object} slotInfo
 * @param {Object} slotInfo.fragment - slot 节点
 * @param {Object} slotInfo.dynamicContent - slot 动态内容
 * @param {Object} slotInfo.defaultContent - slot 默认内容
 */
function compileSlotContent(vm, parentVm, name, useDynamic, slotInfo) {
  const fragment = slotInfo.fragment
  const dynamicContent = slotInfo.dynamicContent
  const defaultContent = slotInfo.defaultContent
  if (useDynamic) {
    console.trace(
      `### App Framework ### ${name} 组件 (${fragment.ref}) 编译动态内容 - ${JSON.stringify(
        dynamicContent
      )}`
    )
    removeNode(fragment, true)
    if (parentVm && parentVm._rootElement) {
      // 动态节点使用父组件的样式
      fragment._styleObjectId = parentVm._rootElement._styleObjectId
    }
    compileChildren(parentVm, dynamicContent, fragment)
  } else {
    console.trace(
      `### App Framework ### ${name} 组件 (${fragment.ref}) 编译默认内容 - ${JSON.stringify(
        defaultContent
      )}`
    )
    removeNode(fragment, true)
    // 默认节点删除父组件的样式
    delete fragment._styleObjectId
    compileChildren(vm, defaultContent, fragment)
  }
}
/**
 * 编译block
 * @param vm
 * @param target
 * @param dest
 * @param meta
 */
function compileBlock(vm, target, dest, meta) {
  meta = meta || {}

  if (isForNode(target, meta)) {
    console.trace(`### App Framework ### 编译 block 节点 ---- for`)
    compileFor(vm, target, dest)
    return
  }

  if (isIfNode(target, meta)) {
    console.trace(`### App Framework ### 编译 block 节点 ---- if`)
    compileIf(vm, target, dest, meta)
    return
  }

  // 如果block没有for或者if属性，则看作隐形节点
  const frag = createFragment(vm, dest)
  compileChildren(vm, target, frag)
}

/**
 * 编译片段
 * @param vm
 * @param target
 * @param dest
 * @param meta
 */
function compileFragment(vm, target, dest, meta) {
  const frag = createFragment(vm, dest)
  target.forEach(child => {
    compile(vm, child, frag, meta)
  })
}

/**
 * 编译for节点
 * @param vm
 * @param target <template>定义
 * @param dest 父element
 */
function compileFor(vm, target, dest) {
  const repeat = target.repeat
  // 获取数据源函数
  let getter = repeat.exp || repeat
  // 确保getter是函数，则添加空函数
  if (typeof getter !== 'function') {
    getter = function() {
      return []
    }
  }
  // $idx $item为缺省关键字
  const key = repeat.key || '$idx'
  const value = repeat.value || '$item'
  // 用于跟踪for循环元素的属性名,必须唯一(通过tid属性指定)
  const tid = target.attr && target.attr.tid

  const frag = createFragment(vm, dest)
  frag.data = []
  frag.vms = []

  bindFor(vm, target, frag, { getter, key, value, tid })
}

/**
 * 编译可隐藏节点
 * @param vm
 * @param target  <template>定义
 * @param dest 父element
 * @param meta
 */

function compileIf(vm, target, dest, meta) {
  // 添加标记
  const newMeta = { shown: true }
  const fragment = createFragment(vm, dest)

  // 向下传递repeat上下文, 如果嵌套for, 则内层覆盖外层
  if (meta.hasOwnProperty('repeat')) {
    newMeta.repeat = meta.repeat
  }

  bindIf(vm, target, fragment, newMeta)
}

/**
 * 编译自定义组件（复合）
 * @param vm
 * @param component  组件定义包含template,style,script等信息
 * @param template  <template>组件实例定义
 * @param dest 父doc节点
 * @param type 组件类型
 * @param meta
 */
function compileCustomComponent(vm, component, template, dest, type, meta) {
  const pageAccessTypeList = ['public', 'protected', 'private']
  pageAccessTypeList.forEach(accessType => {
    if (
      component.hasOwnProperty(accessType) &&
      isObject(component[accessType]) &&
      !component.hasOwnProperty('data')
    ) {
      console.warn(
        `### App Framework ### 自定义组件 ${type} 不支持 ${accessType} 数据模型，请使用 data 代替！`
      )
    }
  })

  // 处理组件的自定义指令
  const dirs = template.directives
  if (dirs && dirs.length && component && component.template) {
    component.template.appendDirectives = []
    for (let i = 0, len = dirs.length; i < len; i++) {
      const elDir = dirs[i]
      const vmDirs = vm._directives
      // 在使用自定义组件时，如果节点的自定义指令名称在vm上有定义，则将当前指令信息push到组件根节点的 appendDirectives 数组中
      // 在生成组件vm时会合并组件根节点的 directives 和 appendDirectives
      if (vmDirs && vmDirs[elDir.name]) {
        component.template.appendDirectives.push(elDir)
      }
    }
  }

  if (component && component.props && !component._hasnormalizeProps) {
    component._hasnormalizeProps = true
    // 格式化props信息
    normalizeProps(component, type)
    console.trace(`### App Framework ### 格式化自定义组件 ${type} props`, JSON.stringify(component))
  }

  const Ctor = vm.constructor // Vm构造函数
  console.trace(`### App Framework ### 编译自定义组件 ${type} ----`, JSON.stringify(template))
  // 使用自定义组件定义创建一个vm
  const subVm = new Ctor(
    component,
    type,
    vm,
    dest,
    undefined,
    {
      'xlc:onCreate': function() {
        // 如果父vm是静态，则子vm也是静态
        if (vm._static) {
          this._static = vm._static
        }
        // 如果节点有id属性，则在父vm._ids中注册
        setVmId(vm, null, template.attr.id, this)

        // 设置外部绑定信息
        this._externalBinding = {
          parent: vm,
          template: template // 组件实例定义，例如<aaa class='xxx'/>
        }
        // 当vm创建成功后
        bindSubVm(vm, this, template, meta.repeat)
      },
      'xlc:onInit': function() {},
      'xlc:onReady': function() {}
    },
    template
  )
  postBindSubVm(vm, subVm, template, dest)
}

/**
 * 编译原生组件
 * @param vm  父vm
 * @param template  组件定义
 * @param dest  doc中的父节点
 * @param type 组件类型，例如'div','text'
 */
function compileNativeComponent(vm, template, dest, type) {
  const page = vm._page || {}
  const doc = page.doc || {}
  const app = page.app || {}

  console.trace('### App Framework ### 编译原生组件----', JSON.stringify(template))
  // 获取原生组件的公共配置
  applyNativeComponentOptions(template)

  let element
  if (dest === doc.documentElement) {
    // 如果父节点是根元素，则该组件为Body元素
    console.trace(`### App Framework ### 编译Body组件 ${type}`)
    element = createElement(vm, type)
    element._vm = vm
  } else {
    console.trace(`### App Framework ### 编译原生组件 ${type}`)
    element = createElement(vm, type)
  }

  // element._vm 用于某些场景的判断（可查询 unbindNode 方法）， 涉及 销毁vm 和 触发页面 onDestroy 生命周期的逻辑
  // 所以只有页面或组件的根节点才会携带 _vm （只有页面根节点的 _vm 在 compileNativeComponent 方法内部赋值，组件的在其他地方）
  // 所以不能给每个 node 都赋值 _vm
  // 为了能在每个 node 节点都能访问到 vm，此处另起一个 _xvm 属性, 用来保存 vm
  element._xvm = vm

  template.attr = template.attr || {}

  if (!vm._rootElement) {
    // 设置vm在doc上对应的根元素
    vm._rootElement = element

    // 定义样式到文档中
    const isDocLevel = app._shareDocStyle && dest === doc.documentElement

    let styleObject = {}
    // 合并父组件externalClasses传入的样式
    assignObjectInOrder(styleObject, vm._options && vm._options.style)
    assignObjectInOrder(styleObject, vm._externalClasses)

    styleObject = Object.keys(styleObject).length === 0 ? vm._options.style : styleObject
    context.quickapp.runtime.helper.registerStyleObject(
      vm._type,
      styleObject,
      isDocLevel,
      vm._rootElement
    )

    // 如果顶层元素是自定义组件的话
    const binding = vm._externalBinding || {}
    const target = binding.template
    const parentVm = binding.parent

    if (target && target.events && parentVm && element) {
      for (const type in target.events) {
        const typeVal = target.events[type]
        const handler = typeof typeVal === 'string' ? parentVm[typeVal] : typeVal
        if (handler) {
          nodeAddEventListener(element, type, $bind(handler, parentVm), false)
        } else {
          console.warn(`### App Framework ### 忽略使用自定义组件时的DOM事件绑定：${type}`)
        }
      }
    }
  }

  // 将richtext属性保存为html
  if (isRichTextNode(template)) {
    template.content = template.attr.value
    // 获取template的scene或type属性
    template.contentType = template.attr.scene || template.attr.type
    // 注释：循环遍历richtext时，后面的richtext需要有value模板
    // delete template.attr['value']
  }

  // 将template中定义属性传递给element
  bindElement(vm, element, template)

  // 获取append属性, 'tree'表示组件全部构建完成后添加到父节点上，否则递归创建并添加到父节点上
  if (template.attr && template.attr.append) {
    template.append = template.attr.append
  }

  // 如果是tree模式，则是等整个组件编译完成后整体加入到父节点中
  const treeMode = template.append === 'tree'

  if (page.lastSignal !== -1 && !treeMode) {
    console.trace('### App Framework ### 编译单个节点----', element)
    // attachNode会通过doc的listener向原生发送dom操作通知，如果发送失败则返回-1
    page.lastSignal = nodeAppendChild(dest, element)
  }

  if (page.lastSignal !== -1) {
    // 编译孩子节点
    if (isRichTextNode(template)) {
      // richtext没有子节点
      compileRichText(vm, template, element)
    } else {
      compileChildren(vm, template, element)
    }
  }

  if (page.lastSignal !== -1 && treeMode) {
    console.trace('### App Framework ### 编译整个树----', element)
    page.lastSignal = nodeAppendChild(dest, element)
  }
}

/**
 * 编译孩子节点
 * @param vm
 * @param template  <template>定义
 * @param dest 父element
 */
function compileChildren(vm, template, dest) {
  const page = vm._page || {}
  // 子节点定义
  const children = template.children
  if (children && children.length) {
    // 如果发生错误，则退出循环
    for (let i = 0, len = children.length; i < len && page.lastSignal !== -1; i++) {
      console.trace('### App Framework ### 编译孩子节点----', children[i].type)
      try {
        compile(vm, children[i], dest)
      } catch (e) {
        xHandleError(e, vm, 'render')
      }
    }
  }
}

/**
 * 监听for属性
 * @param vm
 * @param target <template>定义
 * @param frag 片段
 * @param info for描述信息
 */
function bindFor(vm, target, frag, info) {
  const vms = frag.vms
  const { getter, tid } = info
  // key, value名称（如果是数组,则key为索引）
  const keyName = info.key
  const valueName = info.value

  // context代表
  function compileItem(item, index, context) {
    const mergedData = {}
    mergedData[keyName] = index
    mergedData[valueName] = item

    // 创建一个新vm（增加了$idx,$item等属性）
    const newContext = mergeContext(context, mergedData)
    vms.push(newContext)
    // 基于新的vm编译节点（附带关联的数据item）
    compile(newContext, target, frag, { repeat: item })
  }

  let list = watchFragment(vm, frag, getter, 'for', data => {
    if (!frag || !data) {
      return
    }
    console.trace('### App Framework ### For 节点被改变----', data)

    // 如果数据结果发生变化，根据tid进行新旧数据比较，生成尽可能少的differ任务
    const oldChildren = frag.layoutChildren.slice()

    const oldVms = vms.slice()
    const oldData = frag.data.slice()

    // 将数据转换为key-value格式
    const trackMap = {}
    const reusedMap = {}
    data.forEach((item, index) => {
      const key = '@' + (tid ? item[tid] : index)

      if (key == null || key === '@') {
        console.error(`### App Framework ### for 循环数据的tid 属性不存在或为空`)
        return
      }

      if (!trackMap[key]) {
        trackMap[key] = []
      }
      trackMap[key].push(item)
      if (trackMap[key].length > 1) {
        console.warn(
          `### App Framework ### for 循环数据的tid 属性 '${key}' 不唯一, 可能导致性能问题`
        )
      }
    })

    // 遍历旧数据，找出在新数据中出现的元素，放入reuse列表(防止被回收)
    const reusedList = []
    oldData.forEach((item, index) => {
      const key = '@' + (tid ? item[tid] : index)
      const track = trackMap[key]
      if (track && track.length > 0) {
        if (!reusedMap[key]) {
          reusedMap[key] = []
        }
        reusedMap[key].push({
          item,
          index,
          key,
          target: oldChildren[index], // 旧的element对象
          vm: oldVms[index] // 旧的vm对象
        })
        reusedList.push(item)
      } else {
        removeNode(oldChildren[index])
      }
    })

    context.quickapp.runtime.helper.resetNodeChildren(frag)
    vms.length = 0
    frag.data = data.slice()

    data.forEach((item, index) => {
      const key = '@' + (tid ? item[tid] : index)
      const reused = reusedMap[key]
      if (typeof reused === 'object' && reused.length > 0) {
        const reusedItem = reused.shift()
        // 重用已有节点
        const childNode = reusedItem.target
        context.quickapp.runtime.helper.restoreNodeChildren(frag, childNode)

        vms.push(reusedItem.vm)
        // 更新节点的vm中添加保留属性$idx,$item
        reusedItem.vm[valueName] = item
        reusedItem.vm[keyName] = index
      } else {
        // 如果没有重用，则重新编译节点
        compileItem(item, index, vm)
      }
    })

    Object.keys(reusedMap).forEach(key => {
      while (reusedMap[key].length) {
        const restReuseItem = reusedMap[key].shift()
        removeNode(restReuseItem.target)
      }
    })
  })

  // for循环固定值
  if (typeof list === 'number') {
    const ret = new Array(list)
    for (let i = 0; i < list; i++) {
      ret[i] = i + 1
    }
    list = ret
  }

  // 初次展开循环节点
  if (list && list.length > 0) {
    if (!Array.isArray(list)) {
      return
    }
    frag.data = list.slice(0)
    list.forEach((item, index) => {
      compileItem(item, index, vm)
    })
  } else {
    frag.data = []
  }
}

/**
 * 监听if属性，根据值来添加/移除元素
 * @param vm
 * @param target <template>定义
 * @param fragment 所在片段定义
 * @param meta
 */
function bindIf(vm, target, fragment, meta) {
  let value = !!watchFragment(vm, fragment, target.shown, 'if', v => {
    console.trace(`### App Framework ### If 节点 (${fragment.ref})状态改变----`, v)
    // 如果片段的显示状态没有变化，则返回
    if (!fragment || value === !!v) {
      return
    }
    value = !!v
    if (v) {
      try {
        compile(vm, target, fragment, meta)
      } catch (e) {
        xHandleError(e, vm, 'render')
      }
    } else {
      // 针对fragment节点, 仅删除内部子节点
      removeNode(fragment, true)
    }
  })

  if (value) {
    compile(vm, target, fragment, meta)
  }
}

/**
 * JSX模板
 * 采用深度优先, 分层比较, 如果节点类型不一样, 则删除重建，否则更新节点属性;
 * @param vm
 * @param fragment
 * @param value
 */
function compileSlotRender(vm, fragment, value) {
  if (!value) {
    removeNode(fragment, true)
  } else {
    removeNode(fragment, true)
    compile(vm, value, fragment)
  }
}

/**
 * 绑定slot渲染
 * @param vm
 * @param target
 * @param fragment
 */
function bindSlotRender(vm, target, fragment) {
  const content = watchFragment(vm, fragment, target.__render, 'render', value => {
    console.trace(`### App Framework ### Slot 节点 (${fragment.ref})内容更新 ----`)
    compileSlotRender(vm, fragment, value)
    fragment.content = value
  })
  fragment.content = content
  if (content) {
    compile(vm, content, fragment)
  }
}

/**
 * 编译富文本内容
 * @param vm
 * @param target <template>定义
 * @param dest 所在片段定义
 * @param meta
 */
function compileRichText(vm, target, dest) {
  const parser = context.quickapp.platform.requireBundle('parser.js')
  const textType = target.contentType
  // 创建fragment节点
  const fragment = createFragment(vm, dest)
  let content
  // 确保value属性是函数
  if (typeof target.content === 'function') {
    content = watchFragment(vm, fragment, target.content, 'html', value => {
      console.trace(`### App Framework ### Html 节点 (${fragment.ref})数据改变----`, value)

      // 针对fragment节点, 仅删除内部子节点
      removeNode(fragment, true)

      // 如果有新值, 重新编译
      if (value) {
        // 当类型为book或者html时不用编译节点
        if (textType !== 'html' && textType !== 'book') {
          // 解析value为template
          const xContent = parser.compile(value, textType)
          compile(vm, xContent, fragment)
        }
      }
    })
  } else {
    content = target.content
  }

  if (content) {
    // 当类型为book或者html时不用编译节点
    if (textType !== 'html' && textType !== 'book') {
      const xContent = parser.compile(content, textType)
      compile(vm, xContent, fragment)
    }
  }
}

/**
 * 观察可计算的变量，创建对应的Watcher
 * @param vm
 * @param frag  片段
 * @param calc  条件函数
 * @param type  节点类型
 * @param handler
 * @returns {*}
 */
function watchFragment(vm, frag, calc, type, handler) {
  const watcher = watch(
    vm,
    calc,
    value => {
      if (vm.$valid) {
        if (isNodeInDocument(frag)) {
          handler(value)
        } else {
          console.trace(`### App Framework ### 节点(${frag.ref})已删除：${JSON.stringify(frag)}`)
        }
      } else {
        console.error('### App Framework ### 试图更新已销毁页面')
      }
    },
    frag
  )

  return watcher.value
}

/**
 * 合并上下文
 * @param context
 * @param mergedData
 * @returns {context}
 */
function mergeContext(context, mergedData) {
  const newContext = Object.create(context)
  newContext._data = mergedData

  initData(newContext)

  newContext._realParent = context
  if (context._static) {
    newContext._static = context._static
  }
  return newContext
}

/**
 * 初始化vm状态
 * @param vm
 */
function initData(cxt) {
  cxt._watchers = []
  let data = cxt._data

  if (!isPlainObject(data)) {
    data = {}
  }

  // 将data属性添加到实例对象上（代理函数）
  const keys = Object.keys(data)
  const keyWords = ['$idx', '$item', '$evt']
  let i = keys.length
  while (i--) {
    const key = keys[i]
    if (keyWords.indexOf(key) > -1 || !isReserved(key)) {
      // 不能以$或_开头
      Object.defineProperty(cxt, key, {
        configurable: true,
        enumerable: true,
        get: function proxyGetter() {
          return cxt._data[key]
        },
        set: function proxySetter(val) {
          cxt._data[key] = val
        }
      })
    } else {
      console.error(`### App Framework ### 页面数据属性名 '${key}' 非法, 属性名不能以$或_开头`)
    }
  }

  // 给<script>中的导出的data添加观察器
  XObserver.$ob(data, cxt)
}

/**
 * 格式化props
 * @param component
 * @param type
 */
function normalizeProps(component, type) {
  const props = component.props
  const result = {}
  let name
  if (Array.isArray(props)) {
    props.forEach(val => {
      if (typeof val === 'string') {
        name = $camelize(val)
        result[name] = { type: null }
      } else {
        console.warn(`### App Framework ### 组件${type} props为数组时，数组元素必须为字符串`)
      }
    })
  } else if (isPlainObject(props)) {
    for (const key in props) {
      const val = props[key]
      name = $camelize(key)
      result[name] = isPlainObject(val) ? val : { type: val }
    }
  } else {
    console.warn(`### App Framework ### 组件${type} props属性值无效，必须为数组或对象`)
  }
  component.props = result
}

/**
 * 从Dom中移除节点
 * @param node 待移除的节点
 * @param preserved 是否保留（fragment）
 */
function removeNode(node, preserved = false) {
  if (!node) {
    return
  }
  unbindNode(node, preserved)
  removeNodeImpl(node, preserved)
}

export { build }
