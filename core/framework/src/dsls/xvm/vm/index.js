/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $extend, isPlainObject, isReserved, isEmptyObject } from 'src/shared/util'

import { $own, $proxy, $unproxy, isReservedKey, isReservedAttr } from '../util'

import { fireEventWrap } from './dom'

import { requireCustomComponent } from '../app/custom'

import { updatePageActions, destroyVm } from '../page/misc'

import { build } from './compiler'

import XWatcher from './watcher'
import { XObserver } from './observer'
import context from '../context'
import XEvent from '../app/event'
import XLinker from './linker'

// 访问校验类型
const AccessTypeList = ['public', 'protected', 'private']

// page module
let pageModule = null

/**
 * 权限控制，外部无法覆盖私有变量
 * @param vm
 * @param externalData
 */
function initExternalData(vm, externalData) {
  if (vm._parent) {
    // 子VM无需权限
    return
  }

  if (!externalData) {
    return
  }

  if (!vm._options._descriptor) {
    // 直接合并
    $extend(vm._data, externalData)
    return
  }

  // 请求是否来自于外部
  let fromExternal = vm._page.intent && vm._page.intent.fromExternal

  if (!vm._page.intent || vm._page.intent.fromExternal === undefined) {
    // 不传递则走按严格校验
    fromExternal = true
  }

  console.trace(
    `### App Framework ### 页面VM中声明的权限定义：${JSON.stringify(vm._options._descriptor)}`
  )

  // 增加提示
  if (vm._options.props && !isEmptyObject(externalData)) {
    console.warn(`### App Framework ### 页面VM中不支持props，推荐在public或protected中声明参数`)
  }

  // 校验合并
  for (const extName in externalData) {
    const extDesc = vm._options._descriptor[extName]

    if (!extDesc) {
      console.trace(`### App Framework ### 传递外部数据${extName}在VM中未声明，放弃更新`)
      continue
    }

    const matchFromExPackage = fromExternal && AccessTypeList.indexOf(extDesc.access) > 0
    const matchFromInPackage = !fromExternal && AccessTypeList.indexOf(extDesc.access) > 1
    if (matchFromExPackage || matchFromInPackage) {
      console.warn(
        `### App Framework ### 传递外部数据${extName}在VM中声明为${extDesc.access}，放弃更新`
      )
    } else {
      console.trace(
        `### App Framework ### 传递外部数据${extName}，原值为:${JSON.stringify(vm._data[extName])}`
      )
      console.trace(
        `### App Framework ### 传递外部数据${extName}，新值为:${JSON.stringify(
          externalData[extName]
        )}`
      )
      vm._data[extName] = externalData[extName]
    }
  }
}

/**
 * 合并全局、页面级自定义指令
 * @param vm
 */
function initCustomDirective(vm) {
  const appCustomDirective =
    (vm._page && vm._page.app && vm._page.app._def && vm._page.app._def.directives) || {}
  const vmCustomDirective = vm._options.directives || {}
  const directives = {}
  // 合并指令存入一个新对象中，页面覆盖全局级别的指令
  const dirs = $extend({}, appCustomDirective, vmCustomDirective)
  for (const dirName in dirs) {
    // 将指令名称转为小写
    directives[dirName.toLocaleLowerCase()] = dirs[dirName]
  }
  vm._directives = directives
}

/**
 * 初始化vm状态
 * @param vm
 */
function initState(vm) {
  vm._watchers = []
  let data = vm._data

  if (!isPlainObject(data)) {
    data = {}
  }
  // 将data属性添加到实例对象上（代理函数）
  const keys = Object.keys(data)
  let i = keys.length
  while (i--) {
    const v = keys[i]
    if (isReservedAttr(v)) {
      console.warn(`### App Framework ### data 属性 '${v}' 是保留字, 可能会导致应用运行异常`)
    }
    if ($own(vm._props, v)) {
      console.warn(`### App Framework ### data 请在data中使用另一个名称声明：${v}，不要与props重复`)
      if (vm._attrs.hasOwnProperty(v)) continue
    }
    $proxy(vm, '_data', keys[i])
  }

  // 给<script>中的导出的data添加观察器
  XObserver.$ob(data, vm)

  // 将<script>中自定义函数添加到vm中
  const methods = vm._methods
  if (methods) {
    for (const key in methods) {
      if (data.hasOwnProperty(key)) {
        console.warn(`### App Framework ### 检测到同名数据属性和方法：${key}，可能导致异常`)
      }
      if (vm._options && vm._options.computed && vm._options.computed.hasOwnProperty(key)) {
        console.warn(`### App Framework ### 检测到同名计算属性和方法：${key}，方法定义无效`)
        continue
      }
      vm[key] = methods[key]
    }
  }

  // 初始化computed, 将computed函数添加到vm中
  initComputed(vm)
}

