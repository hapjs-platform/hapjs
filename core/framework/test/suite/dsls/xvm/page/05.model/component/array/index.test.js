/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
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

  it('props是array场合', async () => {
    // part1
    const subVmPart1 = pageVm.$child('vmInst1')

    expect(subVmPart1.d1).to.equal('page-d1')
    expect(subVmPart1.d2.name).to.equal('page-d2')
    expect(subVmPart1.d3).to.equal(undefined)
    expect(subVmPart1.d4.name).to.equal('page-d4')
    expect(subVmPart1.id).to.equal(undefined)
    expect(subVmPart1.tid).to.equal('name')
    expect(subVmPart1.show).to.equal('true')

    expect(warnMsgs[0])
      .to.include('part1')
      .include('tid')
      .include('保留字')
    expect(warnMsgs[1])
      .to.include('part1')
      .include('show')
      .include('保留字')
    expect(warnMsgs[2])
      .to.include('class属性')
      .include('part1-class')

    // part2
    const subVmPart2 = pageVm.$child('vmInst2')

    expect(subVmPart2.d1).to.equal('page-d1')
    expect(subVmPart2.d2.name).to.equal('page-d2')
    expect(subVmPart2.d3.name).to.equal('page-d2-d3')

    expect(warnMsgs[3])
      .to.include('d3')
      .include('不要与props重复')
    expect(warnMsgs[4])
      .to.include('d2')
      .include('不要与props重复')
    expect(warnMsgs[5])
      .to.include('d1')
      .include('不要与props重复')

    // 子组件中修改prop，不传递给父组件
    subVmPart2.d4 = 'part2-d4'
    expect(pageVm.d4.name).to.equal('page-d4')
    expect(subVmPart2.d4).to.equal('part2-d4')
    expect(errorMsgs[0])
      .to.include('part2')
      .include('禁止修改props')
      .include('d4')

    // 父组件修改prop，覆盖子组件的修改
    pageVm.d4 = { name: 'page-d4-modify' }
    await waitForOK()
    expect(pageVm.d4.name)
      .to.equal(subVmPart2.d4.name)
      .equal('page-d4-modify')

    expect(warnMsgs.length).to.equal(6)
    expect(errorMsgs.length).to.equal(1)
  })
})
