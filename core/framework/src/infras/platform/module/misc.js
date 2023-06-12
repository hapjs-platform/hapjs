/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $typeof, isObject, isFunction, uniqueCallbackId } from 'src/shared/util'

import { xInvokeWithErrorHandling } from 'src/shared/error'

// 回调上下文映射
const _callbackSourceMap = {}

// 模块宿主的映射
const _moduleHostMap = {}

// 原生模块定义表
let _nativeModuleMap = {}

// 原生模块常量
const MODULES = {
  // 同步,回调,订阅
  MODE: {
    SYNC: 0,
    CALLBACK: 1,
    SUBSCRIBE: 2,
    SYNC_CALLBACK: 3
  },
  // 方法,属性,事件
  TYPE: {
    METHOD: 0,
    ATTRIBUTE: 1,
    EVENT: 2
  },
  // 传递参数的类型
  NORMALIZE: {
    RAW: 0,
    JSON: 1
  },
  // 方法返回的数据类型
  RESULT: {
    MODULE_INST: 0
  },
  // 是否支持多回调
  MULTIPLE: {
    SINGLE: 0,
    MULTI: 1
  }
}

/**
 * 初始化原生模块配置信息对象
 * @param modulesDef
 * @param ifReplace  是否覆盖原来的实现
 */
function initModulesDef(modulesDef, ifReplace = true) {
  // 转换为数组
  let arr = []
  if (Array.isArray(modulesDef)) {
    arr = modulesDef
  } else {
    arr.push(modulesDef)
  }

  arr.forEach(item => {
    const name = item.name
    console.trace(`### App Framework ### 注册模块---- ${name} <${item.__type__}>`)
    // 初始化`modulesDef[moduleName]`
    let nativeModuleDef = _nativeModuleMap[name]
    if (!nativeModuleDef) {
      nativeModuleDef = {
        type: item.__type__,
        name: item.name,
        methods: {},
        attributes: {},
        events: {},
        // 是否可实例化，可实例化为类，否则为普通模块
        instantiable: item.instantiable
      }
      _nativeModuleMap[name] = nativeModuleDef
    }

    if (!nativeModuleDef.methods) {
      nativeModuleDef.methods = {}
    }

    const methods = nativeModuleDef.methods
    // 记录模块的所有方法名
    if (item.methods && item.methods.length) {
      item.methods.forEach(method => {
        const methodName = method.name

        // 默认:同步
        if (method.mode === undefined) {
          method.mode = MODULES.MODE.SYNC
        }
        // 默认:方法
        if (method.type === undefined) {
          method.type = MODULES.TYPE.METHOD
        }
        // 默认:调底层时的类型
        if (method.normalize === undefined) {
          method.normalize = MODULES.NORMALIZE.JSON
        }
        // 默认:模块的方法
        if (method.instanceMethod === undefined) {
          method.instanceMethod = false
        }

        // 不允许以下情况存在
        if (!nativeModuleDef.instantiable && method.instanceMethod) {
          throw new Error(`模块 ${nativeModuleDef.name} 配置定义错误`)
        }

        if (method.type === MODULES.TYPE.ATTRIBUTE) {
          const alias = method.alias
          const access = method.access
          nativeModuleDef.attributes[alias] = nativeModuleDef.attributes[alias] || {}
          nativeModuleDef.attributes[alias][access] = method
          nativeModuleDef.attributes[alias].instanceMethod = method.instanceMethod
        } else if (method.type === MODULES.TYPE.EVENT) {
          const alias = method.alias
          nativeModuleDef.events[alias] = method
        }

        if (!methodName) {
          console.warn(`### App Framework ### 模块 ${name} 的接口没有name属性`)
        } else {
          // 覆盖旧方法
          if (!methods[methodName] || ifReplace) {
            methods[methodName] = method
            console.trace(`### App Framework ### 注册模块 ${name} 接口---- ${methodName}`)
          }
        }
      })
    }
  })
}

/**
 * 创建模块对象（loader中声明）
 * @param inst
 * @param name
 * @param moduleInstId
 */
