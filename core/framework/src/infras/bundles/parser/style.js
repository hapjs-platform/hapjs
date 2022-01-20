/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $hyphenate, $camelize, splitAttr, isValidValue } from './util'

// 日志类型
const logTypes = ['NOTE', 'WARNING', 'ERROR']

const colorNames = new Map([
  ['aliceblue', '#F0F8FF'],
  ['antiquewhite', '#FAEBD7'],
  ['aqua', '#00FFFF'],
  ['aquamarine', '#7FFFD4'],
  ['azure', '#F0FFFF'],
  ['beige', '#F5F5DC'],
  ['bisque', '#FFE4C4'],
  ['black', '#000000'],
  ['blanchedalmond', '#FFEBCD'],
  ['blue', '#0000FF'],
  ['blueviolet', '#8A2BE2'],
  ['brown', '#A52A2A'],
  ['burlywood', '#DEB887'],
  ['cadetblue', '#5F9EA0'],
  ['chartreuse', '#7FFF00'],
  ['chocolate', '#D2691E'],
  ['coral', '#FF7F50'],
  ['cornflowerblue', '#6495ED'],
  ['cornsilk', '#FFF8DC'],
  ['crimson', '#DC143C'],
  ['cyan', '#00FFFF'],
  ['darkblue', '#00008B'],
  ['darkcyan', '#008B8B'],
  ['darkgoldenrod', '#B8860B'],
  ['darkgray', '#A9A9A9'],
  ['darkgreen', '#006400'],
  ['darkgrey', '#A9A9A9'],
  ['darkkhaki', '#BDB76B'],
  ['darkmagenta', '#8B008B'],
  ['darkolivegreen', '#556B2F'],
  ['darkorange', '#FF8C00'],
  ['darkorchid', '#9932CC'],
  ['darkred', '#8B0000'],
  ['darksalmon', '#E9967A'],
  ['darkseagreen', '#8FBC8F'],
  ['darkslateblue', '#483D8B'],
  ['darkslategray', '#2F4F4F'],
  ['darkslategrey', '#2F4F4F'],
  ['darkturquoise', '#00CED1'],
  ['darkviolet', '#9400D3'],
  ['deeppink', '#FF1493'],
  ['deepskyblue', '#00BFFF'],
  ['dimgray', '#696969'],
  ['dimgrey', '#696969'],
  ['dodgerblue', '#1E90FF'],
  ['firebrick', '#B22222'],
  ['floralwhite', '#FFFAF0'],
  ['forestgreen', '#228B22'],
  ['fuchsia', '#FF00FF'],
  ['gainsboro', '#DCDCDC'],
  ['ghostwhite', '#F8F8FF'],
  ['gold', '#FFD700'],
  ['goldenrod', '#DAA520'],
  ['gray', '#808080'],
  ['green', '#008000'],
  ['greenyellow', '#ADFF2F'],
  ['grey', '#808080'],
  ['honeydew', '#F0FFF0'],
  ['hotpink', '#FF69B4'],
  ['indianred', '#CD5C5C'],
  ['indigo', '#4B0082'],
  ['ivory', '#FFFFF0'],
  ['khaki', '#F0E68C'],
  ['lavender', '#E6E6FA'],
  ['lavenderblush', '#FFF0F5'],
  ['lawngreen', '#7CFC00'],
  ['lemonchiffon', '#FFFACD'],
  ['lightblue', '#ADD8E6'],
  ['lightcoral', '#F08080'],
  ['lightcyan', '#E0FFFF'],
  ['lightgoldenrodyellow', '#FAFAD2'],
  ['lightgray', '#D3D3D3'],
  ['lightgreen', '#90EE90'],
  ['lightgrey', '#D3D3D3'],
  ['lightpink', '#FFB6C1'],
  ['lightsalmon', '#FFA07A'],
  ['lightseagreen', '#20B2AA'],
  ['lightskyblue', '#87CEFA'],
  ['lightslategray', '#778899'],
  ['lightslategrey', '#778899'],
  ['lightsteelblue', '#B0C4DE'],
  ['lightyellow', '#FFFFE0'],
  ['lime', '#00FF00'],
  ['limegreen', '#32CD32'],
  ['linen', '#FAF0E6'],
  ['magenta', '#FF00FF'],
  ['maroon', '#800000'],
  ['mediumaquamarine', '#66CDAA'],
  ['mediumblue', '#0000CD'],
  ['mediumorchid', '#BA55D3'],
  ['mediumpurple', '#9370DB'],
  ['mediumseagreen', '#3CB371'],
  ['mediumslateblue', '#7B68EE'],
  ['mediumspringgreen', '#00FA9A'],
  ['mediumturquoise', '#48D1CC'],
  ['mediumvioletred', '#C71585'],
  ['midnightblue', '#191970'],
  ['mintcream', '#F5FFFA'],
  ['mistyrose', '#FFE4E1'],
  ['moccasin', '#FFE4B5'],
  ['navajowhite', '#FFDEAD'],
  ['navy', '#000080'],
  ['oldlace', '#FDF5E6'],
  ['olive', '#808000'],
  ['olivedrab', '#6B8E23'],
  ['orange', '#FFA500'],
  ['orangered', '#FF4500'],
  ['orchid', '#DA70D6'],
  ['palegoldenrod', '#EEE8AA'],
  ['palegreen', '#98FB98'],
  ['paleturquoise', '#AFEEEE'],
  ['palevioletred', '#DB7093'],
  ['papayawhip', '#FFEFD5'],
  ['peachpuff', '#FFDAB9'],
  ['peru', '#CD853F'],
  ['pink', '#FFC0CB'],
  ['plum', '#DDA0DD'],
  ['powderblue', '#B0E0E6'],
  ['purple', '#800080'],
  ['red', '#FF0000'],
  ['rosybrown', '#BC8F8F'],
  ['royalblue', '#4169E1'],
  ['saddlebrown', '#8B4513'],
  ['salmon', '#FA8072'],
  ['sandybrown', '#F4A460'],
  ['seagreen', '#2E8B57'],
  ['seashell', '#FFF5EE'],
  ['sienna', '#A0522D'],
  ['silver', '#C0C0C0'],
  ['skyblue', '#87CEEB'],
  ['slateblue', '#6A5ACD'],
  ['slategray', '#708090'],
  ['slategrey', '#708090'],
  ['snow', '#FFFAFA'],
  ['springgreen', '#00FF7F'],
  ['steelblue', '#4682B4'],
  ['tan', '#D2B48C'],
  ['teal', '#008080'],
  ['thistle', '#D8BFD8'],
  ['tomato', '#FF6347'],
  ['turquoise', '#40E0D0'],
  ['violet', '#EE82EE'],
  ['wheat', '#F5DEB3'],
  ['white', '#FFFFFF'],
  ['whitesmoke', '#F5F5F5'],
  ['yellow', '#FFFF00'],
  ['yellowgreen', '#9ACD32']
])