/**
 * 初始化computed状态
 * @param {Object} vm - 组件实例
 */
function initComputed(vm) {
  let computed = vm._options.computed
  computed = isPlainObject(computed) ? computed : {}

  for (const key in computed) {
    let userDef = computed[key]

    if (isReservedAttr(key)) {
      console.warn(`### App Framework ### computed 属性 ${key} 是保留字, 可能会导致应用运行异常`)
    }

    if (vm.hasOwnProperty(key)) {
      console.warn(`### App Framework ### computed 实例已定义 ${key} 属性，不可重复设置`)
      continue
    }

    const computedSharedDefinition = {
      enumerable: true,
      configurable: true,
      get: function() {},
      set: function() {
        console.warn(`### App Framework ### computed 计算属性：${key} 未设置setter函数`)
      }
    }

    if (typeof userDef === 'function') {
      computedSharedDefinition.get = createComputedGetter(userDef, vm)

      Object.defineProperty(vm, key, computedSharedDefinition)
    }

    if (typeof userDef === 'object') {
      userDef = userDef || {}

      if (typeof userDef.get !== 'function') {
        console.warn(`### App Framework ### computed 请设置计算属性：${key} 的getter函数`)
        continue
      }

      computedSharedDefinition.get = createComputedGetter(userDef.get, vm)

      if (typeof userDef.set === 'function') {
        computedSharedDefinition.set = userDef.set
      }

      Object.defineProperty(vm, key, computedSharedDefinition)
    }

    if (typeof userDef !== 'function' && typeof userDef !== 'object') {
      console.warn(`### App Framework ### computed 请正确设置计算属性：${key}`)
    }
  }
}

/**
 * 缓存computed的值，如果依赖改变则重新计算
 * @param getter
 * @param vm
 */
function createComputedGetter(getter, vm) {
  const watcher = new XWatcher(vm, getter, function() {}, { lazy: true })

  return function computedGetter() {
    if (watcher.dirty) {
      watcher.evaluate()
    }
    if (XLinker.target) {
      watcher.depend()
    }
    return watcher.value
  }
}

/**
 * 初始化事件
 * @param  {Vm} vm
 * @param  {object} externalEvents
 */
function initEvents(vm, externalEvents) {
  const options = vm._options || {}

  const events = options.events || {}
  for (const evt in events) {
    vm.$on(evt, events[evt])
  }

  for (const ext in externalEvents) {
    vm.$on(ext, externalEvents[ext])
  }

  // 插件定制的生命周期
  for (const key in XVm.pluginEvents) {
    const listeners = XVm.pluginEvents[key]
    listeners.forEach(fn => {
      vm.$on(key, fn)
    })
  }

  // 绑定生命周期接口函数
  XEvent.reserveEvents.forEach(type => {
    vm.$on(`xlc:${type}`, options[type])
  })
}

/**
 * @param {Object} vm - 组件示例
 * @param {Object} template - slot模版
 */
function preDealWithSlot(vm, template) {
  // 缓存slot的模板
  const namedSlotCache = {}
  const templateChildren = template.children || []

  templateChildren.forEach(child => {
    namedSlotCache.default = namedSlotCache.default || []
    if (child.attr.slot == null) {
      namedSlotCache.default.push(child)
    } else {
      const name = child.attr.slot
      namedSlotCache[name] = namedSlotCache[name] || []
      namedSlotCache[name].push(child)
    }
  })

  vm._slot = {
    template,
    namedSlotCache
  }
}

let _uniqueId = 1
/**
 * ViewModel，只有自定义组件才对应vm
 */
