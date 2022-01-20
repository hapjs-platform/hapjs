/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Doc对象Map集合.一个Doc对应一个页面
 */
const _documentMap = {}

/**
 * 添加指定Id的Doc对象
 * @param {string} id
 * @param {object} doc
 */
function setDocument(id, doc) {
  if (id) {
    _documentMap[id] = doc
  }
}

/**
 * 移除指定Id的Doc对象
 * @param {string} id
 */
function removeDocument(id) {
  _documentMap[id] = null
}

/**
 * 获取指定id的Doc对象
 * @param id
 * @return {*}
 */
function getDocument(id) {
  return _documentMap[id]
}

/**
 * 获取指定Id的Doc对象的Listener.
 * @param {Document}
 * @return {object}
 */
function getListener(id) {
  const doc = getDocument(id)
  if (doc && doc.listener) {
    return doc.listener
  }
  return null
}

/**
 * 注册节点
 * @param node
 */
function defineNodeInDocument(node, docId) {
  if (node._docId === docId) {
    return
  }

  const doc = getDocument(docId)
  if (doc) {
    node._docId = docId
    doc._nodeMap[node.ref] = node
    // 子节点
    for (let i = 0, len = node.childNodes.length; i < len; i++) {
      defineNodeInDocument(node.childNodes[i], node._docId)
    }
  }
}

/**
 * 删除节点
 * @param node
 */
function removeNodeInDocument(node, docId) {
  if (node._docId !== docId) {
    return
  }

  const doc = getDocument(docId)
  if (doc) {
    // 子节点
    for (let i = 0, len = node.childNodes.length; i < len; i++) {
      removeNodeInDocument(node.childNodes[i], node._docId)
    }
    node._depth = null
    doc._nodeMap[node.ref] = null
    node._docId = null
  }
}

/**
 * 获取在Document中的节点
 * @param document
 * @param ref
 */
function getDocumentNodeByRef(document, ref) {
  return document._nodeMap[ref]
}

/**
 * 关联父子节点.
 * @param {object} child node
 * @param {object} parent node
 */
function setParent(node, parent) {
  node.parentNode = parent
  if (parent._docId) {
    defineNodeInDocument(node, parent._docId)
    node._depth = parent._depth + 1
  }
  for (let i = 0, len = node.childNodes.length; i < len; i++) {
    setParent(node.childNodes[i], node)
  }
}

/**
 * 插入节点到指定索引位置
 * @param {object} target
 * @param {array} list
 * @param {number} newIndex
 * @param {boolean} changeSibling
 */
function insertIndex(target, list, newIndex, changeSibling) {
  if (newIndex < 0) {
    newIndex = 0
  }
  const before = list[newIndex - 1]
  const after = list[newIndex]
  list.splice(newIndex, 0, target)

  // 如果需要调整兄弟链表索引
  if (changeSibling) {
    before && (before.nextSibling = target || null)
    target.previousSibling = before || null
    target.nextSibling = after || null
    after && (after.previousSibling = target || null)
  }
  return newIndex
}

/**
 * 从列表中移除指定节点
 * @param {object} target node
 * @param {array} list
 * @param {boolean} changeSibling
 */
function removeIndex(target, list, changeSibling) {
  const index = list.indexOf(target)
  if (index < 0) {
    return
  }
  if (changeSibling) {
    const before = list[index - 1]
    const after = list[index + 1]
    before && (before.nextSibling = after || null)
    after && (after.previousSibling = before || null)
    target.previousSibling = null
    target.nextSibling = null
  }
  list.splice(index, 1)
}

/**
 * 改变节点在列表中的索引位置
 * @param {object} target node
 * @param {array} list
 * @param {number} newIndex
 * @param {boolean} changeSibling
 */
function moveIndex(target, list, newIndex, changeSibling) {
  const index = list.indexOf(target)
  if (index < 0 || index === newIndex) {
    return -1
  }

  // 将元素从队列中取出，然后重新插入
  removeIndex(target, list, changeSibling)

  let newIndexAfter = newIndex
  if (index <= newIndex) {
    newIndexAfter = newIndex - 1
  }

  insertIndex(target, list, newIndexAfter, changeSibling)

  return newIndex
}

function getNodeStyleObjectId(node) {
  if (node._styleObjectId) {
    return node._styleObjectId
  }

  let curr = node
  // 私有
  while (curr && !curr._styleObjectId) {
    curr = curr.parentNode
  }
  if (curr) {
    node._styleObjectId = curr._styleObjectId
  }

  return node._styleObjectId
}

let styleObjectIdUnique = 1

/**
 * 缓存styleObject，并返回唯一的ID标识
 * @param docId
 * @param styleObject
 * @return {*}
 */
function genIdByStyleObject(docId, styleObject) {
  let styleObjectId

  const doc = getDocument(docId)
  if (doc && doc._styleObjectMap) {
    styleObjectId = doc._styleObjectMap.get(styleObject)
    if (!styleObjectId) {
      _removeRuleDefInStyleObject(styleObject)
      styleObjectId = styleObjectIdUnique++
      doc._styleObjectMap.set(styleObject, styleObjectId)
    }
  } else {
    styleObjectId = styleObjectIdUnique++
    console.trace(
      `### App Runtime ### 获取样式对象ID：文档(${docId})已销毁，生成无缓存时的ID：${styleObjectId}`
    )
  }
  return styleObjectId
}

/**
 * 删除后代选择中的样式解析meta
 * @param styleObject
 */
function _removeRuleDefInStyleObject(styleObject) {
  for (const k in styleObject) {
    const styleItem = styleObject[k]
    if (styleItem && styleItem._meta) {
      delete styleItem._meta
    }
  }
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
  genIdByStyleObject
}
