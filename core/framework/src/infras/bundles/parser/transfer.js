/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { decode } from './discode'

import { $camelize, isValidValue } from './util'

import HtmlParser from './htmlparser'

import validateStyle from './style'

/**
 * 删除DOCTYPE
 * @param html
 * @returns {string|XML}
 */
function removeDOCTYPE(html) {
  return html
    .replace(/<\?xml.*\?>\n/, '')
    .replace(/<.*!doctype.*>\n/, '')
    .replace(/<.*!DOCTYPE.*>\n/, '')
}

/**
 * 标签映射
 * @type {Map<any, any>}
 */
const tagMap = new Map([
  ['img', 'image'],
  ['p', 'text'],
  ['h1', 'text'],
  ['h2', 'text'],
  ['h3', 'text'],
  ['h4', 'text'],
  ['h5', 'text'],
  ['h6', 'text'],
  ['b', 'span'],
  ['strong', 'span'],
  ['i', 'span'],
  ['del', 'span'],
  ['article', 'div'],
  ['br', 'span']
])

function xTag(tag) {
  const xtag = tagMap.get(tag)
  return xtag !== undefined ? xtag : tag
}

/**
 * 标签内建样式
 */
const tagBuildinStyles = {
  div: {},
  h1: {
    fontSize: '60px',
    fontWeight: 'normal'
  },
  h2: {
    fontSize: '45px',
    fontWeight: 'normal'
  },
  h3: {
    fontWeight: 'normal',
    fontSize: '35px'
  },
  h4: {
    fontWeight: 'normal',
    fontSize: '30px'
  },
  h5: {
    fontWeight: 'normal',
    fontSize: '25px'
  },
  h6: {
    fontWeight: 'normal',
    fontSize: '20px'
  },
  b: {
    fontWeight: 'bold'
  },
  strong: {
    fontWeight: 'bold'
  },
  i: {
    fontStyle: 'italic'
  },
  a: {
    color: '#00BFFF'
  },
  del: {
    textDecoration: 'line-through'
  }
}

function xStyle(tag) {
  let style = {}
  if (tagBuildinStyles[tag]) {
    style = tagBuildinStyles[tag]
  }
  return style
}

/**
 * 标签内建样式
 */
const tagBuildinAttrs = {
  br: {
    value: '\n'
  }
}

function xAttr(tag) {
  let attr = {}
  if (tagBuildinAttrs[tag]) {
    attr = tagBuildinAttrs[tag]
  }
  return attr
}

/**
 * HTML解析为AST
 * @param html
 * @param type
 * @returns {{type: string, children: Array, events: {}, classList: Array, attr: {}, style: {flex: number, flexDirection: string}}}
 */
