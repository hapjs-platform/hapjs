/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $newMap } from './util'

const startTag = /^<([-A-Za-z0-9_]+)((?:\s+[a-zA-Z_:][-a-zA-Z0-9_:.]*(?:\s*=\s*(?:(?:"[^"]*")|(?:'[^']*')|[^>\s]+))?)*)\s*(\/?)>/
const endTag = /^<\/([-A-Za-z0-9_]+)[^>]*>/
const attr = /([a-zA-Z_:][-a-zA-Z0-9_:.]*)(?:\s*=\s*(?:(?:"((?:\\.|[^"])*)")|(?:'((?:\\.|[^'])*)')|([^>\s]+)))?/g

// 空元素
const empty = $newMap(
  'area,base,basefont,br,col,frame,hr,img,input,link,meta,param,embed,command,keygen,source,track,wbr'
)
// 块级元素
const block = $newMap(
  'br,a,code,address,article,applet,aside,audio,blockquote,button,canvas,center,dd,del,dir,div,dl,dt,fieldset,figcaption,figure,footer,form,frameset,h1,h2,h3,h4,h5,h6,header,hgroup,hr,iframe,ins,isindex,li,map,menu,noframes,noscript,object,ol,output,p,pre,section,script,table,tbody,td,tfoot,th,thead,tr,ul,video'
)
// 内联元素
const inline = $newMap(
  'abbr,acronym,applet,b,basefont,bdo,big,button,cite,del,dfn,em,font,i,iframe,img,input,ins,kbd,label,map,object,q,s,samp,script,select,small,span,strike,strong,sub,sup,textarea,tt,u,var'
)
// 自封闭元素
const closeSelf = $newMap('colgroup,dd,dt,li,options,p,td,tfoot,th,thead,tr')
// 自填充属性
const fillAttrs = $newMap(
  'checked,compact,declare,defer,disabled,ismap,multiple,nohref,noresize,noshade,nowrap,readonly,selected'
)
// 特殊元素
const special = $newMap('script,style,block')

/**
 * HTML解析
 * @param html
 * @param handler
 * @constructor
 */
function HtmlParser(html, handler) {
  let index
  let chars
  let match
  const stack = []
  let last = html

  stack.last = function() {
    return this[this.length - 1]
  }

  while (html) {
    chars = true
    if (!stack.last() || !special.has(stack.last())) {
      // 确保没有在style或script中
      // 处理注释
      if (html.indexOf('<!--') === 0) {
        index = html.indexOf('-->')

        if (index >= 0) {
          if (handler.comment) {
            handler.comment(html.substring(4, index))
          }
          html = html.substring(index + 3)
          chars = false
        }
      } else if (html.indexOf('</') === 0) {
        // 处理结束tag
        match = html.match(endTag)
        if (match) {
          html = html.substring(match[0].length)
          match[0].replace(endTag, parseEndTag)
          chars = false
        }
      } else if (html.indexOf('<') === 0) {
        // 处理开始tag
        match = html.match(startTag)
        if (match) {
          html = html.substring(match[0].length)
          match[0].replace(startTag, parseStartTag)
          chars = false
        }
      }

      if (chars) {
        index = html.indexOf('<')
        let text = ''
        while (index === 0) {
          text += '<'
          html = html.substring(1)
          index = html.indexOf('<')
        }
        text += index < 0 ? html : html.substring(0, index)
        html = index < 0 ? '' : html.substring(index)

        if (handler.text) {
          handler.text(text)
        }
      }
    } else {
      html = html.replace(new RegExp('([\\s\\S]*?)(</' + stack.last() + '[^>]*>)', 'i'), function(
        all,
        text
      ) {
        text = text.replace(/<!--([\s\S]*?)-->/g, '$1').replace(/<!\[CDATA\[([\s\S]*?)]]>/g, '$1')

        if (handler.text) {
          handler.text(text)
        }

        return ''
      })

      parseEndTag('', stack.last())
    }

    if (html === last) {
      console.error('### App Parser ### 解析错误：' + html)
    }
    last = html
  }

  parseEndTag()

  function parseStartTag(tag, tagName, rest, unary) {
    tagName = tagName.toLowerCase()

    if (block.has(tagName)) {
      while (stack.last() && inline.has(stack.last())) {
        parseEndTag('', stack.last())
      }
    }

    if (closeSelf.has(tagName) && stack.last() === tagName) {
      parseEndTag('', tagName)
    }

    unary = empty.has(tagName) || !!unary

    if (!unary) {
      stack.push(tagName)
    }

    if (handler.start) {
      const attrs = []

      rest.replace(attr, function(match, name) {
        const value = arguments[2]
          ? arguments[2]
          : arguments[3]
          ? arguments[3]
          : arguments[4]
          ? arguments[4]
          : fillAttrs.has(name)
          ? name
          : ''

        attrs.push({
          name: name,
          value: value,
          escaped: value.replace(/(^|[^\\])"/g, '$1\\"')
        })
      })

      if (handler.start) {
        handler.start(tagName, attrs, unary)
      }
    }
  }

  function parseEndTag(tag, tagName) {
    let pos = 0
    if (tagName) {
      tagName = tagName.toLowerCase()
      for (pos = stack.length - 1; pos >= 0; pos--) {
        if (stack[pos] === tagName) {
          break
        }
      }
    }

    if (pos >= 0) {
      // 关闭open标签
      for (let i = stack.length - 1; i >= pos; i--) {
        if (handler.end) {
          handler.end(stack[i])
        }
      }

      // 移除close标签
      stack.length = pos
    }
  }
}

export default HtmlParser
