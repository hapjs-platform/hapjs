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

  it('props是object场合', async () => {
    // part1
    const subVmPart1 = pageVm.$child('vmInst1')
    expect(subVmPart1.id).to.equal('vmInst1')
    expect(subVmPart1.d1).to.equal('page-d1')
    expect(subVmPart1.d2.name).to.equal('page-d2')

    expect(warnMsgs[0])
      .to.include('part1')
      .include('无props')
    // part2
    const subVmPart2 = pageVm.$child('vmInst2')

    expect(subVmPart2.d1).to.equal('page-d1')
    expect(subVmPart2.d2.name).to.equal('page-d2')
    expect(subVmPart2.d3.name).to.equal('part2-default-d3')
    expect(subVmPart2.d4).to.equal(false)
    expect(subVmPart2.d5.name).to.equal('part2-default-d5')
    expect(subVmPart2.d6).to.equal(true)
    expect(subVmPart2.d7).to.equal('')
    expect(typeof subVmPart2.d8).to.equal('function')

    expect(errorMsgs[0])
      .to.include('part2')
      .include('d1')
      .include('type')
      .include('Number')
    expect(errorMsgs[1])
      .to.include('part2')
      .include('d2')
      .include('validator')
    expect(errorMsgs[2])
      .to.include('part2')
      .include('d3')
      .include('必填')
    expect(errorMsgs[3])
      .to.include('part2')
      .include('d4')
      .include('validator')
    expect(errorMsgs[4])
      .to.include('part2')
      .include('d5')
      .include('type')
      .include('String')

    // part3
    const subVmPart3 = pageVm.$child('vmInst3')
    expect(subVmPart3.d1).to.equal('page-d1')
    expect(subVmPart3.d2.name).to.equal('page-d2')
    expect(subVmPart3.d3.name).to.equal('page-d2-d3')
    expect(subVmPart3.d4.name).to.equal('page-d4')

    expect(warnMsgs[1])
      .to.include('d3')
      .include('不要与props重复')
    expect(warnMsgs[2])
      .to.include('d2')
      .include('不要与props重复')
    expect(warnMsgs[3])
      .to.include('d1')
      .include('不要与props重复')

    // 子组件中修改prop，不传递给父组件
    subVmPart3.d1 = 'part3-d1'
    expect(subVmPart3.d1).to.equal('part3-d1')
    expect(pageVm.d1).to.equal('page-d1')

    expect(errorMsgs[5])
      .to.include('part3')
      .include('禁止修改props')
      .include('d1')

    // 父组件修改prop，覆盖子组件的修改
    pageVm.d1 = 'page-d1-modify'
    await waitForOK()
    expect(pageVm.d1)
      .to.equal(subVmPart3.d1)
      .equal('page-d1-modify')
    // 动态修改数据，验证
    expect(errorMsgs[6])
      .to.include('part2')
      .include('d1')
      .include('type')
      .include('Number')

    expect(errorMsgs.length).to.equal(7)
    expect(warnMsgs.length).to.equal(4)
  })
})