export default class XVm {
  /**
   * @param {Object} options - 组件定义，如果没有指定，则根据type从app查找
   * @param {string} type - 组件名
   * @param {Object} parentObj - 父vm,for指令上下文,或者页面VM初始化时的对象
   * @param {Object} parentElement - 父element
   * @param {Object} externalData - 外部数据
   * @param {Object} externalEvents - 外部事件
   * @param {Object} slotContentTemplate - slot节点中替换的内容模板
   * @constructor
   */
  constructor(
    options,
    type,
    parentObj,
    parentElement,
    externalData,
    externalEvents,
    slotContentTemplate
  ) {
    parentObj = parentObj || {}
    const parentContext = parentObj instanceof XVm ? parentObj : undefined
    const page = parentObj._page

    this.__id__ = _uniqueId++
    this._type = type // 组件类型
    this._page = page || {}
    // 父VM或undefined
    this._parent = parentObj._realParent ? parentObj._realParent : parentContext
    // 顶Vm或自己
    this._root = this._parent ? this._parent._root : this
    // 父级Vm或者for指令上下文
    this._parentContext = parentContext

    this._attrs = {}
    this._$attrs = {}
    this.__$attrs__ = {}
    this._$listeners = []
    this._directives = {}
    // 如果是顶级Vm，增加自定义指令上下文数据
    if (this._root === this) {
      this._directivesContext = {}
    }
    this._destroyed = false

    Object.defineProperties(this, {
      // vm对应的页面是否有效
      $valid: {
        get: () => !!page && page._valid,
        configurable: false
      },
      // vm对应的页面是否正在展示中
      $visible: {
        get: () => !!page && page._valid && page._visible,
        configurable: false
      },
      // vm本身是否已销毁
      $destroyed: {
        get: () => this._destroyed,
        configurable: false
      }
    })

    // 缓存slot的模板
    if (slotContentTemplate) {
      preDealWithSlot(this, slotContentTemplate)
    }
    // 根VM没有_childrenVms属性
    if (parentObj && parentObj._childrenVms) {
      parentObj._childrenVms.push(this)
    }

    //  如果没有指定父节点，则默认为Doc节点
    this._parentElement = parentElement || (this._page.doc && this._page.doc.documentElement)

    // 如果没有提供组件元信息，则根据type在注册组件列表中查找
    if (!options && this._page.app) {
      options = requireCustomComponent(this._page.app, this._page, type)
    }
    options = options || {}

    // 页面级Vm：i18n配置合并
    if (this._isPageVm() && this._page.app) {
      const ret = this._page.app.getLocaleConfig()
      const dup = JSON.parse(JSON.stringify(ret))
      options.i18n = Object.assign({}, dup, options.i18n)
    }

    // 在组件<script>中导出的data
    const data = options.data || {}
    // 备份组件完整定义
    this._options = options
    // 提取<script>中定义的自定义函数（非系统接口函数）
    this._methods = {}
    Object.keys(options).forEach(key => {
      if (
        (isReservedKey(key) && key !== 'computed') ||
        (XEvent.isReservedEvent(key) && key !== 'onRefresh')
      ) {
        return
      }

      if (key === 'onRefresh') {
        if (!global.isRpkMinPlatformVersionGEQ(1050)) {
          console.warn(
            `### App Framework ### onRefresh()为1050版本中新增的Vm生命周期，不再当做Vm方法`
          )
          console.warn(
            `### App Framework ### 如果用于方法调用或组件事件响应，请使用其它名称，后续版本不再兼容`
          )
        } else {
          return
        }
      }

      if (key === 'computed') {
        if (!global.isRpkMinPlatformVersionGEQ(1050)) {
          console.warn(`### App Framework ### computed为1050版本中新增的计算属性，不再当做Vm方法`)
          console.warn(
            `### App Framework ### 如果用于方法调用或组件事件响应，请使用其它名称，后续版本不再兼容`
          )
        } else {
          return
        }
      }

      const func = options[key]
      if (typeof func === 'function') {
        this._methods[key] = func
      }
    })

    this._isXVm = true // a flag to avoid this being observed
    this._ids = {} // <template>中带有id属性节点的映射表
    this._vmEvents = {} //
    this._childrenVms = [] // 包含的子vm数组
    this._props = {}
    this._parentWatchers = []

    // 绑定外部事件
    initEvents(this, externalEvents)

    console.trace(`### App Framework ### 组件Vm(${this._type})初始化完成`)
    this.$emit('xlc:onCreate')
    this._created = true // 目前为内部接口（暂时不暴露）

    if (typeof data === 'function') {
      this._data = data.call(this) || {}
    } else {
      // 复制数据
      this._data = {}
      $extend(this._data, data)
    }

    // 合并全局数据
    if (this._page && this._page.app) {
      $extend(this._data, this._page.app.$data)
    }

    // 权限控制
    initExternalData(this, externalData)

    // 初始化vm状态, 将自定义函数添加到vm中
    initState(this)

    // 初始化自定义指令
    initCustomDirective(this)

    console.trace(`### App Framework ### 组件Vm(${this._type})创建成功`)
    // 自定义Vm与仅做数据驱动的Vm：无携带
    const hasQuery = this._isPageVm() && this._page && this._page._meta
    this._emit('xlc:onInit', hasQuery ? this._page._meta.query : undefined)
    this._inited = true

    // app的doc之前已经初始化
    if (!this._page.doc) {
      return
    }

    if (this.$root() === this) {
      profiler.record(
        `### App Performance ### 编译页面Vm模板[PERF:compileVmTmpl]开始：${new Date().toJSON()}`
      )
      profiler.time(`PERF:compileVmTmpl`)
    }
    // 递归创建子节点模型
    build(this)
    if (this.$root() === this) {
      profiler.timeEnd(`PERF:compileVmTmpl`)
      profiler.record(
        `### App Performance ### 编译页面Vm模板[PERF:compileVmTmpl]开始：${new Date().toJSON()}`
      )
    }
  }

