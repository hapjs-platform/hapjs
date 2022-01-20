/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../imports'

describe('框架：01.页面以及框架初始化', () => {
  const pageId = uniqueId()
  let page, pageVm

  const intent = {
    action: 'view',
    uri: 'hap://app/com.application.demo/specified',
    orientation: 'portrait'
  }

  const meta = {
    name: 'vm/01.base/create',
    component: 'index',
    path: '/specified'
  }

  const query = {
    k1: 'v1'
  }

  before(() => {
    callActionJsonList.splice(0)

    initPage(pageId, null, __dirname, query, intent, meta)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {
    callActionJsonList.splice(0)

    global.destroyPage(pageId)
  })

  beforeEach(() => {})

  afterEach(() => {})

  it('应用信息正确', () => {
    // 应用级别样式存在
    expect(pageVm.$app.$def.style).to.not.equal(undefined)
  })

  it('页面的路由信息正确', () => {
    // intent信息
    expect(pageVm.$page.action).to.equal(intent.action)
    expect(pageVm.$page.uri).to.equal(intent.uri)
    expect(pageVm.$page.orientation).to.equal(intent.orientation)
    // meta信息
    expect(pageVm.$page.name).to.equal(meta.name)
    expect(pageVm.$page.component).to.equal(meta.component)
    expect(pageVm.$page.path).to.equal(meta.path)
    // 请求参数
    expect(pageVm.$page.query).to.equal(query)
  })

  it('数据初始化正确', () => {
    expect(pageVm.d1Str).to.equal('v1Str')
    // 请求参数
    expect(pageVm.query).to.equal(query)
  })

  it('DOM初始化正确', () => {
    const d1Comp = pageVm.$element('d1Id')

    expect(d1Comp.attr.value).to.equal('v1Str')
  })

  it('Action消息：发送正确', () => {
    expect(callActionJsonList.length).to.equal(3)
    expect(callActionJsonList[0]).to.include('createBody')
    expect(callActionJsonList[1]).to.include('v1Str')
    expect(callActionJsonList[2]).to.include('createFinish')
    callActionJsonList.splice(0)
  })

  it('Action消息：正则转换正确', async () => {
    pageVm.webInst1If = true

    await waitForOK()

    // 正则转换为对象：属性type标识类型，value为toString()
    expect(callActionJsonList[0]).to.include('{"type":"regexp","source":"reg","flags":"i"}')
    callActionJsonList.splice(0)
  })

  it('getPageRoot测试', () => {
    const pageRootStr = JSON.stringify(global.getPageRoot(pageId))
    expect(pageRootStr).to.include('children')
    expect(pageRootStr).to.include('v1Str')
    expect(pageRootStr).to.include('d1Id')
  })

  it('vm.$page功能', () => {
    const docPage = pageVm.$page

    const attr = { k1: 'v1' }
    docPage.setTitleBar(attr)

    // 强制生成Finish的DOM消息
    pageVm.$forceUpdate()

    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('updateTitleBar')
    expect(callActionJsonList[0]).to.include(JSON.stringify(attr))

    callActionJsonList.splice(0)

    docPage.setStatusBar(attr)
    // 强制生成Finish的DOM消息
    pageVm.$forceUpdate()
    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('updateStatusBar')
    expect(callActionJsonList[0]).to.include(JSON.stringify(attr))
    callActionJsonList.splice(0)

    docPage.exitFullscreen()

    // 强制生成Finish的DOM消息
    pageVm.$forceUpdate()

    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('exitFullscreen')

    callActionJsonList.splice(0)

    docPage.setSecure(true)

    // 强制生成Finish的DOM消息
    pageVm.$forceUpdate()

    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('setSecure')
    callActionJsonList.splice(0)
  })
})
