/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { invokeScript } from 'src/shared/function'
import {
  getSessionInstance,
  changeIsFirstOnShowToFalse,
  getIsFirstOnShow,
  isPlainObject
} from 'src/shared/util'

import { APP_KEYS } from 'src/shared/events'

import XApp from './app'
import XPage from './page'

import { destroyApp } from './app/misc'

import { applyLocaleConfig } from './app/locale'

import {
  normalize,
  recreatePage as recreatePageCore,
  destroyPage as destroyPageCore,
  getRootElement,
  fireEvent,
  callback,
  getElementStyles,
  setElementStyles,
  setElementAttrs,
  compileFragmentData,
  processNextTickCallbacks,
  processCustomDirectiveCallback
} from './page/misc'

import config from './config'
import { makeTimer } from './timer'

import { bootstrap, methodMap } from './bootstrap'

import { xHandleError } from 'src/shared/error'

// App页面实例表
const _appMap = {}
const _pageMap = {}

/**
 * 初始化平台，关联到Native供调用
 * @param glue
 */
function init(glue) {
  config.platform = glue.platform
  config.runtime = glue.runtime
  config.dock = glue.dock

  bootstrap(glue)
}

/**
 * 定位Dsl，由native调用
 */
function locateDsl() {
  profiler.record(`### App Performance ### 启动框架[PERF:locateDsl]开始：${new Date().toJSON()}`)

  let dslName = global.getManifestField('config.dsl.name')
  if (!dslName) {
    console.trace(`### App Framework ### 无法从manifest找到对应的dsl名称`)
    dslName = 'xvm'
  }
  let dslCode = null
  console.trace(`### App Framework ### 从平台中获取dsl-${dslName}`)
  dslCode = config.platform.loadResource(`assets:///js/dsls/dsl-${dslName}.js`)
  if (!dslCode) {
    throw new Error(`### App Framework ### 未找到相应的dsl文件`)
  }
  registerDsl(dslCode)
  profiler.record(`### App Performance ### 启动框架[PERF:locateDsl]结束：${new Date().toJSON()}`)
}

/**
 * 注册Dsl，由Native调用
 * @param {string} dslCode 底层传递的DSL源代码
 */
function registerDsl(dslCode) {
  let dslExport = dslCode
  if (typeof dslCode === 'string') {
    dslExport = invokeScript({}, `${dslCode}; return dsl;`)
  }
  installDsl(dslExport)
}

/**
 * 初始化Dsl，用于完成DSL到平台的适配工作
 * @param {object} dslExport dsl导出的针对快应用的封装对象
 */
function installDsl(dslExport) {
  dslExport.init(config)
}

/**
 * 修改APP的locale定义
 * @param localeObject {object} locale定义
 * @param resources {array} 各类i18n的资源定义
 */
function changeAppLocale(localeObject, resources) {
  applyLocaleConfig(localeObject, resources)
}

/**
 * 创建App
 * @param id {integer} 应用ID
 * @param code {string} 应用的JS代码
 * @returns {*}
 */
function createApplication(id, code) {
  // 根据id查找App实例（由原生端生成）
  profiler.record(
    `### App Performance ### 调用APPJS整体[PERF:createApplication]开始：${new Date().toJSON()}`
  )
  profiler.time(`PERF:createApplication`)
  let app = _appMap[id]
  let result
  if (!app) {
    console.trace(`### App Framework ### 平台配置信息：`, JSON.stringify(global.Env))
    profiler.time(`PERF:newAppClass`)
    app = new XApp(id, {})
    profiler.timeEnd(`PERF:newAppClass`)
    _appMap[id] = app

    // 初始化全局接口
    config.platform.initInterface(app)

    result = config.publish(APP_KEYS.initApp, [app, code])
  } else {
    result = new Error(`createApplication: 无效应用Id '${id}'`)
  }
  profiler.timeEnd(`PERF:createApplication`)
  profiler.record(
    `### App Performance ### 调用APPJS整体[PERF:createApplication]结束：${new Date().toJSON()}`
  )
  return result
}

/**
 * App收到外部打开快应用请求
 * @param id
 */
function onRequestApplication(id) {
  applicationLifecycle(id, 'onRequest')
}

/**
 * App显示
 * @param id
 */
function onShowApplication(id) {
  applicationLifecycle(id, 'onShow')
}

/**
 * App隐藏
 * @param id
 */
