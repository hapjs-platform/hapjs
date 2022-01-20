/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { removeAppPrefix } from 'src/shared/util'
import ModuleHost from '../../platform/module/index'
import { postMessage, onMessageInternal, onMessageErrorInternal } from './message'

const config = {}

function initWorker(inst, code) {
  console.trace(`### App Worker ### 平台配置信息：`, JSON.stringify(global.Env))

  const instRequireModule = name => {
    return config.platform.requireModule(inst, removeAppPrefix(name))
  }
  // 处理代码
  let functionBody
  if (typeof code === 'function') {
    functionBody = code
  } else if (code) {
    functionBody = code.toString()
    // 重新封装代码
    functionBody = `(function(global){"use strict"; \n ${functionBody} \n })(Object.create(this))`
  }
  console.trace(`### App Worker ### 开始编译代码`)

  if (ENV_PLATFORM === 'na') {
    // 如果是原生渲染
    const fn = new Function('$app_require$', functionBody)

    fn(instRequireModule)
  } else {
    // H5 渲染
    global.$app_require$ = instRequireModule

    functionBody()
  }
}

function createWorker(id, code, options) {
  const inst = new ModuleHost(id, options)
  const result = initWorker(inst, code)

  return result
}

function init(glue) {
  config.platform = glue.platform
}

export default {
  init,
  exposure: {
    createWorker,
    postMessage,
    onMessageInternal,
    onMessageErrorInternal
  }
}
