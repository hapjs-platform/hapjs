/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { getNodeAsJSON, supportRender, calcRenderCount } from './dom/index'

/**
 * 创建一个Action对象
 * @param name
 * @param args
 * @returns {{module: string, method: *, args: Array}}
 */
function _createAction(name, args = []) {
  return { module: 'dom', method: name, args: args }
}

/**
 * 侦听VDom操作，创建相应的Action（发送给Render更新UI）
 */
class Listener {
  constructor(id, streamer) {
    this.id = id
    // 处理消息所属类
    this.streamer = streamer
    // 本轮创建的Action数量
    this.actionLen = 0
  }

  /**
   * 发送"createFinish"信号
   * @param callback
   * @returns {*}
   */
  createFinish(callback, hasCallbacks) {
    console.trace('### App Runtime ### createFinish---- ')

    const args = []
    args.push({ jsCallbacks: hasCallbacks })
    const result = this.streamer.over(this.id, [_createAction('createFinish', args)], callback)
    this.resetActionLen()
    return result
  }

  /**
   * 发送"updateFinish"信号.
   * @param callback
   * @returns {*}
   */
  updateFinish(callback, hasCallbacks) {
    console.trace('### App Runtime ### updateFinish---- ')

    const args = []
    args.push({ jsCallbacks: hasCallbacks })
    const result = this.streamer.over(this.id, [_createAction('updateFinish', args)], callback)
    this.resetActionLen()
    return result
  }

