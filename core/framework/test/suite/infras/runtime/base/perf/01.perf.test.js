/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../../imports'

import data from './01.perf.data'

// 暴露出来
let document, nodeHtml, nodeBody
// 是否断言
let needs

describe('基础：01.性能测试', () => {
  beforeEach(() => {
    callNativeMessageList.splice(0)

    document = config.helper.createDocument(1)
    nodeHtml = document.documentElement
    nodeBody = document.createElement('div')
    nodeHtml.appendChild(nodeBody)
  })

  afterEach(() => {
    callNativeMessageList.splice(0)

    config.helper.destroyTagNode(document)
    document = null
    nodeHtml = null
    nodeBody = null
  })

  describe('有断言测试', () => {
    before(() => {
      needs = true
    })
    after(() => {
      needs = false
    })

    // 确定功能基本正确
    it('appendChild：Element作为父节点', perfTestElementAsParentAppendChild)
    it('appendChild：Figment作为父节点', perfTestFigmentAsParentAppendChild)

    it('insertBefore：Element作为父节点', perfTestElementAsParentInsertBefore)
    it('insertBefore：Figment作为父节点', perfTestFigmentAsParentInsertBefore)
  })

  describe('无断言测试', () => {
    before(() => {
      needs = false
    })
    after(() => {
      needs = true
    })

    it('registerStyleObject：注册样式对象', perfTestRegisterStyleObject)
    it('registerStyleObject：注册样式对象', perfTestRegisterStyleObject)
    it('registerStyleObject：注册样式对象', perfTestRegisterStyleObject)

    it('appendChild：Element作为父节点', perfTestElementAsParentAppendChild)
    it('appendChild：Element作为父节点', perfTestElementAsParentAppendChild)
    it('appendChild：Element作为父节点', perfTestElementAsParentAppendChild)

    it('appendChild：Figment作为父节点', perfTestFigmentAsParentAppendChild)
    it('appendChild：Figment作为父节点', perfTestFigmentAsParentAppendChild)
    it('appendChild：Figment作为父节点', perfTestFigmentAsParentAppendChild)

    it('insertBefore：Element作为父节点', perfTestElementAsParentInsertBefore)
    it('insertBefore：Element作为父节点', perfTestElementAsParentInsertBefore)
    it('insertBefore：Element作为父节点', perfTestElementAsParentInsertBefore)

    it('insertBefore：Figment作为父节点', perfTestFigmentAsParentInsertBefore)
    it('insertBefore：Figment作为父节点', perfTestFigmentAsParentInsertBefore)
    it('insertBefore：Figment作为父节点', perfTestFigmentAsParentInsertBefore)
  })
})

/**
 * 注册样式对象
 */
function perfTestRegisterStyleObject() {
  // 清理
  callNativeMessageList.splice(0)

  config.helper.setElementAttr(nodeBody, 'class', 'doc-page')

  // 新增元素
  const nodeParent1 = document.createElement('div')
  config.helper.setElementAttr(nodeParent1, 'class', 'doc-block')
  nodeBody.appendChild(nodeParent1)

  callNativeMessageList.splice(0)

  // 初测：1e4个节点
  for (let i = 0; i < 1e4; i++) {
    const nodeText1 = document.createElement('text')
    config.helper.registerStyleObject(null, data.styleNodeCase1, false, nodeText1)
    nodeBody.appendChild(nodeText1)
  }
}

/**
 * appendChild：Element作为父节点
 */
function perfTestElementAsParentAppendChild() {
  // 清理
  callNativeMessageList.splice(0)

  config.helper.setElementAttr(nodeBody, 'class', 'doc-page')

  // 定义样式节点
  config.helper.registerStyleObject(null, data.styleNodeCase1, false, nodeBody)

  // 新增元素
  const nodeParent1 = document.createElement('div')
  config.helper.setElementAttr(nodeParent1, 'class', 'doc-block')
  nodeBody.appendChild(nodeParent1)

  callNativeMessageList.splice(0)

  // 初测：1e4个节点
  for (let i = 0; i < 1e4; i++) {
    const nodeText1 = document.createElement('text')
    config.helper.setElementAttr(nodeText1, 'id', 'idTest1')
    config.helper.setElementAttr(nodeText1, 'class', 'class-test1')
    nodeParent1.appendChild(nodeText1)

    needs && expect(nodeParent1.layoutChildren[i]).to.equal(nodeText1)
    needs && expect(callNativeMessageList[0]).to.include('"idTest1"')
    needs && expect(callNativeMessageList[0]).to.include('"class-test1"')
    callNativeMessageList.splice(0)
  }
}

