/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../imports'

describe('框架：04.样式模块的确认', () => {
  const pageId = uniqueId()
  let page, pageVm

  before(() => {
    callActionJsonList.splice(0)

    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {
    callActionJsonList.splice(0)

    global.destroyPage(pageId)
  })

  beforeEach(() => {})

  afterEach(() => {})

  it('APP级别的样式', () => {
    const elId1 = pageVm.$element('elId1')
    // 样式有效
    if (global.STYLING) {
      const elId1New = global.getStylingNode(elId1)
      const elId1NewStr = elId1New.toString()
      expect(elId1NewStr)
        .to.include('color')
        .include('#ff0000')
    }
  })
})
