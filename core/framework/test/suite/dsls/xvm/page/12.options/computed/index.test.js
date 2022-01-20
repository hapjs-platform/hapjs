/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../imports'

describe('框架：12.computed', () => {
  let pageId, page, pageVm, oriConsoleWarn
  const warnMsgs = []

  before(() => {
    oriConsoleWarn = console.warn

    console.warn = function(msg) {
      warnMsgs.push(msg)
    }
  })

  after(() => {
    console.warn = oriConsoleWarn
  })

  beforeEach(() => {
    warnMsgs.splice(0)

    initPage((pageId = uniqueId()), null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  afterEach(() => {
    global.destroyPage(pageId)
  })

  it('基本用法', async () => {
    const el = pageVm.$element('fullName')

    // 初始化数据
    expect(pageVm.firstName).to.equal('hello')
    expect(pageVm.lastName).to.equal('world')
    expect(pageVm.fullName).to.equal('hello world')
    expect(el.attr.value).to.equal('hello world')

    // 更改依赖
    pageVm.firstName = 'hello2'
    expect(pageVm.fullName).to.equal('hello2 world')
    await waitForOK()
    expect(el.attr.value).to.equal('hello2 world')
  })

  it('使用setter', async () => {
    const el = pageVm.$element('reverseMsg')

    // 初始化数据
    expect(pageVm.message).to.equal('hi jack')
    expect(pageVm.reverseMessage).to.equal('kcaj ih')
    expect(el.attr.value).to.equal('kcaj ih')

    // 更改依赖
    pageVm.message = 'hi jack 2'
    expect(pageVm.reverseMessage).to.equal('2 kcaj ih')
    await waitForOK()
    expect(el.attr.value).to.equal('2 kcaj ih')

    // 使用setter
    pageVm.reverseMessage = '3 kcaj ih'
    expect(pageVm.message).to.equal('hi jack 3')
    expect(pageVm.reverseMessage).to.equal('3 kcaj ih')
    await waitForOK()
    expect(el.attr.value).to.equal('3 kcaj ih')
  })

  it('computed属性传递给子组件', async () => {
    const subVm = pageVm.$child('part1')
    const el = subVm.$element('el')
    const el2 = subVm.$element('el2')

    // 初始化数据
    expect(pageVm.initData).to.equal('data')
    expect(subVm.relayData).to.equal('relay: data')
    expect(subVm.relayRelayData).to.equal('relay: relay: data')
    expect(el.attr.value).to.equal('relay: data')
    expect(el2.attr.value).to.equal('relay: relay: data')

    // 更改依赖
    pageVm.initData = 'data2'
    await waitForOK()
    expect(subVm.relayData).to.equal('relay: data2')
    expect(subVm.relayRelayData).to.equal('relay: relay: data2')
    await waitForOK()
    expect(el.attr.value).to.equal('relay: data2')
    expect(el2.attr.value).to.equal('relay: relay: data2')
  })

  it('计算属性缓存', async () => {
    // 第一次不走get，返回undefined，不会执行computed属性的函数。
    expect(pageVm.getCount()).to.equal(0)

    // num不变。第一次读取执行watcher.evalute()，会执行computed属性的函数。
    pageVm.num = 0
    // eslint-disable-next-line
    const updatedNum1 = pageVm.updatedNum
    expect(pageVm.getCount()).to.equal(1)

    // num改变，count增加
    pageVm.num = 1
    // eslint-disable-next-line
    const updatedNum2 = pageVm.updatedNum
    expect(pageVm.getCount()).to.equal(2)

    // num不变，count不变
    pageVm.num = 1
    // eslint-disable-next-line
    const updatedNum3 = pageVm.updatedNum
    expect(pageVm.getCount()).to.equal(2)
  })

  it('错误提示', async () => {
    expect(warnMsgs.length).to.equal(10)

    // computed 提示
    expect(warnMsgs[0]).to.include('computed为1050版本中新增的计算属性，不再当做Vm方法')
    expect(warnMsgs[1]).to.include('请使用其它名称，后续版本不再兼容')

    // 定义同名method
    expect(warnMsgs[2]).to.include('repeatWidthMethod')

    // 保留字
    expect(warnMsgs[3]).to.include('show')

    // 已定义同名data
    expect(warnMsgs[4]).to.include('repeatWidthData')

    // 没有getter
    expect(warnMsgs[5]).to.include('noGetter')

    // errorAttr
    expect(warnMsgs[6]).to.include('errorAttr')

    // computed 提示
    expect(warnMsgs[7]).to.include('computed为1050版本中新增的计算属性，不再当做Vm方法')
    expect(warnMsgs[8]).to.include('请使用其它名称，后续版本不再兼容')

    // 已定义同名props
    expect(warnMsgs[9]).to.include('repeatWidthProps')

    // 没有setter
    pageVm.noSetter = ''
    expect(warnMsgs[10]).to.include('noSetter')
  })
})
