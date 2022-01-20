/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { proxySendActions, unproxySendActions } from '../imports'

describe('action: interface 接口与数据格式', () => {
  let document
  const docLevelStyle = {
    div: {
      ca1: 'ca1-div-1'
    }
  }

  before(() => {
    // 不再代理
    unproxySendActions()
  })

  after(() => {
    // 恢复代理
    proxySendActions()
  })

  beforeEach(() => {
    callNativeMessageList.splice(0)
    // 创建文档
    document = config.helper.createDocument(1)
  })

  afterEach(() => {
    // 销毁
    config.helper.destroyTagNode(document)
  })

  it('createFinish', () => {
    document.listener.createFinish()
    callNativeMessageList[0] = JSON.parse(callNativeMessageList[0])

    expect(callNativeMessageList[0]).to.include({ module: 'dom', method: 'createFinish' })
  })

  it('updateFinish', () => {
    document.listener.updateFinish()
    callNativeMessageList[0] = JSON.parse(callNativeMessageList[0])

    expect(callNativeMessageList[0]).to.include({ module: 'dom', method: 'updateFinish' })
  })

  it('createBody', () => {
    const nodeHtml = document.documentElement

    // 创建节点
    const nodeBody = document.createElement('div')
    // 加入文档
    nodeHtml.appendChild(nodeBody)
    callNativeMessageList[0] = JSON.parse(callNativeMessageList[0])

    expect(callNativeMessageList[0]).to.include({ module: 'dom', method: 'createBody' })
    expect(callNativeMessageList[0].args[0]).to.include({ type: 'div' })
  })

  it('addElement', () => {
    const nodeHtml = document.documentElement
    // 创建节点
    const nodeBody = document.createElement('div')
    const child1 = document.createElement('span')
    // 加入文档
    nodeHtml.appendChild(nodeBody)
    nodeBody.appendChild(child1)
    callNativeMessageList[1] = JSON.parse(callNativeMessageList[1])

    expect(callNativeMessageList[1]).to.include({ module: 'dom', method: 'addElement' })
    expect(callNativeMessageList[1].args[1]).to.include({ type: 'span' })
  })

  it('removeElement', () => {
    const nodeHtml = document.documentElement
    // 创建节点
    const nodeBody = document.createElement('div')
    const ref = nodeBody.ref

    // 加入文档
    nodeHtml.appendChild(nodeBody)
    nodeHtml.removeChild(nodeBody)
    callNativeMessageList[1] = JSON.parse(callNativeMessageList[1])

    expect(callNativeMessageList[1]).to.include({ module: 'dom', method: 'removeElement' })
    expect(callNativeMessageList[1].args[0]).to.equal(ref)
  })

  it('moveElement', () => {
    const nodeHtml = document.documentElement
    // 创建节点
    const nodeBody = document.createElement('div')
    const child1 = document.createElement('span')
    const child2 = document.createElement('a')
    // 加入文档
    nodeHtml.appendChild(nodeBody)
    nodeBody.appendChild(child1)
    nodeBody.appendChild(child2)
    nodeBody.insertBefore(child2, child1)
    callNativeMessageList[3] = JSON.parse(callNativeMessageList[3])

    expect(callNativeMessageList[3]).to.include({ module: 'dom', method: 'moveElement' })
    expect(callNativeMessageList[3].args[0]).to.equal(child2.ref)
    expect(callNativeMessageList[3].args[1]).to.equal(nodeBody.ref)
  })

  it('updateProps', () => {
    const nodeHtml = document.documentElement
    // 创建节点
    const nodeBody = document.createElement('div')
    // 加入文档
    nodeHtml.appendChild(nodeBody)
    config.helper.setElementProp(nodeBody, 'propKey', 'propValue', false)
    callNativeMessageList[1] = JSON.parse(callNativeMessageList[1])

    expect(callNativeMessageList[1]).to.include({ module: 'dom', method: 'updateProps' })
    expect(callNativeMessageList[1].args[0]).to.equal(nodeBody.ref)
    expect(callNativeMessageList[1].args[1]).to.deep.equal({ prop: { propKey: 'propValue' } })
  })

  it('updateAttrs', () => {
    const nodeHtml = document.documentElement
    // 创建节点
    const nodeBody = document.createElement('div')
    // 加入文档
    nodeHtml.appendChild(nodeBody)
    document.listener.setAttr(nodeBody.ref, 'Key', 'Value')
    callNativeMessageList[1] = JSON.parse(callNativeMessageList[1])

    expect(callNativeMessageList[1]).to.include({ module: 'dom', method: 'updateAttrs' })
    expect(callNativeMessageList[1].args[0]).to.equal(nodeBody.ref)
    expect(callNativeMessageList[1].args[1]).to.deep.equal({ attr: { Key: 'Value' } })
  })

  it('updateStyle', () => {
    const nodeHtml = document.documentElement
    // 创建节点
    const nodeBody = document.createElement('div')
    // 加入文档
    nodeHtml.appendChild(nodeBody)
    document.listener.setStyle(nodeBody.ref, 'width', '400px')
    callNativeMessageList[1] = JSON.parse(callNativeMessageList[1])

    expect(callNativeMessageList[1]).to.include({ module: 'dom', method: 'updateStyle' })
    expect(callNativeMessageList[1].args[0]).to.equal(nodeBody.ref)
    expect(callNativeMessageList[1].args[1]).to.deep.equal({ style: { width: '400px' } })
  })

  it('updateStyles', () => {
    const nodeHtml = document.documentElement
    // 创建节点
    const nodeBody = document.createElement('div')
    // 加入文档
    nodeHtml.appendChild(nodeBody)
    document.listener.setStyles(
      nodeBody.ref,
      { width: '100%', height: '50%' },
      { class: 'container' }
    )
    callNativeMessageList[1] = JSON.parse(callNativeMessageList[1])

    expect(callNativeMessageList[1]).to.include({ module: 'dom', method: 'updateStyles' })
    expect(callNativeMessageList[1].args[0]).to.equal(nodeBody.ref)
    expect(callNativeMessageList[1].args[1]).to.deep.equal({
      style: { width: '100%', height: '50%' },
      attr: { class: 'container' }
    })
  })

  it('updateStyleObject', () => {
    const nodeHtml = document.documentElement
    // 创建节点
    const nodeBody = document.createElement('div')
    // 加入文档
    nodeHtml.appendChild(nodeBody)
    // 文档级别
    config.helper.registerStyleObject('documentLevelCustom', docLevelStyle, true, document.body)
    callNativeMessageList[1] = JSON.parse(callNativeMessageList[1])

    expect(callNativeMessageList[1]).to.include({ module: 'dom', method: 'updateStyleObject' })
    expect(callNativeMessageList[1].args[0]).to.equal(nodeBody.ref)
    expect(callNativeMessageList[1].args[1]).to.equal(true)
    expect(callNativeMessageList[1].args[2]).to.equal('documentLevelCustom')
    expect(callNativeMessageList[1].args[4]).to.be.deep.eql({ div: { ca1: 'ca1-div-1' } })
  })

  it('addEvent', () => {
    const nodeHtml = document.documentElement
    // 创建节点
    const nodeBody = document.createElement('div')
    // 加入文档
    nodeHtml.appendChild(nodeBody)
    const eventHandler = function(e) {}
    nodeBody.addEventListener('click', eventHandler, true)
    callNativeMessageList[1] = JSON.parse(callNativeMessageList[1])

    expect(callNativeMessageList[1]).to.include({ module: 'dom', method: 'addEvent' })
    expect(callNativeMessageList[1].args[0]).to.equal(nodeBody.ref)
    expect(callNativeMessageList[1].args[1]).to.equal('click')
  })
  // TODO: 目前removeEvent 没有发现调用的地方
  // it('removeEvent', () => {
  //   const nodeHtml = document.documentElement
  //   // 创建节点
  //   const nodeBody = document.createElement('div')
  //   // 加入文档
  //   nodeHtml.appendChild(nodeBody)
  //   const eventHandler = function(e) {}
  //   nodeBody.addEventListener('click', eventHandler, true)
  //   nodeBody.removeEventListener('click', eventHandler, true)

  //   expect(callNativeMessageList[0].module).to.equal('dom')
  //   expect(callNativeMessageList[0].method).to.equal('removeEvent')
  //   expect(callNativeMessageList[0].args[0]).to.equal(nodeBody.ref)
  //   expect(callNativeMessageList[0].args[1]).to.equal('click')
  // })

  it('updateTitleBar', () => {
    const attr = { k1: 'v1' }
    config.helper.updatePageTitleBar(document, attr)
    callNativeMessageList[0] = JSON.parse(callNativeMessageList[0])

    expect(callNativeMessageList[0]).to.include({ module: 'dom', method: 'updateTitleBar' })
    expect(callNativeMessageList[0].args[0]).to.deep.include(attr)
  })

  it('updateStatusBar', () => {
    config.helper.updatePageStatusBar(document, { immersive: true, textStyle: 'dark' })
    callNativeMessageList[0] = JSON.parse(callNativeMessageList[0])

    expect(callNativeMessageList[0]).to.include({ module: 'dom', method: 'updateStatusBar' })
    expect(callNativeMessageList[0].args[0]).to.deep.include({ immersive: true, textStyle: 'dark' })
  })

  it('scrollTo', () => {
    config.helper.scrollTo(document, { index: 0 })
    callNativeMessageList[0] = JSON.parse(callNativeMessageList[0])

    expect(callNativeMessageList[0]).to.include({ module: 'dom', method: 'scrollTo' })
  })

  it('scrollBy', () => {
    config.helper.scrollBy(document, { left: 20, top: 50 })
    callNativeMessageList[0] = JSON.parse(callNativeMessageList[0])

    expect(callNativeMessageList[0]).to.include({ module: 'dom', method: 'scrollBy' })
    expect(callNativeMessageList[0].args[0]).to.deep.include({ left: 20, top: 50 })
  })

  it('exitFullscreen', () => {
    config.helper.exitFullscreen(document)
    callNativeMessageList[0] = JSON.parse(callNativeMessageList[0])

    expect(callNativeMessageList[0]).to.include({ module: 'dom', method: 'exitFullscreen' })
  })

  it('statistics', () => {
    document.listener.collectStatistics({ type: 'click', ref: 1, listeners: [3, 5] })
    callNativeMessageList[0] = JSON.parse(callNativeMessageList[0])

    expect(callNativeMessageList[0]).to.include({ module: 'dom', method: 'statistics' })
    expect(callNativeMessageList[0].args[0]).to.be.deep.eql({
      type: 'click',
      ref: 1,
      listeners: [3, 5]
    })
  })

  it('invokeComponentMethod', () => {
    document.listener.invokeComponentMethod('canvas', 8, 'getContext', ['2d'])
    callNativeMessageList[0] = JSON.parse(callNativeMessageList[0])

    expect(callNativeMessageList[0].module).to.be.an('undefined')
    expect(callNativeMessageList[0].component).to.equal('canvas')
    expect(callNativeMessageList[0].ref).to.equal(8)
    expect(callNativeMessageList[0].method).to.equal('getContext')
    expect(callNativeMessageList[0].args).to.deep.include('2d')
  })
})
