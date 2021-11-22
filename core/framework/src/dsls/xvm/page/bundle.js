/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { isPlainObject, isComponent, removeAppPrefix } from 'src/shared/util'

import XVm from '../vm'

import { defineCustomComponent } from '../app/custom'
import { defineModule } from '../app/bundle'

export { $defineWrap } from '../app/bundle'

/**
 * 创建页面实例
 * @param page
 * @param name  ‘@app-component/xxxx’
 * @param config
 * @param query
 * @returns {Error}
 */
const $bootstrap = function(page, name, config, query) {
  console.trace(`### App Framework ### 启动页面---- ${name}`)

  // 验证页面‘@app-component/xxxx’
  let pageName
  if (isComponent(name)) {
    pageName = removeAppPrefix(name)
  } else {
    return new Error(`bootstrap: 错误页面名: ${name}`)
  }

  // 验证配置信息（确保config是对象）,确保文件打包版本与Runtime版本保持兼容
  config = isPlainObject(config) ? config : {}

  profiler.record(`### App Performance ### 实例化页面VM[PERF:newVm]开始：${new Date().toJSON()}`)
  profiler.time(`PERF:newVm`)
  // 当前页面名
  page.name = pageName
  console.trace(`### App Framework ### 创建页面VM---- ${pageName}`)
  page.vm = new XVm(null, pageName, { _page: page }, null, query, {})
  profiler.timeEnd(`PERF:newVm`)
  profiler.record(`### App Performance ### 实例化页面VM[PERF:newVm]结束：${new Date().toJSON()}`)
}

/**
 * 定义组件
 * @param app
 * @param name
 * @param args
 */
const $define = function(page, name, ...args) {
  console.trace(`### App Framework ### 定义组件---- ${name}`)

  // 应用定义
  if (!isComponent(name)) {
    return new Error(`define: 非法组件名 ${name}, 必须是"@app-component/XXXX"格式`)
  }

  const definition = defineModule(page, name, args)

  // 应用定义
  const cleanName = removeAppPrefix(name)
  // 将组件定义放入app.customComponentMap中
  console.trace(`### App Framework ### 注册自定义组件---- ${cleanName}`)
  defineCustomComponent(page.app, page, cleanName, definition)
}

export { $bootstrap, $define }
