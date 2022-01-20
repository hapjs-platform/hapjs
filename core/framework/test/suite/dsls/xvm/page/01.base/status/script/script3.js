/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * @file 独立加载的JS文件
 */

// hack做法：不推荐对外使用
var quickGlobal = Object.getPrototypeOf(global) || global
var testJsonp = quickGlobal.testJsonp || {}

testJsonp.script2 &&
  testJsonp.script2.loaded &&
  (testJsonp.script3 = {
    loaded: true
  })
