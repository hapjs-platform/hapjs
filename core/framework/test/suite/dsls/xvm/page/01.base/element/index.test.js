/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../imports'

describe('框架：01.组件的测试', () => {
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

  it('检查组件注册的方法', () => {
    pageVm.$element('node1Id').method1()
    pageVm.$element('node1Id').method2()
  })

  it('viewId不一致', () => {
    const node1ViewId = pageVm.$element('node1Id').getViewId()
    const node2ViewId = pageVm.$element('node2Id').getViewId()
    expect(node1ViewId).to.not.equal(node2ViewId)
  })

  it('data属性赋值', () => {
    const elComp2 = pageVm.$element('node2Id')

    expect(elComp2.dataset).to.be.an('object')
    expect(elComp2.dataset.attr1).to.equal('v101')
    expect(elComp2.dataset.attr2Type1).to.equal('v201')
    expect(elComp2.dataset['-Attr3-2']).to.equal('v301')
    expect(elComp2.dataset['Attr4-2']).to.equal('v401')
    expect(elComp2.dataset['Attr5-A']).to.equal('v501')
  })
})
