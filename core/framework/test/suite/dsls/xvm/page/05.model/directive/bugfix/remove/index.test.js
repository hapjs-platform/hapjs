/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../../imports'

describe('框架：04.指令渲染', () => {
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

  it('更新数据时删除DOM子节点', async () => {
    pageVm.changeData()
    await waitForOK()
    expect(pageVm.v1).to.equal(false)
  })

  it('删除DOM节点后调用组件方法不报错', async () => {
    pageVm.v3 = false
    await waitForOK()
    pageVm.$element('compId1').scrollTo({ index: 0 })
  })
})
