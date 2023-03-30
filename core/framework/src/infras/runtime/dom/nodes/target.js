/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Node from './node'
import Event from './event/event'
import { getListener } from '../model'
import { renderParents } from '../misc'
import { xInvokeWithErrorHandling } from 'src/shared/error'

/**
 * 仅触发最深层次的事件发生绑定者
 * @param node
 * @param evt {Event}
 * @param options
 */
export function fireTargetEventListener(node, evt, options) {
  // 转换为对象
  if (!(options instanceof Object)) {
    options = {
      capture: !!options
    }
  }
  evt._currentTarget = node
  evt._target = node
  const evtName = evt.type
  const eventConf = node._eventTargetListeners[evtName]
  const finalEventPhase = options.capture ? Event.CAPTURING_PHASE : Event.BUBBLING_PHASE

  if (eventConf && eventConf[finalEventPhase] && eventConf[finalEventPhase].list) {
    const listCache = eventConf[finalEventPhase].list

    for (let i = 0, len = listCache.length; i < len; i++) {
      const fnListener = listCache[i]

      console.trace(
        `### App Runtime ### fireTargetEventListener() 事件响应:${evt.type}，节点:${node.ref}`
      )
      if (fnListener) {
        const info = `component: event handler for "${evtName}"`
        const functionResult = xInvokeWithErrorHandling(
          fnListener,
          evt.target,
          [evt],
          node._xvm,
          info
        )

        if (evtName === 'key') {
          // onkey组件回调返回true，即组件自己处理
          if (functionResult === true) {
            global.callKeyEvent(functionResult, evt.hashcode)
          } else {
            // 若onkey组件回调不返回true，则手动调用页面onKey生命周期
            const { action, code, repeatCount } = evt
            const onKeyLifeCycleResult = global.keyPressPage(node._docId, {
              action,
              code,
              repeatCount
            })
            global.callKeyEvent(onKeyLifeCycleResult, evt.hashcode)
          }
        }
        evt._listenNodes[evt.target.ref] = true
      }
    }
  }
}

/**
 * 清除节点事件的所有监听
 * @param node
 * @param evtName
 * @return {*|{}}
 */
export function clearTargetEventListener(node, evtName, options) {
  // 转换为对象
  if (!(options instanceof Object)) {
    options = {
      capture: !!options
    }
  }

  const phase = options.capture ? Event.CAPTURING_PHASE : Event.BUBBLING_PHASE
  if (node._eventTargetListeners[evtName]) {
    delete node._eventTargetListeners[evtName][phase]
  }
}

/**
 * W3C的EventTarget
 */
class EventTarget extends Node {
  constructor() {
    super(...arguments)
    this._eventTargetListeners = {}
  }

  /**
   * @param evtName {string}
   * @param fnListener {function}
   * @param options {boolean|Object} capture默认为false，否则为对象
   * @param options.capture 是否是捕获阶段
   * @param options.once 调用执行过后删除
   * @param options.passive 暂未实现
   */
  addEventListener(evtName, fnListener, options) {
    if (arguments.length < 2) {
      throw new Error(
        `### App Runtime ### addEventListener() 至少需要传递两个参数:${arguments.length}`
      )
    }
    if (typeof evtName !== 'string') {
      throw new Error(`### App Runtime ### addEventListener() 参数1必须是字符串，事件类型`)
    }
    if (typeof fnListener !== 'function') {
      throw new Error(`### App Runtime ### addEventListener() 参数2必须是函数，监听事件`)
    }
    console.trace(`### App Runtime ### addEventListener() 节点(${this.ref})注册事件(${evtName})`)

    // 转换为对象
    if (!(options instanceof Object)) {
      options = {
        capture: !!options,
        once: false,
        passive: false
      }
    }

    // 根据PHASE分别保存为对象，持有:hash,list属性
    const phase = options.capture ? Event.CAPTURING_PHASE : Event.BUBBLING_PHASE
    const eventConf = (this._eventTargetListeners[evtName] =
      this._eventTargetListeners[evtName] || {})
    eventConf[phase] = eventConf[phase] || {}
    eventConf[phase].list = eventConf[phase].list || []
    eventConf[phase].hash = eventConf[phase].hash || {}

    const index = eventConf[phase].list.indexOf(fnListener)
    if (index === -1) {
      // 不存在则添加
      const len = eventConf[phase].list.push(fnListener)
      eventConf[phase].hash[len - 1] = {
        capture: !!options.capture,
        once: !!options.once,
        passive: !!options.passive
      }
    }

    // 发送消息，Native才会通知
    const listener = getListener(this._docId)
    if (listener) {
      listener.addEvent(this.ref, evtName)
    }
  }

