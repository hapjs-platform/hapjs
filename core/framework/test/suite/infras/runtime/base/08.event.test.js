/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../imports'

/**
 * 事件操作
 */
describe('基础：8.事件操作', () => {
  let document, nodeHtml, nodeBody
  let evtOrigin, evtJSON

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

  it('综合测试：捕获冒泡', () => {
    // 节点绑定
    const nodeElem1 = document.createElement('text')
    nodeBody.appendChild(nodeElem1)

    // 监听函数与结果
    const evtListenList = []
    const evtListener1 = function(evt) {
      const msg = `正常:evtListener1:${evt.type}:${evt.eventPhase}:${evt.target.nodeName}:${evt.currentTarget.nodeName}`
      evtListenList.push(msg)
    }
    const evtListener2 = function(evt) {
      const msg = `正常:evtListener2:${evt.type}:${evt.eventPhase}:${evt.target.nodeName}:${evt.currentTarget.nodeName}:${evt.param}`
      evtListenList.push(msg)
    }
    const evtListenerErr = function(evt) {
      const msg = `抛错:${evt.type}:${evt.eventPhase}:${evt.target.nodeName}:${evt.currentTarget.nodeName}`
      evtListenList.push(msg)
      throw new Error(msg)
    }
    const evtListenerStop = function(evt) {
      const msg = `停止:${evt.type}:${evt.eventPhase}:${evt.target.nodeName}:${evt.currentTarget.nodeName}`
      evtListenList.push(msg)
      evt.stopPropagation()
    }
    const evtListenerStopNow = function(evt) {
      const msg = `立停:${evt.type}:${evt.eventPhase}:${evt.target.nodeName}:${evt.currentTarget.nodeName}`
      evtListenList.push(msg)
      evt.stopImmediatePropagation()
    }

    // 捕获期
    document.addEventListener('click', evtListener1, true)
    nodeBody.addEventListener('click', evtListener1, true)
    nodeElem1.addEventListener('click', evtListener1, true)
    // 冒泡期
    document.addEventListener('click', evtListener1)
    nodeBody.addEventListener('click', evtListener1)
    nodeElem1.addEventListener('click', evtListener1)

    console.trace(``)

    // 创建: 默认不冒泡，走捕获
    nodeElem1.dispatchEvent(new config.Event('click'))

    expect(evtListenList.length).to.equal(4)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:1:TEXT:#document')
    expect(evtListenList[1]).to.equal('正常:evtListener1:click:1:TEXT:DIV')
    expect(evtListenList[2]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[3]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    evtListenList.splice(0)
    console.trace(``)

    // 创建: 添加同一个监听函数：只会注册一次
    nodeElem1.addEventListener('click', evtListener1, true)
    nodeElem1.addEventListener('click', evtListener1)

    nodeElem1.dispatchEvent(new config.Event('click'))

    expect(evtListenList.length).to.equal(4)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:1:TEXT:#document')
    expect(evtListenList[1]).to.equal('正常:evtListener1:click:1:TEXT:DIV')
    expect(evtListenList[2]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[3]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    evtListenList.splice(0)
    console.trace(``)

    // 创建: 添加另一个监听函数：会注册成功；同时事件携带参数
    nodeElem1.addEventListener('click', evtListener2, true)
    nodeElem1.addEventListener('click', evtListener2)

    nodeElem1.dispatchEvent(Object.assign(new config.Event('click'), { param: 'paramValue5' }))

    expect(evtListenList.length).to.equal(6)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:1:TEXT:#document')
    expect(evtListenList[1]).to.equal('正常:evtListener1:click:1:TEXT:DIV')
    expect(evtListenList[2]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[3]).to.equal('正常:evtListener2:click:2:TEXT:TEXT:paramValue5')
    expect(evtListenList[4]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[5]).to.equal('正常:evtListener2:click:2:TEXT:TEXT:paramValue5')
    evtListenList.splice(0)
    console.trace(``)

    // 删除：删除另外的监听函数
    nodeElem1.removeEventListener('click', evtListener2, true)
    nodeElem1.removeEventListener('click', evtListener2)

    nodeElem1.dispatchEvent(new config.Event('click'))

    expect(evtListenList.length).to.equal(4)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:1:TEXT:#document')
    expect(evtListenList[1]).to.equal('正常:evtListener1:click:1:TEXT:DIV')
    expect(evtListenList[2]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[3]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    evtListenList.splice(0)
    console.trace(``)

    // 创建: 冒泡
    nodeElem1.dispatchEvent(new config.Event('click', { bubbles: true }))

    expect(evtListenList.length).to.equal(6)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:1:TEXT:#document')
    expect(evtListenList[1]).to.equal('正常:evtListener1:click:1:TEXT:DIV')
    expect(evtListenList[2]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[3]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[4]).to.equal('正常:evtListener1:click:3:TEXT:DIV')
    expect(evtListenList[5]).to.equal('正常:evtListener1:click:3:TEXT:#document')
    evtListenList.splice(0)
    console.trace(``)

    // 删除：document捕获
    document.removeEventListener('click', evtListener1, true)
    nodeElem1.dispatchEvent(new config.Event('click', { bubbles: true }))

    expect(evtListenList.length).to.equal(5)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:1:TEXT:DIV')
    expect(evtListenList[1]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[2]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[3]).to.equal('正常:evtListener1:click:3:TEXT:DIV')
    expect(evtListenList[4]).to.equal('正常:evtListener1:click:3:TEXT:#document')
    evtListenList.splice(0)
    console.trace(``)

    // 删除：document冒泡
    document.removeEventListener('click', evtListener1)
    nodeElem1.dispatchEvent(new config.Event('click', { bubbles: true }))

    expect(evtListenList.length).to.equal(4)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:1:TEXT:DIV')
    expect(evtListenList[1]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[2]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[3]).to.equal('正常:evtListener1:click:3:TEXT:DIV')
    evtListenList.splice(0)
    console.trace(``)

    // 删除
    nodeElem1.removeEventListener('click', evtListener1, true)
    nodeElem1.removeEventListener('click', evtListener1)
    nodeElem1.dispatchEvent(new config.Event('click', { bubbles: true }))

    expect(evtListenList.length).to.equal(2)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:1:TEXT:DIV')
    expect(evtListenList[1]).to.equal('正常:evtListener1:click:3:TEXT:DIV')
    evtListenList.splice(0)
    console.trace(``)

    // 删除
    nodeBody.removeEventListener('click', evtListener1, true)
    nodeBody.removeEventListener('click', evtListener1)
    nodeElem1.dispatchEvent(new config.Event('click', { bubbles: true }))

    expect(evtListenList.length).to.equal(0)
    evtListenList.splice(0)
    console.trace(``)

    // 绑定出错函数
    nodeBody.addEventListener('click', evtListenerErr, true)
    nodeBody.addEventListener('click', evtListenerErr)
    nodeElem1.addEventListener('click', evtListenerErr, true)
    nodeElem1.addEventListener('click', evtListenerErr)
    suppressConsole(() => {
      const evt = new config.Event('click', { bubbles: true })
      evt._throwError = false
      nodeElem1.dispatchEvent(evt)
    })

    expect(evtListenList.length).to.equal(4)
    expect(evtListenList[0]).to.equal('抛错:click:1:TEXT:DIV')
    expect(evtListenList[1]).to.equal('抛错:click:2:TEXT:TEXT')
    expect(evtListenList[2]).to.equal('抛错:click:2:TEXT:TEXT')
    expect(evtListenList[3]).to.equal('抛错:click:3:TEXT:DIV')
    evtListenList.splice(0)
    nodeBody.removeEventListener('click', evtListenerErr, true)
    nodeBody.removeEventListener('click', evtListenerErr)
    nodeElem1.removeEventListener('click', evtListenerErr, true)
    nodeElem1.removeEventListener('click', evtListenerErr)
    console.trace(``)

    // 停止传播
    nodeBody.addEventListener('click', evtListener1, true)
    nodeBody.addEventListener('click', evtListenerStop, true)
    nodeElem1.addEventListener('click', evtListener1, true)
    nodeElem1.addEventListener('click', evtListener1)
    nodeElem1.dispatchEvent(new config.Event('click', { bubbles: true }))
    nodeBody.removeEventListener('click', evtListenerStop, true)

    expect(evtListenList.length).to.equal(2)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:1:TEXT:DIV')
    expect(evtListenList[1]).to.equal('停止:click:1:TEXT:DIV')
    evtListenList.splice(0)
    console.trace(``)

    // 立即停止
    nodeElem1.addEventListener('click', evtListenerStopNow, true)
    nodeElem1.dispatchEvent(new config.Event('click', { bubbles: true }))

    expect(evtListenList.length).to.equal(3)
    expect(evtListenList[0]).to.equal('正常:evtListener1:click:1:TEXT:DIV')
    expect(evtListenList[1]).to.equal('正常:evtListener1:click:2:TEXT:TEXT')
    expect(evtListenList[2]).to.equal('立停:click:2:TEXT:TEXT')
    evtListenList.splice(0)
    console.trace(``)
  })

  it('基础：序列化事件对象', () => {
    // 初始化节点并绑定事件
    const nodeElem2 = document.createElement('text')
    nodeBody.appendChild(nodeElem2)

    const evtListener = function(evt) {
      evtOrigin = evt
      evtJSON = JSON.parse(JSON.stringify(evt))
    }

    // 通用事件
    nodeElem2.addEventListener('click', evtListener)
    nodeElem2.dispatchEvent(new config.Event('click'))

    // 属性对外暴露
    expect(evtJSON).to.have.property('type')
    expect(evtJSON).to.have.property('timeStamp')
    expect(evtJSON).to.have.property('target')
    expect(evtOrigin._type).to.equal(evtJSON.type)
    expect(evtOrigin._timeStamp).to.equal(evtJSON.timeStamp)
    expect(JSON.stringify(evtOrigin._target)).to.equal(JSON.stringify(evtJSON.target))

    // touch事件
    nodeElem2.addEventListener('touchstart', evtListener)
    nodeElem2.dispatchEvent(new config.TouchEvent('touchstart'))

    // 属性对外暴露
    expect(evtJSON).to.have.property('type')
    expect(evtJSON).to.have.property('timeStamp')
    expect(evtJSON).to.have.property('target')
    expect(evtJSON).to.have.property('touches')
    expect(evtJSON).to.have.property('changedTouches')
  })
})