function onHideApplication(id) {
  applicationLifecycle(id, 'onHide')
}

/**
 * App生命周期
 * @param id
 * @param lifecycle
 */
function applicationLifecycle(id, lifecycle) {
  const app = _appMap[id]
  if (!app) {
    console.error(`### App Framework ### ${lifecycle} app不存在， id： ${id}`)
  } else {
    console.trace(`### App Framework ### ${lifecycle} 应用(${id})响应`)
    if (app.$valid) {
      // applc means application lifecycle
      app._emit(`applc:${lifecycle}`)
      console.trace(`### App Framework ### 调用 ${lifecycle} 回调成功`)
    } else {
      console.trace(`### App Framework ### 调用 ${lifecycle} 回调时应用(${id})已销毁`)
    }
  }
}

/**
 *
 * @param id
 * @returns {*}
 */
function getPage(id) {
  return _pageMap[id]
}

/**
 * 创建Page实例
 * @param {string} id - 页面索引
 * @param {string} appid - APP索引
 * @param {string} code - 页面的JS代码
 * @param {object} query - 打开页面时传递业务数据
 * @param {object} intent - 统传递到页面的数据
 * @param {object} meta - 页面路径元信息，对应router对象
 * @returns {*}
 */
function createPage(id, appid, code, query, intent, meta) {
  profiler.record(
    `### App Performance ### 调用PageJS整体[PERF:createPage]开始：${new Date().toJSON()}`
  )
  profiler.time(`PERF:createPage`)
  const app = _appMap[appid]
  if (!app) {
    return new Error(`createPage: 无效应用Id "${appid}"`)
  }

  query = query || {}

  // 根据id查找App实例（由原生端生成）
  let page = _pageMap[id]
  let result
  if (!page) {
    console.trace(`### App Framework ### 创建页面(${id})---- `, JSON.stringify(intent))
    profiler.time(`PERF:newPageClass`)
    page = new XPage(id, app, intent, meta, query)
    profiler.timeEnd(`PERF:newPageClass`)
    profiler.record(
      `### App Performance ### 实例化Page对象[PERF:newPageClass]结束：${new Date().toJSON()}`
    )
    _pageMap[id] = page
    // 页面中的全局覆盖
    const globals = makeTimer(page, callback, normalize)
    result = config.publish(APP_KEYS.initPage, [page, code, query, globals])
  } else {
    result = new Error(`createPage: 无效页面Id '${id}'`)
  }
  profiler.timeEnd(`PERF:createPage`)
  profiler.record(
    `### App Performance ### 调用PageJS整体[PERF:createPage]结束：${new Date().toJSON()}`
  )
  return result
}

const jsHandlers = {
  1: (inst, ...args) => {
    // dom操作
    return fireEvent(inst, ...args)
  },
  2: (inst, ...args) => {
    // 事件回调
    return callback(inst, ...args)
  }
}

/**
 * 接受来自原生的事件或回调
 * @param id
 * @param tasks
 * @returns {*} 结果数组,无返回值, 'invalid'表示无效事件
 */
function processCallbacks(id, events) {
  if (!Array.isArray(events)) {
    return new Error(`processCallbacks: 无效任务回调数据 "${id}"`)
  }

  // 判断回调来源
  let instance = _pageMap[id]
  if (!instance) {
    instance = _appMap[id]
  }

  if (instance) {
    const results = []
    events.forEach(evt => {
      const handler = jsHandlers[evt.action]
      const args = [...evt.args]
      if (typeof handler === 'function') {
        args.unshift(instance)
        results.push(handler(...args))
      } else {
        // 无效事件
        console.error(`### App Framework ### 无法识别的回调函数类型 (${evt.action})`)
        results.push('invalid')
      }
    })
    return results
  }
  return new Error(`processCallbacks: 无效回调来源Id "${id}"`)
}

/**
 * 处理原生在渲染节点和页面时的回调函数
 * @param pageId 页面id
 * @param type render hook 类型
 * @param args 客户端回调的其他参数
 */
function processRenderHooks(pageId, type, args) {
  switch (type) {
    case 'updateFinish':
      processNextTickCallbacks(_pageMap[pageId], args)
      break
    case 'createFinish':
      processNextTickCallbacks(_pageMap[pageId], args)
      break
    case 'nodeMounted':
    case 'nodeUpdate':
    case 'nodeDestroy':
      // 处理节点上的自定义指令回调
      processCustomDirectiveCallback(_pageMap[pageId], type, args)
      break
  }
}

