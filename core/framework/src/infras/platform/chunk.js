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

export { registerBundleChunks, requireBundleChunk }