// 长度单位
const cssLengthUnits = ['px', '%']
const REGEXP_LENGTH = /^[-+]?[0-9]*\.?[0-9]+(.*)$/
const REGEXP_COLOR_LONG = /^#[0-9a-fA-F]{6}$/
const REGEXP_COLOR_SHORT = /^#[0-9a-fA-F]{3}$/
const REGEXP_COLOR_RGB = /^rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)$/
const REGEXP_COLOR_RGBA = /^rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d*\.?\d+)\s*\)$/
const REGEXP_COLOR_HSL = /^hsl\(\s*(\d+)\s*,\s*(\d+%)\s*,\s*(\d+%)\s*\)$/
const REGEXP_COLOR_HSLA = /^hsla\(\s*(\d+)\s*,\s*(\d+%)\s*,\s*(\d+%)\s*,\s*(\d*\.?\d+)\s*\)$/
const REGEXP_INT = /^[-+]?[0-9]+$/
const REGEXP_URL = /^url\(\s*['"]?\s*([^()]+?)\s*['"]?\s*\)$/
const REGEXP_NAME = /^[a-zA-Z_][a-zA-Z0-9]*$/
const REGEXP_NUMBER = /^[-+]?[0-9]*\.?[0-9]+$/

const validator = {
  /**
   * 长度值校验
   * @param v
   * @param units 支持的单位
   * @returns {*}
   * @constructor
   */
  length: function(v, units) {
    v = (v || '').toString()
    const match = v.match(REGEXP_LENGTH)
    if (!units) {
      units = cssLengthUnits
    }

    if (match) {
      // 尝试检查单位
      const unit = match[1]
      if (!unit) {
        return {
          value: parseFloat(v) + units[0],
          reason: function reason(k, v, result) {
            return 'WARNING: 属性 `' + $hyphenate(k) + '` 没有指定单位，默认为 `' + units[0] + '`'
          }
        }
      } else if (units.indexOf(unit.toLowerCase()) >= 0) {
        // 如果单位合法
        return { value: v }
      } else {
        // 其余格式单位，一律默认为px
        return {
          value: parseFloat(v) + units[0],
          reason: function reason(k, v, result) {
            return (
              'ERROR: 属性 `' +
              $hyphenate(k) +
              '` 不支持单位 `' +
              unit +
              '`, 目前仅支持 `' +
              JSON.stringify(units) +
              '`'
            )
          }
        }
      }
    }
    return {
      value: null,
      reason: function reason(k, v, result) {
        return 'ERROR: 属性 `' + $hyphenate(k) + '` 的值 `' + v + '` 不正确(仅支持数值)'
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
    v = (v || '').toString()

    if (v.match(REGEXP_COLOR_LONG)) {
      return { value: v }
    }

    if (v.match(REGEXP_COLOR_SHORT)) {
      // 如果是#XXX，则转换为#XXXXXX
      return {
        value: '#' + v[1] + v[1] + v[2] + v[2] + v[3] + v[3],
        reason: function reason(k, v, result) {
          return 'NOTE: 颜色值 `' + v + '` 转换为 `' + result + '`'
        }
      }
    }

    if (colorNames.get(v)) {
      // 如果颜色值为颜色名字符串
      return {
        value: colorNames.get(v),
        reason: function reason(k, v, result) {
          return 'NOTE: 颜色值 `' + v + '` 转换为 `' + result + '`'
        }
      }
    }

    // rgb/rgbag格式颜色处理
    let arrColor
    let r, g, b, a

    arrColor = REGEXP_COLOR_RGB.exec(v)
    if (arrColor) {
      r = parseInt(arrColor[1])
      g = parseInt(arrColor[2])
      b = parseInt(arrColor[3])
      if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
        return { value: 'rgb(' + [r, g, b].join(',') + ')' }
      }
    }

    arrColor = REGEXP_COLOR_RGBA.exec(v)
    if (arrColor) {
      r = parseInt(arrColor[1])
      g = parseInt(arrColor[2])
      b = parseInt(arrColor[3])
      a = parseFloat(arrColor[4])
      if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255 && a >= 0 && a <= 1) {
        return { value: 'rgba(' + [r, g, b, a].join(',') + ')' }
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
      reason: function reason(k, v, result) {
        return 'ERROR: 属性`' + $hyphenate(k) + '` 的颜色值 `' + v + '` 无效`'
      }
    }
  },
  /**
   * 整数/浮点数校验
   * @param v
   * @returns {*}
   */
  number: function(v) {
    v = (v || '').toString()
    const match = v.match(REGEXP_NUMBER)

    if (match && !match[1]) {
      return { value: parseFloat(v) }
    }

    return {
      value: null,
      reason: function reason(k, v, result) {
        return 'ERROR: 属性`' + $hyphenate(k) + '` 的值 `' + v + '` 无效 ` (仅支持数值)'
      }
    }
  },
  /**
   * 整数校验
   * @param v
   * @returns {*}
   */
  integer: function(v) {
    v = (v || '').toString()

    if (v.match(REGEXP_INT)) {
      return { value: parseInt(v, 10) }
    }

    return {
      value: null,
      reason: function reason(k, v, result) {
        return 'ERROR: 属性`' + $hyphenate(k) + '` 的值 `' + v + '` 无效 ` (仅支持整数)'
      }
    }
  },
  /**
   * url校验
   * @param v
   * @returns {*}
   */
  url: function(v) {
    v = (v || '').toString().trim()
    if (v.match(/^none$/i)) {
      return { value: 'none' }
    }

    const url = REGEXP_URL.exec(v)
    if (url) {
      return { value: url[1] }
    }

    return {
      value: null,
      reason: function reason(k, v, result) {
        return 'WARNING: 属性`' + $hyphenate(k) + '` 的值 `' + v + '` 必须是 none 或者 url(...)'
      }
    }
  },

  /**
   * 命名校验
   * @param v
   * @returns {*}
   */
  name: function(v) {
    v = (v || '').toString()
    if (v.match(REGEXP_NAME)) {
      return { value: v }
    }

    return {
      value: null,
      reason: function reason(k, v, result) {
        return 'ERROR: 属性`' + $hyphenate(k) + '` 的值 `' + v + '` 格式不正确'
      }
    }
  },

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
        reason: function reason(k, v, result) {
          return 'NOTE:  属性`' + $hyphenate(k) + '` 的值 `' + v + '` 是缺省值(可以忽略不写)'
        }
      }
    } else {
      return {
        value: null,
        reason: function reason(k, v, result) {
          return (
            'ERROR: 属性`' +
            $hyphenate(k) +
            '` 的值 `' +
            v +
            '` 无效 ` (有效枚举值为: `' +
            list.join('`|`') +
            '`)'
          )
        }
      }
    }
  },

  /**
   * 数组长度值校验, 包括padding, margin, border-width
   * @param v
   * @param units 支持的单位
   * @returns {*}
   * @constructor
   */
  arraylength: function(names, v, units) {
    v = (v || '').toString()
    // 空格分隔
    const items = v.split(/\s+/)

    if (items && items.length <= 4) {
      // logType为当前日志类型在logTypes数组对应的下标
      const values = []
      let result
      const logs = []
      let logType = 0

      items.forEach((it, index) => {
        result = validator.length(it, units)

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
            ? function reason(k, v, result) {
                return (
                  logTypes[logType] +
                  ': 属性`' +
                  $hyphenate(k) +
                  '` 的值 `' +
                  v +
                  '` 存在问题: \n  ' +
                  logs.join('\n  ')
                )
              }
            : null
      }
    }

    return {
      value: null,
      reason: function reason(k, v, result) {
        return 'ERROR: 属性`' + $hyphenate(k) + '` 的值 `' + v + '` 格式不正确'
      }
    }
  },

  /**
   * 数组颜色值校验, 包括border-color
   * @param v
   * @returns {*}
   * @constructor
   */
  arraycolor: function(names, v) {
    v = (v || '').toString()
    // 空格分隔
    const items = v.split(/\s+/)

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
            ? function reason(k, v, result) {
                return (
                  logTypes[logType] +
                  ': 属性`' +
                  $hyphenate(k) +
                  '` 的值 `' +
                  v +
                  '` 存在问题: \n  ' +
                  logs.join('\n  ')
                )
              }
            : null
      }
    }

    return {
      value: null,
      reason: function reason(k, v, result) {
        return 'ERROR: 属性`' + $hyphenate(k) + '` 的值 `' + v + '` 格式不正确'
      }
    }
  },

  /**
   * border校验: border
   * @param v
   * @param units 支持的单位
   * @returns {*}
   * @constructor
   */
  border: function(v, units) {
    v = (v || '').toString()
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
          result = validatorMap.borderWidth(it)
          if (result.value instanceof Array) {
            values = values.concat(result.value)
          }
        } else if (isValidValue(validatorMap.borderStyle(it).value)) {
          typeList.push(1)
          result = validatorMap.borderStyle(it)
          values.push({
            n: 'borderStyle',
            v: it
          })
        } else if (isValidValue(validator.color(it).value)) {
          typeList.push(2)
          result = validatorMap.borderColor(it)
          if (result.value instanceof Array) {
            values = values.concat(result.value)
          }
        } else {
          result = {}
          logType = 2
          logs.push(
            '属性`' +
              index +
              '` 的值 `' +
              it +
              '` 存在问题: \n  不满足width、style和color的检验标准'
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
            ? function reason(k, v, result) {
                return (
                  logTypes[logType] +
                  ': 属性`' +
                  $hyphenate(k) +
                  '` 的值 `' +
                  v +
                  '` 存在问题: \n  ' +
                  logs.join('\n  ')
                )
              }
            : null
      }
    }

    return {
      value: null,
      reason: function reason(k, v, result) {
        return 'ERROR: 属性`' + $hyphenate(k) + '` 的值 `' + v + '` 格式不正确'
      }
    }
  },

  /**
   * display兼容性'block'校验
   * @param v
   * @returns {*}
   * @constructor
   */
  display: function(v) {
    v = (v || '').toString()
    const list = ['flex', 'none']
    const index = list.indexOf(v)
    if (index > 0) {
      return { value: v }
    }
    if (v === 'block') {
      return {
        value: 'flex',
        reason: function reason(k, v, result) {
          return (
            'ERROR: 属性`' +
            $hyphenate(k) +
            '` 的值 `' +
            v +
            '` 需修改为flex ` (有效枚举值为: `' +
            list.join('`|`') +
            '`)'
          )
        }
      }
    }
    if (index === 0) {
      return {
        value: v,
        // 关闭默认值的提示
        reason:
          false &&
          function reason(k, v, result) {
            return 'NOTE:  属性`' + $hyphenate(k) + '` 的值 `' + v + '` 是缺省值(可以忽略不写)'
          }
      }
    } else {
      return {
        value: null,
        reason: function reason(k, v, result) {
          return (
            'ERROR: 属性`' +
            $hyphenate(k) +
            '` 的值 `' +
            v +
            '` 无效 ` (有效枚举值为: `' +
            list.join('`|`') +
            '`)'
          )
        }
      }
    }
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
 * 生成指定类型的简写属性校验函数
 * @param type  简写属性校验函数类型
 * @param list  拆分后的样式名数组
 * @returns {validator_$type}
 */
function makeAbbrAttrValidator(type, list) {
  return validator[type].bind(null, list)
}

// CSS属性校验器映射表
const validatorMap = {
  // boxModel
  width: validator.length,
  height: validator.length,
  padding: makeAbbrAttrValidator('arraylength', [
    'paddingTop',
    'paddingRight',
    'paddingBottom',
    'paddingLeft'
  ]),
  paddingLeft: validator.length,
  paddingRight: validator.length,
  paddingTop: validator.length,
  paddingBottom: validator.length,
  margin: makeAbbrAttrValidator('arraylength', [
    'marginTop',
    'marginRight',
    'marginBottom',
    'marginLeft'
  ]),
  marginLeft: validator.length,
  marginRight: validator.length,
  marginTop: validator.length,
  marginBottom: validator.length,
  border: validator.border,
  borderWidth: makeAbbrAttrValidator('arraylength', [
    'borderTopWidth',
    'borderRightWidth',
    'borderBottomWidth',
    'borderLeftWidth'
  ]),
  borderLeftWidth: validator.length,
  borderTopWidth: validator.length,
  borderRightWidth: validator.length,
  borderBottomWidth: validator.length,
  borderColor: makeAbbrAttrValidator('arraycolor', [
    'borderTopColor',
    'borderRightColor',
    'borderBottomColor',
    'borderLeftColor'
  ]),
  borderLeftColor: validator.color,
  borderTopColor: validator.color,
  borderRightColor: validator.color,
  borderBottomColor: validator.color,
  borderStyle: makeEnumValidator(['solid', 'dotted', 'dashed']),
  borderRadius: validator.length,
  borderBottomLeftRadius: validator.length,
  borderBottomRightRadius: validator.length,
  borderTopLeftRadius: validator.length,
  borderTopRightRadius: validator.length,
  // flexbox
  flex: validator.number,
  flexGrow: validator.number,
  flexShrink: validator.number,
  flexBasis: validator.length,
  flexDirection: makeEnumValidator(['row', 'column']),
  flexWrap: makeEnumValidator(['nowrap', 'wrap', 'wrap-reverse']),
  justifyContent: makeEnumValidator(['flex-start', 'flex-end', 'center', 'space-between']),
  alignItems: makeEnumValidator(['stretch', 'flex-start', 'flex-end', 'center']),
  alignContent: makeEnumValidator([
    'stretch',
    'flex-start',
    'flex-end',
    'center',
    'space-between',
    'space-around'
  ]),
  // position
  position: makeEnumValidator(['none', 'fixed']),
  top: validator.length,
  bottom: validator.length,
  left: validator.length,
  right: validator.length,
  zIndex: validator.integer,
  // common
  opacity: validator.number,
  backgroundColor: validator.color,
  backgroundImage: validator.url,
  backgroundRepeat: makeEnumValidator(['no-repeat', 'repeat', 'repeat-x', 'repeat-y']),
  backgroundPosition: validator.position,
  display: validator.display,
  visibility: makeEnumValidator(['visible', 'hidden']),
  // text
  lines: validator.integer,
  color: validator.color,
  fontSize: validator.length,
  fontStyle: makeEnumValidator(['normal', 'italic']),
  fontWeight: makeEnumValidator(['normal', 'bold']),
  textDecoration: makeEnumValidator(['none', 'underline', 'line-through']),
  textAlign: makeEnumValidator(['left', 'center', 'right']),
  lineHeight: validator.length,
  textOverflow: makeEnumValidator(['clip', 'ellipsis']),
  // custom
  placeholderColor: validator.color,
  selectedColor: validator.color,
  textColor: validator.color,
  timeColor: validator.color,
  textHighlightColor: validator.color,
  strokeWidth: validator.length,
  progressColor: validator.color,
  resizeMode: makeEnumValidator(['cover', 'contain', 'stretch', 'center']),
  columns: validator.number,
  columnSpan: validator.number
}

/**
 * 校验CSS属性
 * @param name
 * @param value
 * @returns {{value: *, log: *}}
 */
function validate(name, value) {
  let result, log
  const validator = validatorMap[name]

  if (typeof validator === 'function') {
    if (typeof value !== 'function') {
      result = validator(value)
    } else {
      // 如果样式值是函数，则跳过校验
      result = { value: value }
    }

    if (result.reason) {
      log = { reason: result.reason(name, value, result.value) }
    }
  } else {
    // 如果没有类型校验器, 未知样式
    result = { value: value }
    log = { reason: 'ERROR: 样式名 `' + $hyphenate(name) + '` 不支持' }
  }

  return {
    value: result.value instanceof Array ? result.value : [{ n: name, v: result.value }],
    log: log
  }
}

/**
 * 校验样式, 如果失败, value为undefined
 * @param name
 * @param value
 * @returns {{}}
 */
function validateStyle(name, value) {
  const result = {}
  const valueTemp = []
  // 校验属性值
  const camelCasedName = $camelize(name)
  const subResult = validate(camelCasedName, value)

  subResult.value.forEach(item => {
    // 如果校验成功，则保存转换后的属性值
    if (isValidValue(item.v)) {
      valueTemp.push(item)
    }
  })

  if (valueTemp) {
    result.value = valueTemp
  }

  if (subResult.log) {
    result.log = subResult.log.reason
  }
  return result
}

export default validateStyle
