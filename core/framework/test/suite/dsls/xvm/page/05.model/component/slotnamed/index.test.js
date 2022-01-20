/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../imports'

describe('框架：05.slot 具名slot', () => {
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

  it('具名slot', () => {
    const part1Vm = pageVm.$child('part1')
    const slotFigments = part1Vm.$element('rootEl').childNodes

    expect(slotFigments.length).to.equal(2)
    expect(slotFigments[0].childNodes[0].attr.value).to.equal('default: content from part1')
    expect(slotFigments[1].childNodes[0].attr.value).to.equal('slot1: content from parent')
  })

  it('具名slot 重复定义及使用', () => {
    const part2Vm = pageVm.$child('part2')
    const slotFigments = part2Vm.$element('rootEl').childNodes

    expect(slotFigments.length).to.equal(4)
    expect(slotFigments[0].childNodes[0].attr.value).to.equal('slot1: content1 from parent')
    expect(slotFigments[0].childNodes[1].attr.value).to.equal('slot1: content2 from parent')
    expect(slotFigments[1].childNodes[0].attr.value).to.equal('default: content1 from parent')
    expect(slotFigments[1].childNodes[1].attr.value).to.equal('default: content2 from parent')
    expect(slotFigments[2].childNodes[0].attr.value).to.equal('default: content1 from parent')
    expect(slotFigments[2].childNodes[1].attr.value).to.equal('default: content2 from parent')
    expect(slotFigments[3].childNodes[0].attr.value).to.equal('slot1: content1 from parent')
    expect(slotFigments[3].childNodes[1].attr.value).to.equal('slot1: content2 from parent')
  })

  it('具名slot 后代节点使用slot属性无效', () => {
    const part3Vm = pageVm.$child('part3')
    const slotFigments = part3Vm.$element('rootEl').childNodes

    expect(slotFigments.length).to.equal(2)
    expect(slotFigments[0].childNodes[0].childNodes[0].attr.value).to.equal(
      'default: content from parent'
    )
    expect(slotFigments[1].childNodes[0].attr.value).to.equal('slot1: content from part3')
  })

  it('具名slot 使用插槽处也可以定义新插槽并设置name和slot属性', () => {
    const part5Vm = pageVm.$child('part4').$child('part5')
    const slotFigments = part5Vm.$element('rootEl').childNodes

    expect(slotFigments.length).to.equal(2)
    expect(slotFigments[0].childNodes[0].nodeName).to.equal('#figment')
    expect(slotFigments[0].childNodes[0].childNodes[0].attr.value).to.equal(
      'default: content from part4'
    )
    expect(slotFigments[1].childNodes[0].nodeName).to.equal('#figment')
    expect(slotFigments[1].childNodes[0].childNodes[0].attr.value).to.equal(
      'slot1: content from parent'
    )
  })

  it('具名slot slot嵌套', () => {
    const part6Vm = pageVm.$child('part6')
    const slotFigments = part6Vm.$element('rootEl').childNodes

    expect(slotFigments.length).to.equal(2)
    expect(slotFigments[0].childNodes[0].attr.value).to.equal('default: content1 from part6')
    expect(slotFigments[0].childNodes[1].childNodes[0].attr.value).to.equal(
      'default: content2 from part6'
    )
    expect(slotFigments[1].childNodes.length).to.equal(1)
    expect(slotFigments[1].childNodes[0].attr.value).to.equal('slot1: content from parent')
  })

  it('具名slot slot属性和name属性的默认值为default', () => {
    const part7Vm = pageVm.$child('part7')
    const slotFigments = part7Vm.$element('rootEl').childNodes

    expect(slotFigments.length).to.equal(2)
    expect(slotFigments[0].childNodes[0].attr.value).to.equal('default: content1 from parent')
    expect(slotFigments[0].childNodes[1].attr.value).to.equal('default: content2 from parent')
    expect(slotFigments[1].childNodes[0].attr.value).to.equal('default: content1 from parent')
    expect(slotFigments[1].childNodes[1].attr.value).to.equal('default: content2 from parent')
  })

  it('动态修改多个具名slot，组件渲染正常', async () => {
    let slotFigments = pageVm.$child('part8').$element('rootEl').childNodes
    expect(slotFigments[0].childNodes[0].attr.value).to.equal('demo1: from parent 0')
    expect(slotFigments[1].childNodes[0].attr.value).to.equal('demo2: from parent 0')

    pageVm.changeShow()
    await waitForOK()
    pageVm.changeShow()
    await waitForOK()

    slotFigments = pageVm.$child('part8').$element('rootEl').childNodes
    expect(slotFigments[0].childNodes[0].attr.value).to.equal('demo1: from parent 2')
    expect(slotFigments[1].childNodes[0].attr.value).to.equal('demo2: from parent 2')
  })
})