function requireModule(inst, name, moduleInstId) {
  console.trace(`### App Framework ### require模块：${name}`)

  const matchedModuleNames = [name]
  // 模块对象
  let moduleObj = {}
  // 模块，可能为模块对象，也可能处于模块对象的某一层级
  let moduleItem

  // 如果没有定义子模块，则遍历查找所有匹配的模块
  if (name.indexOf('.') < 0) {
    const prefix = name + '.'
    for (const moduleName in getNativeModuleMap()) {
      if (moduleName.startsWith(prefix)) {
        matchedModuleNames.push(moduleName)
      }
    }
  }

  // 初始化模块名对应的模块
  matchedModuleNames.forEach(fullName => {
    const nativeModuleDef = getNativeModuleMap()[fullName]
    // 如果找不到定义, 则跳过
    if (!nativeModuleDef) {
      return
    }

    // 子模块名
    let subName = fullName.replace(name, '')
    if (subName.substr(0, 1) === '.') {
      subName = subName.substr(1)
    }

    // 模块为类，特殊处理
    if (nativeModuleDef.instantiable) {
      const classDef = function(...args) {
        const retObj = classDef.__init__(...args)
        Object.defineProperty(this, '_instId', {
          enumerable: false,
          configurable: false,
          writable: false,
          value: retObj && retObj.instId
        })
      }
      if (subName.length === 0) {
        // 模块对象仅包含一个模块，直接覆盖模块对象
        moduleObj = classDef
      } else {
        // 模块对象包含多个模块，写模块对象的对应位置
        const subPath = subName.split('.')
        if (subPath.length > 0) {
          const lastKey = subPath.pop()
          createObjByPath(moduleObj, subPath)[lastKey] = classDef
        }
      }
    }

    // 获取模块对象中模块的指针
    if (fullName === name) {
      moduleItem = moduleObj
    } else {
      if (subName.length > 0) {
        const subPath = subName.split('.')
        if (subPath.length > 0) {
          moduleItem = createObjByPath(moduleObj, subPath)
        }
      }
    }

    // 初始化模块
    initModule(inst, moduleItem, nativeModuleDef, moduleInstId)
  })

  if (Object.keys(moduleObj).length === 0) {
    throw new Error(`请确认引入的模块[${name}]：名称正确并且在manifest.json的features中声明`)
  }

  return moduleObj
}

/**
 * 创建层级对象
 * @param obj
 * @param pathList
 */
function createObjByPath(obj, pathList) {
  if (!obj) {
    return
  }
  let cur = obj
  pathList.forEach(p => {
    if (!(p in cur)) {
      cur[p] = {}
    }
    cur = cur[p]
  })
  return cur
}

/**
 * 初始化模块
 * @param inst
 * @param module
 * @param moduleDef
 * @param moduleInstId
 */
