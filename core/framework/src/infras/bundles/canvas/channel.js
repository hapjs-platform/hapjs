/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const channel = (function() {
  let nativeModule = null
  const moduleCache = {}

  return {
    getModule(componentId) {
      return nativeModule
    },

    setModule(appid, module) {
      const key = '__native_module__' + appid
      if (moduleCache[key]) return
      moduleCache[key] = module
      nativeModule = module
    },

    callGetContext(pageId, componentId, type) {
      nativeModule.getContext({
        pageId: pageId,
        componentId: componentId,
        type: type
      })
    },

    render2dSync(pageId, componentId, commands) {
      return nativeModule.canvasNative2DSync({
        pageId: pageId,
        componentId: componentId,
        commands: commands
      })
    },

    render2d: function(pageId, componentId, commands) {
      if (commands) {
        console.trace(`### App Canvas render2d ### ${commands} `)
        nativeModule.canvasNative2D({
          pageId: pageId,
          componentId: componentId,
          commands: commands
        })
      }
    },

    preloadImage([url, id], callback) {
      console.trace(`### App Canvas preloadImage url ### ${url} `)
      nativeModule.preloadImage({
        url: url,
        id: id,
        success: function(image) {
          image.url = url
          image.id = id
          callback && callback(image)
        }
      })
    }
  }
})()

export default channel
