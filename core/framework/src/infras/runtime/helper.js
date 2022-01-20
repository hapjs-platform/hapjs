/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * @file 封装DOM API，为上层Frameworks提供接口实现；
 * @desc 它只能引入DOM，但不能被DOM引入
 */

import { $typeof } from 'src/shared/util'

import {
  DomDocument,
  getListener,
  genIdByStyleObject,
  registerComponents,
  getComponentDefaultOptions,
  bindComponentMethods,
  createEvent,
  createFigment,
  updatePageTitleBar,
  updatePageStatusBar,
  setMeta,
  scrollTo,
  scrollBy,
  exitFullscreen,
  hideSkeleton,
  callHostFunction,
  setElementProp,
  setElementAttr,
  setElementStyle,
  setElementStyles,
  getElementMatchedCssRuleList,
  setElementMatchedCssRule,
  resetNodeChildren,
  restoreNodeChildren,
  fireTargetEventListener,
  clearTargetEventListener,
  getDocumentNodeByRef,
  destroyTagNode,
  getNodeAsJSON,
  getNodeDepth
} from './dom/index'

import Listener from './listener'
import Streamer from './streamer'

let isFirst = true

// callNative为原生函数，异步调用，无返回结果
const sendActions = function(instId, actionList, ...args) {
  console.trace(
    `### App Bridge ### sendActions() 发送消息：${instId},${actionList.length},${JSON.stringify(
      actionList
    )}`
  )

  if (isFirst) {
    global.profiler &&
      global.profiler.record(
        `### App Performance ### 首次发送消息[PERF:callNative]开始(${
          actionList.length
        })：${new Date().toJSON()}`
      )
    isFirst = false
  }

  // 转换为字符串
  actionList = JSON.stringify(actionList, function(k, v) {
    if (v && v.constructor === RegExp) {
      return { type: $typeof(v), source: v.source, flags: v.flags }
    }
    return v
  })

  return global.callNative(instId, actionList, ...args)
}

/**
 * 创建Document
 * @param docId
 */
function createDocument(docId) {
  const streamer = new Streamer(sendActions)
  const listener = new Listener(docId, streamer)
  const document = new DomDocument(docId, listener)
  return document
}

/**
 * 编译样式并定义在Node或者Document节点中
 * @param styleSheetName
 * @param styleObject
 * @param isDocLevel
 * @param node
 */
function registerStyleObject(styleSheetName, styleObject, isDocLevel, node) {
  console.trace(`### App Runtime ### 基于节点(${node && node.ref})注册样式节点(${isDocLevel})`)
  if (!node._ownerDocument) {
    return
  }

  let styleObjectId
  const styleObjectInfo = (styleObject && styleObject[`@info`]) || {}
  if (styleObjectInfo && styleObjectInfo.styleObjectId) {
    // 样式独立：会拥有@info信息
    styleObjectId = styleObjectInfo.styleObjectId
    styleObject = null
  } else {
    // 样式在JS：无@info信息
    styleObjectId = genIdByStyleObject(node._ownerDocument._docId, styleObject)
  }

  if (!isDocLevel) {
    node._styleObject = styleObject
    node._styleObjectId = styleObjectId
  }

  const listener = getListener(node._docId)
  if (listener) {
    listener.setStyleObject(node.ref, isDocLevel, styleSheetName, styleObjectId, styleObject)
  }
}

/**
 * 判断节点是否在文档中
 * @param node
 * @return {boolean}
 */
function isNodeInDocument(node) {
  if (!node || !node._docId) {
    return false
  }
  return true
}

/**
 * 重新生成Action
 * @param document
 */
function recreateDocument(document) {
  const body = document.body
  if (!body) {
    console.error(`### App Runtime ### Document没有body节点, 无法重建`)
    return
  }
  document.listener.createBody(body)
}

export {
  registerComponents,
  getComponentDefaultOptions,
  bindComponentMethods,
  createDocument,
  registerStyleObject,
  createEvent,
  createFigment,
  updatePageTitleBar,
  updatePageStatusBar,
  setMeta,
  scrollTo,
  scrollBy,
  exitFullscreen,
  hideSkeleton,
  callHostFunction,
  setElementProp,
  setElementAttr,
  setElementStyle,
  setElementStyles,
  getElementMatchedCssRuleList,
  setElementMatchedCssRule,
  resetNodeChildren,
  restoreNodeChildren,
  fireTargetEventListener,
  clearTargetEventListener,
  getDocumentNodeByRef,
  isNodeInDocument,
  recreateDocument,
  destroyTagNode,
  getNodeAsJSON,
  getNodeDepth,
  sendActions
}
