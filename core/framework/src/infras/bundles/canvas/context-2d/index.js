/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import CanvasPattern from './canvas-pattern'
import CanvasGradient from './canvas-gradient'
import TextMetrics from './text-metrics'
import Base64 from './../base64'
import Image from './../image'

export default class CanvasRenderingContext2D {
  constructor(context) {
    this.className = 'CanvasRenderingContext2D'
    this._drawCommands = ''
    this._globalAlpha = 1.0

    this._fillStyle = 'rgb(0,0,0)'
    this._strokeStyle = 'rgb(0,0,0)'

    this._lineWidth = 1
    this._lineDashOffset = 0
    this._lineDash = []
    this._lineCap = 'butt'
    this._lineJoin = 'miter'

    this._miterLimit = 10

    this._globalCompositeOperation = 'source-over'

    this._textAlign = 'start'
    this._textBaseline = 'alphabetic'

    this._font = '10px sans-serif'

    this._savedGlobalAlpha = []

    this._shadowBlur = 0
    this._shadowColor = 'rgba(0,0,0,0)'
    this._shadowOffsetX = 0
    this._shadowOfssetY = 0

    this.timer = null
    this._pageId = context._docId
    this.componentId = context.ref
    this.canvas = context
    this.schedule = false
  }

  set fillStyle(value) {
    this._fillStyle = value
    let command = ''

    if (typeof value === 'string') {
      this._drawCommands = this._drawCommands.concat('AA' + value + ';')
    } else if (value instanceof CanvasPattern) {
      const image = value._img
      this._drawCommands = this._drawCommands.concat('AB' + image._id + ',' + value._style + ';')
    } else if (value._type === 'linear') {
      command =
        'AC' +
        value._start_pos._x.toFixed(2) +
        ',' +
        value._start_pos._y.toFixed(2) +
        ',' +
        value._end_pos._x.toFixed(2) +
        ',' +
        value._end_pos._y.toFixed(2)

      for (let i = 0; i < value._stop_count; ++i) {
        command += ',' + value._stops[i]._pos + ',' + value._stops[i]._color
      }
      this._drawCommands = this._drawCommands.concat(command + ';')
    } else if (value._type === 'radial') {
      command =
        'AD' +
        value._start_pos._x.toFixed(2) +
        ',' +
        value._start_pos._y.toFixed(2) +
        ',' +
        value._start_pos._r.toFixed(2) +
        ',' +
        value._end_pos._x.toFixed(2) +
        ',' +
        value._end_pos._y.toFixed(2) +
        ',' +
        value._end_pos._r.toFixed(2)

      for (let i = 0; i < value._stop_count; ++i) {
        command += ',' + value._stops[i]._pos + ',' + value._stops[i]._color
      }
      this._drawCommands = this._drawCommands.concat(command + ';')
    }
    this.usePromise()
  }

  get fillStyle() {
    return this._fillStyle
  }

  get globalAlpha() {
    return this._globalAlpha
  }

  set globalAlpha(value) {
    this._globalAlpha = value
    this._drawCommands = this._drawCommands.concat('C?' + value.toFixed(2) + ';')
    this.usePromise()
  }

  get strokeStyle() {
    return this._strokeStyle
  }

  set strokeStyle(value) {
    this._strokeStyle = value
    let command = ''

    if (typeof value === 'string') {
      this._drawCommands = this._drawCommands.concat('NA' + value + ';')
    } else if (value instanceof CanvasPattern) {
      const image = value._img
      this._drawCommands = this._drawCommands.concat('NB' + image._id + ',' + value._style + ';')
    } else if (value._type === 'linear') {
      command =
        'NC' +
        value._start_pos._x.toFixed(2) +
        ',' +
        value._start_pos._y.toFixed(2) +
        ',' +
        value._end_pos._x.toFixed(2) +
        ',' +
        value._end_pos._y.toFixed(2)

      for (let i = 0; i < value._stop_count; ++i) {
        command += ',' + value._stops[i]._pos + ',' + value._stops[i]._color
      }
      this._drawCommands = this._drawCommands.concat(command + ';')
    } else if (value._type === 'radial') {
      command =
        'ND' +
        value._start_pos._x.toFixed(2) +
        ',' +
        value._start_pos._y.toFixed(2) +
        ',' +
        value._start_pos._r.toFixed(2) +
        ',' +
        value._end_pos._x.toFixed(2) +
        ',' +
        value._end_pos._y.toFixed(2) +
        ',' +
        value._end_pos._r.toFixed(2)

      for (let i = 0; i < value._stop_count; ++i) {
        command += ',' + value._stops[i]._pos + ',' + value._stops[i]._color
      }
      this._drawCommands = this._drawCommands.concat(command + ';')
    }
    this.usePromise()
  }

