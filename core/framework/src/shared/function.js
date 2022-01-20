/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * 使用Function构造函数创建函数并执行
 * @param  {object} globalObjects - 参数组成的对象
 * @param  {string} body - 函数体
 * @return {any}
 */
function callFunction(globalObjects, body) {
  const globalKeys = []
  const globalValues = []
  for (const key in globalObjects) {
    globalKeys.push(key)
    globalValues.push(globalObjects[key])
  }
  globalKeys.push(body)

  profiler.record(`### App Performance ### 编译JS[PERF:compileJS]开始：${new Date().toJSON()}`)
  profiler.time(`PERF:compileJS`)
  const fn = new Function(...globalKeys)
  profiler.timeEnd(`PERF:compileJS`)
  profiler.record(`### App Performance ### 编译JS[PERF:compileJS]结束：${new Date().toJSON()}`)

  profiler.record(`### App Performance ### 执行JS[PERF:executeJS]开始：${new Date().toJSON()}`)
  profiler.time(`PERF:executeJS`)
  const ret = fn(...globalValues)
  profiler.timeEnd(`PERF:executeJS`)
  profiler.record(`### App Performance ### 执行JS[PERF:executeJS]结束：${new Date().toJSON()}`)

  return ret
}

// global.__dateCompileCost = global.__dateCompileCost || 0
// global.__timeInvokeCount = global.__timeInvokeCount || 1

/**
 * 使用V8原生方法创建函数并执行
 * @param  {object} globalObjects - 参数组成的对象
 * @param  {string} body - 函数体
 * @param  {string} bundleUrl - 关联文件来源
 * @return {any}
 */
function callFunctionNative(globalObjects, body, bundleUrl) {
  let script = '(function ('
  const globalKeys = []
  const globalValues = []
  for (const key in globalObjects) {
    globalKeys.push(key)
    globalValues.push(globalObjects[key])
  }
  for (let i = 0; i < globalKeys.length - 1; ++i) {
    script += globalKeys[i]
    script += ','
  }
  script += globalKeys[globalKeys.length - 1]
  script += ') {'
  script += body
  script += `
  })`

  // const timeInvokeIndex = global.__timeInvokeCount++
  // const dateCompileS = new Date()

  // profiler.record(`### App Performance ### 编译JS[PERF:compileJS]开始：${new Date().toJSON()}`)
  // profiler.time(`PERF:compileJS:${timeInvokeIndex}`)
  let ret = global.compileAndRunScript(script, bundleUrl)
  // profiler.timeEnd(`PERF:compileJS:${timeInvokeIndex}`)
  // profiler.record(`### App Performance ### 编译JS[PERF:compileJS]结束：${new Date().toJSON()}`)

  // const dateCompileE = new Date()
  // global.__dateCompileCost += dateCompileE - dateCompileS
  // profiler.record(
  //   `### App Performance ### 编译JS[PERF:compileJS]累积耗时：${global.__dateCompileCost}ms`
  // )

  // profiler.record(`### App Performance ### 执行JS[PERF:executeJS]开始：${new Date().toJSON()}`)
  // profiler.time(`PERF:executeJS:${timeInvokeIndex}`)
  if (ret && typeof ret === 'function') {
    ret = ret(...globalValues)
  }
  // profiler.timeEnd(`PERF:executeJS:${timeInvokeIndex}`)
  // profiler.record(`### App Performance ### 执行JS[PERF:executeJS]结束：${new Date().toJSON()}`)
  return ret
}

/**
 * 使用脚本创建函数并执行
 * @param  {object} globalObjects - 参数组成的对象
 * @param  {string} body - 函数体
 * @param  {string} bundleUrl - 关联文件来源
 * @return {any}
 */
function invokeScript(globalObjects, body, bundleUrl) {
  if (typeof global.compileAndRunScript === 'function') {
    return callFunctionNative(globalObjects, body, bundleUrl)
  }
  return callFunction(globalObjects, body)
}

export { invokeScript }
