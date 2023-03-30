/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import fs from 'fs'
import path from 'path'

import { uniqueId, initPage, callActionJsonList } from '../../../imports'

// 收集依赖的JS缓存
function collectBundleChunkCache() {
  const chunkNameList = ['script0.js', 'script1.js', 'script2.js', 'script3.js']
  const chunkHash = {}
  chunkNameList.forEach(chunkName => {
    const chunkPath = path.join(__dirname, 'script', chunkName)
    chunkHash[chunkPath] = fs.readFileSync(chunkPath, {
      encoding: 'UTF-8'
    })
  })
  return chunkHash
}

describe('框架：01.页面以及VM的状态管理', () => {
  const pageId = uniqueId()
  let page, pageVm, VmClass, app

  before(() => {
    callActionJsonList.splice(0)

    // 注入依赖的JS缓存
    const chunkHash = collectBundleChunkCache()
    global.registerBundleChunks(chunkHash)

    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
    VmClass = pageVm.constructor
    app = page.app
  })

  after(() => {
    callActionJsonList.splice(0)

    suppressConsole(() => {
      global.destroyPage(pageId)
    })
  })

  beforeEach(() => {})

  afterEach(() => {})

  it(`插件的生命周期的绑定`, () => {
    expect(app[`plugin.onCreate`]).to.equal(true)

    expect(VmClass[`plugin.install`]).to.equal(true)
    expect(VmClass.demoPluginOptions).to.equal('installOptions')

    expect(pageVm[`plugin.onInit`]).to.equal(true)
    expect(pageVm[`plugin.onReady`]).to.equal(true)

    // test onError function
    const errParams = {
      stack: '',
      message: 'params-value'
    }
    global.notifyAppError(app.id, errParams)
    expect(app[`app.onError`]).to.equal(true)
    expect(app[`app.onError.params`].message).to.equal(errParams.message)

    expect(app[`app.onErrorHandler`][0]).to.equal(true)
    expect(app[`app.onErrorHandler.err`][0]).to.equal(errParams.message)
    // 此场景下无 vm 对象
    expect(app[`app.onErrorHandler.vm`][0]).to.equal(undefined)
    expect(app[`app.onErrorHandler.info`][0]).to.equal(errParams.message)

    expect(app[`plugin.onError`]).to.equal(true)
    // onErrorHandler 不支持插件混入
    expect(app[`plugin.onErrorHandler`]).to.equal(undefined)

    // test onPageNotFound function
    const errorPageParams = { uri: 'hap://app/com.application.demo/nowhereToBeFound' }
    global.notifyPageNotFound(app.id, errorPageParams)
    expect(app[`app.onPageNotFound`]).to.equal(true)
    expect(app[`app.onPageNotFound.params`]).to.equal(errorPageParams)
    expect(app[`plugin.onPageNotFound`]).to.equal(true)
  })

  it(`生命周期onRefresh的触发`, () => {
    // 全局函数
    const query = { k1: 'v1' }
    const intent = {}
    global.refreshPage(pageId, query, intent)

    // 刷新调用
    expect(pageVm.refreshQuery).to.equal(query)
    expect(pageVm.refreshIntent).to.equal(intent)

    // 兼容onRefresh作为Vm方法
    expect(typeof pageVm.onRefresh).to.equal('function')
  })

  it(`生命周期onBackPress的触发`, () => {
    // 全局函数
    const ret = global.backPressPage(pageId)

    expect(pageVm[`plugin.onBackPress`]).to.equal(true)
    expect(ret).to.equal(false)
  })

  it(`生命周期onMenuButtonPress的触发`, () => {
    // 全局函数
    const ret = global.menuButtonPressPage(pageId)

    expect(pageVm[`plugin.onMenuButtonPress`]).to.equal(true)
    expect(ret).to.equal(false)
  })

  it(`生命周期onMenuPress的触发`, () => {
    // 全局函数
    const ret = global.menuPressPage(pageId)

    expect(pageVm[`plugin.onMenuPress`]).to.equal(true)
    expect(ret).to.equal(true)
  })

  it(`生命周期onKey的触发`, () => {
    const keyOptions = { action: 0, code: 21, repeatCount: 0 }
    const ret = global.keyPressPage(pageId, keyOptions)
    expect(pageVm[`plugin.onKey`]).to.equal(true)
    expect(ret).to.equal(false)
  })

  it('页面中加载独立的JS文件', () => {
    // 页面中加载
    pageVm.testEvalInPage(`${__dirname}/script/script1.js`)
    const script1Ret = pageVm.testEval.getGlobalTestJsonp('script1')

    expect(script1Ret.loaded).to.equal(true)

    // 脚本中加载
    pageVm.testEval.requireInScript(`${__dirname}/script/script2.js`)
    const script2Ret = pageVm.testEval.getGlobalTestJsonp('script2')

    expect(script2Ret.loaded).to.equal(true)

    // 普通js文件
    pageVm.testEval.requireInScript(`${__dirname}/script/script3.js`)
    const script3Ret = pageVm.testEval.getGlobalTestJsonp('script3')
    // 依赖script2执行结果
    expect(script3Ret.loaded).to.equal(true)
  })

  it('属性$valid,$visbible', async () => {
    // 创建页面之后
    expect(page.$valid).to.equal(true)
    expect(page.$visible).to.equal(false)
    expect(pageVm.$valid).to.equal(true)
    expect(pageVm.$visible).to.equal(false)
    expect(pageVm.$child('vm1Id').$valid).to.equal(true)
    expect(pageVm.$child('vm1Id').$visible).to.equal(false)

    // 全局API：改变页面状态
    global.changeVisiblePage(page.id, true)

    // 生命周期
    expect(pageVm[`plugin.onShow`]).to.equal(true)

    expect(page.$valid).to.equal(true)
    expect(page.$visible).to.equal(true)
    expect(pageVm.$valid).to.equal(true)
    expect(pageVm.$visible).to.equal(true)
    expect(pageVm.$child('vm1Id').$valid).to.equal(true)
    expect(pageVm.$child('vm1Id').$visible).to.equal(true)

    // 新的自定义组件
    pageVm.toggleRender()
    await waitForOK()

    expect(pageVm.$child('vm2Id').$valid).to.equal(true)
    expect(pageVm.$child('vm2Id').$visible).to.equal(true)

    // 全局API：改变页面状态
    global.changeVisiblePage(page.id, false)

    // 生命周期
    expect(pageVm[`plugin.onHide`]).to.equal(true)

    expect(page.$valid).to.equal(true)
    expect(page.$visible).to.equal(false)
    expect(pageVm.$valid).to.equal(true)
    expect(pageVm.$visible).to.equal(false)
    expect(pageVm.$child('vm1Id').$valid).to.equal(true)
    expect(pageVm.$child('vm1Id').$visible).to.equal(false)
    expect(pageVm.$child('vm2Id').$valid).to.equal(true)
    expect(pageVm.$child('vm2Id').$visible).to.equal(false)

    // 全局API：改变页面状态
    global.changeVisiblePage(page.id, true)

    expect(page.$valid).to.equal(true)
    expect(page.$visible).to.equal(true)
    expect(pageVm.$valid).to.equal(true)
    expect(pageVm.$visible).to.equal(true)
    expect(pageVm.$child('vm1Id').$valid).to.equal(true)
    expect(pageVm.$child('vm1Id').$visible).to.equal(true)
    expect(pageVm.$child('vm2Id').$valid).to.equal(true)
    expect(pageVm.$child('vm2Id').$visible).to.equal(true)

    // 销毁自定义组件
    pageVm.toggleRender()
    await waitForOK()

    expect(pageVm.$child('vm2Id').$valid).to.equal(true)
    expect(pageVm.$child('vm2Id').$visible).to.equal(true)

    // 获取引用：销毁后无法通过父组件引用
    const subVm1 = pageVm.$child('vm1Id')
    const subVm2 = pageVm.$child('vm2Id')

    // 销毁页面
    global.destroyPage(page.id)

    // 生命周期
    expect(pageVm[`plugin.onDestroy`]).to.equal(true)

    expect(page.$valid).to.equal(false)
    expect(page.$visible).to.equal(false)
    expect(pageVm.$valid).to.equal(false)
    expect(pageVm.$visible).to.equal(false)
    expect(subVm1.$valid).to.equal(false)
    expect(subVm1.$visible).to.equal(false)
    expect(subVm2.$valid).to.equal(false)
    expect(subVm2.$visible).to.equal(false)
  })
})