function initModule(inst, module, moduleDef, moduleInstId) {
  // 初始化模块方法
  const methods = moduleDef.methods
  for (const methodName in methods) {
    const method = methods[methodName]
    // 原型：承载方法的定义
    const obj = method.instanceMethod ? module.prototype : module
    // 将原生模块的函数定义通过invoke函数封装为Action函数
    if (methodName in obj) {
      console.warn(
        `### App Framework ### 模块${moduleDef.name}的接口函数${methodName}---- 重复定义`
      )
    }
    const modMethod = function(...args) {
      if (inst && inst._isApp && !inst._defined) {
        throw new Error(
          `请确认Native方法调用[${moduleDef.name}.${methodName}()]发生在应用app的生命周期的创建['onCreate()']之后`
        )
      }
      const instId = Object.prototype.hasOwnProperty.call(this, '_instId')
        ? this._instId
        : moduleInstId
      return invokeNative(inst, moduleDef, method, args, instId)
    }

    Object.defineProperty(obj, methodName, {
      configurable: false,
      enumerable: true,
      get() {
        return modMethod.bind(this)
      },
      set(val) {
        console.warn(
          `### App Framework ### 接口${moduleDef.name}的方法(${methodName})为可读，不可覆盖`
        )
      }
    })
    console.trace(`### App Framework ### require---- 模块${moduleDef.name}接口函数${methodName}`)
  }

  // 初始化模块属性
  const attributes = moduleDef.attributes
  for (const attributeName in attributes) {
    const attributeItem = attributes[attributeName]
    // 原型：承载方法的定义
    const obj = attributeItem.instanceMethod ? module.prototype : module
    Object.defineProperty(obj, attributeName, {
      configurable: false,
      enumerable: true,
      get() {
        if (!attributeItem[1]) {
          console.warn(
            `### App Framework ### 模块${moduleDef.name}的接口属性(${attributeName})不可读`
          )
        } else {
          let result = this[attributeItem[1].name]()
          // 处理子属性
          if (
            (attributeItem[1] && attributeItem[1].subAttrs) ||
            (attributeItem[2] && attributeItem[2].subAttrs)
          ) {
            const _this = this
            if (!result) {
              result = {}
            }
            let allAttrs = []
            if (attributeItem[1] && attributeItem[1].subAttrs) {
              allAttrs = allAttrs.concat(attributeItem[1].subAttrs)
            }
            if (attributeItem[2] && attributeItem[2].subAttrs) {
              allAttrs = allAttrs.concat(attributeItem[1].subAttrs)
            }
            const attrSet = new Set(allAttrs)
            allAttrs = Array.from(attrSet)
            for (const subIndex in allAttrs) {
              const subAttr = allAttrs[subIndex]
              Object.defineProperty(result, subAttr, {
                configurable: true,
                enumerable: true,
                get() {
                  if (
                    !attributeItem[1] ||
                    !attributeItem[1].subAttrs ||
                    attributeItem[1].subAttrs.indexOf(subAttr) < 0
                  ) {
                    console.warn(
                      `### App Framework ### 模块${moduleDef.name}的接口属性(${attributeName})的子属性(${subAttr})不可读`
                    )
                  } else {
                    return _this[attributeItem[1].name]()[subAttr]
                  }
                },
                set(val) {
                  if (
                    !attributeItem[2] ||
                    !attributeItem[2].subAttrs ||
                    attributeItem[2].subAttrs.indexOf(subAttr) < 0
                  ) {
                    console.warn(
                      `### App Framework ### 模块${moduleDef.name}的接口属性(${attributeName})的子属性(${subAttr})不可写`
                    )
                  } else {
                    const valObj = {}
                    valObj[subAttr] = val
                    _this[attributeItem[2].name]({ value: valObj })
                  }
                }
              })
            }
          }
          return result
        }
      },
      set(val) {
        if (!attributeItem[2]) {
          console.warn(
            `### App Framework ### 模块${moduleDef.name}的接口属性(${attributeName})不可写`
          )
        } else {
          this[attributeItem[2].name]({ value: val })
        }
      }
    })
  }

  // 初始化模块事件
  const events = moduleDef.events
  for (const eventName in events) {
    const eventItem = events[eventName]
    eventItem.cache = eventItem.cache || {}
    // 原型：承载方法的定义
    const obj = eventItem.instanceMethod ? module.prototype : module
    Object.defineProperty(obj, eventName, {
      configurable: false,
      enumerable: true,
      get() {
        const instId = this._instId === undefined ? -1 : this._instId
        return eventItem.cache[instId]
      },
      set(val) {
        if (typeof val !== 'function' && [null, undefined].indexOf(val) === -1) {
          console.warn(
            `### App Framework ### 模块${moduleDef.name}的接口事件(${eventName})值类型必须是函数或null`
          )
        } else {
          const instId = this._instId === undefined ? -1 : this._instId
          const cb = typeof val === 'function' ? val.bind(this) : val
          this[eventItem.name]({ success: cb })
          eventItem.cache[instId] = val
        }
      }
    })
  }
}

/**
 * 调用接口方法的函数
 * @param inst
 * @param module
 * @param method
 * @param args
 * @param moduleInstId 对象实例id
 */
const _callbackArgs = ['success', 'cancel', 'fail', 'complete']

