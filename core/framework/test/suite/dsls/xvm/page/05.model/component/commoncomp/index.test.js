/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../imports'
import { expect } from 'chai'

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

  it('使用公共组件', async () => {
    const subVmPart = pageVm.$child('part2')

    expect(subVmPart.id).to.equal('part2')
    expect(subVmPart.d1).to.equal('comp')
  })

  it('页面和公共组件同时注册', () => {
    const subVmPart = pageVm.$child('part1')

    expect(subVmPart.id).to.equal('part1')
    expect(subVmPart.sign).to.equal('页面级别组件')
  })
})
