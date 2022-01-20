/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../imports'

describe('框架：05.自定义子组件', () => {
  const pageId = uniqueId()
  let page, pageVm

  before(() => {
    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {
    global.destroyPage(pageId)
  })

  beforeEach(() => {})

  afterEach(() => {})

  it('动态编译组件', async () => {
    let comp = pageVm.$child('comp')
    expect(comp).to.equal(undefined)

    pageVm.changeComponent()
    await waitForOK()
    comp = pageVm.$child('comp')
    comp.emitEvent()
    expect(pageVm.message).to.equal('part2')

    pageVm.changeComponent()
    await waitForOK()
    comp = pageVm.$child('comp')
    comp.emitEvent()
    expect(pageVm.message).to.equal('part1')

    pageVm.changeComponent()
    await waitForOK()
    comp = pageVm.$child('comp')
    comp.emitEvent()
    expect(pageVm.message).to.equal('part2')
  })
})
