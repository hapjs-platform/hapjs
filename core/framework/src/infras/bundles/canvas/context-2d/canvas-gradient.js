/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

class CanvasGradient {
  constructor() {
    if (arguments && arguments.length === 3) {
      this._start_pos = { _x: arguments[0], _y: arguments[1], _r: 0 }
      this._end_pos = { _x: arguments[0], _y: arguments[1], _r: arguments[2] }
      this._type = 'radial'
    } else if (arguments && arguments.length === 4) {
      this._start_pos = { _x: arguments[0], _y: arguments[1] }
      this._end_pos = { _x: arguments[2], _y: arguments[3] }
      this._type = 'linear'
    } else if (arguments && arguments.length === 6) {
      this._start_pos = { _x: arguments[0], _y: arguments[1], _r: arguments[2] }
      this._end_pos = { _x: arguments[3], _y: arguments[4], _r: arguments[5] }
      this._type = 'radial'
    }

    this._stop_count = 0
    this._stops = [0, 0, 0, 0, 0]
  }

  addColorStop(pos, color) {
    if (this._stop_count < 5 && pos >= 0.0 && pos <= 1.0) {
      this._stops[this._stop_count] = { _pos: pos, _color: color }
      this._stop_count++
    }
  }
}

export default CanvasGradient
