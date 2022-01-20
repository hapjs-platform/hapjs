/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../imports'

/**
 * 节点操作
 */
describe('基础4:节点操作', () => {
  let document, nodeHtml, nodeBody

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

  it('文本节点', () => {
    // 清理
    callNativeMessageList.splice(0)

    const el10 = document.createElement('text')
    const el11 = document.createTextNode('文本节点0')
    el10.appendChild(el11)
    document.body.appendChild(el10)

    const el20 = document.createElement('text')
    const el21 = document.createTextNode('文本节点1')
    config.helper.setElementStyle(el20, 'fontSize', '32px')
    el20.appendChild(el21)
    document.body.appendChild(el20)

    const el30 = document.createElement('text')
    const el31 = document.createTextNode('文本节点2')
    el30.appendChild(el31)

    // 加入Document中
    document.body.appendChild(el30)

    expect(callNativeMessageList.length).to.equal(3)
    expect(callNativeMessageList[0]).to.include('文本节点0')
    expect(callNativeMessageList[1]).to.include('文本节点1')
    expect(callNativeMessageList[1]).to.include('fontSize')
    expect(callNativeMessageList[2]).to.include('文本节点2')
    callNativeMessageList.splice(0)

    // 通过Fragment添加
    const fg10 = document.createDocumentFragment()

    const elFragmentText11 = document.createElement('text')
    config.helper.setElementAttr(elFragmentText11, 'value', '片段0-文本节点0')
    fg10.appendChild(elFragmentText11)

    const elFragmentText12 = document.createElement('text')
    config.helper.setElementAttr(elFragmentText12, 'value', '片段0-文本节点1')
    config.helper.setElementStyle(elFragmentText12, 'fontSize', '32px')
    fg10.appendChild(elFragmentText12)

    const elFragmentText13 = document.createElement('text')
    config.helper.setElementAttr(elFragmentText13, 'value', '片段0-文本节点2')
    fg10.appendChild(elFragmentText13)

    expect(fg10.childNodes.length).to.equal(3)
    expect(fg10.layoutChildren.length).to.equal(3)

    // 加入Document中
    document.body.appendChild(fg10)

    expect(fg10.childNodes.length).to.equal(0)
    expect(fg10.layoutChildren.length).to.equal(0)

    expect(callNativeMessageList.length).to.equal(3)
    expect(callNativeMessageList[0]).to.include('片段0-文本节点0')
    expect(callNativeMessageList[1]).to.include('片段0-文本节点1')
    expect(callNativeMessageList[1]).to.include('fontSize')
    expect(callNativeMessageList[2]).to.include('片段0-文本节点2')
    callNativeMessageList.splice(0)

    // 父子引用正确
    expect(nodeBody.childNodes.length).to.equal(6)
    expect(nodeBody.childNodes[0]).to.equal(el10)
    expect(nodeBody.childNodes[1]).to.equal(el20)
    expect(nodeBody.childNodes[2]).to.equal(el30)
    expect(nodeBody.childNodes[3]).to.equal(elFragmentText11)
    expect(nodeBody.childNodes[4]).to.equal(elFragmentText12)
    expect(nodeBody.childNodes[5]).to.equal(elFragmentText13)

    expect(nodeBody.layoutChildren.length).to.equal(6)
    expect(nodeBody.layoutChildren[0]).to.equal(el10)
    expect(nodeBody.layoutChildren[1]).to.equal(el20)
    expect(nodeBody.layoutChildren[2]).to.equal(el30)
    expect(nodeBody.layoutChildren[3]).to.equal(elFragmentText11)
    expect(nodeBody.layoutChildren[4]).to.equal(elFragmentText12)
    expect(nodeBody.layoutChildren[5]).to.equal(elFragmentText13)
  })
})
