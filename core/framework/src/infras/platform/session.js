/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
// An adoption from https://github.com/nodejs/node/blob/f70261fb3089b8acf5f68584ef0285a1c11f54fd/lib/inspector.js#L35
import Pubsub from '../../shared/pubsub'
import { isFunction, isString } from '../../shared/util'

const connectionSymbol = Symbol('connectionProperty')
const messageCallbacksSymbol = Symbol('messageCallbacks')
const nextIdSymbol = Symbol('nextId')
const onMessageSymbol = Symbol('onMessage')
export default class Session extends Pubsub {
  constructor() {
    super()
    this[connectionSymbol] = null
    this[nextIdSymbol] = 1
    this[messageCallbacksSymbol] = new Map()
  }

  connect() {
    if (this[connectionSymbol]) throw new Error('The inspector session has already connected')
    this[connectionSymbol] =
      // Connection is global Variable in JsEnv
      // eslint-disable-next-line no-undef
      new global.Connection(message => this[onMessageSymbol](message))
  }

  [onMessageSymbol](message) {
    const parsed = JSON.parse(message)
    try {
      if (parsed.id) {
        const callback = this[messageCallbacksSymbol].get(parsed.id)
        this[messageCallbacksSymbol].delete(parsed.id)
        if (callback) {
          if (parsed.error) {
            return callback(new Error(parsed.error.code, parsed.error.message))
          }

          callback(null, parsed.result)
        }
      } else {
        this.publish(parsed.method, parsed, null)
        this.publish('inspectorNotification', parsed, null)
      }
    } catch (error) {
      console.log(`### App Framework ### ${error}`)
    }
  }

  post(method, params, callback) {
    if (!isString(method)) {
      throw new Error('method must be a string')
    }
    if (!callback && isFunction(params)) {
      callback = params
      params = null
    }
    if (params && typeof params !== 'object') {
      throw new Error('params not object')
    }
    if (callback && typeof callback !== 'function') {
      throw new Error('callback is not valid')
    }

    if (!this[connectionSymbol]) {
      throw new Error('ERR_INSPECTOR_NOT_CONNECTED')
    }
    const id = this[nextIdSymbol]++
    const message = { id, method }
    if (params) {
      message.params = params
    }
    if (callback) {
      this[messageCallbacksSymbol].set(id, callback)
    }
    this[connectionSymbol].dispatch(JSON.stringify(message))
  }

  disconnect() {
    if (!this[connectionSymbol]) return
    this[connectionSymbol].disconnect()
    this[connectionSymbol] = null
    const remainingCallbacks = this[messageCallbacksSymbol].values()
    if (remainingCallbacks.length > 0) {
      console.warn(
        '### App Framework ### remainingCallbacks will not executed due to inspector closed'
      )
    }

    this[messageCallbacksSymbol].clear()
    this[nextIdSymbol] = 1
  }
}
