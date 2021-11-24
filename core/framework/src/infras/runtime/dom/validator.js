/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { splitAttr, camelCaseToHyphened, isValidValue, hyphenedToCamelCase } from './util'

// 长度单位
const cssLengthUnits = ['px', '%', 'dp']
// 角度单位
const cssAngleUnits = ['deg']

const REGEXP_LENGTH = /^[-+]?[0-9]*\.?[0-9]+(.*)$/
const REGEXP_URL = /^url\(\s*(['"]?)\s*([^'"()]+?)\s*\1\s*\)$/
const REGEXP_COLOR_LONG = /^#[0-9a-fA-F]{6}$/
const REGEXP_COLOR_SHORT = /^#[0-9a-fA-F]{3}$/
const REGEXP_COLOR_RGB = /^rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)$/
const REGEXP_COLOR_RGBA = /^rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d*\.?\d+)\s*\)$/
const REGEXP_COLOR_HSL = /^hsl\(\s*(\d+)\s*,\s*(\d+%)\s*,\s*(\d+%)\s*\)$/
const REGEXP_COLOR_HSLA = /^hsla\(\s*(\d+)\s*,\s*(\d+%)\s*,\s*(\d+%)\s*,\s*(\d*\.?\d+)\s*\)$/
const REGEXP_ARRAYCOLOR = /(?:.+?\s(?=[#a-zA-Z]))|.+/g
const REGEXP_GRADIENT_DIRECTION = /^\s*(to|bottom|right|left|top)|[-+]?[0-9]*\.?[0-9]+(.*)/
const REGEXP_ANGLE = /^\s*[-+]?[0-9]*\.?[0-9]+(.*)/
const REGEXP_ARRAYCOLORSTOP = /(rgba|rgb)\([0-9,.\spx%]+\)\s?[0-9-+pxdp%]*|[#]?\w+\s?[0-9+-\spxdp%]*/gi

const colorNames = {
  aliceblue: '#F0F8FF',
  antiquewhite: '#FAEBD7',
  aqua: '#00FFFF',
  aquamarine: '#7FFFD4',
  azure: '#F0FFFF',
  beige: '#F5F5DC',
  bisque: '#FFE4C4',
  black: '#000000',
  blanchedalmond: '#FFEBCD',
  blue: '#0000FF',
  blueviolet: '#8A2BE2',
  brown: '#A52A2A',
  burlywood: '#DEB887',
  cadetblue: '#5F9EA0',
  chartreuse: '#7FFF00',
  chocolate: '#D2691E',
  coral: '#FF7F50',
  cornflowerblue: '#6495ED',
  cornsilk: '#FFF8DC',
  crimson: '#DC143C',
  cyan: '#00FFFF',
  darkblue: '#00008B',
  darkcyan: '#008B8B',
  darkgoldenrod: '#B8860B',
  darkgray: '#A9A9A9',
  darkgreen: '#006400',
  darkgrey: '#A9A9A9',
  darkkhaki: '#BDB76B',
  darkmagenta: '#8B008B',
  darkolivegreen: '#556B2F',
  darkorange: '#FF8C00',
  darkorchid: '#9932CC',
  darkred: '#8B0000',
  darksalmon: '#E9967A',
  darkseagreen: '#8FBC8F',
  darkslateblue: '#483D8B',
  darkslategray: '#2F4F4F',
  darkslategrey: '#2F4F4F',
  darkturquoise: '#00CED1',
  darkviolet: '#9400D3',
  deeppink: '#FF1493',
  deepskyblue: '#00BFFF',
  dimgray: '#696969',
  dimgrey: '#696969',
  dodgerblue: '#1E90FF',
  firebrick: '#B22222',
  floralwhite: '#FFFAF0',
  forestgreen: '#228B22',
  fuchsia: '#FF00FF',
  gainsboro: '#DCDCDC',
  ghostwhite: '#F8F8FF',
  gold: '#FFD700',
  goldenrod: '#DAA520',
  gray: '#808080',
  green: '#008000',
  greenyellow: '#ADFF2F',
  grey: '#808080',
  honeydew: '#F0FFF0',
  hotpink: '#FF69B4',
  indianred: '#CD5C5C',
  indigo: '#4B0082',
  ivory: '#FFFFF0',
  khaki: '#F0E68C',
  lavender: '#E6E6FA',
  lavenderblush: '#FFF0F5',
  lawngreen: '#7CFC00',
  lemonchiffon: '#FFFACD',
  lightblue: '#ADD8E6',
  lightcoral: '#F08080',
  lightcyan: '#E0FFFF',
  lightgoldenrodyellow: '#FAFAD2',
  lightgray: '#D3D3D3',
  lightgreen: '#90EE90',
  lightgrey: '#D3D3D3',
  lightpink: '#FFB6C1',
  lightsalmon: '#FFA07A',
  lightseagreen: '#20B2AA',
  lightskyblue: '#87CEFA',
  lightslategray: '#778899',
  lightslategrey: '#778899',
  lightsteelblue: '#B0C4DE',
  lightyellow: '#FFFFE0',
  lime: '#00FF00',
  limegreen: '#32CD32',
  linen: '#FAF0E6',
  magenta: '#FF00FF',
  maroon: '#800000',
  mediumaquamarine: '#66CDAA',
  mediumblue: '#0000CD',
  mediumorchid: '#BA55D3',
  mediumpurple: '#9370DB',
  mediumseagreen: '#3CB371',
  mediumslateblue: '#7B68EE',
  mediumspringgreen: '#00FA9A',
  mediumturquoise: '#48D1CC',
  mediumvioletred: '#C71585',
  midnightblue: '#191970',
  mintcream: '#F5FFFA',
  mistyrose: '#FFE4E1',
  moccasin: '#FFE4B5',
  navajowhite: '#FFDEAD',
  navy: '#000080',
  oldlace: '#FDF5E6',
  olive: '#808000',
  olivedrab: '#6B8E23',
  orange: '#FFA500',
  orangered: '#FF4500',
  orchid: '#DA70D6',
  palegoldenrod: '#EEE8AA',
  palegreen: '#98FB98',
  paleturquoise: '#AFEEEE',
  palevioletred: '#DB7093',
  papayawhip: '#FFEFD5',
  peachpuff: '#FFDAB9',
  peru: '#CD853F',
  pink: '#FFC0CB',
  plum: '#DDA0DD',
  powderblue: '#B0E0E6',
  purple: '#800080',
  red: '#FF0000',
  rosybrown: '#BC8F8F',
  royalblue: '#4169E1',
  saddlebrown: '#8B4513',
  salmon: '#FA8072',
  sandybrown: '#F4A460',
  seagreen: '#2E8B57',
  seashell: '#FFF5EE',
  sienna: '#A0522D',
  silver: '#C0C0C0',
  skyblue: '#87CEEB',
  slateblue: '#6A5ACD',
  slategray: '#708090',
  slategrey: '#708090',
  snow: '#FFFAFA',
  springgreen: '#00FF7F',
  steelblue: '#4682B4',
  tan: '#D2B48C',
  teal: '#008080',
  thistle: '#D8BFD8',
  tomato: '#FF6347',
  turquoise: '#40E0D0',
  violet: '#EE82EE',
  wheat: '#F5DEB3',
  white: '#FFFFFF',
  whitesmoke: '#F5F5F5',
  yellow: '#FFFF00',
  yellowgreen: '#9ACD32'
}

// 日志类型
const logTypes = ['NOTE', 'WARNING', 'ERROR']

const validator = {
  /**
   * 枚举值校验
   * @param list
   * @param v
   * @returns {*}
   */
  enum: function(list, v) {
    const index = list.indexOf(v)
    if (index > 0) {
      return { value: v }
    }
    if (index === 0) {
      return {
        value: v,
        // 关闭默认值的提示
        reason:
          false &&
          function reason(k, v) {
            return `NOTE:  属性'${camelCaseToHyphened(k)}' 的值 '${v}' 是缺省值(可以忽略不写)`
          }
      }
    } else {
      return {
        value: null,
        reason: function reason(k, v) {
          const ccth = camelCaseToHyphened(k)
          const lists = list.join('|')
          return `ERROR: 属性 '${ccth}' 的值 '${v}' 无效 ' (有效枚举值为: '${lists}')`
        }
      }
    }
  },
  /**
   * 长度值校验
   * @param v
   * @param units 支持的单位
   * @param defaultValueIfNotSupported 如果单位不准确，需要回退的默认值
   * @returns {*}
   * @constructor
   */
  length: function(v, units, defaultValueIfNotSupported) {
    v = (v || '').toString().trim()
    const match = v.match(REGEXP_LENGTH)
    if (!units) {
      units = cssLengthUnits
    }

    if (match) {
      // 尝试检查单位
      const unit = match[1]
      if (+v === 0 && !unit) {
        return {
          value: +v + units[0]
        }
      } else if (!unit) {
        return {
          value: parseFloat(v) + units[0],
          reason: function reason(k) {
            const ccth = camelCaseToHyphened(k)
            return `WARNING: '${ccth}' 没有指定单位，默认为 '${units[0]}'`
          }
        }
      } else if (units.indexOf(unit.toLowerCase()) >= 0) {
        // 如果单位合法
        return { value: v }
      } else {
        // 如果validator提供了其单位不合法时可以回退的默认值，则应用此默认值
        // 其余情况，一律添加px为其长度单位
        const fixedValue = defaultValueIfNotSupported || parseFloat(v) + units[0]
        return {
          value: fixedValue,
          reason: function reason(k) {
            const ccth = camelCaseToHyphened(k)
            const unitsJson = JSON.stringify(units)
            return `ERROR: 属性 '${ccth}' 不支持单位 '${unit}', 目前仅支持 '${unitsJson}'`
          }
        }
      }
    }
    return {
      value: null,
      reason: function reason(k, v) {
        const ccth = camelCaseToHyphened(k)
        return `ERROR: 属性 '${ccth}' 的值 '${v}' 不正确(仅支持数值)`
      }
    }
  },
  /**
   * 整数、auto 长度校验
   * @param {String} v
   * @param {Array} units - 支持的单位
   * @returns {*}
   */
  multipleLength: function(v, units) {
    v = (v || '').toString().trim()
    if (v === 'auto') {
      return { value: v }
    } else if (/^[-+]?[0-9]+.*/.test(v)) {
      return validator.length(v, units)
    } else {
      return {
        value: null,
        reason: function reason(k, v) {
          const ccth = camelCaseToHyphened(k)
          return `ERROR: 属性 '${ccth}' 的值 '${v}' 的值不正确`
        }
      }
    }
  },

  /**
   * 颜色值校验, 支持 rgb, rgba, #fff, #ffffff, named-color
   * @param v
   * @returns {*}
   * @constructor
   */
  color: function(v) {
    v = (v || '').toString().trim()

    if (v.match(REGEXP_COLOR_LONG)) {
      return { value: v }
    }

    // 如果是#XXX，则转换为#XXXXXX
    if (v.match(REGEXP_COLOR_SHORT)) {
      return {
        value: '#' + v[1] + v[1] + v[2] + v[2] + v[3] + v[3]
      }
    }

    // 如果颜色值为颜色名字符串
    if (colorNames[v]) {
      return {
        value: colorNames[v]
      }
    }

    // rgb/rgbag格式颜色处理
    let arrColor, r, g, b, a

    if (REGEXP_COLOR_RGB.exec(v)) {
      arrColor = REGEXP_COLOR_RGB.exec(v)
      r = parseInt(arrColor[1])
      g = parseInt(arrColor[2])
      b = parseInt(arrColor[3])
      if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
        return { value: `rgb(${[r, g, b].join(',')})` }
      }
    }

    if (REGEXP_COLOR_RGBA.exec(v)) {
      arrColor = REGEXP_COLOR_RGBA.exec(v)
      r = parseInt(arrColor[1])
      g = parseInt(arrColor[2])
      b = parseInt(arrColor[3])
      a = parseFloat(arrColor[4])
      if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255 && a >= 0 && a <= 1) {
        return { value: `rgba(${[r, g, b, a].join(',')})` }
      }
    }
    let h, s, l
    arrColor = REGEXP_COLOR_HSL.exec(v) || REGEXP_COLOR_HSLA.exec(v)
    if (arrColor) {
      h = parseInt(arrColor[1])
      s = parseInt(arrColor[2])
      l = parseInt(arrColor[3])
      a = parseFloat(arrColor[4])
      if (h >= 0 && h <= 360 && s >= 0 && s <= 100 && l >= 0 && l <= 100) {
        if (a >= 0 && a <= 1) {
          return { value: `hsla(${h},${s}%,${l}%,${a})` }
        }
        return { value: `hsl(${h},${s}%,${l}%)` }
      }
    }
    // 透明色
    if (v === 'transparent') {
      return { value: 'rgba(0,0,0,0)' }
    }

    // 无效颜色值
    return {
      value: null,
      reason: function reason(k, v) {
        const ccth = camelCaseToHyphened(k)
        return `ERROR: 属性' ${ccth} ' 的颜色值 ' ${v} ' 无效`
      }
    }
  },
  /**
   * 数组颜色值校验, 包括border-color
   * @param names ['borderTopColor', 'borderRightColor', 'borderBottomColor', 'borderLeftColor']
   * @param v blue red blue red
   * @returns {*}
   * @constructor
   */
  arraycolor: function(names, v) {
    v = (v || '').toString()
    const items = v.match(REGEXP_ARRAYCOLOR)

    if (items && items.length <= 4) {
      // logType为当前日志类型在logTypes数组对应的下标
      const values = []
      let result
      const logs = []
      let logType = 0

      items.forEach((it, index) => {
        result = validator.color(it)

        // 如果校验成功，则保存转换后的属性值
        if (isValidValue(result.value)) {
          values.push(result.value)
        }

        if (result.reason) {
          let str = result.reason(index.toString(), it, result.value)
          // 提取日志类型
          const match = str.match(/^([A-Z]+):/)

          if (match) {
            const idx = logTypes.indexOf(match[1])
            if (logType < logTypes.indexOf(match[1])) {
              logType = idx
            }
            str = str.replace(match[0], '').trim()
          }
          logs.push(str)
        }
      })

      return {
        value: logType < 2 ? splitAttr(names, values) : null,
        reason:
          logs.length > 0
            ? function reason(k, v) {
                const lt = logTypes[logType]
                const ccth = camelCaseToHyphened(k)
                const logsStr = logs.join('\n  ')
                return `${lt}: 属性 '${ccth}' 的值 '${v}' 存在问题: \n ${logsStr}`
              }
            : null
      }
    }

    return {
      value: null,
      reason: function reason(k, v) {
        const ccth = camelCaseToHyphened(k)
        return `ERROR: 属性 '${ccth}' 的值 '${v}' 格式不正确`
      }
    }
  },

  /**
   * 数组长度值校验, 包括padding, margin, border-width, translate
   * @param {String} v
   * @param {Array} units - 支持的单位
   * @returns {*}
   */
  arraylength: function(names, v, units) {
    v = (v || '').toString().trim()
    // 空格或逗号分隔
    const items = v.split(/[,\s]+/)

    if (items && items.length <= names.length) {
      // logType为当前日志类型在logTypes数组对应的下标
      const values = []
      let result
      const logs = []
      let logType = 0
      // 是否为margin-* 类型的样式属性
      const isMultipleLength = /^margin.*/.test(names[0])

      items.forEach((it, index) => {
        if (isMultipleLength) {
          result = validator.multipleLength(it, units)
        } else {
          result = validator.length(it, units)
        }
        // 如果校验成功，则保存转换后的属性值
        if (isValidValue(result.value)) {
          values.push(result.value)
        }

        if (result.reason) {
          let str = result.reason(index.toString(), it, result.value)
          // 提取日志类型
          const match = str.match(/^([A-Z]+):/)

          if (match) {
            const idx = logTypes.indexOf(match[1])
            if (logType < logTypes.indexOf(match[1])) {
              logType = idx
            }
            str = str.replace(match[0], '').trim()
          }
          logs.push(str)
        }
      })
      return {
        value: logType < 2 ? splitAttr(names, values) : null,
        reason:
          logs.length > 0
            ? function reason(k) {
                const lt = logTypes[logType]
                const ccth = camelCaseToHyphened(k)
                const logsStr = logs.join('\n  ')
                return `${lt} : 属性 '${ccth}' 的值 '${v}' 存在问题: \n ${logsStr}`
              }
            : null
      }
    }

    return {
      value: null,
      reason: function reason(k, v) {
        const ccth = camelCaseToHyphened(k)
        return `ERROR: 属性 '${ccth}' 的值 '${v}' 格式不正确`
      }
    }
  },

  /**
   * border校验: border
   * @param v
   * @param units 支持的单位
   * @param position 具体的位置
   * @returns {*}
   * @constructor
   */
  border: function(v, units, position) {
    v = (v || '').toString()

    // 处理颜色内有逗号分割的情况
    v = v.replace(/\s*,\s*/g, ',')
    position = (position || '').toString()
    // 空格分隔
    const items = v.split(/\s+/)

    if (items && items.length <= 3) {
      // logType为当前日志类型在logTypes数组对应的下标，typeList记录简写属性对应的类别数组
      let values = []
      let result
      const logs = []
      let logType = 0
      const typeList = []
      let prevType = -1

      items.forEach((it, index) => {
        // 检测简写属性值的合法性，区分值为width、style和color的情况,如果校验成功，则保存转换后的属性值和类别
        if (isValidValue(validator.length(it, units).value)) {
          typeList.push(0)
          const selName = 'border' + position + 'Width'
          result = validateStyleMap[selName](it)
          if (result.value instanceof Array) {
            values = values.concat(result.value)
          } else if (position && isValidValue(result.value)) {
            values.push({
              n: selName,
              v: result.value
            })
          }
        } else if (isValidValue(validateStyleMap.borderStyle(it).value)) {
          typeList.push(1)
          result = validateStyleMap.borderStyle(it)
          values.push({
            n: 'border' + position + 'Style',
            v: it
          })
        } else if (isValidValue(validator.color(it).value)) {
          typeList.push(2)
          const selName = 'border' + position + 'Color'
          result = validateStyleMap[selName](it)
          if (result.value instanceof Array) {
            values = values.concat(result.value)
          } else if (position && isValidValue(result.value)) {
            values.push({
              n: selName,
              v: result.value
            })
          }
        } else {
          result = {}
          logType = 2
          logs.push(
            `属性 '${index}' 的值 '${it}' 存在问题: \n  不满足width、style和color的检验标准`
          )
        }

        if (result && result.reason) {
          let str = result.reason(index.toString(), it, result.value)
          // 提取日志类型
          const match = str.match(/^([A-Z]+):/)

          if (match) {
            const idx = logTypes.indexOf(match[1])
            if (logType < logTypes.indexOf(match[1])) {
              logType = idx
            }
            str = str.replace(match[0], '').trim()
          }
          logs.push(str)
        }
      })
      // 检测简写属性值中width、style和color的顺序是否符合标准
      typeList.forEach(it => {
        if (it > prevType) {
          prevType = it
        } else {
          logType = 2
          logs.push('必须按顺序设置属性width style color')
        }
      })

      return {
        value: logType < 2 ? values : null,
        reason:
          logs.length > 0
            ? function reason(k, v) {
                const lt = logTypes[logType]
                const ccth = camelCaseToHyphened(k)
                const logsStr = logs.join('\n  ')
                return `${lt} : 属性 '${ccth}' 的值 '${v}' 存在问题: \n ${logsStr}`
              }
            : null
      }
    }

    return {
      value: null,
      reason: function reason(k, v) {
        const ccth = camelCaseToHyphened(k)
        return `ERROR: 属性 '${ccth}' 的值 '${v}' 格式不正确`
      }
    }
  },
  borderLeft: function(v, units) {
    return validator.border(v, units, 'Left')
  },
  borderRight: function(v, units) {
    return validator.border(v, units, 'Right')
  },
  borderTop: function(v, units) {
    return validator.border(v, units, 'Top')
  },
  borderBottom: function(v, units) {
    return validator.border(v, units, 'Bottom')
  },
  /**
   * 命名校验
   * @param v
   * @returns {*}
   */
  background: function(v) {
    v = (v || '').toString().trim()
    // 预留接口：分解background所有参数，存入数组
    let items = v.split()
    // 处理多组渐变
    if (v.indexOf('-gradient') > 0) {
      const reg = /(repeating-linear|linear)[\s\S]*?(?=\s*(repeating|linear)|$)/g
      items = v.match(reg)
    }
    // 初始化返回对象
    const value = {
      values: []
    }
    if (items && items.length) {
      const logs = []
      let logType = 0
      // 逐项处理，校验后的值存入value
      items.forEach(it => {
        let key
        let validator

        // 参数分类处理
        // 参数为(repeating-)?linear-gradient(xxx)
        if (it.indexOf('-gradient') >= 0) {
          // (repeating-)?linear-gradient(xxx)按同一种模式校验
          key = it.indexOf('repeating') >= 0 ? 'repeatingLinearGradient' : 'linearGradient'
          validator = backgroundValidatorMap[key]
        }

        if (typeof validator === 'function') {
          const result = validator(it)
          // 如果校验成功，则保存转换后的属性值
          if (isValidValue(result.value)) {
            const parseObj = JSON.parse(result.value)
            value.values.push(parseObj)
          }
          if (result.reason) {
            let str = result.reason(key, it, result.value)
            // 提取日志类型
            const match = str.match(/^([A-Z]+):/)
            if (match) {
              const idx = logTypes.indexOf(match[1])
              if (logType < logTypes.indexOf(match[1])) {
                logType = idx
              }
              str = str.replace(match[0], '').trim()
            }
            logs.push(str)
          }
        } else {
          logType = 2
          logs.push(`背景类型 '${it}' 暂不支持`)
        }
      })

      return {
        value: logType < 2 ? JSON.stringify(value) : null,
        reason:
          logs.length > 0
            ? function(k, v) {
                const lt = logTypes[logType]
                const ccth = camelCaseToHyphened(k)
                const logsStr = logs.join('\n  ')
                return `${lt} : 属性 '${ccth}' 的值 '${v}' 存在问题: \n ${logsStr}`
              }
            : null
      }
    }

    return {
      value: null,
      reason: function reason(k, v) {
        const ccth = camelCaseToHyphened(k)
        return `ERROR: 属性 '${ccth}' 的值 '${v}' 格式不正确`
      }
    }
  },
  /**
   * 属性校验
   * @param v
   * @returns {*}
   */
  linearGradient: function(v) {
    v = (v || '').toString().trim()
    // 初始化返回对象格式
    const result = {
      type: '',
      directions: ['to', 'bottom'], // 默认从上到下
      values: []
    }

    let objColor = {}
    let objDirection = {}
    const logs = []
    let logType = 0
    // 分离(repeating-)linear-gradient函数名与参数
    const inMatchs = v.match(/^([0-9a-zA-Z-]+)\(([\s\S]*)\)/)
    if (inMatchs) {
      const key = hyphenedToCamelCase(inMatchs[1])
      result.type = key // type类型
      const valueList = inMatchs[2].split(/,/)

      // 校验direction或angle部分(非必要参数)
      if (REGEXP_GRADIENT_DIRECTION.test(valueList[0])) {
        let directionValidator
        // direction
        if (/(to|bottom|right|left|top)/.test(valueList[0])) {
          directionValidator = backgroundValidatorMap.linearGradientDirection
          // angle
        } else if (valueList[0].match(REGEXP_ANGLE)) {
          directionValidator = backgroundValidatorMap.linearGradientAngle
        }

        if (typeof directionValidator === 'function') {
          objDirection = directionValidator(valueList[0])
          // 分离direction或angle，剩下color-stop部分
          valueList.splice(0, 1)
          if (isValidValue(objDirection.value)) {
            result.directions = objDirection.value.split(/\s+/)
          }
          if (objDirection.reason) {
            let str = objDirection.reason(key, valueList[0], objDirection.value)
            if (str) {
              const match = str.match(/^([A-Z]+):/)
              if (match) {
                const idx = logTypes.indexOf(match[1])
                if (logType < logTypes.indexOf(match[1])) {
                  logType = idx
                }
                str = str.replace(match[0], '').trim()
              }
              logs.push(str)
            }
          }
        }
      }

      // 校验color-stop部分
      if (valueList.length > 0) {
        const validator = backgroundValidatorMap.linearGradientColor
        objColor = validator(valueList)
        if (isValidValue(objColor.value)) {
          result.values = JSON.parse(objColor.value)
        }
        if (objColor.reason) {
          let str = objColor.reason(key, valueList, objColor.value)
          if (str) {
            const match = str.match(/^([A-Z]+):/)
            if (match) {
              const idx = logTypes.indexOf(match[1])
              if (logType < logTypes.indexOf(match[1])) {
                logType = idx
              }
              str = str.replace(match[0], '').trim()
            }
            logs.push(str)
          }
        }
      } else {
        logType = 2
        logs.push(`参数 '${v}' 缺少过渡颜色`)
      }

      return {
        value: logType < 2 ? JSON.stringify(result) : null,
        reason:
          logs.length > 0
            ? function(k, v) {
                const lt = logTypes[logType]
                const ccth = camelCaseToHyphened(k)
                const logsStr = logs.join('\n  ')
                return `${lt} : 属性 '${ccth}' 的值 '${v}' 存在问题: \n ${logsStr}`
              }
            : null
      }
    }

    // 匹配不成功，格式错误
    return {
      value: null,
      reason: function reason(k, v) {
        const ccth = camelCaseToHyphened(k)
        return `ERROR: 属性 '${ccth}' 的值 '${v}' 格式不正确`
      }
    }
  },

  /**
   * 渐变参数color-stop值校验, 渐变宽度、百分比
   * @param v
   * @returns {*}
   */
  arraycolorstop: function(v) {
    v = (v || '').toString().trim()
    // 匹配color-stop组合
    const items = v.match(REGEXP_ARRAYCOLORSTOP)

    // 至少指定两种颜色
    if (items && items.length > 1) {
      const value = []
      const logs = []
      let logType = 0

      items.forEach((it, index) => {
        // 匹配stop部分
        const arrStop = it.match(/[\s]+[-+0-9]+(px|%|dp)?$/)
        // 存放color与stop校验后的值
        const groupValue = []

        // 校验stop部分
        if (arrStop) {
          const objStop = validator.length(arrStop[0])
          const num = it.indexOf(arrStop[0])
          // 得到color部分
          it = it.substring(0, num)
          if (isValidValue(objStop.value)) {
            groupValue.push(objStop.value)
          }

          if (objStop.reason) {
            let str = objStop.reason(index.toString(), arrStop[0], objStop.value)
            // 提取日志类型
            const match = str.match(/^([A-Z]+):/)
            if (match) {
              const idx = logTypes.indexOf(match[1])
              if (logType < logTypes.indexOf(match[1])) {
                logType = idx
              }
              str = str.replace(match[0], '').trim()
            }
            logs.push(str)
          }
        }

        if (it) {
          const objColor = validator.color(it)
          // 如果校验成功，则保存转换后的属性值
          if (isValidValue(objColor.value)) {
            // 校验后的color放到stop之前前
            groupValue.unshift(objColor.value)
          }

          // 存入校验后的color-stop值
          value.push(groupValue.join(' '))
          if (objColor.reason) {
            let str = objColor.reason(index.toString(), it, objColor.value)
            // 提取日志类型
            const match = str.match(/^([A-Z]+):/)
            if (match) {
              const idx = logTypes.indexOf(match[1])
              if (logType < logTypes.indexOf(match[1])) {
                logType = idx
              }
              str = str.replace(match[0], '').trim()
            }
            logs.push(str)
          }
        } else {
          logType = 2
          logs.push(`参数 '${v}' 格式不正确`)
        }
      })

      return {
        value: logType < 2 ? JSON.stringify(value) : null,
        reason:
          logs.length > 0
            ? function reason(k, v) {
                const lt = logTypes[logType]
                const ccth = camelCaseToHyphened(k)
                const logsStr = logs.join('\n  ')
                return `${lt} : 属性 '${ccth}' 的值 '${v}' 存在问题: \n ${logsStr}`
              }
            : null
      }
    }

    return {
      value: null,
      reason: function reason(k, v) {
        const ccth = camelCaseToHyphened(k)
        return `ERROR: 属性 '${ccth}' 的值 '${v}' 格式不正确，至少指定两种颜色`
      }
    }
  },
  /**
   * 角度校验
   * @param v
   * @returns {*}
   */
  angle: function(v) {
    v = (v || '').toString().trim()
    const match = v.match(REGEXP_ANGLE)

    if (match) {
      // 尝试检查单位
      const unit = match[1]
      if (!unit) {
        return {
          value: parseFloat(v) + cssAngleUnits[0],
          reason: function reason(k) {
            const ccth = camelCaseToHyphened(k)
            return `WARNING: 属性 '${ccth}' 没有指定单位，默认为 '${cssAngleUnits[0]}'`
          }
        }
      } else if (cssAngleUnits.indexOf(unit.toLowerCase()) >= 0) {
        // 如果单位合法
        return { value: v }
      } else {
        // 其余格式单位，一律默认为ms
        let msv = parseFloat(v)
        // TODO: 暂时实现rad到deg的转换
        if (unit.toLowerCase() === 'rad') {
          msv = Math.round((msv * 180) / Math.PI)
          return {
            value: msv + cssAngleUnits[0],
            reason: function reason(k) {
              const ccth = camelCaseToHyphened(k)
              return `WARNING: 属性 '${ccth}' 不支持单位 '${unit}', 自动转换为 '${cssAngleUnits[0]}'`
            }
          }
        }

        return {
          value: msv + cssAngleUnits[0],
          reason: function reason(k) {
            const ccth = camelCaseToHyphened(k)
            const cssAngleUnitsJson = JSON.stringify(cssAngleUnits)
            return `ERROR: 属性 '${ccth}' 不支持单位 '${unit}', 目前仅支持 '${cssAngleUnitsJson}'`
          }
        }
      }
    }

    return {
      value: null,
      reason: function reason(k, v) {
        const ccth = camelCaseToHyphened(k)
        return `ERROR: 属性 '${ccth}' 的值不正确 '${v}' (仅支持数值)`
      }
    }
  },
  /**
   * gradient方向校验
   * @param v
   * @returns {*}
   * @constructor
   */
  gradientdirection: function(v) {
    v = (v || '').toString().trim()
    // 空格分开的字符串转化为数组
    const items = v.split(/\s+/)
    let misMatch = []
    const arr = []
    items.forEach(it => {
      if (it === 'to') {
        arr.push(0)
      } else if ((it === 'top') | (it === 'bottom')) {
        arr.push(1)
      } else if ((it === 'left') | (it === 'right')) {
        arr.push(2)
      } else {
        // 出现(to|left|top|right|bottom)以外的参数
        misMatch.push(it)
      }
    })

    if (misMatch.length === 0 && arr.length > 1 && arr.length < 4) {
      if (arr[0] === 0 && arr[1] !== 0) {
        // 存在第三个参数
        if (arr[2]) {
          // 非相邻组合或第三个参数为‘to’
          if (arr[1] + arr[2] !== 3) {
            misMatch = items
          }
        }
      } else {
        misMatch = items
      }
    } else {
      misMatch = items
    }

    return {
      value: misMatch.length > 0 ? null : items.join(' '),
      reason:
        misMatch.length > 0
          ? function reason(k) {
              const ccth = camelCaseToHyphened(k)
              const misMatchs = misMatch.join(' ')
              return `ERROR: 属性 '${ccth}' 的属性值 '${misMatchs}' 格式不正确`
            }
          : null
    }
  },
  url: function(v) {
    v = (v || '').toString().trim()
    if (v.match(/^none$/i)) {
      return { value: null }
    }

    const url = REGEXP_URL.exec(v)
    const value = url && url[2].trim()
    if (value) {
      return { value: value }
    }

    return {
      value: null,
      reason: function reason(k, v) {
        const ccth = camelCaseToHyphened(k)
        return `WARNING: 属性 '${ccth}' 的值 '${v}' 必须是 none 或者 url(...)`
      }
    }
  }
}

// background属性校验表
const backgroundValidatorMap = {
  linearGradient: validator.linearGradient,
  repeatingLinearGradient: validator.linearGradient,
  linearGradientColor: validator.arraycolorstop,
  linearGradientAngle: validator.angle,
  linearGradientDirection: validator.gradientdirection
}

const validateStyleMap = {
  border: validator.border,
  borderLeft: validator.borderLeft,
  borderRight: validator.borderRight,
  borderTop: validator.borderTop,
  borderBottom: validator.borderBottom,
  borderWidth: styleValue => {
    return validator.arraylength(
      ['borderTopWidth', 'borderRightWidth', 'borderBottomWidth', 'borderLeftWidth'],
      styleValue
    )
  },
  borderLeftWidth: validator.length,
  borderTopWidth: validator.length,
  borderRightWidth: validator.length,
  borderBottomWidth: validator.length,
  borderColor: styleValue => {
    return validator.arraycolor(
      ['borderTopColor', 'borderRightColor', 'borderBottomColor', 'borderLeftColor'],
      styleValue
    )
  },
  borderLeftColor: validator.color,
  borderTopColor: validator.color,
  borderRightColor: validator.color,
  borderBottomColor: validator.color,
  borderStyle: makeEnumValidator(['solid', 'dotted', 'dashed']),
  padding: styleValue => {
    return validator.arraylength(
      ['paddingTop', 'paddingRight', 'paddingBottom', 'paddingLeft'],
      styleValue
    )
  },
  margin: styleValue => {
    return validator.arraylength(
      ['marginTop', 'marginRight', 'marginBottom', 'marginLeft'],
      styleValue
    )
  },
  backgroundImage: validator.url,
  background: validator.background
}

function makeArrayValuesToObject(values = [], key = 'n', val = 'v') {
  const result = {}
  Array.isArray(values) &&
    values.forEach(item => {
      if (item && item[key]) {
        result[item[key]] = item[val]
      }
    })
  return result
}

function printLog(str) {
  if (!str) return
  let logType = 0
  const match = str.match(/^([A-Z]+):/)
  if (match) {
    const idx = logTypes.indexOf(match[1])
    if (logType < logTypes.indexOf(match[1])) {
      logType = idx
    }
    str = str.replace(match[0], '').trim()
  }
  str = `### App Runtime ### ${str}`
  switch (logType) {
    case 0:
      console.log(str)
      break
    case 1:
      console.warn(str)
      break
    case 2:
      console.error(str)
      break
    default:
      console.log(str)
  }
}

/**
 * 生成枚举类型校验函数
 * @param list
 * @returns {validator_enum}
 */
function makeEnumValidator(list) {
  return validator.enum.bind(null, list)
}

/**
 * 把validator的返回值处理为统一格式
 * @param {Object} validRes
 * @param {String} styleKey
 * @returns {Object}
 */
function formatValidator(validRes, styleKey) {
  const value =
    validRes.value instanceof Array ? validRes.value : [{ n: styleKey, v: validRes.value }]
  const result = {
    value: makeArrayValuesToObject(value),
    reason: validRes.reason
  }
  return result
}

/**
 * 校验动态设置的内联样式值是否合法，转换不合法的样式值
 * @param {Object} styleObj
 * @returns {Object}
 */
export default function $validateStyle(styleKey, styleValue) {
  let styleObj = {}
  const validator = validateStyleMap[styleKey]
  if (validator) {
    const validRes = validator(styleValue)
    if (validRes && validRes.value) {
      const formatRes = formatValidator(validRes, styleKey)
      styleObj = formatRes.value
      if (typeof formatRes.reason === 'function') {
        const log = formatRes.reason(styleKey, styleValue)
        printLog(log)
      }
    } else {
      styleObj[styleKey] = styleValue
    }
  } else {
    styleObj[styleKey] = styleValue
  }
  return styleObj
}
