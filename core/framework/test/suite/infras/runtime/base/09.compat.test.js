/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../imports'

/**
 * 兼容以前的方式
 */
describe('基础：9.兼容性测试', () => {
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

  it('序列化各类对象', () => {
    // 序列化各类对象
    function serialize(obj) {
      return function() {
        return JSON.stringify(obj)
      }
    }

    const elem11 = document.createElement('elem11')
    nodeBody.appendChild(elem11)

    const figt12 = document.createElement('figt12')
    nodeBody.appendChild(figt12)

    const elem121 = document.createElement('elem121')
    figt12.appendChild(elem121)

    // 序列化；DOM节点
    expect(serialize(nodeHtml)).to.not.throw()
    expect(serialize(nodeBody)).to.not.throw()

    // 监听函数与结果
    const evtListenList = []
    const evtListener1 = function(evt) {
      evtListenList.push(serialize(nodeBody)())
    }
    nodeBody.addEventListener('click', evtListener1, false)
    // 触发事件
    const nodeElem1Evt1 = new config.Event('click')
    config.helper.fireTargetEventListener(nodeBody, nodeElem1Evt1)

    // 序列化：事件
    expect(evtListenList.length).to.equal(1)
    expect(JSON.parse(evtListenList[0])).to.an('object')
  })

  it('能够获取节点的属性', () => {
    config.helper.setElementAttr(nodeBody, 'id', 'idTest1')

    // 获取组件名称
    expect(nodeBody.type).to.equal('div')
    // 获取组件的id属性
    expect(nodeBody.id).to.equal('idTest1')
    // 获取组件属性对象
    expect(nodeBody.attr).to.an('object')
  })

  it('节点生成Action时属性序列化', () => {
    // 清理
    callNativeMessageList.splice(0)

    const elem11 = document.createElement('elem11')
    config.helper.setElementAttr(elem11, 'attr1', null)
    nodeBody.appendChild(elem11)

    // null属性会转换为''
    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.not.include('null')
    callNativeMessageList.splice(0)
  })

  it('删除节点后，再次设置属性等不会报错', () => {
    // 清理
    callNativeMessageList.splice(0)

    const elem11 = document.createElement('elem11')
    nodeBody.appendChild(elem11)
    nodeBody.removeChild(elem11)

    // 不会报错
    config.helper.setElementAttr('elem11', 'attr1', 'attr1value1')
    config.helper.setElementStyle('elem11', 'style1', 'style1value1')
    config.helper.setElementStyles('elem11', { style1: 'style1value1' })
  })
})