/**
 * appendChild：Figment作为父节点
 */
function perfTestFigmentAsParentAppendChild() {
  // 清理
  callNativeMessageList.splice(0)

  config.helper.setElementAttr(nodeBody, 'class', 'doc-page')

  // 定义样式节点
  config.helper.registerStyleObject(null, data.styleNodeCase1, false, nodeBody)

  // 新增元素
  const nodeParent1 = document.createElement('div')
  config.helper.setElementAttr(nodeParent1, 'class', 'doc-block')
  nodeBody.appendChild(nodeParent1)

  const nodeFigment1 = config.helper.createFigment(document)
  nodeParent1.appendChild(nodeFigment1)

  callNativeMessageList.splice(0)

  // 初测：1e4个节点
  for (let i = 0; i < 1e4; i++) {
    const nodeText1 = document.createElement('text')
    config.helper.setElementAttr(nodeText1, 'id', 'idTest1')
    config.helper.setElementAttr(nodeText1, 'class', 'class-test1')
    nodeFigment1.appendChild(nodeText1)

    needs && expect(nodeFigment1.layoutChildren[i]).to.equal(nodeText1)
    needs && expect(callNativeMessageList[0]).to.include('"idTest1"')
    needs && expect(callNativeMessageList[0]).to.include('"class-test1"')
    callNativeMessageList.splice(0)
  }
}

/**
 * insertBefore：Element作为父节点
 */
function perfTestElementAsParentInsertBefore() {
  // 清理
  callNativeMessageList.splice(0)

  config.helper.setElementAttr(nodeBody, 'class', 'doc-page')

  // 定义样式节点
  config.helper.registerStyleObject(null, data.styleNodeCase1, false, nodeBody)

  // 新增元素
  const nodeParent1 = document.createElement('div')
  config.helper.setElementAttr(nodeParent1, 'class', 'doc-block')
  nodeBody.appendChild(nodeParent1)

  // 末尾节点
  const nodeTextLast = document.createElement('text')
  nodeParent1.appendChild(nodeTextLast)

  callNativeMessageList.splice(0)

  // 初测：1e4个节点
  for (let i = 0; i < 1e4; i++) {
    const nodeText1 = document.createElement('text')
    config.helper.setElementAttr(nodeText1, 'id', 'idTest1')
    config.helper.setElementAttr(nodeText1, 'class', 'class-test1')
    nodeParent1.insertBefore(nodeText1, nodeTextLast)

    needs && expect(nodeParent1.layoutChildren[i]).to.equal(nodeText1)
    needs && expect(callNativeMessageList[0]).to.include('"idTest1"')
    needs && expect(callNativeMessageList[0]).to.include('"class-test1"')
    callNativeMessageList.splice(0)
  }
}

/**
 * insertBefore：Figment作为父节点
 */
function perfTestFigmentAsParentInsertBefore() {
  // 清理
  callNativeMessageList.splice(0)

  config.helper.setElementAttr(nodeBody, 'class', 'doc-page')

  // 定义样式节点
  config.helper.registerStyleObject(null, data.styleNodeCase1, false, nodeBody)

  // 新增元素
  const nodeParent1 = document.createElement('div')
  config.helper.setElementAttr(nodeParent1, 'class', 'doc-block')
  nodeBody.appendChild(nodeParent1)

  const nodeFigment1 = config.helper.createFigment(document)
  nodeParent1.appendChild(nodeFigment1)

  // 末尾节点
  const nodeTextLast = document.createElement('text')
  nodeFigment1.appendChild(nodeTextLast)

  callNativeMessageList.splice(0)

  // 初测：1e4个节点
  for (let i = 0; i < 1e4; i++) {
    const nodeText1 = document.createElement('text')
    config.helper.setElementAttr(nodeText1, 'id', 'idTest1')
    config.helper.setElementAttr(nodeText1, 'class', 'class-test1')
    nodeFigment1.insertBefore(nodeText1, nodeTextLast)

    needs && expect(nodeFigment1.layoutChildren[i]).to.equal(nodeText1)
    needs && expect(callNativeMessageList[0]).to.include('"idTest1"')
    needs && expect(callNativeMessageList[0]).to.include('"class-test1"')
    callNativeMessageList.splice(0)
  }
}