/**
 * 获取App实例的整个树
 * @param id
 * @returns {*}
 */
function getPageRoot(id) {
  const page = _pageMap[id]
  let result
  if (page) {
    result = getRootElement(page)
  } else {
    result = new Error(`getPageRoot: 无效页面Id "${id}"`)
  }
  return result
}

/**
 * 获取App配置
 * @param id
 * @returns {*}
 */
function getAppConfig(id) {
  const app = _appMap[id]
  let result
  if (app) {
    result = app.$def
  } else {
    result = new Error(`getAppConfig: 无效应用Id "${id}"`)
  }
  return result
}

/**
 * 获取节点的匹配到的样式定义
 * @param pageId
 * @param elementRef
 * @return {*}
 */
function getPageElementStyles(pageId, elementRef) {
  const page = _pageMap[pageId]
  let result
  if (page && page.doc) {
    result = getElementStyles(page, elementRef)
  } else {
    result = new Error(`getPageElementStyles: 无效页面Id "${pageId}"`)
  }
  return result
}

/**
 * 更新节点的样式
 * @param pageId
 * @param elementRef
 * @param ruleName
 * @param editCssPropertyList {Array} 数组中每个元素格式：{name: '', value: '', disabled: true}
 * @return {*}
 */
function setPageElementStyles(pageId, elementRef, ruleName, editCssPropertyList) {
  const page = _pageMap[pageId]
  let result
  if (page && page.doc) {
    console.trace(`### App Framework ### 更新页面元素样式: `, JSON.stringify(arguments))
    result = setElementStyles(page, elementRef, ruleName, editCssPropertyList)
  } else {
    result = new Error(`setPageElementStyles: 无效页面Id "${pageId}"`)
  }
  return result
}

/**
 * 更新节点的属性
 * @param pageId
 * @param elementRef
 * @param actionList
 */
function setPageElementAttrs(pageId, elementRef, actionList) {
  const page = _pageMap[pageId]
  let result
  if (page && page.doc) {
    console.trace(`### App Framework ### 更新页面元素的属性: `, JSON.stringify(arguments))
    result = setElementAttrs(page, elementRef, actionList)
  } else {
    result = new Error(`setPageElementAttrs: 无效页面Id "${pageId}"`)
  }
  return result
}

/**
 * 调试器：替换节点
 * @param pageId
 * @param elementRef
 * @param htmlStr
 */
function replacePageElementWithHtml(pageId, elementRef, htmlStr) {
  const page = _pageMap[pageId]
  let result
  if (page && page.doc) {
    const element = config.runtime.helper.getDocumentNodeByRef(page.doc, elementRef)
    const parentNode = element.parentNode
    const layoutIndex = parentNode.layoutChildren.indexOf(element)
    try {
      // 解析
      const parser = config.platform.requireBundle('parser.js')
      const rawData = parser.parseHTML(htmlStr)
      // 编译
      const newNodeData = compileFragmentData(rawData, page.doc)
      if (newNodeData) {
        // 删除
        parentNode.removeChild(element)
        // 替换
        const newNodeList = newNodeData.hasOwnProperty('length') ? newNodeData : [newNodeData]
        for (let i = 0, len = newNodeList.length; i < len; i++) {
          const newNodeItem = newNodeList[i]
          parentNode.insertBefore(newNodeItem, parentNode.layoutChildren[layoutIndex + i])
        }
        // 更新操作
        page.doc.listener.updateFinish()
        console.trace(
          `### App Framework ### 使用HTML替换元素:`,
          JSON.stringify(arguments),
          JSON.stringify(newNodeList)
        )
      } else {
        result = new Error(
          `replacePageElementWithHtml: 使用HTML替换元素，编译出错：${JSON.stringify(rawData)}`
        )
      }
    } catch (err) {
      result = new Error(`replacePageElementWithHtml: 使用HTML替换元素：${err}`)
    }
  } else {
    result = new Error(`replacePageElementWithHtml: 无效页面Id "${pageId}"`)
  }
  return result
}

/**
 * 重建页面（并不是真的重建，而是利用之前的vdom模型重新创建view）
 * @param id
 * @returns {*}
 */
function recreatePage(id) {
  const page = _pageMap[id]
  let result
  if (page) {
    result = recreatePageCore(page)
  } else {
    result = new Error(`recreatePage: 无效页面Id "${id}"`)
  }
  return result
}

