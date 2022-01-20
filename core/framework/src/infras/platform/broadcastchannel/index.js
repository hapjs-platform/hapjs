/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const channels = {}

class MessageEvent {
  constructor(type = 'message', dict = {}) {
    this.type = type
    this.data = dict.data || null
    this.timeStamp = Date.now()
  }
}

/**
 * 对外提供一种消息通讯的机制
 */
class BroadcastChannel {
  constructor(name) {
    if (global.Env.engine === global.ENGINE_TYPE.CARD) {
      throw new Error(`BroadcastChannel is not supported.`)
    }
    // name readOnly
    Object.defineProperty(this, 'name', {
      configurable: false,
      enumerable: true,
      writable: false,
      value: String(name)
    })

    this._closed = false
    this.onmessage = null

    if (!channels[this.name]) {
      channels[this.name] = []
    }
    channels[this.name].push(this)
  }

  postMessage(message) {
    if (this._closed) {
      throw new Error(`BroadcastChannel "${this.name}" is closed.`)
    }

    const subscribers = channels[this.name]
    if (subscribers && subscribers.length) {
      for (let i = 0; i < subscribers.length; ++i) {
        const member = subscribers[i]

        if (member._closed || member === this) continue

        if (typeof member.onmessage === 'function') {
          member.onmessage(new MessageEvent('message', { data: message }))
        }
      }
    }
  }

  close() {
    if (this._closed) {
      return
    }

    this._closed = true

    // remove itself from channels.
    if (channels[this.name]) {
      const index = channels[this.name].indexOf(this)
      if (index > -1) {
        channels[this.name].splice(index, 1)
      } else {
        delete channels[this.name]
      }
    }
  }
}

export default BroadcastChannel