  /**
   * 发送"createBody"信号
   * @param node
   * @returns {undefined|number}
   */
  createBody(node) {
    const body = getNodeAsJSON(node, true, true, false, true)
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

  /**
   * 发送"addElement"信号
   * @param node  新添加节点 可能是element或者figment
   * @param parent  渲染父节点
   * @param index  索引
   * @returns {undefined|number}
   */
  addNode(node, parent, index) {
    if (!parent || !supportRender(parent)) {
      console.error(`### App Runtime ### addNode的parent(${parent.type})必须是可渲染节点`)
    }

    // 任何无效索引替换为-1
    if (index < 0) {
      index = -1
    }

    let result
    if (supportRender(node)) {
      result = this.addElement(node, parent.ref, index)
    } else {
      let startIndex = index
      node.layoutChildren.every(child => {
        if (supportRender(child)) {
          result = this.addElement(child, parent.ref, startIndex)
        } else {
          result = this.addNode(child, parent, startIndex)
        }
        startIndex += calcRenderCount(child)
        return result !== -1
      })
    }
    return result
  }

  /**
   *
   * @param node
   * @param ref
   * @param index
   * @returns {undefined|number}
   */
  addElement(node, ref, index) {
    // 任何无效索引替换为-1
    if (!(index >= 0)) {
      index = -1
    }
    return this.addActions(
      _createAction('addElement', [ref, getNodeAsJSON(node, true, true, false, true), index])
    )
  }

  /**
   * 发送"removeElement"信号
   * @param node 待删除的节点,可能是element或者fragment
   */
  removeNode(node) {
    let result
    if (supportRender(node)) {
      result = this.removeElement(node.ref)
    } else {
      node.layoutChildren.every(child => {
        if (supportRender(child)) {
          result = this.removeElement(child.ref)
        } else {
          result = this.removeNode(child)
        }
        return result !== -1
      })
    }
    return result
  }

  /**
   * 发送"removeElement"信号
   * @param ref  待删除的节点引用列表
   * @returns {undefined|number}
   */
  removeElement(ref) {
    if (Array.isArray(ref)) {
      const actions = ref.map(r => _createAction('removeElement', [r]))
      return this.addActions(actions)
    }
    return this.addActions(_createAction('removeElement', [ref]))
  }

  /**
   * 发送"moveElement"动作
   * @param node 移动的节点
   * @param parent 渲染父节点
   * @param index
   */
  moveNode(node, parent, index) {
    if (!parent || !supportRender(parent)) {
      console.error(`### App Runtime ### moveNode的parent必须是可渲染节点`)
    }

    // 任何无效索引替换为-1
    if (!(index >= 0)) {
      index = -1
    }
    let result
    if (supportRender(node)) {
      result = this.moveElement(node.ref, parent.ref, index)
    } else {
      let startIndex = index
      node.layoutChildren.every(child => {
        if (supportRender(child)) {
          result = this.moveElement(child.ref, parent.ref, startIndex)
        } else {
          result = this.moveNode(child, parent, startIndex)
        }
        startIndex += calcRenderCount(child)
        return result !== -1
      })
    }
    return result
  }

  /**
   * 发送"moveElement"动作
   * @param target  移动的节点ref
   * @param ref  父节点ref
   * @param index 新索引
   * @returns {undefined|number}
   */
  moveElement(target, ref, index) {
    return this.addActions(_createAction('moveElement', [target, ref, index]))
  }

  /**
   * 更新element属性，发送"updateProps"信号.
   * @param ref
   * @param key
   * @param value
   * @returns {undefined|number}
   */
  setProp(ref, key, value) {
    const result = { prop: {} }
    result.prop[key] = value
    return this.addActions(_createAction('updateProps', [ref, result]))
  }

  /**
   * 更新element属性，发送"updateAttrs"信号.
   * @param ref
   * @param key
   * @param value
   * @returns {undefined|number}
   */
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

  /**
   * 更新elemnt样式，发送"updateStyle"信号.
   * @param ref
   * @param key
   * @param value
   * @returns {undefined|number}
   */
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

  /**
   * 发送"updateStyle"信号.
   * @param ref
   * @param style
   * @param attr class或者id
   * @returns {undefined|number}
   */
  setStyles(ref, style, attr) {
    const result = {
      attr: attr
    }
    if (style) {
      result.style = style
    }
    return this.addActions(_createAction('updateStyles', [ref, result]))
  }

  /**
   * 设置某个节点或者全局级别的样式对象
   * @param ref
   * @param isDocLevel
   * @param name
   * @param styleObjectId
   * @param styleObject
   * @return {undefined|number}
   */
  setStyleObject(ref, isDocLevel, name, styleObjectId, styleObject) {
    return this.addActions(
      _createAction('updateStyleObject', [ref, isDocLevel, name, styleObjectId, styleObject])
    )
  }

  /**
   * 发送"addEvent"事件
   * @param ref
   * @param type
   * @returns {undefined|number}
   */
  addEvent(ref, type) {
    return this.addActions(_createAction('addEvent', [ref, type]))
  }

  /**
   * 发送"removeEvent"事件
   * @param {stirng} reference id
   * @param {string} event type
   * @return {undefined | number}
   */
  removeEvent(ref, type) {
    return this.addActions(_createAction('removeEvent', [ref, type]))
  }

  /**
   * 更新页面标题栏
   * @param attr
   * @returns {undefined|number}
   */
  updatePageTitleBar(attr) {
    return this.addActions(_createAction('updateTitleBar', [attr]))
  }

  /**
   * 更新页面状态栏
   * @param attr
   * @returns {undefined|number}
   */
  updatePageStatusBar(attr) {
    return this.addActions(_createAction('updateStatusBar', [attr]))
  }

  /**
   * 设置页面元信息
   * @param attr
   * @returns {undefined|number}
   */
  setMeta(attr) {
    if (ENV_PLATFORM === 'h5') {
      return this.addActions(_createAction('setMeta', [attr]))
    }
  }

  /**
   * 页面滚动至指定位置
   * @param attr
   * @returns {undefined|number}
   */
  scrollTo(attr) {
    return this.addActions(_createAction('scrollTo', [attr]))
  }

  /**
   * 页面按指定的偏移量滚动文档
   * @param attr
   * @returns {undefined|number}
   */
  scrollBy(attr) {
    return this.addActions(_createAction('scrollBy', [attr]))
  }

  /**
   * 页面退出全屏模式
   * @param attr
   * @returns {undefined|number}
   */
  exitFullscreen() {
    return this.addActions(_createAction('exitFullscreen', []))
  }

  /**
   * 隐藏骨架屏
   * @param
   * @return {undefined|number}
   */
  hideSkeleton(pageId) {
    return this.addActions(_createAction('hideSkeleton', [pageId]))
  }

  /**
   * 发送统计数据
   * @param hash
   * @return {undefined|number}
   */
  collectStatistics(hash) {
    return this.addActions(_createAction('statistics', [hash]))
  }

  /**
   * 调用页面的方法，如：生命周期等
   * @param name {string} 方法名称
   * @param args {Array} 方法参数
   * @param extra 非方法参数外的信息
   * @returns {undefined|number}
   */
  callHostFunction(name, args, extra) {
    return this.addActions(_createAction(name, [args, extra]))
  }

  /**
   * 调用组件方法
   * @param component {String} 组件类型
   * @param ref
   * @param method {String} 方法名称
   * @param args
   * @return {undefined|number}
   */
  invokeComponentMethod(component, ref, method, args) {
    return this.addActions({ component, ref, method, args })
  }

  /**
   * 向更新队列中添加Action
   * @param {object | array}
   * @return {undefined | number}
   */
  addActions(actions) {
    // 将输入参数转变为数组类型
    if (!Array.isArray(actions)) {
      actions = [actions]
    }

    console.trace('### App Runtime ### addActions---- ', JSON.stringify(actions))
    this.actionLen += actions.length
    this.streamer.push(this.id, actions)
  }

  /**
   * 当前页面是否有需要更新的action
   * @return {boolean}
   */
  hasActions() {
    return !!this.actionLen
  }

  /**
   * 重置本次轮询
   * @private
   */
  resetActionLen() {
    this.actionLen = 0
  }
}

export default Listener
