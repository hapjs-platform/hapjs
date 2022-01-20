/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../../imports'

describe('框架：05.指令渲染', () => {
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

  it('自定义组件上支持show指令', async () => {
    const vmInst1 = pageVm.$child('vmInst1')
    const node1 = vmInst1.$element()

    // 首次渲染
    expect(node1.attr.show).to.equal(true)
    // 兼容原有逻辑：暂时拥有该属性，但是会有warning
    expect(vmInst1.show).to.equal(true)

    // 更新数据
    pageVm.part1ShowValue1 = false
    await waitForOK()

    expect(node1.attr.show).to.equal(false)
  })
})