  _isPageVm() {
    return this._root === this
  }

  /**
   * 检查vm是否已销毁
   * @returns {boolean}
   * @private
   */
  _isVmDestroyed() {
    if (this._destroyed) {
      console.warn(`### App Framework ### 组件Vm(${this._type})已销毁，中断Vm方法的调用`)
    }
    return this._destroyed
  }

  get $attrs() {
    return this._$attrs
  }

  get $listeners() {
    return this._$listeners
  }

  /**
   * 当前App对象
   */
  get $app() {
    if (global.Env.engine === global.ENGINE_TYPE.CARD) {
      return null
    }

    if (this._isVmDestroyed()) {
      return
    }

    return this._page.app
  }

  /**
   * 获取页面接口对象
   * @returns {{setTitleBar: setTitleBar}}
   */
  get $page() {
    if (global.Env.engine === global.ENGINE_TYPE.CARD) {
      return null
    }

    if (this._isVmDestroyed()) {
      return
    }

    const page = this._page
    const app = page.app
    return Object.assign(
      {
        setTitleBar: function(attr) {
          // 如果是页面对象
          if (page && page.doc) {
            console.trace(`### App Framework ### 页面 ${page.id} 调用 setTitleBar ----`)
            context.quickapp.runtime.helper.updatePageTitleBar(page.doc, attr)
          }
        },
        setMeta: function(attr) {
          // 如果是页面对象
          if (page && page.doc) {
            console.trace(`### App Framework ### 页面 ${page.id} 调用 setMeta ----`)
            context.quickapp.runtime.helper.setMeta(page.doc, attr)
          }
        },
        scrollTo: function(attr) {
          // 如果是页面对象
          if (page && page.doc) {
            console.trace(`### App Framework ### 页面 ${page.id} 调用 scrollTo ----`)
            context.quickapp.runtime.helper.scrollTo(page.doc, attr)
          }
        },
        scrollBy: function(attr) {
          // 如果是页面对象
          if (page && page.doc) {
            console.trace(`### App Framework ### 页面 ${page.id} 调用 scrollBy ----`)
            context.quickapp.runtime.helper.scrollBy(page.doc, attr)
          }
        },
        setStatusBar: function(attr) {
          // 如果是页面对象
          if (page && page.doc) {
            console.trace(`### App Framework ### 页面 ${page.id} 调用 setStatusBar ----`)
            context.quickapp.runtime.helper.updatePageStatusBar(page.doc, attr)
          }
        },
        exitFullscreen: function() {
          // 如果是页面对象
          if (page && page.doc) {
            console.trace(`### App Framework ### 页面 ${page.id} 调用 exitFullscreen ----`)
            context.quickapp.runtime.helper.exitFullscreen(page.doc)
          }
        },
        finish: function() {
          // 调用native侧提供的接口，销毁页面对象
          if (page && page.doc) {
            if (pageModule === null) {
              pageModule = context.quickapp.platform.requireModule(app, 'system.page')
            }
            pageModule.finishPage(page.id)
          }
        },
        getMenuBarRect: function() {
          // 调用native侧提供的接口，获取menubar位置对象
          if (page && page.doc) {
            if (pageModule === null) {
              pageModule = context.quickapp.platform.requireModule(app, 'system.page')
            }
            return pageModule.getMenuBarRect(page.id)
          }
        },
        getMenuBarBoundingRect: function() {
          // 调用native侧提供的接口，获取menubar转换为设计尺寸位置对象
          if (page && page.doc) {
            if (pageModule === null) {
              pageModule = context.quickapp.platform.requireModule(app, 'system.page')
            }
            return pageModule.getMenuBarBoundingRect(page.id)
          }
        },
        setMenubarData: function(attr) {
          // 调用native侧提供的接口，设置menubardata数据
          if (page && page.doc) {
            if (pageModule === null) {
              pageModule = context.quickapp.platform.requireModule(app, 'system.page')
            }
            return pageModule.setMenubarData({ id: page.id, attr })
          }
        },
        setTabBarItem: function(attr) {
          // 调用native侧提供的接口，设置TabBardata数据
          if (page && page.doc) {
            if (pageModule === null) {
              pageModule = context.quickapp.platform.requireModule(app, 'system.page')
            }
            return pageModule.setTabBarItem({ id: page.id, attr })
          }
        },
        hideSkeleton: function() {
          if (page && page.doc) {
            console.trace(`### App Framework ### 页面 ${page.id} 调用 hideSkeleton ----`)
            context.quickapp.runtime.helper.hideSkeleton(page.doc, page.id)
          }
        },
        setSecure: function(isSecure) {
          if (page && page.doc) {
            context.quickapp.runtime.helper.callHostFunction(page.doc, 'setSecure', [!!isSecure])
          }
        },
        setMenubarTips: function(attr) {
          // 调用native侧提供的接口，设置menubarTips数据
          if (page && page.doc) {
            if (pageModule === null) {
              pageModule = context.quickapp.platform.requireModule(app, 'system.page')
            }
            return pageModule.setMenubarTips({ id: page.id, attr })
          }
        }
      },
      page && page._meta
    )
  }

