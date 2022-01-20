/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import {
  setDocument,
  removeDocument,
  getDocument,
  getListener,
  defineNodeInDocument,
  removeNodeInDocument,
  getDocumentNodeByRef,
  setParent,
  insertIndex,
  removeIndex,
  moveIndex,
  getNodeStyleObjectId
} from '../../runtime/dom/model'

// 保存APP与页面的公共样式缓存
const cssContentInfoHash = {}

/**
 * 注册APP或页面中的公共样式
 * @param appOrPageId
 * @param content
 * @param shouldCareStyleObjectId
 */
function registerFromCssFile(appOrPageId, content, shouldCareStyleObjectId) {
  const instId = !appOrPageId ? null : getStylingDocumentId(appOrPageId)
  cssContentInfoHash[instId] = {
    shouldCareStyleObjectId,
    content
  }
}

/**
 * 安装APP或页面中的公共样式到某文档
 * @param appOrPageId
 * @param content
 * @param shouldCareStyleObjectId
 */
function installCssFiles(node) {
  installFromCssFile(node, cssContentInfoHash.null, true)
  installFromCssFile(node, cssContentInfoHash[node._docId], false)
}

/**
 * 安装APP或页面中的公共样式
 * @param node
 * @param cssContentInfo
 */
function installFromCssFile(node, cssContentInfo, isDocLevel) {
  const { shouldCareStyleObjectId, content } = cssContentInfo || {}
  if (!content) {
    return
  }

  const css = JSON.parse(content)
  const cssObjectList = (css && css.list) || []
  for (let i = 0, len = cssObjectList.length; i < len; i++) {
    const styleObject = cssObjectList[i]
    let styleObjectId = null
    if (styleObject[`@info`] && styleObject[`@info`].styleObjectId) {
      styleObjectId = styleObject[`@info`].styleObjectId
    }

    if (shouldCareStyleObjectId) {
      global.registerStyleObject('', styleObjectId, cssObjectList[i], isDocLevel, node, false)
    } else {
      global.registerStyleObject('', null, cssObjectList[i], true, node, false)
    }
  }
}

/**
 * 获取对应的文档ID
 * @param instId
 * @return {string}
 */
function getStylingDocumentId(instId) {
  return `###${instId}`
}

/**
 * 获取原始页面的PageId
 * @param docId
 * @return {Number}
 */
function getOriginalDocumentId(docId) {
  return Number(docId.slice(3))
}

/**
 * 在节点上定义编译后的样式对象定义
 * @param document
 * @param styleObjectId
 * @param styleSheetInst
 * @param isDocLevel
 * @param node
 */
function defineNodeStyleSheet(document, styleObjectId, styleSheetInst, isDocLevel, node) {
  // 注册
  if (styleObjectId) {
    document._styleSheetHash[styleObjectId] = styleSheetInst
  }

  if (isDocLevel) {
    document._styleSheetHash[0].push(styleSheetInst)
  } else {
    node._styleObjectId = styleObjectId
  }
}

/**
 * 获取文档中定义的样式表
 * @param docId
 * @param styleObjectId
 * @return {null}
 */
function getDocumentStyleSheetById(docId, styleObjectId) {
  const document = getDocument(docId)
  if (document) {
    return document._styleSheetHash[styleObjectId]
  }
  return null
}

/**
 * 获取节点所依赖的样式表列表
 * @param node
 * @return {Array}
 */
function getNodeStyleSheetList(node) {
  const list = []
  let curr = node

  const docId = node._docId
  const doc = getDocument(docId)

  // 公共
  const shareList = doc._styleSheetHash[0]
  list.push(...shareList)

  if (node._useParentStyle) {
    curr = node.parentNode
  }

  // 私有
  while (curr && !curr._styleObjectId) {
    curr = curr.parentNode
  }
  if (curr && curr._styleObjectId) {
    const currInst = getDocumentStyleSheetById(docId, curr._styleObjectId)
    if (currInst && list.indexOf(currInst) === -1) {
      list.push(currInst)
    }
  }

  console.trace(
    `### App Runtime ### 获取节点(${node.ref}:${node._type})的样式表:${list
      .map(inst => inst.id)
      .join(',')}`
  )
  return list
}

export {
  setDocument,
  removeDocument,
  getDocument,
  getListener,
  defineNodeInDocument,
  removeNodeInDocument,
  getDocumentNodeByRef,
  setParent,
  insertIndex,
  removeIndex,
  moveIndex,
  getNodeStyleObjectId,
  registerFromCssFile,
  installCssFiles,
  getStylingDocumentId,
  getOriginalDocumentId,
  defineNodeStyleSheet,
  getDocumentStyleSheetById,
  getNodeStyleSheetList
}
