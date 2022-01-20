/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../imports'

/**
 * body节点
 */
describe('基础：2.测试Document', () => {
  beforeEach(() => {
    callNativeMessageList.splice(0)
  })

  afterEach(() => {
    callNativeMessageList.splice(0)
  })

  it('创建body并销毁', () => {
    // 创建文档
    const document = config.helper.createDocument(1)

    const nodeHtml = document.documentElement

    // 创建节点
    const nodeBody = document.createElement('div')

    expect(nodeBody.tagName).to.equal('DIV')
    expect(nodeBody.type).to.equal('div')

    const nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(Object.keys(nodeBodyAttr).length, '默认attr对象为空').to.equal(0)

    const nodeBodyStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(Object.keys(nodeBodyStyle).length, '默认style对象为空').to.equal(0)

    const nodeBodyClassList = config.misc.getNodeAttrClassList(nodeBody)
    expect(nodeBodyClassList.length, '默认classList.length=0').to.equal(0)

    expect(config.helper.getNodeDepth(nodeBody)).to.equal(null)

    // 加入文档
    nodeHtml.appendChild(nodeBody)

    const nodeBodyAttr2 = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(Object.keys(nodeBodyAttr2).length, '默认attr对象为空').to.equal(0)

    const nodeBodyStyle2 = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(Object.keys(nodeBodyStyle2).length, '默认style对象为空').to.equal(0)

    const nodeBodyClassList2 = config.misc.getNodeAttrClassList(nodeBody)
    expect(nodeBodyClassList2.length, '默认classList.length=0').to.equal(0)

    expect(nodeHtml.childNodes.length).to.equal(1)
    expect(nodeHtml.layoutChildren.length).to.equal(1)
    expect(nodeBody.parentNode).to.equal(nodeHtml)

    expect(document.body, 'document.body可访问').to.equal(nodeBody)
    expect(config.helper.getNodeDepth(nodeBody)).to.equal(1)

    // 删除节点
    nodeHtml.removeChild(nodeBody)

    expect(nodeHtml.childNodes.length).to.equal(0)
    expect(nodeHtml.layoutChildren.length).to.equal(0)
    expect(nodeBody.parentNode).to.equal(null)

    config.helper.destroyTagNode(nodeBody)

    expect(callNativeMessageList.length).to.equal(2)
    expect(callNativeMessageList[0]).to.include('createBody')
    expect(callNativeMessageList[1]).to.include('removeElement')

    // 销毁
    config.helper.destroyTagNode(document)
  })

  it('更新文档的标题', () => {
    // 创建文档
    const document = config.helper.createDocument(1)

    const attr = { k1: 'v1' }

    config.helper.updatePageTitleBar(document, attr)

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('updateTitleBar')
    expect(callNativeMessageList[0]).to.include(JSON.stringify(attr))

    // 销毁
    config.helper.destroyTagNode(document)
  })
})
