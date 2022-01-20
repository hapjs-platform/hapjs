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

  it('修复for与if指令混用，并且item为逻辑false，循环创建for/if的figment的问题', async () => {
    pageVm.show0 = true
    // 触发堆栈溢出
    await waitForOK()

    pageVm.show1 = true
    // 触发堆栈溢出
    await waitForOK()

    pageVm.show2 = true
    // 触发堆栈溢出
  })
})
