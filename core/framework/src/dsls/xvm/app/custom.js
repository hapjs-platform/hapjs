/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * 通过key=id隔离并返回context
 * @param app
 * @param id
 * @return {string}
 */
function contextCustomComponent(app, id) {
  const { customComponentMap } = app
  customComponentMap[id] = customComponentMap[id] || {}
  return customComponentMap[id]
}

/**
 * 注册自定义组件, 不能重复注册
 * @param app
 * @param inst
 * @param name
 * @param def
 */
function defineCustomComponent(app, inst, name, def) {
  if (!app || !inst) {
    console.warn(`### App Framework ### app或inst不存在，注册自定义组件 '${name}' 失败`)
    return
  }
  const contextCustomComponentMap = contextCustomComponent(app, inst.id)
  if (contextCustomComponentMap[name]) {
    console.warn(`### App Framework ### 组件 '${name}' 被重复注册`)
  }
  contextCustomComponentMap[name] = def
}

/**
 * 获取依赖自定义组件
 * @param app
 * @param inst
 * @param name
 * @returns {*}
 */
function requireCustomComponent(app, inst, name) {
  if (!app || !inst) {
    console.warn(`### App Framework ### app或inst不存在，加载自定义组件 '${name}' 失败`)
    return
  }

  const contextCustomComponentMap = contextCustomComponent(app, inst.id)
  const ret = contextCustomComponentMap[name]

  if (!ret) {
    if (app.id !== inst.id) {
      console.trace(`### App Framework ### requireCustomComponent() 从APP中加载自定义组件：${name}`)
      return requireCustomComponent(app, app, name)
    }
  }

  return ret
}

/**
 * 删除自定义组件
 * @param app
 * @param inst
 * @param name
 */
function removeCustomComponent(app, inst, name) {
  if (!app || !inst) {
    console.warn(`### App Framework ### app或inst不存在，删除自定义组件 '${name}' 失败`)
    return
  }
  const contextCustomComponentMap = contextCustomComponent(app, inst.id)
  if (name) {
    console.trace(
      `### App Framework ### removeCustomComponent() 删除id=${
        inst.id
      }上注册的自定义组件(${name}):${!!contextCustomComponentMap[name]}`
    )
    delete contextCustomComponentMap[name]
  } else {
    for (const componentName in contextCustomComponentMap) {
      console.trace(
        `### App Framework ### removeCustomComponent() 删除id=${
          inst.id
        }上注册的自定义组件(${componentName}):${!!contextCustomComponentMap[componentName]}`
      )
      delete contextCustomComponentMap[componentName]
    }

    app.customComponentMap[inst.id] = null
  }
}

export { defineCustomComponent, requireCustomComponent, removeCustomComponent }
