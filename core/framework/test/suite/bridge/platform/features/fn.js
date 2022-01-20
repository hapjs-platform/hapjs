/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import * as sample from './sample'
import * as canvas from './canvas'
import * as router from './router'
import * as animation from './animation'
import * as worker from './worker'
import * as class1 from './class1'
import * as websocket from './websocket'
import * as websocketfactory from './websocketfactory'
import * as ad from './ad'
import * as bannerAd from './bannerAd'
import * as prompt from './prompt'
import * as decode from './decode'

// Feature全局注册的配置
export const allFeatureMethList = []
// Feature实现
export const allFeatureInstHash = {}

// Feature列表
const fnList = [
  sample,
  canvas,
  router,
  animation,
  worker,
  class1,
  websocket,
  websocketfactory,
  ad,
  bannerAd,
  prompt,
  decode
]

// 注册Module
fnList.forEach(fn => {
  const fnConfig = fn.config
  const fnDefault = fn.default

  // 是否可实例化
  if (fnConfig.instantiable === undefined) {
    fnConfig.instantiable = false
  }

  // 方法是否静态
  fnConfig.methods.forEach(function(methodItem) {
    if (methodItem.instanceMethod === undefined) {
      methodItem.instanceMethod = false
    }
  })

  allFeatureInstHash[fnDefault.name] = fnDefault
  allFeatureMethList.push(fnConfig)
})
