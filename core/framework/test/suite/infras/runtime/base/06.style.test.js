/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
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

  it('border样式', () => {
    callNativeMessageList.splice(0)

    // border width style color
    config.helper.setElementStyles(nodeBody, 'border: 10px dotted red;')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.borderTopWidth).to.equal('10px')
    expect(nodeBodyAttrStyle.borderRightWidth).to.equal('10px')
    expect(nodeBodyAttrStyle.borderBottomWidth).to.equal('10px')
    expect(nodeBodyAttrStyle.borderLeftWidth).to.equal('10px')
    expect(nodeBodyAttrStyle.borderStyle).to.equal('dotted')
    expect(nodeBodyAttrStyle.borderTopColor).to.equal('#FF0000')
    expect(nodeBodyAttrStyle.borderRightColor).to.equal('#FF0000')
    expect(nodeBodyAttrStyle.borderBottomColor).to.equal('#FF0000')
    expect(nodeBodyAttrStyle.borderLeftColor).to.equal('#FF0000')
    expect(callNativeMessageList[0]).to.include('borderTopWidth')
    expect(callNativeMessageList[0]).to.include('borderRightWidth')
    expect(callNativeMessageList[0]).to.include('borderBottomWidth')
    expect(callNativeMessageList[0]).to.include('borderLeftWidth')
    expect(callNativeMessageList[0]).to.include('borderStyle')
    expect(callNativeMessageList[0]).to.include('borderTopColor')
    expect(callNativeMessageList[0]).to.include('borderRightColor')
    expect(callNativeMessageList[0]).to.include('borderBottomColor')
    expect(callNativeMessageList[0]).to.include('borderLeftColor')
    expect(callNativeMessageList[0]).to.include('10px')
    expect(callNativeMessageList[0]).to.include('dotted')
    expect(callNativeMessageList[0]).to.include('#FF0000')
    callNativeMessageList.splice(0)

    // border-width
    config.helper.setElementStyles(nodeBody, 'border-width: 0px 10px 20px 30px;')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.borderTopWidth).to.equal('0px')
    expect(nodeBodyAttrStyle.borderRightWidth).to.equal('10px')
    expect(nodeBodyAttrStyle.borderBottomWidth).to.equal('20px')
    expect(nodeBodyAttrStyle.borderLeftWidth).to.equal('30px')
    expect(callNativeMessageList[0]).to.include('borderTopWidth')
    expect(callNativeMessageList[0]).to.include('borderRightWidth')
    expect(callNativeMessageList[0]).to.include('borderBottomWidth')
    expect(callNativeMessageList[0]).to.include('borderLeftWidth')
    expect(callNativeMessageList[0]).to.include('0px')
    expect(callNativeMessageList[0]).to.include('10px')
    expect(callNativeMessageList[0]).to.include('20px')
    expect(callNativeMessageList[0]).to.include('30px')
    callNativeMessageList.splice(0)

    // border-color
    config.helper.setElementStyles(nodeBody, 'border-color: blue red blue red')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.borderTopColor).to.equal('#0000FF')
    expect(nodeBodyAttrStyle.borderRightColor).to.equal('#FF0000')
    expect(nodeBodyAttrStyle.borderBottomColor).to.equal('#0000FF')
    expect(nodeBodyAttrStyle.borderLeftColor).to.equal('#FF0000')
    expect(callNativeMessageList[0]).to.include('borderTopColor')
    expect(callNativeMessageList[0]).to.include('borderRightColor')
    expect(callNativeMessageList[0]).to.include('borderBottomColor')
    expect(callNativeMessageList[0]).to.include('borderLeftColor')
    expect(callNativeMessageList[0]).to.include('#0000FF')
    expect(callNativeMessageList[0]).to.include('#FF0000')
    callNativeMessageList.splice(0)

    // border-style
    config.helper.setElementStyles(nodeBody, 'border-style: dotted;')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.borderStyle).to.equal('dotted')
    expect(callNativeMessageList[0]).to.include('borderStyle')
    expect(callNativeMessageList[0]).to.include('dotted')
    callNativeMessageList.splice(0)

    // border width;
    config.helper.setElementStyles(nodeBody, 'border: 10px;')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.borderTopWidth).to.equal('10px')
    expect(nodeBodyAttrStyle.borderRightWidth).to.equal('10px')
    expect(nodeBodyAttrStyle.borderBottomWidth).to.equal('10px')
    expect(nodeBodyAttrStyle.borderLeftWidth).to.equal('10px')
    expect(callNativeMessageList[0]).to.include('borderTopWidth')
    expect(callNativeMessageList[0]).to.include('borderRightWidth')
    expect(callNativeMessageList[0]).to.include('borderBottomWidth')
    expect(callNativeMessageList[0]).to.include('borderLeftWidth')
    expect(callNativeMessageList[0]).to.include('10px')
    callNativeMessageList.splice(0)

    // border 分开写;
    config.helper.setElementStyles(
      nodeBody,
      'borderTopColor: #0000FF;borderRightColor: #FF0000;borderBottomColor: #0000FF;borderLeftColor: #FF0000;borderTopWidth: 10px;borderRightWidth: 10px;borderBottomWidth: 10px;borderLeftWidth: 10px'
    )
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.borderTopColor).to.equal('#0000FF')
    expect(nodeBodyAttrStyle.borderRightColor).to.equal('#FF0000')
    expect(nodeBodyAttrStyle.borderBottomColor).to.equal('#0000FF')
    expect(nodeBodyAttrStyle.borderLeftColor).to.equal('#FF0000')
    expect(nodeBodyAttrStyle.borderTopWidth).to.equal('10px')
    expect(nodeBodyAttrStyle.borderRightWidth).to.equal('10px')
    expect(nodeBodyAttrStyle.borderBottomWidth).to.equal('10px')
    expect(nodeBodyAttrStyle.borderLeftWidth).to.equal('10px')
    callNativeMessageList.splice(0)
  })

  it('padding样式', () => {
    callNativeMessageList.splice(0)
    // padding样式1
    config.helper.setElementStyles(nodeBody, 'padding: 10px')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.paddingTop).to.equal('10px')
    expect(nodeBodyAttrStyle.paddingRight).to.equal('10px')
    expect(nodeBodyAttrStyle.paddingBottom).to.equal('10px')
    expect(nodeBodyAttrStyle.paddingLeft).to.equal('10px')
    expect(callNativeMessageList[0]).to.include('paddingTop')
    expect(callNativeMessageList[0]).to.include('paddingRight')
    expect(callNativeMessageList[0]).to.include('paddingBottom')
    expect(callNativeMessageList[0]).to.include('paddingLeft')
    expect(callNativeMessageList[0]).to.include('10px')
    callNativeMessageList.splice(0)

    // padding样式1
    config.helper.setElementStyles(nodeBody, 'padding: 0px 10px 20px 30px')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.paddingTop).to.equal('0px')
    expect(nodeBodyAttrStyle.paddingRight).to.equal('10px')
    expect(nodeBodyAttrStyle.paddingBottom).to.equal('20px')
    expect(nodeBodyAttrStyle.paddingLeft).to.equal('30px')
    expect(callNativeMessageList[0]).to.include('paddingTop')
    expect(callNativeMessageList[0]).to.include('paddingRight')
    expect(callNativeMessageList[0]).to.include('paddingBottom')
    expect(callNativeMessageList[0]).to.include('paddingLeft')
    expect(callNativeMessageList[0]).to.include('0px')
    expect(callNativeMessageList[0]).to.include('10px')
    expect(callNativeMessageList[0]).to.include('20px')
    expect(callNativeMessageList[0]).to.include('30px')
    callNativeMessageList.splice(0)
  })

  it('margin样式', () => {
    callNativeMessageList.splice(0)

    // margin样式1
    config.helper.setElementStyles(nodeBody, 'margin: 0px 10px 20px 30px')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.marginTop).to.equal('0px')
    expect(nodeBodyAttrStyle.marginRight).to.equal('10px')
    expect(nodeBodyAttrStyle.marginBottom).to.equal('20px')
    expect(nodeBodyAttrStyle.marginLeft).to.equal('30px')
    expect(callNativeMessageList[0]).to.include('marginTop')
    expect(callNativeMessageList[0]).to.include('marginRight')
    expect(callNativeMessageList[0]).to.include('marginBottom')
    expect(callNativeMessageList[0]).to.include('marginLeft')
    expect(callNativeMessageList[0]).to.include('10px')
    expect(callNativeMessageList[0]).to.include('20px')
    expect(callNativeMessageList[0]).to.include('30px')
    callNativeMessageList.splice(0)

    // margin样式2
    config.helper.setElementStyles(nodeBody, 'margin: 10px;')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.marginTop).to.equal('10px')
    expect(nodeBodyAttrStyle.marginRight).to.equal('10px')
    expect(nodeBodyAttrStyle.marginBottom).to.equal('10px')
    expect(nodeBodyAttrStyle.marginLeft).to.equal('10px')
    expect(callNativeMessageList[0]).to.include('marginTop')
    expect(callNativeMessageList[0]).to.include('marginRight')
    expect(callNativeMessageList[0]).to.include('marginBottom')
    expect(callNativeMessageList[0]).to.include('marginLeft')
    expect(callNativeMessageList[0]).to.include('10px')
    callNativeMessageList.splice(0)
  })

  it('background样式', () => {
    callNativeMessageList.splice(0)

    config.helper.setElementStyles(nodeBody, 'background: linear-gradient(red, blue)')
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    const background = JSON.stringify(nodeBodyAttrStyle.background)
    expect(background).to.include('linearGradient')
    expect(background).to.include('#FF0000')
    expect(background).to.include('#0000FF')
    callNativeMessageList.splice(0)

    config.helper.setElementStyles(nodeBody, `backgroundImage: /assets/images/logo.png`)
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.backgroundImage).to.equal('/assets/images/logo.png')
    callNativeMessageList.splice(0)

    config.helper.setElementStyles(
      nodeBody,
      'background: {"values":[{"type":"linearGradient","directions":["to","bottom"],"values":["#FF0000","#0000FF"]}]}'
    )
    nodeBodyAttrStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyAttrStyle.background).to.equal(
      '{"values":[{"type":"linearGradient","directions":["to","bottom"],"values":["#FF0000","#0000FF"]}]}'
    )
    callNativeMessageList.splice(0)
  })
})
