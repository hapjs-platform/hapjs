/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

let _uniqueId = 0

// animIdCache 缓存用户设置的id，避免内部自增id与其重复
const animIdCache = new Set()
const _uniqueIdCache = new Set()

// 用户设置id则复用，未设置则自增不复用
function generateUniqueId(animId) {
  if (_uniqueIdCache.has(animId)) {
    console.warn(
      `### App Framework ### animate 输入参数 options.id 与内部 id 重复，可能导致动画实例事件回调出错，请更换其它字符串作为标识`
    )
  }
  let retId = animId
  if (animId) {
    animIdCache.add(animId)
  } else {
    retId = (_uniqueId--).toString()
    while (animIdCache.has(retId)) {
      retId = (_uniqueId--).toString()
    }
    _uniqueIdCache.add(retId)
  }
  return retId
}

class Animation {
  constructor(page, element, params = {}, animationModule) {
    const animId = params.options && params.options.id
    this.page = page
    this.commonParams = {
      componentId: element.ref,
      animationId: generateUniqueId(animId)
    }

    // 序列化Infinity时转换
    if (params.options && params.options.iterations === Infinity) {
      params.options.iterations = -1
    }

    this.initParams = Object.assign(params, this.commonParams)
    this.animationModule = animationModule
    this.animationModule.enable(this.initParams)
  }

  set finished(val) {
    console.warn(`### App Framework ### Animation Api: finished 是只读属性`)
  }

  get finished() {
    return this.animationModule.getFinished(this.commonParams)
  }

  set playState(state) {
    console.trace(`### App Framework ### Animation Api: playState 是只读属性`)
  }

  get playState() {
    const currentPlayState = this.animationModule.getPlayState(this.commonParams)
    console.trace(`### App Framework ### Animation Api: 获取 playState 状态为：${currentPlayState}`)
    return currentPlayState
  }

  set startTime(startTime = '') {
    Object.assign(this.commonParams, {
      startTime: startTime
    })
    this.animationModule.setStartTime(this.commonParams)
  }

  get startTime() {
    const startTime = this.animationModule.getStartTime(this.commonParams)
    console.trace(`### App Framework ### Animation Api: 获取 startTime 状态为：${startTime}`)
    return startTime
  }

  set onfinish(callback) {
    this.animationModule.onfinish({
      componentId: this.commonParams.componentId,
      animationId: this.commonParams.animationId,
      success: () => {
        return callback && callback()
      }
    })
  }

  get onfinish() {
    return () => {}
  }

  set oncancel(callback) {
    this.animationModule.oncancel({
      componentId: this.commonParams.componentId,
      animationId: this.commonParams.animationId,
      success: () => {
        return callback && callback()
      }
    })
  }

  get oncancel() {
    return () => {}
  }

  play() {
    console.trace('### App Framework ### Animation Api: 调用 play 方法')
    this.animationModule.play(this.commonParams)
  }

  finish() {
    console.trace('### App Framework ### Animation Api: 调用 finish 方法')
    this.animationModule.finish(this.commonParams)
  }

  pause() {
    console.trace('### App Framework ### Animation Api: 调用 pause 方法')
    this.animationModule.pause(this.commonParams)
  }

  cancel() {
    console.trace('### App Framework ### Animation Api: 调用 cancel 方法')
    this.animationModule.cancel(this.commonParams)
  }

  reverse() {
    console.trace('### App Framework ### Animation Api: 调用 reverse 方法')
    this.animationModule.reverse(this.commonParams)
  }
}

export default Animation
