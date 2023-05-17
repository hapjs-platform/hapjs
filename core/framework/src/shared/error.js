/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import XLinker from 'src/dsls/xvm/vm/linker'
import { isPromise } from 'src/shared/util'

/**
 * 在卡片环境下获取页面的vm
 * @param page page 实例
 */
function getPageVmInCard(page) {
  let vm
  if (global.Env.engine === global.ENGINE_TYPE.CARD && page && page._isPage) {
    vm = page.vm
  }
  return vm
}

/**
 * 触发 页面/组件 的 onErrorCaptured 回调
 * @param err 捕获的错误
 * @param vm vm
 * @param info 附加错误信息
 * @param app app 实例
 * @param page page 实例
 */
function xHandleError(err, vm, info = '', app, page) {
  // 在处理错误处理程序时禁用 linker 跟踪，以避免可能的无限渲染
  XLinker.pushTarget()
  try {
    let curVm

    info = String(info)
    if (vm) {
      curVm = vm
    } else {
      // 卡片环境无法收集 app.ux 中定义的生命周期回调
      // 部分拿不到vm的场景（定时器回调、接口回调）无法触发 onErrorHandler 回调
      // 故在此做兼容，使其触发页面的 onErrorCaptured 回调
      curVm = getPageVmInCard(page)
    }

    while (curVm) {
      const hooks = curVm._errorCapturedCbs

      if (hooks) {
        for (let i = 0; i < hooks.length; i++) {
          try {
            // 是否需要将错误向上传递
            const capture = hooks[i].call(curVm, err, vm, info) === false
            if (capture) return
          } catch (e) {
            xGlobalHandleError(e, curVm, `page/component: lifecycle for "onErrorCaptured"`, app)
          }
        }
      }
      curVm = curVm._parent
    }
    xGlobalHandleError(err, vm, info, app)
  } finally {
    XLinker.popTarget()
  }
}

/**
 * 用于回调错误的捕获，当回调有可能为 async 函数时(主要是用户输入)，则应该使用此函数进行捕获，否则可使用 xHandleError
 * @param handler 回调函数
 * @param context 回调所需上下文
 * @param args 回调参数，应保证为数组类型或null
 * @param vm vm
 * @param info 附加错误信息
 * @param app app 实例
 * @param page page 实例
 */
function xInvokeWithErrorHandling(handler, context, args, vm, info, app, page) {
  let res
  try {
    res = args ? handler.apply(context, args) : handler.call(context)
    // 回调有可能是 async 函数，返回值为 promise，所以需要对该 promise 内部的错误进行捕获
    if (res && !res._isXVm && isPromise(res) && !res._handled) {
      res.catch(e => xHandleError(e, vm, info, app, page))
      // 防止因 xInvokeWithErrorHandling 嵌套导致错误重复捕获
      res._handled = true
    }
  } catch (e) {
    xHandleError(e, vm, info, app, page)
  }
  return res
}

/**
 * 触发 app.ux 的 onErrorHandler 回调或直接输出错误信息
 * @param err 捕获的错误
 * @param vm vm
 * @param info 附加错误信息
 * @param app app 实例
 */
function xGlobalHandleError(err, vm, info, app) {
  let errorHandler
  const _app = vm ? vm.$app : app

  if (_app && _app._isApp) {
    errorHandler = _app._errorHandler
    // 同时触发 onError，避免影响使用 onError 的用户
    appError(_app, err)
  }

  if (errorHandler) {
    try {
      // errorHandler 需指向 app 实例，与 app 生命周期保持一致
      errorHandler.call(_app, err, vm, info)
    } catch (e) {
      // 避免相同错误触发两次（防止用户在 onErrorHandler 中抛出原来的错误）
      if (e !== err) {
        xLogError(e, undefined, `app: lifecycle for "onErrorHandler"`)
      }
    }
    return
  }
  // 若 errorHandler 未定义，保证错误信息能正常输出
  xLogError(err, vm, info)
}

/**
 * 触发 onError 生命周期
 * @param app app 实例
 * @param err 错误
 */
function appError(app, err) {
  try {
    const errHandlers = app._events['applc:onError']

    if (errHandlers) {
      const params = {
        message: err.message || 'null',
        stack: err.stack
      }
      for (let i = 0; i < errHandlers.length; i++) {
        errHandlers[i].call(app, params)
      }
    }
  } catch (e) {
    xLogError(e, undefined, 'app: lifecycle for "onError"')
  }
}

/**
 * 控制台输出错误信息
 * @param err 错误
 * @param vm vm
 * @param info 附加错误信息
 */
function xLogError(err, vm, info) {
  console.error('### App Framework ### error: ', err)
  console.error('### App Framework ### vm of component: ', vm)
  console.error('### App Framework ### error info: ', info)
}

export { xHandleError, xInvokeWithErrorHandling }
