/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../imports'

/**
 * 样式操作
 */
describe('基础：6.样式操作', () => {
  let document, nodeHtml, nodeBody
  let nodeBodyAttrStyle

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

  it('单个样式', () => {
    // 清理
    callNativeMessageList.splice(0)

    // 赋值
    config.helper.setElementStyle(nodeBody, 'width', '100px')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.width).to.equal('100px')
    expect(callNativeMessageList[0]).to.include('width')
    expect(callNativeMessageList[0]).to.include('100px')
    callNativeMessageList.splice(0)

    // 赋值为空串
    config.helper.setElementStyle(nodeBody, 'width', '')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.width).to.equal('')
    expect(callNativeMessageList[0]).to.include('width')
    expect(callNativeMessageList[0]).to.include('""')
    callNativeMessageList.splice(0)

    // 赋值
    config.helper.setElementStyle(nodeBody, 'width', '100px')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.width).to.equal('100px')
    expect(callNativeMessageList[0]).to.include('width')
    expect(callNativeMessageList[0]).to.include('100px')
    callNativeMessageList.splice(0)

    // 赋值为null
    config.helper.setElementStyle(nodeBody, 'width', null)
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.width).to.equal(null)
    expect(callNativeMessageList[0]).to.include('width')
    expect(callNativeMessageList[0]).to.include('""')
    callNativeMessageList.splice(0)
  })

  it('整个样式对象', () => {
    // 清理
    callNativeMessageList.splice(0)

    // 赋值
    config.helper.setElementStyles(nodeBody, { width: '100px', height: '10%' })
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.width).to.equal('100px')
    expect(nodeBodyAttrStyle.height).to.equal('10%')
    expect(callNativeMessageList[0]).to.include('width')
    expect(callNativeMessageList[0]).to.include('100px')
    expect(callNativeMessageList[0]).to.include('height')
    expect(callNativeMessageList[0]).to.include('10%')
    callNativeMessageList.splice(0)

    // 赋值字符串：属性名带 -
    config.helper.setElementStyles(nodeBody, 'font-size: 14px')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle).to.eql({
      fontSize: '14px'
    })
    callNativeMessageList.splice(0)

    // 赋值字符串：普通样式
    config.helper.setElementStyles(nodeBody, 'width: 100px; height: 10%')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.width).to.equal('100px')
    expect(nodeBodyAttrStyle.height).to.equal('10%')
    expect(callNativeMessageList[0]).to.include('width')
    expect(callNativeMessageList[0]).to.include('100px')
    expect(callNativeMessageList[0]).to.include('height')
    expect(callNativeMessageList[0]).to.include('10%')
    callNativeMessageList.splice(0)

    // 赋值字符串：非法格式
    config.helper.setElementStyles(nodeBody, ':;:')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle).to.eql({
      '': ''
    })
    callNativeMessageList.splice(0)

    // 输入 undefined
    config.helper.setElementStyles(nodeBody, undefined)
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle).to.eql({})
    callNativeMessageList.splice(0)
  })
})