  /**
   * 父VM
   */
  $parent() {
    return this._parent
  }

  /**
   * 页面的根VM
   * @return {*}
   */
  $root() {
    return this._root
  }

  /**
   * 发送事件
   * @param type
   * @param detail
   */
  $emit(type, detail) {
    if (this._isVmDestroyed()) {
      return
    }

    const events = this._vmEvents
    const handlerList = events[type]
    if (handlerList) {
      const evt = new XEvent(type, detail)
      // 系统接口只有唯一一个handler, 只需返回第一个即可
      const result = []
      handlerList.forEach(handler => {
        result.push(handler.call(this, evt))
      })
      // 插件环境下可能有多个handler，所以返回vm中的结果
      return result.length > 0 ? result[result.length - 1] : false
    }
    return false
  }

  /**
   * 发送事件，供页面VM生命周期使用
   * @param {String} type 事件名称
   * @param {Object} evtHash 给回调方法传入的事件对象
   */
  _emit(type, evtHash, ...args) {
    if (this._isVmDestroyed()) {
      return
    }

    const events = this._vmEvents
    const handlerList = events[type]
    if (handlerList) {
      // 系统接口只有唯一一个handler, 只需返回第一个即可
      const result = []
      handlerList.forEach(handler => {
        result.push(handler.call(this, evtHash, ...args))
      })
      // 插件环境下可能有多个handler，所以返回vm中的结果
      return result.length > 0 ? result[result.length - 1] : false
    }
    return false
  }

  /**
   * 发送事件
   * @param id 元素id id == -1/undefine 代表根节点
   * @param type 事件类型
   * @param detail
   */
  $emitElement(type, detail, id = -1) {
    if (this._isVmDestroyed()) {
      return
    }

    if (id === -1 || id === undefined) {
      // 对应根节点
      id = undefined
    } else if (typeof id !== 'string') {
      // 如果id不是字符串, 则什么也不做
      console.error(`### App Framework ### $emitElement的参数id不合法`)
      return
    }

    const element = this.$element(id)
    if (element) {
      return fireEventWrap(element, type, { detail: detail })
    } else {
      // 如果id不是字符串, 则什么也不做
      console.error(`### App Framework ### $emitElement执行失败: 找不到id为 '${id}' 的组件`)
    }
  }