  get lineWidth() {
    return this._lineWidth
  }

  set lineWidth(value) {
    this._lineWidth = value
    this._drawCommands = this._drawCommands.concat('H?' + value + ';')
    this.usePromise()
  }

  get lineCap() {
    return this._lineCap
  }

  set lineCap(value) {
    this._lineCap = value
    this._drawCommands = this._drawCommands.concat('E?' + value + ';')
    this.usePromise()
  }

  get lineJoin() {
    return this._lineJoin
  }

  set lineJoin(value) {
    this._lineJoin = value
    this._drawCommands = this._drawCommands.concat('G?' + value + ';')
    this.usePromise()
  }

  get miterLimit() {
    return this._miterLimit
  }

  set miterLimit(value) {
    this._miterLimit = value
    this._drawCommands = this._drawCommands.concat('I?' + value + ';')
    this.usePromise()
  }

  get globalCompositeOperation() {
    return this._globalCompositeOperation
  }

  set globalCompositeOperation(value) {
    this._globalCompositeOperation = value
    this._drawCommands = this._drawCommands.concat('D?' + value + ';')
    this.usePromise()
  }

  get textAlign() {
    return this._textAlign
  }

  set textAlign(value) {
    this._textAlign = value
    this._drawCommands = this._drawCommands.concat('O?' + value + ';')
    this.usePromise()
  }

  get textBaseline() {
    return this._textBaseline
  }

  set textBaseline(value) {
    this._textBaseline = value
    this._drawCommands = this._drawCommands.concat('P?' + value + ';')
    this.usePromise()
  }

  get font() {
    return this._font
  }

  set font(value) {
    this._font = value
    this._drawCommands = this._drawCommands.concat('B?' + value + ';')
    this.usePromise()
  }

  setTransform(a, b, c, d, tx, ty) {
    this._drawCommands = this._drawCommands.concat(
      'l?' +
        (a === 1 ? '1' : a.toFixed(2)) +
        ',' +
        (b === 0 ? '0' : b.toFixed(2)) +
        ',' +
        (c === 0 ? '0' : c.toFixed(2)) +
        ',' +
        (d === 1 ? '1' : d.toFixed(2)) +
        ',' +
        tx.toFixed(2) +
        ',' +
        ty.toFixed(2) +
        ';'
    )
    this.usePromise()
  }

  transform(a, b, c, d, tx, ty) {
    this._drawCommands = this._drawCommands.concat(
      'p?' +
        (a === 1 ? '1' : a.toFixed(2)) +
        ',' +
        (b === 0 ? '0' : b.toFixed(2)) +
        ',' +
        (c === 0 ? '0' : c.toFixed(2)) +
        ',' +
        (d === 1 ? '1' : d.toFixed(2)) +
        ',' +
        tx +
        ',' +
        ty +
        ';'
    )
    this.usePromise()
  }

  resetTransform() {
    this._drawCommands = this._drawCommands.concat('r?;')
    this.usePromise()
  }

  scale(a, d) {
    this._drawCommands = this._drawCommands.concat('j?' + a.toFixed(2) + ',' + d.toFixed(2) + ';')
    this.usePromise()
  }

  rotate(angle) {
    this._drawCommands = this._drawCommands.concat('h?' + angle.toFixed(6) + ';')
    this.usePromise()
  }

  translate(tx, ty) {
    this._drawCommands = this._drawCommands.concat('q?' + tx.toFixed(2) + ',' + ty.toFixed(2) + ';')
    this.usePromise()
  }

  save() {
    this._savedGlobalAlpha.push(this._globalAlpha)
    this._drawCommands = this._drawCommands.concat('i?;')
    this.usePromise()
  }

  restore() {
    this._drawCommands = this._drawCommands.concat('g?;')
    this._globalAlpha = this._savedGlobalAlpha.pop()
    this.usePromise()
  }

  createPattern(img, pattern) {
    return new CanvasPattern(img, pattern)
  }

  createLinearGradient(x0, y0, x1, y1) {
    return new CanvasGradient(x0, y0, x1, y1)
  }