  /**
   * @param evtName {string}
   * @param fnListener {function}
   * @param options {boolean|Object} capture默认为false，否则为对象
   */
  removeEventListener(evtName, fnListener, options) {
    if (arguments.length < 2) {
      throw new Error(
        `### App Runtime ### addEventListener() 至少需要传递两个参数:${arguments.length}`
      )
    }
    if (typeof evtName !== 'string') {
      throw new Error(`### App Runtime ### addEventListener() 参数1必须是字符串，事件类型`)
    }
    if (typeof fnListener !== 'function') {
      throw new Error(`### App Runtime ### addEventListener() 参数2必须是函数，监听事件`)
    }
    console.trace(
      `### App Runtime ### Element ${this.ref} 执行 removeEventListener(${evtName})---- `
    )

    // 转换为对象
    if (!(options instanceof Object)) {
      options = {
        capture: !!options
      }
    }

    // 根据PHASE分别保存为对象，持有:hash,list属性
    const phase = options.capture ? Event.CAPTURING_PHASE : Event.BUBBLING_PHASE
    const eventConf = (this._eventTargetListeners[evtName] =
      this._eventTargetListeners[evtName] || {})
    eventConf[phase] = eventConf[phase] || {}
    eventConf[phase].list = eventConf[phase].list || []
    eventConf[phase].hash = eventConf[phase].hash || {}

    const index = eventConf[phase].list.indexOf(fnListener)
    if (index !== -1) {
      eventConf[phase].list.splice(index, 1)
      eventConf[phase].hash[index] = null
    }
    // 清除
    if (eventConf[phase].list.length === 0) {
      eventConf[phase] = null
    }
  }

  dispatchEvent(evt) {
    if (!(evt instanceof Event)) {
      throw new Error(`### App Runtime ### dispatchEvent() 参数1所属类必须是事件类`)
    }
    console.trace(`### App Runtime ### dispatchEvent() 执行事件:${evt.type}, 来自节点:${this.ref}`)

    if (!evt._supportW3C) {
      fireTargetEventListener(this, evt)
    } else {
      evt._target = this

      const evtName = evt.type
      const parentList = renderParents(this, true)
      const nodeList = parentList
        .slice()
        .reverse()
        .concat(parentList)

      // 尝试添加document
      if (nodeList[0] && nodeList[0].parentNode === nodeList[0].ownerDocument) {
        const document = nodeList[0].ownerDocument
        nodeList.unshift(document)
        nodeList.push(document)
      }

      while (nodeList.length > 0) {
        const node = nodeList[0]
        // 节点所在索引
        const nodeIndex = nodeList.indexOf(node)
        const thisIndex = nodeList.indexOf(this)

        if (nodeIndex < thisIndex) {
          // 捕获
          evt._eventPhase = Event.CAPTURING_PHASE
        } else if (nodeIndex === thisIndex) {
          // 当前
          evt._eventPhase = Event.AT_TARGET
        } else {
          // 冒泡
          evt._eventPhase = Event.BUBBLING_PHASE
        }

        evt._currentTarget = node

        if (!evt.bubbles && evt.eventPhase === Event.BUBBLING_PHASE) {
          // 如果禁用冒泡，则放弃冒泡阶段
          break
        }

        console.trace(
          `### App Runtime ### dispatchEvent() 执行事件:${evtName}, 阶段:${evt.eventPhase}, Target:${evt.target.ref}, CurrentTarget:${evt.currentTarget.ref}`
        )

        const eventConf = node._eventTargetListeners[evtName]

        let finalEventPhase = evt.eventPhase
        if (evt.target === evt.currentTarget) {
          // 如果发生在target元素，则使用对应的缓存
          finalEventPhase = node === nodeList[1] ? Event.CAPTURING_PHASE : Event.BUBBLING_PHASE
        }
        if (eventConf && eventConf[finalEventPhase] && eventConf[finalEventPhase].list) {
          const hashCache = eventConf[finalEventPhase].hash
          const listCache = eventConf[finalEventPhase].list.slice()
          for (let i = 0, len = listCache.length; i < len; i++) {
            if (evt._flagStopImmediatePropagation) {
              break
            }
            const fnListener = listCache[i]

            try {
              console.trace(
                `### App Runtime ### dispatchEvent() 事件响应:${evt.type}，阶段:${evt.eventPhase}`
              )
              // 执行fnListener回调
              if (fnListener) {
                const info = `component: event handler for "${evtName}"`
                xInvokeWithErrorHandling(fnListener, evt.currentTarget, [evt], node._xvm, info)
                evt._listenNodes[evt.currentTarget.ref] = true
              }
            } catch (err) {
              console.error(
                `### App Runtime ### dispatchEvent() 事件响应:${evt.type}，阶段:${evt.eventPhase}, JS报错:`,
                err.message,
                fnListener
              )
              console.error(err.stack)
              // 向上报错
              evt._throwError &&
                global.setTimeout(() => {
                  throw err
                }, 0)
            }

            // 判断是否是once:仅调用一次并移除
            if (hashCache[i] && hashCache[i].once) {
              node.removeEventListener(evt.type, fnListener, hashCache[i])
            }
          }
        }

        if (evt._flagStopImmediatePropagation || evt._flagStopPropagation) {
          break
        }

        // 删除
        nodeList.shift()
      }

      // 清理事件本身
      evt._currentTarget = null
    }

    // 卡片模式：通知底层所处理的节点
    if (evt.type === 'click' && global.Env && global.Env.engine === global.ENGINE_TYPE.CARD) {
      const listener = getListener(this._docId)
      if (listener) {
        listener.collectStatistics({
          type: evt.type,
          ref: this.ref,
          listeners: Object.keys(evt._listenNodes)
        })
      }
    }
    evt._listenNodes = {}
  }
}

export default EventTarget
