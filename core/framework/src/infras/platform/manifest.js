/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * 应用的配置信息
 * @property {integer} minPlatformVersion 默认为1
 * @property {string} package
 * @property {string} name
 * @property {string} versionName
 * @property {integer} versionCode
 * @property {object} config
 */
let manifestJSON = {}

/**
 * 注册RPK中的应用配置
 * @param {string} manifest 开发者包中的manifest对象
 */
function registerManifest(manifest) {
  profiler.record(
    `### App Performance ### 注册manifest[PERF:registerManifest]开始：${new Date().toJSON()}`
  )
  console.trace(`### App Framework ### 注册manifest：${JSON.stringify(manifest)}`)
  if (typeof manifest === 'string') {
    manifest = JSON.parse(manifest)
  }
  manifestJSON = manifest || {}
}

/**
 * 获取Manifest中的字段值
 * @param {string} keypath 通过"."分割获取进一步的值
 */
function getManifestField(keypath) {
  const path = keypath.split('.')
  let result = manifestJSON
  for (let i = 0, len = path.length; i < len; i++) {
    result = result[path[i]]
    if (result === null || result === undefined) {
      break
    }
  }
  return result
}

/**
 * 开发者包是否满足指定值
 * @param val {Integer}
 * @return {boolean}
 */
function isRpkMinPlatformVersionGEQ(val) {
  return manifestJSON.minPlatformVersion >= val
}

function isRpkCardMinPlatformVersionGEQ(val, vm) {
  const widgetKey = vm._page.currentPageName
  const widgetsOption = (manifestJSON.router && manifestJSON.router.widgets) || {}
  const widgetManiest = widgetsOption[widgetKey] || {}

  return widgetManiest.minCardPlatformVersion >= val
}

let mode = null

function isRpkDebugMode() {
  if (mode !== null) return mode
  mode = false
  if (manifestJSON.config && manifestJSON.config.debug === true) {
    mode = true
  }
  return mode
}

export {
  registerManifest,
  isRpkMinPlatformVersionGEQ,
  isRpkCardMinPlatformVersionGEQ,
  getManifestField,
  isRpkDebugMode
}
