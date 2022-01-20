/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { getNodeAsJSON } from './dom/index'

function _createAction(name, args = []) {
  return { module: 'dom', method: name, args: args }
}

class Listener {
  constructor(id, streamer) {
    this.id = id
    // 处理消息所属类
    this.streamer = streamer
  }

  createFinish(callback) {
    console.trace('### App Runtime ### createFinish---- ')

    const result = this.streamer.over(this.id, [_createAction('createFinish')], callback)
    return result
  }

  updateFinish(callback) {
    console.trace('### App Runtime ### updateFinish---- ')

    const result = this.streamer.over(this.id, [_createAction('updateFinish')], callback)
    return result
  }

  createBody(node) {
    const body = getNodeAsJSON(node, true, false, true)
    const children = body.children
    delete body.children
    const actions = [_createAction('createBody', [body])]
    if (children) {
      actions.push.apply(
        actions,
        children.map(child => {
          return _createAction('addElement', [body.ref, child, -1])
        })
      )
    }
    return this.addActions(actions)
  }

  addElement(node, ref, index) {
    // 任何无效索引替换为-1
    if (!(index >= 0)) {
      index = -1
    }
    return this.addActions(
      _createAction('addElement', [ref, getNodeAsJSON(node, true, false, true), index])
    )
  }

  removeElement(ref) {
    if (Array.isArray(ref)) {
      const actions = ref.map(r => _createAction('removeElement', [r]))
      return this.addActions(actions)
    }
    return this.addActions(_createAction('removeElement', [ref]))
  }

  moveElement(target, ref, index) {
    return this.addActions(_createAction('moveElement', [target, ref, index]))
  }

  setProp(ref, key, value) {
    const result = { prop: {} }
    result.prop[key] = value
    return this.addActions(_createAction('updateProps', [ref, result]))
  }

  setAttr(ref, key, value) {
    const result = {
      attr: {}
    }
    if (value == null) {
      console.warn(
        `### App Runtime ### 组件 ${ref} 的属性 ${key} 被修改为 undefined/null, 自动修改为默认值或空值`
      )
      result.attr[key] = ''
    } else {
      result.attr[key] = value
    }
    return this.addActions(_createAction('updateAttrs', [ref, result]))
  }

  setStyle(ref, key, value) {
    const result = {
      style: {}
    }
    if (value == null) {
      console.warn(
        `### App Runtime ### 组件 ${ref} 的样式 ${key} 被修改为 undefined/null, 自动修改为默认值或空值`
      )
      result.style[key] = ''
    } else {
      result.style[key] = value
    }
    return this.addActions(_createAction('updateStyle', [ref, result]))
  }

  setStyles(ref, style, attr) {
    const result = {
      attr: attr
    }
    if (style) {
      result.style = style
    }
    return this.addActions(_createAction('updateStyles', [ref, result]))
  }

  setStyleObject(ref, isDocLevel, name, styleObjectId, styleObject) {
    return this.addActions(
      _createAction('updateStyleObject', [ref, isDocLevel, name, styleObjectId, styleObject])
    )
  }

  addEvent(ref, type) {
    return this.addActions(_createAction('addEvent', [ref, type]))
  }

  removeEvent(ref, type) {
    return this.addActions(_createAction('removeEvent', [ref, type]))
  }

  updatePageTitleBar(attr) {
    return this.addActions(_createAction('updateTitleBar', [attr]))
  }

  invokeComponentMethod(component, ref, method, args) {
    return this.addActions({ component, ref, method, args })
  }

  addActions(actions) {
    // 将输入参数转变为数组类型
    if (!Array.isArray(actions)) {
      actions = [actions]
    }

    console.trace('### App Runtime ### addActions---- ', JSON.stringify(actions))
    this.streamer.push(this.id, actions)
  }
}

export default Listener
