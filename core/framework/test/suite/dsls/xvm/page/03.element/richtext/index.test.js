/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../imports'

describe('框架：03.组件渲染', () => {
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

  it('ux类型的richtext渲染正确', async () => {
    const richtextInst = pageVm.$element('richtextInst')
    // 直接孩子为Figment
    const childNodeList = richtextInst.childNodes[0].childNodes
    expect(childNodeList.length).to.not.equal(0)
  })
})
