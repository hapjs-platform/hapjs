/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../imports'

describe('框架：05.自定义子组件', () => {
  const warnMsgs = []
  const pageId = uniqueId()
  let page, pageVm
  let oriConsoleWarn

  before(() => {
    oriConsoleWarn = console.warn
    console.warn = function(msg) {
      warnMsgs.push(msg)
    }
    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {
    global.destroyPage(pageId)
    console.warn = oriConsoleWarn
  })

  beforeEach(() => {})

  afterEach(() => {})

  it('通过$listeners触发高阶事件', () => {
    const reset = () => {
      pageVm.evt1Detail = pageVm.evt2Detail = pageVm.evt3Detail = pageVm.evt4Detail = null
      expect(pageVm.evt1Detail).to.equal(null)
      expect(pageVm.evt2Detail).to.equal(null)
      expect(pageVm.evt3Detail).to.equal(null)
      expect(pageVm.evt4Detail).to.equal(null)
    }

    expect(warnMsgs[0])
      .to.include('忽略使用')
      .include('自定义组件')
      .include('DOM事件绑定')

    const part1Vm = pageVm.$child('vmInst1')
    part1Vm.trigger()
    expect(pageVm.evt1Detail).to.equal('evt1-part1')
    expect(pageVm.evt2Detail).to.equal('evt2-part1')
    expect(warnMsgs[2])
      .to.include('子组件')
      .include('父组件不存在')
      .include('handleEvent4')
    reset()

    const part2Vm = part1Vm.$child('vmInst2')
    part2Vm.trigger()
    expect(pageVm.evt1Detail).to.equal('evt1-part2')
    expect(pageVm.evt2Detail).to.equal('evt2-part2')
    expect(part1Vm.evt1Detail).to.equal('evt1-part2')
    expect(part1Vm.evt2Detail).to.equal('evt2-part2')
    expect(warnMsgs[3])
      .to.include('子组件')
      .include('父组件不存在')
      .include('handleEvent4')
    expect(warnMsgs[4])
      .to.include('子组件')
      .include('高阶组件不存在')
      .include('handleEvent4')
  })
})
