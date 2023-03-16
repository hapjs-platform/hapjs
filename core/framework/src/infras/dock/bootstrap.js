/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// 可用框架列表
let _frameworks = {}
// App实例检索表
const _appMap = {}
const _pageMap = {}
// 公共框架函数
const methodMap = {}

/**
 * 封装注册函数，调用指定框架的实现
 * @param {string} methodName
 */
function _registerRegFunc(methodName) {
  methodMap[methodName] = function(...args) {
    for (const name in _frameworks) {
      const framework = _frameworks[name]
      if (framework && framework[methodName]) {
        framework[methodName](...args)
      }
    }
  }
}

/**
 * 封装App接口函数，调用指定框架的实现
 * @param {string} methodName
 */
function _registerAppFunc(methodName) {
  if (methodName === 'createApplication') {
    methodMap[methodName] = function(id, code, ...args) {
      // 获取App实例信息
      let info = _appMap[id]
      // 如果没有，则新建一个实例对象
      if (!info) {
        info = {
          framework: 'xFramework',
          created: Date.now() // 创建时间
        }
        _appMap[id] = info

        console.log(
          `### App Framework ### 创建应用 ${id} ---- 框架: ${info.framework} 版本: ${global.frameworkVersion}`
        )
        // 调用框架的对应API
        return _frameworks[info.framework].createApplication(id, code, ...args)
      }
      return new Error(`Runtime ${methodName}:无效应用id "${id}"`)
    }
  } else if (methodName === 'destroyApplication') {
    methodMap[methodName] = function(id) {
      const info = _appMap[id]
      if (info && _frameworks[info.framework]) {
        const result = _frameworks[info.framework].destroyApplication(id)
        _appMap[id] = null
        return result
      }
      return new Error(`Runtime ${methodName}:无效应用Id:  "${id}"`)
    }
  } else {
    methodMap[methodName] = function(...args) {
      const id = args[0]
      const info = _appMap[id]
      if (info && _frameworks[info.framework]) {
        return _frameworks[info.framework][methodName](...args)
      }
      return new Error(`Runtime ${methodName}:无效应用Id:  "${id}"`)
    }
  }
}

/**
 * 封装页面接口函数，调用指定框架的实现
 * @param {string} methodName
 */
function _registerPageFunc(methodName) {
  let jsName, nativeName
  if (typeof methodName === 'string') {
    jsName = methodName
  } else if (methodName.length && methodName.length > 1) {
    jsName = methodName[0]
    nativeName = methodName[1]
  }

  if (jsName === 'createPage') {
    methodMap[jsName] = function(id, appid, code, data, intent, meta) {
      // 获取App实例信息
      const appinfo = _appMap[appid]
      if (!appinfo) {
        return new Error(`Runtime ${jsName}:无效应用id "${appid}"`)
      }
      let info = _pageMap[id]
      // 如果没有，则新建一个实例对象
      if (!info) {
        info = {
          appId: appid,
          created: Date.now() // 创建时间
        }
        _pageMap[id] = info

        console.log(
          `### App Framework ### 创建页面 ${id} ---- ${intent &&
            intent.currentPageName} ---- 应用Id: ${appid}`
        )
        // 调用框架的对应API
        return _frameworks[appinfo.framework].createPage(id, appid, code, data, intent, meta)
      }
      return new Error(`Runtime ${jsName}:无效页面id "${id}"`)
    }
  } else {
    methodMap[jsName] = function(...args) {
      const id = args[0]
      const info = _pageMap[id]
      if (info) {
        const appinfo = _appMap[info.appId]
        if (appinfo && _frameworks[appinfo.framework]) {
          const result = _frameworks[appinfo.framework][jsName](...args)
          if (methodName === 'destroyPage') {
            _pageMap[id] = null
          }
          return result
        }
        return new Error(`Runtime ${jsName}:无效应用Id:  "${info.appId}"`)
      }
      if (methodName === 'backPressPage') {
        console.error(`### App Framework ### backPressPage 无效页面Id:  "${id}"`)
        return false
      }
      return new Error(`Runtime ${jsName}:无效页面Id:  "${id}"`)
    }

    if (nativeName) {
      methodMap[nativeName] = function(...args) {
        const id = args[0]
        const info = _pageMap[id]
        if (info) {
          const appinfo = _appMap[info.appId]
          if (appinfo && _frameworks[appinfo.framework]) {
            return _frameworks[appinfo.framework][jsName](...args)
          }
          return new Error(`Runtime ${jsName}:无效页面Id: "${id}"`)
        }
      }
    }
  }
}

const frameworkRegApi = ['registerDsl', 'locateDsl', 'changeAppLocale']

const frameworkAppApi = [
  'createApplication',
  'onRequestApplication',
  'onShowApplication',
  'onHideApplication',
  'getAppConfig',
  'notifyAppError',
  'destroyApplication',
  'notifyPageNotFound'
]

const frameworkPageApi = [
  'createPage',
  'destroyPage',
  'recreatePage',
  'changeVisiblePage',
  'backPressPage',
  'menuButtonPressPage',
  'keyPressPage',
  'menuPressPage',
  'notifyConfigurationChanged',
  'orientationChangePage',
  'refreshPage',
  'reachPageTop',
  'reachPageBottom',
  'pageScroll',
  ['processCallbacks', 'execJSBatch'],
  'processRenderHooks',
  'getPage',
  'getPageRoot',
  'getPageElementStyles',
  'setPageElementStyles',
  'setPageElementAttrs',
  'replacePageElementWithHtml'
]

function bootstrap(glue) {
  _frameworks = {
    xFramework: glue.dock
  }

  // 注册框架函数
  frameworkRegApi.forEach(_registerRegFunc)
  frameworkAppApi.forEach(_registerAppFunc)
  frameworkPageApi.forEach(_registerPageFunc)

  return methodMap
}

export { bootstrap, methodMap }
