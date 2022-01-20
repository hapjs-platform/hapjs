/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Event from './event'

class TouchEvent extends Event {
  constructor() {
    super(...arguments)

    this._touches = null
    this._changedTouches = null
  }

  get touches() {
    return this._touches
  }

  set touches(value) {
    if (this._touches) {
      throw new Error(`### App Framework ### TouchEvent 不支持修改 touches 属性`)
    }
    this._touches = value
  }

  get changedTouches() {
    return this._changedTouches
  }

  set changedTouches(value) {
    if (this._changedTouches) {
      throw new Error(`### App Framework ### TouchEvent 不支持修改 changedTouches 属性`)
    }
    this._changedTouches = value
  }

  toJSON() {
    const superJSON = super.toJSON()
    return Object.assign(superJSON, {
      touches: this._touches,
      changedTouches: this._changedTouches
    })
  }
}

export default TouchEvent
