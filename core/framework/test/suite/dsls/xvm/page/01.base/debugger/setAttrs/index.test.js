/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../imports'

describe('框架：01.调试器工具', () => {
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

  it('更新属性', async () => {
    const inst1 = pageVm.$element('inst1')

    // 替换
    const action1 = { action: 'add', name: 'lines', value: 3 }
    const action2 = { action: 'edit', name: 'font-size', value: '64px' }
    const action3 = {
      action: 'edit',
      name: 'style',
      value: 'background-color:#FF0000'
    }
    global.setPageElementAttrs(page.id, inst1.ref, [action1, action2, action3])

    expect(inst1.attr[action1.name]).to.equal(action1.value)
    expect(inst1.attr.fontSize).to.equal(action2.value)
    expect(inst1._style.backgroundColor).to.equal('#FF0000')

    // 空样式对象
    const action4 = { action: 'edit', name: 'style', value: '' }
    global.setPageElementAttrs(page.id, inst1.ref, [action4])

    expect(Object.keys(inst1._style).length).to.equal(0)

    // 空样式对象
    const action5 = {
      action: 'edit',
      name: 'style',
      value: 'background-color:#FF0000; font-size:64px'
    }
    global.setPageElementAttrs(page.id, inst1.ref, [action5])

    expect(inst1._style.backgroundColor).to.equal('#FF0000')
    expect(inst1._style.fontSize).to.equal('64px')
  })
})