  /**
   * 向上派发事件
   * @param type
   * @param detail
   */
  $dispatch(type, detail) {
    if (this._isVmDestroyed()) {
      return
    }

    const evt = new XEvent(type, detail)
    this.$emit(type, evt)

    if (!evt.hasStopped() && this._parent && this._parent.$dispatch) {
      this._parent.$dispatch(type, evt)
    }
  }

  /**
   * 向下广播事件
   * @param  {string} type
   * @param  {any}    detail
   */
  $broadcast(type, detail) {
    if (this._isVmDestroyed()) {
      return
    }

    const evt = new XEvent(type, detail)
    this.$emit(type, evt)

    if (!evt.hasStopped() && this._childrenVms) {
      this._childrenVms.forEach(subVm => {
        subVm.$broadcast(type, evt)
      })
    }
  }

  /**
   * 添加事件listener
   * @param  {string}   type
   * @param  {function} handler
   */
  $on(type, handler) {
    if (this._isVmDestroyed()) {
      return
    }

    if (!type || typeof handler !== 'function') {
      return
    }
    const events = this._vmEvents
    // 每个类型事件对应多个回调函数
    const handlerList = events[type] || []
    handlerList.push(handler)
    events[type] = handlerList
  }

  /**
   * 移除事件句柄
   * @param  {string}   type
   * @param  {function} handler
   */
  $off(type, handler) {
    if (this._isVmDestroyed()) {
      return
    }

    if (!type) {
      return
    }
    const events = this._vmEvents
    if (!handler) {
      delete events[type]
      return
    }
    const handlerList = events[type]
    if (!handlerList) {
      return
    }
    handlerList.$remove(handler)
  }

  /**
   * 强制更新
   */
  $forceUpdate() {
    if (this._isVmDestroyed()) {
      return
    }

    const page = this._page
    // 如果是页面对象
    if (page && page.doc) {
      console.trace(`### App Framework ### 强制更新页面 ---- ${page.id}`)
      updatePageActions(page)
    }
  }

  /**
   * 动态添加数据绑定
   * @desc 当前仅支持对象（aaa.bbb），不支持数组；
   * @param exp
   * @param val
   */
  $set(exp, val) {
    if (this._isVmDestroyed()) {
      return
    }

    const expDict = XVm.parseExpression(exp)
    expDict.set.call(this, this, val)
  }

  /**
   * 动态删除数据绑定
   * @param key
   */
  $delete(key) {
    if (this._isVmDestroyed()) {
      return
    }

    const data = this._data
    XVm.delete(data, key)
  }

  /**
   * 动态添加数据观察
   * @param target  数据名（data|props中定义）
   * @param cb {string|function} 回调函数名
   */
  $watch(target, cb) {
    if (this._isVmDestroyed()) {
      return
    }

    const vm = this

    if (typeof target !== 'string') {
      console.error(`### App Framework ### $watch调用异常: 第一个参数的数据类型必须是字符串`)
      return
    }

    if (typeof cb === 'string') {
      if (!this._methods[cb]) {
        console.warn(`### App Framework ### $watch调用异常: 句柄函数名 '${cb}' 没有定义`)
        return
      } else {
        cb = this._methods[cb]
      }
    }

    const calc = function() {
      return XVm.getPath(vm, target)
    }
    const watcher = new XWatcher(vm, calc, function(value, oldValue) {
      // 如果函数计算结果是对象则始终认为值被改变，如果是基本类型则进行比较
      if (typeof value !== 'object' && value === oldValue) {
        // 如果值没改变则直接返回
        return
      }
      // 执行回调
      cb.apply(vm, [value, oldValue])
    })
    return watcher
  }

  /**
   * 重新计算一遍
   * @private
   */
  _digest() {
    const len = this._watchers.length
    for (let i = 0; i < len; i++) {
      this._watchers[i].update(true)
    }
  }

  _destroyVm() {
    destroyVm(this)
  }
}

/**
 * 解析表达式
 * @param exp
 * @return {{exp: *, get: expDict.get, set: expDict.set}}
 */
XVm.parseExpression = function(exp) {
  const expDict = {
    exp: exp,
    get: function(vm) {
      return XVm.getPath(vm, exp)
    },
    set: function(vm, val) {
      XVm.setPath(vm, exp, val)
    }
  }
  return expDict
}