function invokeNative(inst, module, method, args, moduleInstId) {
  const { name: modName, type: modType } = module
  const {
    name: mthName,
    mode: mthMode,
    type: mthType,
    normalize: mthNmlz,
    multiple: mthMultiple
  } = method

  if (!inst || !inst._callbacks) {
    console.warn(`### App Framework ### 容器已销毁,接口调用(${modName}.${mthName}())无效`)
    return new Error(`invokeNative: 容器已销毁`)
  }
  if (modType === 'feature' && !global.JsBridge) {
    return new Error(`invokeNative: JsBridge没有初始化`)
  } else if (modType === 'module' && !global.ModuleManager) {
    return new Error(`invokeNative: ModuleManager没有初始化`)
  }

  profiler.time(`PERF:invokeMod:${modName}.${mthName}()`)

  const bridge = modType === 'feature' ? global.JsBridge : global.ModuleManager

  // args仅有一个参数，传参且参数为undefined值时，warn
  if (args.length > 0 && args[0] === undefined) {
    console.warn(`### App Framework ### 接口调用${modName}.${mthName}的参数为 undefined`)
  }
  const arg0 = args.length > 0 ? args[0] : {}

  if (arg0 && arg0.callback) {
    if (arg0.success) {
      console.warn(`### App Framework ### invoke函数不能同时出现'success'和'callback'参数`)
    }
    arg0.success = arg0.callback
  }

  // 提取目标参数
  let newArgs = {}
  const cbs = {}

  if (isObject(arg0)) {
    for (const arg in arg0) {
      const value = arg0[arg]
      if (_callbackArgs.indexOf(arg) >= 0) {
        if (typeof value === 'function') {
          cbs[arg] = value
        } else {
          console.warn(`### App Framework ### invoke函数的回调参数${arg}类型不是function`)
        }
      } else if (arg !== 'callback') {
        if (mthNmlz === MODULES.NORMALIZE.JSON) {
          newArgs[arg] = normalize(value, inst)
        } else {
          if (typeof arg === 'function') {
            newArgs[arg] = normalize(value, inst)
          } else {
            newArgs[arg] = value
          }
        }
      }
    }
  } else if (isFunction(arg0)) {
    cbs.success = arg0
  } else {
    newArgs = arg0
  }

  if (mthNmlz === MODULES.NORMALIZE.JSON) {
    newArgs = JSON.stringify(newArgs)
  }

  // 调用原生模块函数
  if (mthMode === MODULES.MODE.SYNC) {
    let cbId = '-1'
    if (mthMultiple === MODULES.MULTIPLE.MULTI && isFunction(arg0)) {
      cbId = mapInvokeCallbackId(arg0).toString()
      console.trace(`${mthName} 方法的回调函数参数id：${cbId}`)
      // 找不到对应的回调，则不执行
      if (cbId === '-1') {
        return
      }
    }
    const ret = bridge.invoke(modName, mthName, newArgs, cbId, moduleInstId)
    if (ret == null) {
      console.warn(`### App Framework ### invoke函数 '${modName}.${mthName}' 返回值为null`)
      return undefined
    }
    console.trace(
      `### App Framework ### invoke函数 '${modName}.${mthName}' 调用成功，返回值为: ${ret}`
    )
    const result = transformModuleResult(inst, ret, module, mthName)
    return result.data
  } else {
    // 异步方法：不传递回调函数时要返回Promsie
    if (mthType === MODULES.TYPE.METHOD && mthMode === MODULES.MODE.CALLBACK) {
      cbs.flagCallback = true
    }

    let pInst, pRes, pRej
    const argList = []
    if (Object.keys(cbs).length) {
      // this指向：1040起更新为undefined
      const thisContext = global.isRpkMinPlatformVersionGEQ(1040) ? undefined : cbs
      // 需要回调
      let cbId = -1

      // 多监听场景，避免同一回调指针，重复监听
      if (mthMultiple === MODULES.MULTIPLE.MULTI) {
        cbId = mapInvokeCallbackId(cbs.success)
        if (cbId === -1) {
          console.trace(`### App Framework ###  新增监听实例，id：${cbId}`)
          cbId = uniqueCallbackId()
        }
      } else {
        cbId = uniqueCallbackId()
      }
      inst._callbacks[cbId] = ret => {
        const callbacks = cbs
        const result = transformModuleResult(inst, ret, module, mthName)
        const code = result.code
        const data = result.data
        // inst 可能是 page 实例，也可能是 app 实例
        const app = inst.app || inst || {}
        let errInfo = ''
        let cbArgs = null
        let curCb

        if (code === 0 && callbacks.success) {
          errInfo = 'success/callback'
          cbArgs = [data]
          curCb = callbacks.success
        } else if (code === 100 && callbacks.cancel) {
          errInfo = 'cancel'
          curCb = callbacks.cancel
        } else if (code >= 200 && callbacks.fail) {
          errInfo = 'fail'
          cbArgs = [data, code]
          curCb = callbacks.fail
        }
        if (curCb) {
          errInfo = `${module.name}: "${errInfo}" callback of "${mthName}"`
          xInvokeWithErrorHandling(curCb, thisContext, cbArgs, undefined, errInfo, app, inst)
        }
        if (callbacks.complete) {
          errInfo = `${module.name}: "complete" callback of "${mthName}"`
          xInvokeWithErrorHandling(
            callbacks.complete,
            thisContext,
            [data],
            undefined,
            errInfo,
            app,
            inst
          )
        }

        if (pInst) {
          code === 0 ? pRes({ data }) : pRej({ data, code })
        }
      }
      _callbackSourceMap[cbId] = {
        instance: inst.id.toString(),
        preserved: mthMode === MODULES.MODE.SUBSCRIBE,
        cbFunc: cbs.success
      }

      argList.push(newArgs)
      argList.push(cbId.toString())
    } else {
      // 无需回调
      argList.push(newArgs)
      argList.push('-1')
    }
    // 调用原生模块函数
    const ret = bridge.invoke(modName, mthName, ...argList, moduleInstId)
    profiler.timeEnd(`PERF:invokeMod:${modName}.${mthName}()`)

    // 是否要返回Promise
    if (mthMode === MODULES.MODE.SYNC_CALLBACK && ret) {
      const result = transformModuleResult(inst, ret, module, mthName)
      return result.data
    }

    if (
      mthType === MODULES.TYPE.METHOD &&
      mthMode === MODULES.MODE.CALLBACK &&
      Object.keys(cbs).length === 1
    ) {
      return (pInst = new Promise((res, rej) => {
        pRes = res
        pRej = rej
      }))
    }
  }
}
/**
 * 分情况返回不同模块
 * @param inst
 * @param result
 * @param module
 * @param mthName
 */
