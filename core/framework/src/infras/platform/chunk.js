/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// 依赖的JS模块映射缓存
const _chunkHashCache = new Map()

/**
 * 注册bundle依赖的JS模块映射
 * @param chunkHash
 */
function registerBundleChunks(chunkHash) {
  if (typeof chunkHash === 'string') {
    chunkHash = JSON.parse(chunkHash)
  }

  for (const path in chunkHash) {
    const cont = chunkHash[path]
    _chunkHashCache.set(path, cont)
  }

  profiler.record(
    `### App Performance ### 注册chunks[PERF:registerBundleChunks]开始：${new Date().toJSON()}`
  )
  console.trace(
    `### App Performance ### 注册chunks[PERF:registerBundleChunks]开始：${new Date().toJSON()}`
  )
}

function requireBundleChunk(filePath) {
  const cont = _chunkHashCache.get(filePath)
  if (!cont) {
    console.warn(`### App Framework ### 加载依赖的JS模块：路径不存在：${filePath}`)
  }
  return cont
}

const templateJsonMap = new Map()
const styleJsonMap = new Map()
function registerComponentJson(templateJson, cssJson) {
  if (typeof templateJson === 'string') {
    templateJson = JSON.parse(templateJson)
  }
  for (const compPath in templateJson) {
    if (templateJson.hasOwnProperty(compPath)) {
      let pathKey = compPath
      if (pathKey.startsWith('/')) {
        pathKey = pathKey.replace(/^\/+/, '')
      }
      const templateObj = templateJson[compPath]
      templateJsonMap.set(pathKey, templateObj)
    }
  }

  if (typeof cssJson === 'string') {
    cssJson = JSON.parse(cssJson)
  }
  for (const compPath in cssJson) {
    if (cssJson.hasOwnProperty(compPath)) {
      let pathKey = compPath
      if (pathKey.startsWith('/')) {
        pathKey = pathKey.replace(/^\/+/, '')
      }
      const styleObj = cssJson[compPath]
      styleJsonMap.set(pathKey, styleObj)
    }
  }
}

function requireJson(compPath, options) {
  try {
    if (options && options.styleObjectId) {
      const styleObj = JSON.parse(styleJsonMap.get(compPath))
      if (!styleObj) {
        console.warn(
          `### App Framework ### requireJson not exist ${compPath} -- options: ${JSON.stringify(
            options
          )}`
        )
        return {}
      }
      const style = styleObj[options.styleObjectId]
      return style
    } else if (options && options.componentPath) {
      const templateObj = JSON.parse(templateJsonMap.get(compPath))
      if (!templateObj) {
        console.warn(
          `### App Framework ### requireJson not exist ${compPath} -- options: ${JSON.stringify(
            options
          )}`
        )
        return {}
      }
      const template = templateObj[options.componentPath].template
      return template
    }
  } catch (e) {
    console.error(`### App Framework ### requireJson error: ${JSON.stringify(e)}`)
  }
}

export { registerBundleChunks, requireBundleChunk, registerComponentJson, requireJson }
