/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../imports'

describe('框架：05.自定义子组件', () => {
  const pageId = uniqueId()
  let page, pageVm, slotContainer, slotDom

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

  it('支持组件内容替换slot', () => {
    const parentVmText = pageVm.parentVmText
    const subVmPart = pageVm.$child('vmInst1')
    slotContainer = subVmPart.$element('slotContainer')
    slotDom = slotContainer.childNodes[0].childNodes[0]
    expect(slotDom.attr.value).to.equal(`reset slot data : ${parentVmText}`)
  })

  it('slot 支持if', async () => {
    const parentVmText = pageVm.parentVmText
    const subVmPart = pageVm.$child('vmInst2')
    slotContainer = subVmPart.$element('slotContainer')

    expect(slotContainer).to.equal(undefined)
    subVmPart.toggleShowSlot()
    await waitForOK()
    slotContainer = subVmPart.$element('slotContainer')
    slotDom = slotContainer.childNodes[0].childNodes[0]
    expect(slotDom.attr.value).to.equal(`reset slot data : ${parentVmText}`)

    // vm3
    const subVmPart3 = pageVm.$child('vmInst3')
    slotContainer = subVmPart3.$element('slotContainer')
    slotDom = slotContainer.childNodes[0]
    expect(slotDom.childNodes.length).to.equal(0)
    pageVm.toggleVmInst3()
    await waitForOK()
    expect(slotDom.childNodes[0].childNodes[0].childNodes[0].attr.value).to.equal(
      `vmInst3 : ${parentVmText}`
    )
  })

  it('slot在for指令中时', async () => {
    // 单层的for指令
    const elNode41 = pageVm.$element('elNode41')
    expect(elNode41.attr.value).to.equal(pageVm.list1[0].attr1)

    // 嵌套的for指令
    const elNode521 = pageVm.$element('elNode521')
    expect(elNode521.attr.value).to.equal(pageVm.list2[0].attr2)
  })

  it('父组件的样式可以在slot生效', async () => {
    const elNode61 = pageVm.$element('elNode61')
    if (global.STYLING) {
      const elNode61New = global.getStylingNode(elNode61)
      const elNode61NewStr = elNode61New.toString()
      expect(elNode61NewStr)
        .to.include('backgroundColor')
        .include('#FF0000')
    }
  })
})
