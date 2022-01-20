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

  it('绑定事件，并传递参数', () => {
    const reset = () => {
      pageVm.evt1Params = pageVm.evt1Detail = pageVm.evt2Detail = null
      expect(pageVm.evt1Params).to.equal(null)
      expect(pageVm.evt1Detail).to.equal(null)
      expect(pageVm.evt2Detail).to.equal(null)
    }

    pageVm.$child('vmInst1').trigger()
    expect(pageVm.evt1Params).to.equal('hi')
    expect(pageVm.evt1Detail).to.equal('evt1-v1')
    expect(pageVm.evt2Detail).to.equal('evt2-v1')
    reset()

    pageVm.$child('vmInst2').trigger()
    expect(pageVm.evt1Params).to.equal('hi')
    expect(pageVm.evt1Detail).to.equal('evt1-v1')
    expect(pageVm.evt2Detail).to.equal('evt2-v1')
    reset()

    pageVm.$child('vmInst3').trigger()
    expect(pageVm.evt1Params).to.equal('test')

    pageVm.$child('vmInst4').triggerTwice()
    expect(pageVm.count).to.equal(2)
  })
})
