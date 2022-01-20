/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * @file 被打包到页面JS中
 */

// hack做法：不推荐对外使用
var quickGlobal = Object.getPrototypeOf(global) || global
quickGlobal.testJsonp = {}

export default {
  requireInScript(path) {
    // eslint-disable-next-line
    return $app_evaluate$(path)
  },
  getGlobalTestJsonp(key) {
    return (quickGlobal.testJsonp && quickGlobal.testJsonp[key]) || {}
  }
}