function compile(html, type, scenario) {
  const isx = type !== 'html'
  const usedForEdit = scenario === 'edit'
  // 处理字符串
  html = removeDOCTYPE(html)
  html = decode(html)
  // 生成node节点
  const bufArray = []
  const results = {
    type: 'div',
    children: [],
    events: {},
    classList: [],
    attr: {},
    style: {
      flex: 1,
      flexDirection: 'column'
    }
  }

  HtmlParser(html, {
    start: function(tag, attrs, unary) {
      const node = {
        type: isx ? tag : xTag(tag), // 标签映射
        children: [],
        events: {},
        classList: [],
        attr: isx ? {} : xAttr(tag),
        style: isx ? {} : xStyle(tag)
      }

      if (attrs.length !== 0) {
        node.attr = attrs.reduce(function(pre, attr) {
          const name = $camelize(attr.name)
          let value = attr.value
          if (name === 'class') {
            node.classList = value.split(/\s+/)
          } else if (name === 'style') {
            // 处理样式
            value.split(';').forEach(function(declarationText) {
              let k, v
              let pair = declarationText.trim().split(':')

              // 如果出现xxx:xxx:xxx的情况, 则将第一个:之后文本作为value
              if (pair.length > 2) {
                pair[1] = pair.slice(1).join(':')
                pair = pair.slice(0, 2)
              }

              if (pair.length === 2) {
                k = pair[0].trim()
                v = validateStyle(k, pair[1].trim())

                if (v.value) {
                  v.value.forEach(t => {
                    // 如果校验成功，则保存转换后的属性值
                    if (isValidValue(t.v)) {
                      node.style[t.n] = t.v
                    }
                  })
                }

                if (v.log) {
                  console.warn('### App Parser ### ', v.log)
                }
              }
            })
          } else if (name === 'id') {
            node.id = value
          } else if (usedForEdit) {
            pre[name] = value
          } else {
            // 其余按普通属性处理
            if (value.match(/ /)) {
              // 如果包含空格, 则按数组处理
              value = value.split(' ')
            }

            if (pre[name]) {
              if (Array.isArray(pre[name])) {
                pre[name].push(value)
              } else {
                pre[name] = [pre[name], value]
              }
            } else {
              pre[name] = value
            }
          }
          return pre
        }, {})
      }

      // 临时记录source资源
      if (!isx && node.type === 'source') {
        results.source = node.attr.src
      }

      if (unary) {
        const parent = bufArray[0] || results
        if (parent.children === undefined) {
          parent.children = []
        }
        parent.children.push(node)
      } else {
        bufArray.unshift(node)
      }
    },
    end: function(tag) {
      const xtag = isx ? tag : xTag(tag)
      const node = bufArray.shift()
      if (node.type !== xtag) {
        console.error('### App Parser ### 结束标签不匹配：', tag)
      }

      // 当有缓存source资源时于于video补上src资源
      if (!isx && node.type === 'video' && results.source) {
        node.attr.src = results.source
        delete results.source
      }

      if (bufArray.length === 0) {
        results.children.push(node)
      } else {
        const parent = bufArray[0]
        if (parent.children === undefined) {
          parent.children = []
        }
        parent.children.push(node)
      }
    },
    text: function(text) {
      const trimtext = text.slice().replace(/\s*/, '')
      if (trimtext !== '') {
        const parent = bufArray[0]
        if (usedForEdit && (!parent || ['text', 'a', 'span'].indexOf(parent.type) === -1)) {
          // 纯文本
          const node = {
            type: 'text',
            children: [],
            events: {},
            classList: [],
            attr: {
              value: text
            },
            style: {}
          }

          const curr = parent || results
          curr.children.push(node)
        } else if (parent) {
          // 如果没有父节点则跳过
          if (['text', 'a'].indexOf(parent.type) !== -1) {
            const node = {
              type: 'span',
              children: [],
              events: {},
              classList: [],
              attr: {
                value: text
              },
              style: {},
              // 标识：为text而存在
              polyfill: true
            }
            if (parent.children === undefined) {
              parent.children = []
            }
            parent.children.push(node)
          } else {
            // 其余标签统一按value处理
            parent.attr.value = text
          }
        }
      }
    },
    comment: function(text) {}
  })
  console.trace(`### App Parser ### 解析${type}文本成功：${JSON.stringify(results)}`)
  return results
}

/**
 * 调试器：解析HTML字符串
 */
function parseHTML(html) {
  // 解析
  const node = compile(html, '', 'edit')
  // 处理
  processNode(node)
  // 仅保留children
  const result = node.children || []

  return result
}

function processNode(node) {
  if (node.children) {
    if (['text', 'a'].indexOf(node.type) !== -1) {
      // 如果有children时，删除value属性
      if (node.children.length > 0) {
        delete node.attr.value
      }
      // 仅有polyfill的span节点时：删除span
      if (node.children.length === 1 && node.children[0].polyfill) {
        node.attr = Object.assign(node.attr, node.children[0].attr)
        node.children = []
      }
    }
    for (let i = 0, len = node.children.length; i < len; i++) {
      const childNode = node.children[i]
      processNode(childNode)
    }
  }
}

export default {
  compile,
  parseHTML
}
