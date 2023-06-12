/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../imports'

describe('框架: 01.框架错误捕获测试', () => {
  const pageId = uniqueId()
  let page, pageVm, app

  before(() => {
    callActionJsonList.splice(0)

    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
    app = page.app
  })

  after(() => {
    callActionJsonList.splice(0)

    global.destroyPage(pageId)
  })

  function resetErrorStatus() {
    pageVm[`page.onErrorCaptured`] = undefined
    pageVm[`page.onErrorCaptured.err`] = undefined
    pageVm[`page.onErrorCaptured.vm`] = undefined
    pageVm[`page.onErrorCaptured.info`] = undefined
    pageVm[`plugin.onErrorCaptured`] = undefined
    pageVm[`plugin.onErrorCaptured.err`] = undefined
    pageVm[`plugin.onErrorCaptured.vm`] = undefined
    pageVm[`plugin.onErrorCaptured.info`] = undefined
    app.resetAppProp()
  }

  it('page 生命周期回调错误捕获', () => {
    // 页面 onErrorCaptured 错误捕获
    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal('onInit error')
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(pageVm)
    expect(pageVm[`page.onErrorCaptured.info`]).to.equal('page/component: lifecycle for "onInit"')
    // 测试插件混入的 onErrorCaptured 生命周期
    expect(pageVm[`plugin.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`plugin.onErrorCaptured.err`]).to.equal('onInit error')
    expect(pageVm[`plugin.onErrorCaptured.vm`]).to.equal(pageVm)
    expect(pageVm[`plugin.onErrorCaptured.info`]).to.equal('page/component: lifecycle for "onInit"')
    // 冒泡到全局
    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal('onInit error')
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(pageVm)
    expect(app[`app.onErrorHandler.info`][0]).to.equal('page/component: lifecycle for "onInit"')

    resetErrorStatus()
  })

  it('app 生命周期回调错误捕获', () => {
    app._emit(`applc:onShow`)

    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal('onShow error')
    // 此场景下无 vm 对象
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.info`][0]).to.equal('app: lifecycle for "onShow"')

    resetErrorStatus()
  })

  it('data() 错误捕获', async () => {
    pageVm.changeComponentStatus(1, true)
    await waitForOK()

    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal('data() error')
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(pageVm._childrenVms[0])
    expect(pageVm[`page.onErrorCaptured.vm`]._type).to.equal('part1')
    expect(pageVm[`page.onErrorCaptured.info`]).to.equal('data()')

    pageVm.changeComponentStatus(1, false)
    resetErrorStatus()
  })

  it('watcher 表达式错误捕获', () => {
    // 触发 computed
    console.trace(pageVm.errorComputed)
    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal('watcher expression error')
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(pageVm)
    expect(pageVm[`page.onErrorCaptured.info`]).to.includes('getter for watcher')

    resetErrorStatus()
  })

  it('watcher 回调错误捕获', async () => {
    pageVm.changeTitle('watcher 回调错误捕获')
    await waitForOK()

    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal('watcher callback error')
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(pageVm)
    expect(pageVm[`page.onErrorCaptured.info`]).to.equal('callback for watcher "title"')

    resetErrorStatus()
  })

  it('$nextTick 回调错误捕获', () => {
    // 模拟引擎触发 nextTick 回调
    global.processRenderHooks(pageId, 'updateFinish')

    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal('$nextTick callback error')
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(pageVm)
    expect(pageVm[`page.onErrorCaptured.info`]).to.includes('callback for nextTick')

    resetErrorStatus()
  })

  it('自定义指令回调错误捕获', () => {
    const node = pageVm.$element('dir-text')
    const ref = node.ref
    setCustomDirective(node, pageVm, ref)
    // 模拟引擎触发自定义指令回调
    global.processRenderHooks(pageId, 'nodeMounted', { ref })

    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal('custom directive callback error')
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(pageVm)
    expect(pageVm[`page.onErrorCaptured.info`]).to.equal('dir:test "mounted" hook')

    resetErrorStatus()

    // 当前 hap-toolkit 版本不支持自定义指令，需手动挂载
    function setCustomDirective(node, pageVm, ref) {
      if (node._directives || pageVm._directivesContext[ref]) return

      const dirObj = {
        mounted() {
          throw new Error('custom directive callback error')
        }
      }
      const dirName = 'test'
      node._directives = [
        {
          callbacks: dirObj,
          name: dirName,
          useDynamic: false,
          value: '自定义指令'
        }
      ]

      pageVm._directivesContext[ref] = {
        [dirName]: pageVm
      }
    }
  })

  it('组件事件回调错误捕获', () => {
    const node = pageVm.$element('click-btn')
    // 模拟引擎触发点击事件
    global.processCallbacks(pageId, [
      {
        action: 1,
        args: [node.ref, 'click', {}, null]
      }
    ])

    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal('component event callback error')
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(pageVm)
    expect(pageVm[`page.onErrorCaptured.info`]).to.equal('component: event handler for "click"')

    resetErrorStatus()
  })

  it('组件方法回调错误捕获', () => {
    const callbackId = 'customId'
    const cb = function() {
      throw new Error(`component method callback error`)
    }
    cb.cbErrorData = {
      vm: pageVm,
      info: 'callback for componentMethods'
    }
    page._callbacks[callbackId] = cb
    // 模拟引擎触发组件方法回调
    global.processCallbacks(pageId, [
      {
        action: 2,
        args: [callbackId, []]
      }
    ])

    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal('component method callback error')
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(pageVm)
    expect(pageVm[`page.onErrorCaptured.info`]).to.equal(`callback for componentMethods "${cb}"`)

    resetErrorStatus()
    delete page._callbacks[callbackId]
  })

  it('接口方法回调错误捕获', async () => {
    const sample = pageVm.sample

    function interfaceCb(errInfo) {
      throw new Error(`"${errInfo}" callback for sample`)
    }

    // 测试 success 回调
    sample.methodCallback1({ success: () => interfaceCb('success') })
    await waitForOK()

    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal('"success" callback for sample')
    // 此场景下无 vm 对象
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.info`][0]).to.equal(
      'system.sample: "success/callback" callback of "methodCallback1"'
    )
    resetErrorStatus()

    // 测试 callback 回调
    sample.methodCallback1({ callback: () => interfaceCb('callback') })
    await waitForOK()

    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal('"callback" callback for sample')
    // 此场景下无 vm 对象
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.info`][0]).to.equal(
      'system.sample: "success/callback" callback of "methodCallback1"'
    )
    resetErrorStatus()

    // 测试 fail 回调
    sample.methodCallback1({
      _code: 300,
      fail: () => interfaceCb('fail')
    })
    await waitForOK()

    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal('"fail" callback for sample')
    // 此场景下无 vm 对象
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.info`][0]).to.equal(
      'system.sample: "fail" callback of "methodCallback1"'
    )
    resetErrorStatus()

    // 测试 complete 回调
    sample.methodCallback1({ complete: () => interfaceCb('complete') })
    await waitForOK()

    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal('"complete" callback for sample')
    // 此场景下无 vm 对象
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.info`][0]).to.equal(
      'system.sample: "complete" callback of "methodCallback1"'
    )
    resetErrorStatus()
  })

  it('$emit 回调错误捕获', async () => {
    pageVm.changeComponentStatus(2, true)
    await waitForOK()

    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal('$emit error')
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(pageVm._childrenVms[0])
    expect(pageVm[`page.onErrorCaptured.vm`]._type).to.equal('part2')
    expect(pageVm[`page.onErrorCaptured.info`]).to.equal(
      'page/component: event handler for "testEmit"'
    )

    pageVm.changeComponentStatus(2, false)
    resetErrorStatus()
  })

  it('定时器回调错误捕获', async () => {
    // 测试 setTimeout
    pageVm.timeout()
    await waitForOK()

    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal('setTimeout error')
    // 此场景下无 vm 对象
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.info`][0]).to.includes('callback for setTimeout')
    resetErrorStatus()

    // 测试 setInterval
    pageVm.interval()
    await waitForOK()

    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal('setInterval error')
    // 此场景下无 vm 对象
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.info`][0]).to.includes('callback for setInterval')
    resetErrorStatus()

    // 测试 requestAnimationFrame
    pageVm.requestAnimationFrame()
    await waitForOK()

    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal('requestAnimationFrame error')
    // 此场景下无 vm 对象
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.info`][0]).to.includes('callback for requestAnimationFrame')
    resetErrorStatus()
  })

  it('错误的捕获与冒泡', async () => {
    pageVm.changeComponentStatus(3, true)
    await waitForOK()

    const curVm = pageVm._childrenVms[0]

    // 测试正常冒泡
    curVm.changeTitle('错误冒泡')
    await waitForOK()

    // 组件本身捕获
    expect(curVm[`part3.onErrorCaptured`]).to.equal(true)
    expect(curVm[`part3.onErrorCaptured.err`]).to.equal(
      'part3: watcher callback error, isBubble: true'
    )
    expect(curVm[`part3.onErrorCaptured.vm`]).to.equal(curVm)
    expect(curVm[`part3.onErrorCaptured.vm`]._type).to.equal('part3')
    expect(curVm[`part3.onErrorCaptured.info`]).to.equal('callback for watcher "title"')
    // 错误冒泡到页面
    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal(
      'part3: watcher callback error, isBubble: true'
    )
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(curVm)
    expect(pageVm[`page.onErrorCaptured.vm`]._type).to.equal('part3')
    expect(pageVm[`page.onErrorCaptured.info`]).to.equal('callback for watcher "title"')
    // 错误冒泡到全局
    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal(
      'part3: watcher callback error, isBubble: true'
    )
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(curVm)
    expect(app[`app.onErrorHandler.vm`][0]._type).to.equal('part3')
    expect(app[`app.onErrorHandler.info`][0]).to.equal('callback for watcher "title"')

    resetErrorStatus()
    resetPart3ErrorStatus()

    // 测试阻止冒泡
    curVm.changeBubbleMode(false)
    curVm.changeTitle('阻止错误冒泡')
    await waitForOK()

    // 组件本身捕获
    expect(curVm[`part3.onErrorCaptured`]).to.equal(true)
    expect(curVm[`part3.onErrorCaptured.err`]).to.equal(
      'part3: watcher callback error, isBubble: false'
    )
    expect(curVm[`part3.onErrorCaptured.vm`]).to.equal(curVm)
    expect(curVm[`part3.onErrorCaptured.vm`]._type).to.equal('part3')
    expect(curVm[`part3.onErrorCaptured.info`]).to.equal('callback for watcher "title"')
    // 阻止错误冒泡到页面
    expect(pageVm[`page.onErrorCaptured`]).to.equal(undefined)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal(undefined)
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(undefined)
    expect(pageVm[`page.onErrorCaptured.info`]).to.equal(undefined)
    // 阻止错误冒泡到全局
    expect(app[`app.onErrorHandler`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.err`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.info`][0]).to.equal(undefined)

    pageVm.changeComponentStatus(3, false)
    resetErrorStatus()
    resetPart3ErrorStatus()

    function resetPart3ErrorStatus() {
      curVm[`part3.onErrorCaptured`] = undefined
      curVm[`part3.onErrorCaptured.err`] = undefined
      curVm[`part3.onErrorCaptured.vm`] = undefined
      curVm[`part3.onErrorCaptured.vm`] = undefined
    }
  })

  it('async 回调错误捕获', async () => {
    pageVm.timeoutAsync()
    await waitForOK()

    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal('async setTimeout error')
    // 此场景下无 vm 对象
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.info`][0]).to.includes('callback for setTimeout')

    resetErrorStatus()
  })

  it('兼容卡片环境', async () => {
    const _oriEngine = global.Env.engine
    // 手动更改为卡片环境
    global.Env.engine = global.ENGINE_TYPE.CARD

    // 定时器回调场景
    pageVm.timeout()
    await waitForOK()

    // 触发页面的 onErrorCaptured
    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal('setTimeout error')
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(undefined)
    expect(pageVm[`page.onErrorCaptured.info`]).to.includes('callback for setTimeout')
    resetErrorStatus()

    // 接口回调场景
    const sample = pageVm.sample

    function interfaceCb(errInfo) {
      throw new Error(`"${errInfo}" callback for sample`)
    }

    sample.methodCallback1({ success: () => interfaceCb('success') })
    await waitForOK()

    // 触发页面的 onErrorCaptured
    expect(pageVm[`page.onErrorCaptured`]).to.equal(true)
    expect(pageVm[`page.onErrorCaptured.err`]).to.equal('"success" callback for sample')
    expect(pageVm[`page.onErrorCaptured.vm`]).to.equal(undefined)
    expect(pageVm[`page.onErrorCaptured.info`]).to.equal(
      'system.sample: "success/callback" callback of "methodCallback1"'
    )

    // 还原宿主环境
    global.Env.engine = _oriEngine
    resetErrorStatus()
  })

  it('onErrorCaptured 自身错误捕获', async () => {
    pageVm.throwError(true)
    pageVm.changeTitle('onErrorCaptured 自身错误')
    await waitForOK()

    // 捕获 onErrorCaptured 自身错误
    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal('onErrorCaptured error')
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(pageVm)
    expect(app[`app.onErrorHandler.info`][0]).to.equal(
      'page/component: lifecycle for "onErrorCaptured"'
    )
    // 同时 watcher 回调错误也能正常捕获
    expect(app[`app.onErrorHandler`][1]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][1]).to.equal('watcher callback error')
    expect(app[`app.onErrorHandler.vm`][1]).to.equal(pageVm)
    expect(app[`app.onErrorHandler.info`][1]).to.equal('callback for watcher "title"')

    pageVm.throwError(false)
    resetErrorStatus()
  })

  it('onErrorHandler 自身错误捕获', async () => {
    const _oriConsoleError = console.error

    // 重写 console.error
    console.error = function(...args) {
      if (args[0] === '### App Framework ### error: ') {
        expect(args[1].message).to.equal('onErrorHandler error')
      } else if (args[0] === '### App Framework ### vm of component: ') {
        expect(args[1]).to.equal(undefined)
      } else if (args[0] === '### App Framework ### error info: ') {
        expect(args[1]).to.equal('app: lifecycle for "onErrorHandler"')
      } else {
        _oriConsoleError(...args)
      }
    }

    app.isThrowError = true
    pageVm.timeout()
    await waitForOK()

    // 还原 console.error
    console.error = _oriConsoleError
    app.isThrowError = false
  })
})
