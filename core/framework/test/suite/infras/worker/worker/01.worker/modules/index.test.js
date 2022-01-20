/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initWorker, callActionJsonList } from '../../../imports'

/**
 * 注意：用例与主线程中的modules保持一致
 */
describe('框架：01.native接口', () => {
  const instId = uniqueId()
  let modRet, model

  before(() => {
    callActionJsonList.splice(0)

    initWorker(instId, __dirname)
    model = global.model
  })

  after(() => {
    callActionJsonList.splice(0)
  })

  beforeEach(() => {})

  afterEach(() => {})

  it('JS与Native的Bridge打通', async () => {
    await waitForOK()
    expect(global.onmessage).to.be.a('function')
    expect(global.onmessageerror).to.be.a('function')
    expect(global.msgObjectReq1).to.not.equal(null)
    expect(global.msgObjectReq1).to.deep.equal(global.msgObjectRes1)

    // 模拟错误
    const msgObjectErr1 = {
      message: '模拟worker错误对象',
      stack: new Error().stack
    }
    global.onMessageErrorInternal(JSON.stringify(msgObjectErr1))
    expect(global.msgObjectErr1).to.deep.equal(msgObjectErr1)
  })

  it('引入system接口', () => {
    // 具体接口为对象，且拥有方法等
    expect(model.system.router).to.be.an('object')
    expect(Object.keys(model.system.router).length).to.not.equal(0)
    // 具体接口为对象，且拥有方法等
    expect(model.system.sample).to.be.an('object')
    expect(Object.keys(model.system.sample).length).to.not.equal(0)
  })

  it('接口的各类方法处理', async () => {
    const sample = model.sample

    let count = 0
    function fnCounter() {
      count++
    }

    // 同步
    modRet = sample.methodSync1()
    expect(Object.keys(modRet).length).to.above(0)

    // 异步：callback
    modRet = sample.methodCallback1({ success: fnCounter })
    await waitForOK()
    expect(count).to.equal(1)

    // 异步：callback
    modRet = sample.methodCallback1({ callback: fnCounter })
    await waitForOK()
    expect(count).to.equal(2)

    // 异步：callback
    modRet = sample.methodCallback1({ complete: fnCounter })
    await waitForOK()
    expect(count).to.equal(3)

    // 异步：callback
    modRet = sample.methodCallback1({ success: fnCounter, fail: fnCounter })
    await waitForOK()
    expect(count).to.equal(4)

    // 异步：Promise
    modRet = sample.methodCallback1({})
    modRet.then(fnCounter, fnCounter)
    await waitForOK()
    expect(modRet).to.instanceof(Promise)
    expect(count).to.equal(5)

    // 异步：Promise
    modRet = sample.methodCallback1({ attr1: 'v1' })
    modRet.then(fnCounter, fnCounter)
    await waitForOK()
    expect(modRet).to.instanceof(Promise)
    expect(count).to.equal(6)

    // 订阅
    modRet = sample.methodSubscribe1({ callback: fnCounter })
    await waitForOK()
    expect(modRet).to.equal(undefined)
    expect(count).to.above(6)

    // 取消订阅
    modRet = sample.methodUnsubscribe1()

    // 订阅
    modRet = sample.methodSubscribe1({ attr1: 'v1' })
    expect(modRet).to.equal(undefined)

    // 取消订阅
    modRet = sample.methodUnsubscribe1()

    // 同步接口返回接口实例
    const metaDataSync1 = { name: 'system.sample', instId: 1, _nativeType: 0 }
    modRet = sample.methodBindInstSync1({ _data: metaDataSync1 })
    Object.keys(sample).forEach(key => {
      expect(modRet).to.have.property(key)
    })
    // 调用接口方法时，底层应该收到上面的moduleInstId
    modRet.methodBindInstSync1({
      _data: Object.assign(metaDataSync1, {
        moduleInstId: metaDataSync1.instId
      })
    })
    modRet.methodBindInstCallback1({
      _data: Object.assign(metaDataSync1, {
        moduleInstId: metaDataSync1.instId
      })
    })

    // 异步接口返回接口实例
    const metaDataCallback1 = {
      name: 'system.sample',
      instId: 2,
      _nativeType: 0
    }
    sample.methodBindInstCallback1({
      _data: metaDataCallback1,
      success: result => {
        modRet = result
      }
    })
    await waitForOK()
    Object.keys(sample).forEach(key => {
      expect(modRet).to.have.property(key)
    })
    // 调用接口方法时，底层应该收到上面的moduleInstId
    modRet.methodBindInstSync1({
      _data: Object.assign(metaDataCallback1, {
        moduleInstId: metaDataCallback1.instId
      })
    })
    modRet.methodBindInstCallback1({
      _data: Object.assign(metaDataCallback1, {
        moduleInstId: metaDataCallback1.instId
      })
    })
  })

  it('接口的属性操作', async () => {
    // 读写
    const rw = model.opsTestReadwrite()
    expect(rw).to.equal('readwrite-v2')
    const rwv = model.opsTestAttrReadwrite()
    expect(rwv).to.equal('readwrite-v2')

    // 只读
    const ro = model.opsTestReadonly()
    const rv = model.opsTestAttrReadonly()
    expect(ro).to.equal(rv)

    // 只写
    const wo = model.opsTestWriteonly()
    expect(wo).to.equal(undefined)
    const wv = model.opsTestAttrWriteonly()
    expect(wv).to.equal('writeonly-v2')
  })

  it('接口的事件操作', async () => {
    // eslint-disable-next-line
    let ret

    // 赋值：函数
    ret = model.opsTestCustomEvent1()
    await waitForOK()
    expect(typeof model.sample.onCustomEvent1).to.equal('function')
    expect(model.onCustomEvent1Result).to.equal('onCustomEvent1')
    model.onCustomEvent1Result = undefined

    // 清空
    ret = model.opsTestCustomEvent1Clear()
    await waitForOK()
    expect(model.sample.onCustomEvent1).to.equal(null)
    expect(model.onCustomEvent1Result).to.equal(undefined)

    // 赋值：函数
    ret = model.opsTestCustomEvent1()
    await waitForOK()
    expect(typeof model.sample.onCustomEvent1).to.equal('function')
    expect(model.onCustomEvent1Result).to.equal('onCustomEvent1')
    model.onCustomEvent1Result = undefined

    // 赋值：忽略对象等非合法值，等于之前的赋值
    ret = model.opsTestCustomEvent1Object()
    expect(typeof model.sample.onCustomEvent1).to.equal('function')
  })

  it('接口调用中原始数据类型', async () => {
    const sample = model.sample

    let callbackData = null
    const rawData1 = new Int8Array()
    const options1 = {
      // 传递过去
      _data: rawData1,
      success: function(data) {
        // 接收回来
        callbackData = data
      }
    }
    sample.methodCallback2(options1)
    await waitForOK()
    expect(callbackData).to.equal(rawData1)
  })
})
