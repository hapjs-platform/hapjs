/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Image from './image'

import RenderingContext2D from './context-2d'
import Channel from './channel'

function enable(el, app, { module } = {}) {
  // record imgTexture
  el._bindTexture = []
  Channel.setModule(app.id, module)
  Image.channel = RenderingContext2D.channel = Channel
  global.Image = Image

  const cached = {}
  el.getContext = function(type) {
    let context = null

    const pageId = this._docId

    if (type.match(/2d/i)) {
      if (!cached[this.ref]) {
        cached[this.ref] = new RenderingContext2D(this)
      }
      context = cached[this.ref]
      Channel.callGetContext(pageId, this.ref, '2d') // 0 for 2d; 1 for webgl
    } else {
      throw new Error('### App Canvas ### not supported context ' + type)
    }

    return context
  }

  const toTempFilePath = el.toTempFilePath
  el.toTempFilePath = (...args) => {
    const context = el.getContext('2d')

    // 确保调用接口前完成绘制
    RenderingContext2D.channel.render2d(context._pageId, context.componentId, context._drawCommands)
    context._drawCommands = ''

    toTempFilePath(...args)
  }

  return el
}

export default {
  enable
}