/**
 * 当页面隐藏或消失时调用
 * @param id
 * @param show
 */
function changeVisiblePage(id, show) {
  const page = _pageMap[id]
  if (page) {
    console.trace(`### App Framework ### changeVisiblePage 页面(${id})响应：${show}`)
    page._visible = show
    const event = show ? APP_KEYS.onShow : APP_KEYS.onHide
    config.publish(event, [page])

    // 用于火焰图判断
    if (global.profiler._isEnabled && getIsFirstOnShow()) {
      const session = getSessionInstance()
      session.post('Profiler.stop', (error, { profile }) => {
        if (!error) {
          profiler.saveProfilerData(JSON.stringify(profile))
        } else {
          console.error(`### App Framework ### session.post(Profiler.stop)方法出错：${error}`)
        }
        session.disconnect()
      })
      console.trace(`### App Framework ### session.post('Profiler.stop') 调用`)
      changeIsFirstOnShowToFalse()
    }
  } else {
    const lc = show ? 'onShow' : 'onHide'
    console.error(`### App Framework ### 执行生命周期${lc}时，页面 '${id}' 无效`)
  }
}

/**
 * 当用户点击back键时触发（仅针对android系统有效）
 * @param id
 */
function backPressPage(id) {
  const page = _pageMap[id]
  let result = false
  if (page) {
    console.trace(`### App Framework ### backPressOnPage 页面(${id})响应`)
    result = config.publish(APP_KEYS.onBackPress, [page])
  } else {
    console.error(`### App Framework ### 执行页面周期onBackPress时，页面 '${id}' 无效`)
  }

  // true代表自己处理逻辑，否则遵循系统默认行为
  if (result !== true) {
    result = false
  }
  return result
}

/**
 * 当用户点击遥控器menu键时触发
 * @param id
 */
function menuButtonPressPage(id) {
  const page = _pageMap[id]
  let result = false
  if (page) {
    console.trace(`### App Framework ### menuButtonPressPage 页面(${id})响应`)
    result = config.publish(APP_KEYS.onMenuButtonPress, [page])
  } else {
    console.error(`### App Framework ### 执行生命周期onMenuButtonPress时，页面 '${id}' 无效`)
  }

  // true代表自己处理逻辑，否则遵循系统默认行为
  if (result !== true) {
    result = false
  }
  return result
}

/**
 * 当用户点击TV遥控器的按键时触发（仅针对android TV系统有效）
 * @param id
 * @param options  包含code action repeatCount 等参数
 */
function keyPressPage(id, options) {
  const page = _pageMap[id]
  let result = false
  if (page) {
    console.trace(`### App Framework ### onKey 页面(${id})响应:${JSON.stringify(options)}`)
    const { action, code, repeatCount } = options
    result = config.publish(APP_KEYS.onKey, [page, { action, code, repeatCount }])
  } else {
    console.error(
      `### App Framework ### 执行生命周期onKey时，页面 '${id}' 无效： ${JSON.stringify(options)}`
    )
  }

  // 布尔类型转换, 防止jscore异常
  if (result !== true) {
    result = false
  }
  return result
}

/**
 * 当用户点击menu键时触发
 * @param id
 */
function menuPressPage(id) {
  const page = _pageMap[id]
  let result = false
  if (page) {
    console.trace(`### App Framework ### menuPressOnPage 页面(${id})响应`)
    result = config.publish(APP_KEYS.onMenuPress, [page])
  } else {
    console.error(`### App Framework ### 执行生命周期onMenuPress时，页面 '${id}' 无效`)
  }

  // true代表自己处理逻辑，否则遵循系统默认行为
  if (result !== true) {
    result = false
  }

  return result
}

/**
 * 通知页面配置的更新
 * @param pageId {number} 页面Id
 * @param options {object} 事件参数，拥有type属性，值为字符串
 */
function notifyConfigurationChanged(pageId, options) {
  const page = _pageMap[pageId]
  if (page) {
    console.trace(
      `### App Framework ### notifyConfigurationChanged 页面(${pageId})响应：${JSON.stringify(
        options
      )}`
    )
    config.publish(APP_KEYS.onConfigurationChanged, [page, options])
  } else {
    console.trace(
      `### App Framework ### 执行生命周期onConfigurationChanged时，页面 '${pageId}' 无效：${JSON.stringify(
        options
      )}`
    )
  }
}