/**
 * 获取路径的值
 * @param obj
 * @param target
 * @return {*}
 */
XVm.getPath = function(obj, target) {
  if (/[^\w.$]/.test(target)) {
    console.warn(`### App Framework ### getPath调用：观察对象 '${target}' 不合法`)
    return
  }
  // 解析target路径
  const nameList = target.split('.')
  const key = nameList.pop()

  const nameListLen = nameList.length
  for (let i = 0; i < nameListLen; i++) {
    const name = nameList[i]
    if (isReserved(name)) {
      console.warn(`### App Framework ### getPath调用：属性名 '${name}' 不能以 $ 或 _ 开头`)
      return
    }
    if (!obj[name]) {
      console.warn(
        `### App Framework ### getPath调用：属性名 '${name}' 在 '${target}' 中值为：${obj[name]}`
      )
      return
    }
    obj = obj[name]
  }
  return obj[key]
}

/**
 * 设置路径为新值
 * @param obj
 * @param target
 * @param val
 */
XVm.setPath = function(obj, target, val) {
  if (/[^\w.$]/.test(target)) {
    console.warn(`### App Framework ### setPath调用：观察对象 '${target}' 不合法`)
    return
  }
  // 解析target路径
  const nameList = target.split('.')
  const key = nameList.pop()

  const nameListLen = nameList.length
  for (let i = 0; i < nameListLen; i++) {
    const name = nameList[i]
    if (isReserved(name)) {
      console.warn(`### App Framework ### setPath调用：属性名 '${name}' 不能以 $ 或 _ 开头`)
      return
    }
    if (!obj[name]) {
      console.warn(
        `### App Framework ### setPath调用：属性名 '${name}' 在 '${target}' 中值为：${obj[name]}`
      )
      return
    }
    obj = obj[name]
  }
  XVm.set(obj, key, val)
}

/**
 * 给对象设置属性，如果属性之前不存在，则触发事件
 */
XVm.set = function(obj, key, val) {
  if (obj == null) {
    return
  }

  if (obj instanceof XVm) {
    XVm.set(obj._data, key, val)
    return
  }

  if (Array.isArray(obj)) {
    return obj.splice(key, 1, val)
  }
  if ($own(obj, key)) {
    obj[key] = val
    return
  }

  const ob = obj.__ob__
  if (!ob) {
    // 如果对象没有被观察，则直接修改值
    obj[key] = val
    return
  }

  ob.convert(key, val)
  ob.dep.notify()

  if (ob.vms) {
    let i = ob.vms.length
    while (i--) {
      const vm = ob.vms[i]
      $proxy(vm, '_data', key)
      vm._digest()
    }
  }
  return val
}

/**
 * 删除一个属性
 */
XVm.delete = function(obj, key) {
  if (obj == null) {
    return
  }

  if (!$own(obj, key)) {
    return
  }
  delete obj[key]
  const ob = obj.__ob__

  if (!ob) {
    return
  }
  ob.dep.notify()
  if (ob.vms) {
    let i = ob.vms.length
    while (i--) {
      const vm = ob.vms[i]
      $unproxy(vm, key)
      vm._digest()
    }
  }
}

/**
 * 初始化App方法
 * @param apis
 */
XVm.initVmMethods = function(apis) {
  const p = XVm.prototype

  for (const apiName in apis) {
    if (!p.hasOwnProperty(apiName)) {
      p[apiName] = apis[apiName]
    }
  }
}

/**
 * 给Vm添加已注册Api模块的函数
 * @param methods
 */
XVm.registerMethods = function(methods) {
  // 解析字符串
  if (typeof methods === 'string') {
    methods = JSON.parse(methods)
  }

  if (typeof methods === 'object') {
    XVm.initVmMethods(methods)
  }
}

/**
 * 插件事件的暂存
 */
XVm.pluginEvents = {}

/**
 * 混合到VM中
 * @param options {object}
 */
XVm.mixin = function(options) {
  for (const key in options) {
    // 绑定保留的生命周期
    if (XEvent.isReservedEvent(key)) {
      const keyName = `xlc:${key}`
      if (!XVm.pluginEvents[keyName]) {
        XVm.pluginEvents[keyName] = []
      }
      XVm.pluginEvents[keyName].push(options[key])
    } else {
      console.warn(`### App Framework ### 插件定义的函数，不属于页面生命周期函数：${key}`)
    }
  }
}
