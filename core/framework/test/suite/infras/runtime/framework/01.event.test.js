/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../imports'

/**
 * 触发事件
 */
describe('框架：1.触发事件', () => {
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

  it('仅触发对应的节点本身', () => {
    // 监听函数与结果
    const evtListenList = []
    const evtListener1 = function(evt) {
      const msg = `正常:evtListener1:${evt.type}:${evt.target.nodeName}:${evt.param}`
      evtListenList.push(msg)
    }
    const evtListener2 = function(evt) {
      const msg = `正常:evtListener2:${evt.type}:${evt.target.nodeName}:${evt.param}`
      evtListenList.push(msg)
    }

    // body绑定
    nodeBody.addEventListener('click', evtListener1, false)

    // 节点绑定
    const nodeElem1 = document.createElement('text')
    nodeBody.appendChild(nodeElem1)

    // 触发事件
    const nodeElem1Evt1 = new config.Event('click')
    config.helper.fireTargetEventListener(nodeElem1, nodeElem1Evt1)
    expect(evtListenList.length, '不会向上传递').to.equal(0)

    // 触发事件
    const nodeElem1Evt2 = new config.Event('click')
    // 携带参数
    Object.assign(nodeElem1Evt2, { param: 'paramValue1' })
    nodeElem1.addEventListener('click', evtListener1, false)
    config.helper.fireTargetEventListener(nodeElem1, nodeElem1Evt2)
    expect(evtListenList.length).to.equal(1)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:TEXT:paramValue1')
    evtListenList.splice(0)

    // 触发事件
    const nodeElem1Evt3 = new config.Event('click')
    // 携带参数
    Object.assign(nodeElem1Evt3, { param: 'paramValue3' })
    nodeElem1.addEventListener('click', evtListener1, false)
    config.helper.fireTargetEventListener(nodeElem1, nodeElem1Evt3)
    expect(evtListenList.length).to.equal(1)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:TEXT:paramValue3')
    evtListenList.splice(0)

    // 触发事件
    const nodeElem1Evt4 = new config.Event('click')
    // 携带参数
    Object.assign(nodeElem1Evt4, { param: 'paramValue4' })
    // 添加同一个监听函数：只会注册一次
    nodeElem1.addEventListener('click', evtListener1, false)
    config.helper.fireTargetEventListener(nodeElem1, nodeElem1Evt4)
    expect(evtListenList.length).to.equal(1)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:TEXT:paramValue4')
    evtListenList.splice(0)

    // 触发事件
    const nodeElem1Evt5 = new config.Event('click')
    // 携带参数
    Object.assign(nodeElem1Evt5, { param: 'paramValue5' })
    // 添加另一个监听函数：会注册成功
    nodeElem1.addEventListener('click', evtListener2, false)
    config.helper.fireTargetEventListener(nodeElem1, nodeElem1Evt5)
    expect(evtListenList.length).to.equal(2)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:TEXT:paramValue5')
    expect(evtListenList[1]).to.equal('正常:evtListener2:click:TEXT:paramValue5')
    evtListenList.splice(0)
  })

  it('更新DOM属性和样式：通知底层', () => {
    // 清除
    callNativeMessageList.splice(0)

    config.helper.setElementAttr(nodeBody, 'attr1', 'attrValue1')
    config.helper.setElementStyle(nodeBody, 'style1', 'styleValue1')

    const nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr.attr1).to.equal('attrValue1')
    const nodeBodyStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyStyle.style1).to.equal('styleValue1')

    expect(callNativeMessageList.length).to.equal(2)
    expect(callNativeMessageList[0]).to.include('attrValue1')
    expect(callNativeMessageList[1]).to.include('styleValue1')
  })

  it('更新DOM属性和样式：不通知底层', () => {
    // 清除
    callNativeMessageList.splice(0)

    // 不通知底层
    config.helper.setElementAttr(nodeBody, 'attr2', 'attrValue2', true)
    config.helper.setElementStyle(nodeBody, 'style2', 'styleValue2', true)

    const nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr.attr2).to.equal('attrValue2')
    const nodeBodyStyle = config.misc.getNodeInlineStyleAsObject(nodeBody)
    expect(nodeBodyStyle.style2).to.equal('styleValue2')

    expect(callNativeMessageList.length).to.equal(0)
  })
})