/**
 * 监听设备屏幕旋转触发
 * @param id
 * @param param
 */
function orientationChangePage(id, param) {
  const page = _pageMap[id]
  if (page) {
    console.trace(
      `### App Framework ### orientationChangePage 页面(${id})响应：${JSON.stringify(param)}`
    )
    config.publish(APP_KEYS.onOrientationChange, [page, param])
  } else {
    console.error(`### App Framework ### 执行生命周期onOrientationChange时，页面 '${id}' 无效`)
  }
}

/**
 * 刷新当前页，但并不重新创建
 * @param id
 * @param query
 * @param intent
 */
function refreshPage(id, query, intent) {
  query = query || {}
  const page = _pageMap[id]
  if (page) {
    console.trace(`### App Framework ### refreshPage 页面(${id})响应：${JSON.stringify(query)}`)
    config.publish(APP_KEYS.onRefresh, [page, query, intent])
  } else {
    console.error(`### App Framework ### 执行生命周期onRefresh时，页面 '${id}' 无效`)
  }
}

/**
 * 监控应用内部错误
 * @param id
 * @param param
 */
function notifyAppError(id, param) {
  const app = _appMap[id]
  if (app) {
    console.trace(`### App Framework ### notifyAppError 应用(${id})响应`)
    if (app.$valid) {
      try {
        let err, info
        if (isPlainObject(param)) {
          err = param
          info = param.message
        } else {
          err = new Error(`${param}`)
          info = param
        }
        // 同时触发新增的 onErrorHandler 生命周期
        xHandleError(err, undefined, info, app)
      } catch (err) {
        err.message = `$INTERRUPTION$:${err.message}`
        throw err
      }
    }
  } else {
    console.error(`### App Framework ### notifyAppError 应用 '${id}' 无效`)
  }
}

/**
 * 销毁页面
 * @param  {string} id
 */
function destroyPage(id) {
  const page = _pageMap[id]
  if (!page) {
    return new Error(`destroyPage: 无效页面Id '${id}'`)
  }
  config.publish(APP_KEYS.destroyPage, [page])
  destroyPageCore(page)
  delete _pageMap[id]

  return _pageMap
}

/**
 * 销毁App
 * @param  {string} id
 */
function destroyApplication(id) {
  const app = _appMap[id]
  if (!app) {
    return new Error(`destroyApplication: 无效应用Id "${id}"`)
  }
  destroyApp(app)
  // 允许销毁后的接口调用
  // delete _appMap[id]

  return _appMap
}

/**
 * 给组件添加原生方法
 * @param page
 * @param element
 * @returns {*}
 */
function bindComponentMethods(page, element) {
  if (element && !element._hasBind) {
    const elementType = element.tagName.toLowerCase()

    // 绑定方法：animate()
    element.animate = _bindAnimation(element, page)
    // 绑定Native组件方法
    const methodNameList = config.runtime.helper.bindComponentMethods(element)
    methodNameList.forEach(function(methodName) {
      const originalDef = element[methodName]
      element[methodName] = function(...args) {
        if (page && page.doc && page.$valid) {
          // 通知组件方法的调用
          config.publish(APP_KEYS.callbackDone, [page])
          // 调用组件方法
          const argList = args.map(arg =>
            normalize(arg, page, {
              vm: element._xvm,
              info: `callback for ${methodName}`
            })
          )
          originalDef.apply(element[methodName], argList)
        }
      }
    })
    // 绑定方法：getViewId()
    element.getViewId = function() {
      return global.getPageElementViewId(element.ref)
    }

    if (elementType === 'canvas') {
      _bindCanvas(element, page)
    }

    element._hasBind = true
  }
  return element
}

/**
 * 打开错误页面时发起的回调
 * @param id
 * @param params
 */
function notifyPageNotFound(id, params = {}) {
  const app = _appMap[id]
  if (!app) {
    console.error(`### App Framework ### notifyPageNotFound app不存在，id：${id}`)
  } else {
    console.trace(
      `### App Framework ### notifyPageNotFound 应用(${id})响应， 参数： ${JSON.stringify(params)}`
    )
    if (app.$valid) {
      // applc means application lifecycle
      app._emit('applc:onPageNotFound', params)
      console.trace(
        `### App Framework ### 调用 onPageNotFound 回调成功， 参数： ${JSON.stringify(params)}`
      )
    } else {
      console.trace(`### App Framework ### 调用 onPageNotFound 回调时应用(${id})已销毁`)
    }
  }
}

