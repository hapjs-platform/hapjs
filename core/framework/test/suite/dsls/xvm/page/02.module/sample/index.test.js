/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../imports'

describe('框架：02.native接口', () => {
  const pageId = uniqueId()
  let page, pageVm, modRet

  before(() => {
    callActionJsonList.splice(0)

    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {
    callActionJsonList.splice(0)

    global.destroyPage(pageId)
  })

  beforeEach(() => {})

  afterEach(() => {})

  it('引入system接口', function() {
    // 具体接口为对象，且拥有方法等
    expect(pageVm.system.router).to.be.an('object')
    expect(Object.keys(pageVm.system.router).length).to.not.equal(0)
    // 具体接口为对象，且拥有方法等
    expect(pageVm.system.sample).to.be.an('object')
    expect(Object.keys(pageVm.system.sample).length).to.not.equal(0)
  })

  it('接口的各类方法处理', async () => {
    const sample = pageVm.sample

    let count = 0

    function fnCounter() {
      count++
    }

    // 同步
    modRet = sample.methodSync1()
    expect(Object.keys(modRet).length).to.above(0)

    modRet = sample.methodSync2()
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
    // expect(count).to.equal(7)

    // 多订阅
    modRet = sample.methodSubscribe2(fnCounter)
    await waitForOK()
    expect(modRet).to.equal(undefined)
    expect(count).to.above(7)

    // 取消订阅
    modRet = sample.methodUnsubscribe1()

    // 订阅
    modRet = sample.methodSubscribe1({ attr1: 'v1' })
    expect(modRet).to.equal(undefined)

    // 取消订阅
    modRet = sample.methodUnsubscribe1()
    modRet = sample.methodUnsubscribe2()

    // 同步接口返回接口实例
    const metaData1 = { name: 'system.sample', instId: 1, _nativeType: 0 }
    modRet = sample.methodBindInstSync1({ _data: metaData1 })
    Object.keys(sample).forEach(key => {
      expect(modRet).to.have.property(key)
    })
    // 调用接口方法时，底层应该收到上面的moduleInstId
    modRet.methodBindInstSync1({
      _data: Object.assign(metaData1, { moduleInstId: metaData1.instId })
    })
    modRet.methodBindInstCallback1({
      _data: Object.assign(metaData1, { moduleInstId: metaData1.instId })
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
    const rw = pageVm.opsTestReadwrite()
    expect(rw).to.equal('readwrite-v2')
    const rwv = pageVm.opsTestAttrReadwrite()
    expect(rwv).to.equal('readwrite-v2')

    // 只读
    const ro = pageVm.opsTestReadonly()
    const rv = pageVm.opsTestAttrReadonly()
    expect(ro).to.equal(rv)

    // 只写
    const wo = pageVm.opsTestWriteonly()
    expect(wo).to.equal(undefined)
    const wv = pageVm.opsTestAttrWriteonly()
    expect(wv).to.equal('writeonly-v2')
  })

  it('接口的事件操作', async () => {
    // eslint-disable-next-line
    let ret

    // 赋值：函数
    ret = pageVm.opsTestCustomEvent1()
    await waitForOK()
    expect(typeof pageVm.sample.onCustomEvent1).to.equal('function')
    expect(pageVm.onCustomEvent1Result).to.equal('onCustomEvent1')
    pageVm.onCustomEvent1Result = undefined

    // 清空
    ret = pageVm.opsTestCustomEvent1Clear()
    await waitForOK()
    expect(pageVm.sample.onCustomEvent1).to.equal(null)
    expect(pageVm.onCustomEvent1Result).to.equal(undefined)

    // 赋值：函数
    ret = pageVm.opsTestCustomEvent1()
    await waitForOK()
    expect(typeof pageVm.sample.onCustomEvent1).to.equal('function')
    expect(pageVm.onCustomEvent1Result).to.equal('onCustomEvent1')
    pageVm.onCustomEvent1Result = undefined

    // 赋值：忽略对象等非合法值，等于之前的赋值
    ret = pageVm.opsTestCustomEvent1Object()
    expect(typeof pageVm.sample.onCustomEvent1).to.equal('function')
  })

  it('接口调用中原始数据类型', async () => {
    const sample = pageVm.sample

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

  it('接口的回调函数中this调整为undefined', async () => {
    const sample = pageVm.sample

    // 1040及以后的回调中如果用到this调用，会报错
    const fnWithThis = function() {
      this.anyAttr = undefined
    }
    modRet = sample.methodCallback1({
      success: fnWithThis
    })
    await waitForOK()
  })

  it('接口的参数为undefined', async () => {
    const sample = pageVm.sample
    modRet = sample.methodSync1(undefined)
    await waitForOK()
  })

  it('模块类，支持new Class1()生成模块实例', async () => {
    // class1 = new Class1()，其中，class1为模块Class1的实例
    const Class1 = pageVm.Class1

    Class1.methodClass1()
    expect(Class1.methodInst1).to.equal(undefined)

    // 实例
    const inst1 = new pageVm.Class1()
    expect(global.moduleInstId).to.equal(undefined)
    const inst2 = new pageVm.Class1()
    expect(global.moduleInstId).to.equal(undefined)

    // 实例方法
    expect(inst1.methodClass1).to.equal(undefined)
    expect(inst2.methodClass1).to.equal(undefined)

    inst1.methodInst1()
    expect(global.moduleInstId).to.not.equal(undefined)
    inst1.instId = global.moduleInstId
    inst2.methodInst1()
    expect(global.moduleInstId).to.not.equal(undefined)
    inst2.instId = global.moduleInstId
    expect(inst2.instId).to.not.equal(inst1.instId)
    global.moduleInstId = undefined

    // 实例属性
    inst1.readwrite = 1
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.readwrite = 2
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    // eslint-disable-next-line
    const inst1Readwrite = inst1.readwrite
    expect(global.moduleInstId).to.equal(inst1.instId)
    // eslint-disable-next-line
    const inst2Readwrite = inst2.readwrite
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    // 实例事件
    inst1.onCustomEvent1 = function() {}
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.onCustomEvent1 = function() {}
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    await waitForOK()
  })

  it('接口:this', async () => {
    const Class1 = pageVm.Class1
    const obj = { name: 'obj' }

    Class1.methodClass1()
    // 实例
    const inst1 = new pageVm.Class1()
    expect(global.moduleInstId).to.equal(undefined)
    const inst2 = new pageVm.Class1()
    expect(global.moduleInstId).to.equal(undefined)

    // 方法：common
    const inst1Mth1 = inst1.methodInst1
    const inst2Mth1 = inst2.methodInst1
    expect(global.moduleInstId).to.equal(undefined)
    inst1Mth1()
    expect(global.moduleInstId).to.not.equal(undefined)
    inst1.instId = global.moduleInstId
    inst2Mth1()
    expect(global.moduleInstId).to.not.equal(undefined)
    inst2.instId = global.moduleInstId
    expect(inst2.instId).to.not.equal(inst1.instId)
    global.moduleInstId = undefined

    // 方法：bind无效
    inst1.methodInst1.bind(obj)()
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.methodInst1.bind(obj)()
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    // 方法：call无效
    inst1.methodInst1.call(obj)
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.methodInst1.call(obj)
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    // 属性：common
    // eslint-disable-next-line
    const inst1Readwrite = inst1.readwrite
    expect(global.moduleInstId).to.equal(inst1.instId)
    // eslint-disable-next-line
    const inst2Readwrite = inst2.readwrite
    expect(global.moduleInstId).to.equal(inst2.instId)
    inst1.readwrite = 1
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.readwrite = 2
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    // 事件：正常写法，无call与bind
    inst1.onCustomEvent1 = function(res) {
      expect(this._instId).to.equal(inst1.instId)
    }
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.onCustomEvent1 = function(res) {
      expect(this._instId).to.equal(inst2.instId)
    }
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined
    await waitForOK()

    // 事件：bind有效
    inst1.onCustomEvent1 = function(res) {
      expect(this.name).to.equal(obj.name)
    }.bind(obj)
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.onCustomEvent1 = function(res) {
      expect(this.name).to.equal(obj.name)
    }.bind(obj)
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    await waitForOK()

    // 事件：call有效
    inst1.onCustomEvent1.call(obj)
    inst2.onCustomEvent1.call(obj)
  })

  it('支持module.create()创建模块实例', async () => {
    // ws = websocketfactory.create({})，其中ws为模块websockt的实例
    const websocketfactory = pageVm.websocketfactory
    const inst1 = websocketfactory.create()
    expect(global.moduleInstId).to.equal(undefined)
    const inst2 = websocketfactory.create()
    expect(global.moduleInstId).to.equal(undefined)

    // 实例方法
    inst1.methodInstance()
    expect(global.moduleInstId).to.not.equal(undefined)
    inst1.instId = global.moduleInstId
    inst2.methodInstance()
    expect(global.moduleInstId).to.not.equal(undefined)
    inst2.instId = global.moduleInstId
    expect(inst2.instId).to.not.equal(inst1.instId)
    global.moduleInstId = undefined

    // 实例属性
    inst1.readwrite = 1
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.readwrite = 2
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    // eslint-disable-next-line
    const inst1Readwrite = inst1.readwrite
    expect(global.moduleInstId).to.equal(inst1.instId)
    // eslint-disable-next-line
    const inst2Readwrite = inst2.readwrite
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    // 实例事件
    inst1.onCustomEvent1 = function() {}
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.onCustomEvent1 = function() {}
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    await waitForOK()
  })

  it('支持广告多回调', async () => {
    const ad = pageVm.ad
    const inst1 = ad.createBannerAd({ adUnitId: 1 })
    expect(global.moduleInstId).to.equal(undefined)
    const inst2 = ad.createBannerAd({ adUnitId: 2 })
    expect(global.moduleInstId).to.equal(undefined)

    // 实例方法
    inst1.methodSyncSimple()
    expect(global.moduleInstId).to.not.equal(undefined)
    inst1.instId = global.moduleInstId
    inst2.methodSyncSimple()
    expect(global.moduleInstId).to.not.equal(undefined)
    inst2.instId = global.moduleInstId
    expect(inst2.instId).to.not.equal(inst1.instId)
    global.moduleInstId = undefined

    // 实例属性
    inst1.readwrite.subAttr1 = 'subAttrVal1'
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.readwrite.subAttr1 = 'subAttrVal2'
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    // eslint-disable-next-line
    const inst1Readwrite = inst1.readwrite.subAttr1
    expect(global.moduleInstId).to.equal(inst1.instId)
    const inst2Readwrite = inst2.readwrite.subAttr1
    expect(global.moduleInstId).to.equal(inst2.instId)
    expect(inst2Readwrite).to.equal('subAttrVal2')
    global.moduleInstId = undefined

    // 实例事件
    const cb1 = function() {}
    const cb2 = function() {}
    inst1.methodEventMulti(cb1)
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.methodEventMulti(cb2)
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    inst1.removeEventSyncMulti(cb1)
    expect(global.moduleInstId).to.equal(inst1.instId)
    inst2.removeEventSyncMulti()
    expect(global.moduleInstId).to.equal(inst2.instId)
    global.moduleInstId = undefined

    await waitForOK()
  })

  // 这项测试必须作为最后一项：因为需要做销毁页面的测试
  it('在页面销毁后调用接口方法', async () => {
    // 销毁页面
    global.destroyPage(pageId)

    pageVm.sample.methodSync1()
    pageVm.sample.methodCallback1({ success: new Function() })
    await waitForOK()
  })
})
