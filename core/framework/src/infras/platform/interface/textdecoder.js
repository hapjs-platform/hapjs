/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { requireModule } from '../module/interface'

let decoder

class TextDecoder {
  constructor(encoding = 'utf-8', options = { fatal: false, ignoreBOM: false }) {
    if (!(typeof encoding === 'string') || !(typeof options === 'object')) {
      console.error(`### App Framework ### params are not valid.`)
      this._isValid = false
      return
    }
    const params = Object.assign({ fatal: false, ignoreBOM: false }, options)
    this._isValid = true
    this._encoding = encoding
    this._fatal = params.fatal
    this._ignoreBOM = params.ignoreBOM
  }

  decode(arrayBuffer) {
    if (!this._isValid) return

    const response = decoder.decode({
      encoding: this._encoding,
      fatal: this._fatal,
      ignoreBom: this._ignoreBOM,
      arrayBuffer: arrayBuffer
    })

    if (response) {
      const responseObj = JSON.parse(response)
      if (
        responseObj.errorCode === 202 ||
        (responseObj.errorCode === 1000 &&
          ((this._fatal && arrayBuffer !== undefined) || arrayBuffer === null))
      ) {
        console.error(`### App Framework ### ${responseObj.errorMsg}`)
      }
      return responseObj.result
    }
  }

  set encoding(val) {
    console.warn(`### App Framework ### TextDecoder: encoding 是只读属性`)
  }

  get encoding() {
    return this._encoding
  }

  set fatal(val) {
    console.warn(`### App Framework ### TextDecoder: fatal 是只读属性`)
  }

  get fatal() {
    return this._fatal
  }

  set ignoreBOM(val) {
    console.warn(`### App Framework ### TextDecoder: ignoreBOM 是只读属性`)
  }

  get ignoreBOM() {
    return this._ignoreBOM
  }
}

function initTextDecoder(appInst) {
  decoder = requireModule(appInst, 'system.decode')
  global.TextDecoder = TextDecoder
}

export default initTextDecoder