/**
 * 支持canvas
 * @param element
 * @param page
 * @returns {*}
 * @private
 */
function _bindCanvas(element, page) {
  const canvas = config.platform.requireBundle('canvas.js')
  const canvasModule = config.platform.requireModule(page.app, 'system.canvas')
  element = canvas.enable(element, page.app, { module: canvasModule })
  return element
}

/**
 * 支持 Animation Api
 * @param {object} element: 元素对象；
 * @param {object} page   : 页面对象；
 */
function _bindAnimation(element, page) {
  return (keyframes, options) => {
    if (options && options.id && typeof options.id !== 'string') {
      console.error(`### App Framework ### animate 输入参数 options.id 非法，类型需为 string`)
      return
    }
    const params = {
      keyframes,
      options
    }
    const Animation = config.platform.requireBundle('animation.js')
    const animationModule = config.platform.requireModule(page.app, 'system.animation')
    const animateInstance = new Animation(page, element, params, animationModule)
    return Object.freeze(animateInstance)
  }
}

// 标识是否用到splitChunks规范
let _useSplitChunksFlag = false

/**
 * 提供带有context的加载独立的JS资源
 * @param context 页面或者APP
 * @param globals
 * @return {evaluateBuildScript}
 */
function makeEvaluateBuildScript(globals) {
  return function evaluateBuildScript(buildFilePath) {
    const globalObjects = Object.assign(
      {
        $app_evaluate$: evaluateBuildScript
      },
      globals
    )

    if (!_useSplitChunksFlag) {
      _useSplitChunksFlag = true
      profiler.record(
        `### App Performance ### 用到加载JS文件的能力[PERF:evaluateBuildScript]：${new Date().toJSON()}`
      )
    }

    console.trace(`### App Performance ### evaluateBuildScript('${buildFilePath}')`)
    return config.platform.requireScriptFile(buildFilePath, globalObjects)
  }
}

/**
 * 页面滚动到顶部触发的逻辑
 * @param id 页面id
 */
function reachPageTop(id) {
  const page = _pageMap[id]
  if (page) {
    console.trace(`### App Framework ### reachPageTop 页面(${id})响应`)
    config.publish(APP_KEYS.onReachTop, [page])
  } else {
    console.error(`### App Framework ### 执行生命周期onReachTop时，页面 '${id}' 无效`)
  }
}

/**
 * 页面滚动到底部触发的逻辑
 * @param id 页面id
 */
function reachPageBottom(id) {
  const page = _pageMap[id]
  if (page) {
    console.trace(`### App Framework ### reachPageBottom 页面(${id})响应`)
    config.publish(APP_KEYS.onReachBottom, [page])
  } else {
    console.error(`### App Framework ### 执行生命周期onReachBottom时，页面 '${id}' 无效`)
  }
}

/**
 * 页面滚动时触发的逻辑
 * @param id 页面id
 * @param params 滚动参数
 * @param params.scrollTop 页面滚动高度
 */
function pageScroll(id, params) {
  const page = _pageMap[id]
  if (page) {
    console.trace(`### App Framework ### pageScroll 页面(${id})响应`)
    config.publish(APP_KEYS.onPageScroll, [page, params])
  } else {
    console.error(`### App Framework ### 执行生命周期onPageScroll时，页面 '${id}' 无效`)
  }
}

const dock = {
  config,
  init,
  locateDsl,
  registerDsl,
  changeAppLocale,
  createApplication,
  onRequestApplication,
  onShowApplication,
  onHideApplication,
  notifyAppError,
  destroyApplication,
  createPage,
  recreatePage,
  destroyPage,
  changeVisiblePage,
  keyPressPage,
  backPressPage,
  menuButtonPressPage,
  menuPressPage,
  notifyConfigurationChanged,
  orientationChangePage,
  refreshPage,
  processCallbacks,
  processRenderHooks,
  getAppConfig,
  getPageRoot,
  getPage,
  reachPageTop,
  reachPageBottom,
  pageScroll,
  getPageElementStyles,
  setPageElementStyles,
  setPageElementAttrs,
  replacePageElementWithHtml,
  bindComponentMethods,
  notifyPageNotFound,
  makeEvaluateBuildScript,
  exposure: methodMap
}

export default dock