  createRadialGradient() {
    if (arguments && arguments.length === 3) {
      return new CanvasGradient(arguments[0], arguments[1], arguments[2])
    } else if (arguments && arguments.length === 6) {
      // deprecated
      return new CanvasGradient(
        arguments[0],
        arguments[1],
        arguments[2],
        arguments[3],
        arguments[4],
        arguments[5]
      )
    }
  }

  strokeRect(x, y, w, h) {
    this._drawCommands = this._drawCommands.concat('n?' + x + ',' + y + ',' + w + ',' + h + ';')
    this.usePromise()
  }

  clearRect(x, y, w, h) {
    this._drawCommands = this._drawCommands.concat('U?' + x + ',' + y + ',' + w + ',' + h + ';')
    this.usePromise()
  }

  clip() {
    this._drawCommands = this._drawCommands.concat('V?;')
    this.usePromise()
  }

  closePath() {
    this._drawCommands = this._drawCommands.concat('W?;')
    this.usePromise()
  }

  moveTo(x, y) {
    this._drawCommands = this._drawCommands.concat('c?' + x.toFixed(2) + ',' + y.toFixed(2) + ';')
    this.usePromise()
  }

  lineTo(x, y) {
    this._drawCommands = this._drawCommands.concat('b?' + x.toFixed(2) + ',' + y.toFixed(2) + ';')
    this.usePromise()
  }

  getLineDash() {
    return this._lineDash
  }

  setLineDash(lineDash) {
    if (!Array.isArray(lineDash)) {
      console.warn('### App Canvas ### setLineDash 的参数必须为数组')
      return
    }

    for (let i = 0, len = lineDash.length; i < len; i++) {
      if (typeof lineDash[i] !== 'number' || lineDash[i] < 0) {
        console.warn(
          '### App Canvas ### setLineDash 的参数必须为数组，且数组元素为数字类型，元素值不小于0'
        )
        return
      }

      if (isNaN(lineDash[i])) {
        console.warn('### App Canvas ### setLineDash 的参数必须为数组且元素值不能为 NaN')
        return
      }
    }

    if (lineDash.length % 2 === 1) {
      lineDash.push(...lineDash)
    }

    this._lineDash = lineDash
    this._drawCommands = this._drawCommands.concat('k?' + lineDash + ';')
    this.usePromise()
  }

  get lineDashOffset() {
    return this._lineDashOffset
  }

  set lineDashOffset(offset) {
    if (typeof offset !== 'number') {
      console.warn('### App Canvas ### lineDashOffset 值必须为数字类型')
      return
    }

    if (isNaN(offset)) {
      console.warn('### App Canvas ### lineDashOffset 值不能为 NaN')
      return
    }

    this._lineDashOffset = offset
    this._drawCommands = this._drawCommands.concat('F?' + offset + ';')
    this.usePromise()
  }

  quadraticCurveTo(cpx, cpy, x, y) {
    this._drawCommands = this._drawCommands.concat('e?' + cpx + ',' + cpy + ',' + x + ',' + y + ';')
    this.usePromise()
  }

  bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y) {
    this._drawCommands = this._drawCommands.concat(
      'T?' +
        cp1x.toFixed(2) +
        ',' +
        cp1y.toFixed(2) +
        ',' +
        cp2x.toFixed(2) +
        ',' +
        cp2y.toFixed(2) +
        ',' +
        x.toFixed(2) +
        ',' +
        y.toFixed(2) +
        ';'
    )
    this.usePromise()
  }

  arcTo(x1, y1, x2, y2, radius) {
    this._drawCommands = this._drawCommands.concat(
      'R?' + x1 + ',' + y1 + ',' + x2 + ',' + y2 + ',' + radius + ';'
    )
    this.usePromise()
  }

  beginPath() {
    this._drawCommands = this._drawCommands.concat('S?;')
  }

  fillRect(x, y, w, h) {
    this._drawCommands = this._drawCommands.concat('Z?' + x + ',' + y + ',' + w + ',' + h + ';')
    this.usePromise()
  }

  rect(x, y, w, h) {
    this._drawCommands = this._drawCommands.concat('f?' + x + ',' + y + ',' + w + ',' + h + ';')
    this.usePromise()
  }

  fill() {
    this._drawCommands = this._drawCommands.concat('Y?;')
    this.usePromise()
  }

  stroke() {
    this._drawCommands = this._drawCommands.concat('m?;')
    this.usePromise()
  }

  arc(x, y, radius, startAngle, endAngle, anticlockwise) {
    let ianticlockwise = 0
    if (anticlockwise) {
      ianticlockwise = 1
    }

    this._drawCommands = this._drawCommands.concat(
      'Q?' +
        x.toFixed(2) +
        ',' +
        y.toFixed(2) +
        ',' +
        radius.toFixed(2) +
        ',' +
        startAngle +
        ',' +
        endAngle +
        ',' +
        ianticlockwise +
        ';'
    )
    this.usePromise()
  }

  fillText() {
    if (!arguments) {
      return
    }

    switch (arguments.length) {
      case 3:
        this._drawCommands = this._drawCommands.concat(
          'a?' + Base64.btoa(arguments[0]) + ',' + arguments[1] + ',' + arguments[2] + ';'
        )
        this.usePromise()
        break
      case 4:
        this._drawCommands = this._drawCommands.concat(
          'a?' +
            Base64.btoa(arguments[0]) +
            ',' +
            arguments[1] +
            ',' +
            arguments[2] +
            ',' +
            arguments[3] +
            ';'
        )
        this.usePromise()
        break
    }
  }

  strokeText() {
    if (!arguments) {
      return
    }

    switch (arguments.length) {
      case 3:
        this._drawCommands = this._drawCommands.concat(
          'o?' + Base64.btoa(arguments[0]) + ',' + arguments[1] + ',' + arguments[2] + ';'
        )
        this.usePromise()
        break
      case 4:
        this._drawCommands = this._drawCommands.concat(
          'o?' +
            Base64.btoa(arguments[0]) +
            ',' +
            arguments[1] +
            ',' +
            arguments[2] +
            ',' +
            arguments[3] +
            ';'
        )
        this.usePromise()
        break
    }
  }

  measureText(text) {
    this.sendImmediately()
    const command = '!?' + Base64.btoa(text) + ',' + this._font + ';'
    const result = CanvasRenderingContext2D.channel.render2dSync(
      this._pageId,
      this.componentId,
      command
    )
    if (result.error) {
      return new TextMetrics(0)
    }
    return new TextMetrics(parseFloat(result.width))
  }

  isPointInPath(x, y) {
    throw new Error('Canvas not supported yet')
  }

  drawImage(image, sx, sy, sw, sh, dx, dy, dw, dh) {
    const numArgs = arguments.length
    function drawImageCommands(img) {
      if (numArgs === 3) {
        const x = parseFloat(sx) || 0.0
        const y = parseFloat(sy) || 0.0

        return (
          'X?' +
          img._id +
          ',0,0,' +
          img.width +
          ',' +
          img.height +
          ',' +
          x +
          ',' +
          y +
          ',' +
          img.width +
          ',' +
          img.height +
          ';'
        )
      } else if (numArgs === 5) {
        const x = parseFloat(sx) || 0.0
        const y = parseFloat(sy) || 0.0
        const width = parseInt(sw) || img.width
        const height = parseInt(sh) || img.height

        return (
          'X?' +
          img._id +
          ',0,0,' +
          img.width +
          ',' +
          img.height +
          ',' +
          x +
          ',' +
          y +
          ',' +
          width +
          ',' +
          height +
          ';'
        )
      } else if (numArgs === 9) {
        sx = parseFloat(sx) || 0.0
        sy = parseFloat(sy) || 0.0
        sw = parseInt(sw) || img.width
        sh = parseInt(sh) || img.height
        dx = parseFloat(dx) || 0.0
        dy = parseFloat(dy) || 0.0
        dw = parseInt(dw) || img.width
        dh = parseInt(dh) || img.height

        return (
          'X?' +
          img._id +
          ',' +
          sx +
          ',' +
          sy +
          ',' +
          sw +
          ',' +
          sh +
          ',' +
          dx +
          ',' +
          dy +
          ',' +
          dw +
          ',' +
          dh +
          ';'
        )
      }
    }

    if (image.type === 'image') {
      const src = image.attr.src
      image = new Image()
      image.width = 0
      image.height = 0
      image.src = src
    }
    this._drawCommands += drawImageCommands(image)
    this.usePromise()
  }

  getImageData(x, y, sw, sh) {
    if (sw <= 0 || sh <= 0) {
      throw new Error('the source width or height must be > 0')
    }

    this.sendImmediately()

    const command = '@?' + x + ',' + y + ',' + sw + ',' + sh + ';'
    const result = CanvasRenderingContext2D.channel.render2dSync(
      this._pageId,
      this.componentId,
      command
    )

    if (result.error) {
      return {
        width: 0,
        height: 0,
        data: new Uint8ClampedArray(0)
      }
    }

    // ArrayBuffer数据会被清掉，j2v8可能有问题，暂时拷贝一份数据可规避此问题
    // TODO
    return {
      data: new Uint8ClampedArray(result.data.slice(0)),
      width: result.width,
      height: result.height
    }
  }

  createImageData(w, h) {
    const numArgs = arguments.length
    const singlePixelLen = 4
    let imageData

    if (numArgs === 1) {
      imageData = w
      w = imageData.width
      h = imageData.height
    }

    if (w <= 0 || h <= 0) {
      w = 0
      h = 0
    }

    return {
      width: w,
      height: h,
      data: new Uint8ClampedArray(w * h * singlePixelLen)
    }
  }

  putImageData(imagedata, dx, dy, dirtyX = 0, dirtyY = 0, dirtyWidth, dirtyHeight) {
    const numArgs = arguments.length
    const initWidth = imagedata.width
    const initHeight = imagedata.height
    const data = imagedata.data
    const singlePixelLen = 4

    if (data.length !== initHeight * initWidth * singlePixelLen) {
      throw new Error('illegal ImageData')
    }

    if (numArgs === 3) {
      this._drawCommands = this._drawCommands.concat(
        'd?' +
          initWidth +
          ',' +
          initHeight +
          ',' +
          Base64.arrayBufferToBase64(data) +
          ',' +
          dx +
          ',' +
          dy +
          ';'
      )
    } else if (numArgs === 7) {
      const rect = {
        left: dirtyX < 0 ? 0 : dirtyX,
        top: dirtyY < 0 ? 0 : dirtyY
      }

      rect.right = Math.min(Math.max(dirtyX + dirtyWidth, rect.left), initWidth)
      rect.bottom = Math.min(Math.max(dirtyY + dirtyHeight, rect.top), initHeight)

      dirtyX = rect.left
      dirtyY = rect.top
      dirtyWidth = rect.right - rect.left
      dirtyHeight = rect.bottom - rect.top

      const destData = new Uint8ClampedArray(dirtyWidth * dirtyHeight * singlePixelLen)
      let imgPos
      let destPos = 0

      for (let i = dirtyY; i < rect.bottom; i++) {
        imgPos = (initWidth * i + dirtyX) * singlePixelLen
        for (let j = 0; j < dirtyWidth; j++) {
          destData[destPos] = data[imgPos]
          destData[destPos + 1] = data[imgPos + 1]
          destData[destPos + 2] = data[imgPos + 2]
          destData[destPos + 3] = data[imgPos + 3]
          destPos += singlePixelLen
          imgPos += singlePixelLen
        }
      }

      this._drawCommands = this._drawCommands.concat(
        'd?' +
          dirtyWidth +
          ',' +
          dirtyHeight +
          ',' +
          Base64.arrayBufferToBase64(destData) +
          ',' +
          dx +
          ',' +
          dy +
          ';'
      )
    }

    this.usePromise()
  }

  get shadowBlur() {
    return this._shadowBlur
  }

  set shadowBlur(value) {
    if (value < 0 || isNaN(value)) {
      return
    }
    this._globalAlpha = value
    this._drawCommands = this._drawCommands.concat('J?' + value + ';')
    this.usePromise()
  }

  get shadowColor() {
    return this._shadowColor
  }

  set shadowColor(value) {
    this._shadowColor = value
    this._drawCommands = this._drawCommands.concat('K?' + value + ';')
    this.usePromise()
  }

  get shadowOffsetX() {
    return this._shadowOffsetX
  }

  set shadowOffsetX(value) {
    if (value < 0 || isNaN(value)) {
      return
    }
    this._shadowOffsetX = value
    this._drawCommands = this._drawCommands.concat('L?' + value + ';')
    this.usePromise()
  }

  get shadowOffsetY() {
    return this._shadowOffsetY
  }

  set shadowOffsetY(value) {
    if (value < 0 || isNaN(value)) {
      return
    }
    this._shadowOffsetY = value
    this._drawCommands = this._drawCommands.concat('M?' + value + ';')
    this.usePromise()
  }

  usePromise() {
    const $this = this
    if (!$this.schedule) {
      Promise.resolve().then(() => {
        $this.sendImmediately()
        $this.schedule = false
      })
      $this.schedule = true
    }
  }

  sendImmediately() {
    CanvasRenderingContext2D.channel.render2d(this._pageId, this.componentId, this._drawCommands)
    this._drawCommands = ''
  }
}
