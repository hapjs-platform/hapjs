/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../imports'

describe('框架：05.自定义子组件', () => {
  const warnMsgs = []
  const errorMsgs = []
  const pageId = uniqueId()
  let page, pageVm
  let oriConsoleWarn, oriConsoleError

  before(() => {
    oriConsoleWarn = console.warn
    oriConsoleError = console.error
    console.warn = function(msg) {
      warnMsgs.push(msg)
    }
    console.error = function(msg) {
      errorMsgs.push(msg)
    }

    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {
    global.destroyPage(pageId)

    console.warn = oriConsoleWarn
    console.error = oriConsoleError
  })

  beforeEach(() => {})

  afterEach(() => {})

  it('$attrs接收高阶组件的值', async () => {
    const subVmPart1 = pageVm.$child('vmInst1')
    const subVmPart2 = subVmPart1.$child('vmInst2')
    const subVmPart3 = subVmPart1.$child('vmInst3')

    const subVm1Elm1 = subVmPart1.$element('ip1')
    const subVm2Elm1 = subVmPart2.$element('ip1')
    const subVm2Elm2 = subVmPart2.$element('ip2')
    const subVm2Elm3 = subVmPart2.$element('ip3')
    const subVm2Elm4 = subVmPart2.$element('ip4')
    const subVm3Elm1 = subVmPart3.$element('ip1')
    const subVm3Elm2 = subVmPart3.$element('ip2')

    // part1
    expect(subVm1Elm1.value).to.equal('page-d1')

    expect(warnMsgs[0])
      .to.include('d1')
      .include('未在props定义')
      .include('$attrs')
    expect(warnMsgs[1])
      .to.include('d3')
      .include('未在props定义')
      .include('$attrs')
    expect(warnMsgs[2])
      .to.include('d5')
      .include('未在props定义')
      .include('$attrs')

    // part2
    expect(subVm2Elm1.value).to.equal('part1-v2')
    expect(subVm2Elm2.value).to.equal('page-d3')
    // 高阶组件变量名重名情况，取最近父组件值
    expect(subVm2Elm3.value).to.equal('part1-d5')
    expect(subVm2Elm4.value).to.equal('10')

    // part3
    expect(subVm3Elm1.value).to.equal('part1-v1')
    expect(subVm3Elm2.value).to.equal('part1-d5')

    // warn message
    expect(warnMsgs[3])
      .to.include('v2')
      .include('未在props定义')
      .include('$attrs')
    expect(warnMsgs[4])
      .to.include('d5')
      .include('未在props定义')
      .include('$attrs')
    expect(warnMsgs[5])
      .to.include('d1')
      .include('未在props定义')
      .include('$attrs')
    expect(warnMsgs[6])
      .to.include('d3')
      .include('未在props定义')
      .include('$attrs')
    expect(warnMsgs[7])
      .to.include('组件part3')
      .include('属性校验')
      .include('增加props属性')
    expect(warnMsgs[8])
      .to.include('data')
      .include('d5')
      .include('与props重复')
    expect(warnMsgs[9])
      .to.include('data')
      .include('v1')
      .include('与props重复')

    // 高阶组件修改值，深层级组件随之改变
    pageVm.d1 = 'page-d1-modify'
    pageVm.d3 = 'page-d3-modify'
    subVmPart1.v2 = 'part1-v2-modify'
    await waitForOK()
    // part1
    expect(subVm1Elm1.value).to.equal('page-d1-modify')
    // part2
    expect(subVm2Elm1.value).to.equal('part1-v2-modify')
    expect(subVm2Elm2.value).to.equal('page-d3-modify')

    expect(warnMsgs.length).to.equal(10)
    expect(errorMsgs.length).to.equal(0)
  })
})