function transformModuleResult(inst, result, module, mthName) {
  const retObj = typeof result === 'string' ? JSON.parse(result) : result || {}
  const retCnt = retObj.content

  if (retCnt && retCnt._nativeType === MODULES.RESULT.MODULE_INST) {
    // 模块实例
    if (module.instantiable && mthName === '__init__') {
      retObj.data = retCnt
    } else {
      retObj.data = requireModule(inst, retCnt.name, retCnt.instId)
    }
    inst._nativeInstList && inst._nativeInstList.push(result)
    // 保持对句柄的引用
    retObj.data.instHandler = retCnt.instHandler
  } else {
    // 普通模块
    retObj.data = retCnt
  }

  return retObj
}

/**
 * 参数转换，全部转为基础类型
 * @param v
 * @param inst
 * @returns {*}
 */
function normalize(v, inst) {
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
    case 'object':
      return v // 其余一律返回原始对象
    case 'function':
      const cbId = uniqueCallbackId()
      if (!inst._callbacks) {
        console.trace(`### App Framework ### normalize() inst实例已经销毁，不再注册回调`)
      } else {
        inst._callbacks[cbId] = v
      }
      _callbackSourceMap[cbId] = {
        instance: inst.id.toString(),
        preserved: false,
        cbFunc: v
      }
      return cbId.toString()
    default:
      return JSON.stringify(v) // 其余一律转换为字符串
  }
}

/**
 * 映射回调id到对象实例Id
 * @param id
 * @returns {*}
 */
function mapInvokeCallback(id) {
  const info = _callbackSourceMap[id]
  if (info && info.preserved === true) {
  } else {
    _callbackSourceMap[id] = undefined
  }
  return info
}

/**
 * 映射回调对象到回调id
 * @param cb
 * @returns {cbId}
 */
function mapInvokeCallbackId(cb) {
  let cbId = -1
  for (const id in _callbackSourceMap) {
    if (
      _callbackSourceMap[id] &&
      _callbackSourceMap[id].cbFunc &&
      _callbackSourceMap[id].cbFunc === cb
    ) {
      cbId = id
      break
    }
  }
  return cbId
}

function getModuleHostMap(id) {
  return _moduleHostMap[id]
}

function setModuleHostMap(id, value) {
  _moduleHostMap[id] = value
}

function getNativeModuleMap() {
  return _nativeModuleMap
}

/**
 * 获取模块定义
 * @param moduleName
 * @returns {*}
 */
function getNativeModule(moduleName) {
  return _nativeModuleMap[moduleName]
}

/**
 * 清空模块
 */
function clearNativeModuleMap() {
  _nativeModuleMap = {}
}

export {
  MODULES,
  initModulesDef,
  requireModule,
  invokeNative,
  mapInvokeCallback,
  getModuleHostMap,
  setModuleHostMap,
  getNativeModuleMap,
  getNativeModule,
  clearNativeModuleMap
}
