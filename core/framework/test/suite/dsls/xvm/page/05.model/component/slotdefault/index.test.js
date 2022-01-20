/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../imports'

describe('框架：05.slot 支持动态内容与默认内容动态切换', () => {
  const pageId = uniqueId()
  let page, pageVm, slotContainer, slotDomChildNodes

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

  it('slot 默认值', () => {
    // 渲染默认slot
    const subVmPart = pageVm.$child('vmInst1')
    const subDataFrom = subVmPart.vmText
    slotContainer = subVmPart.$element('slotContainer1')
    slotDomChildNodes = slotContainer.childNodes[0].childNodes
    expect(slotDomChildNodes[0].attr.value).to.equal(`default slot: ${subDataFrom}`)
    // 样式有效
    if (global.STYLING) {
      const slotContainerNew = global.getStylingNode(slotContainer)
      const slotContainerNewStr = slotContainerNew.toString()
      expect(slotContainerNewStr)
        .to.include('color')
        .include('#4cb4e7')
    }
  })

  it('slot 动态值', () => {
    // 渲染默认slot
    const subVmPart = pageVm.$child('vmInst2')
    const dataFrom = pageVm.parentVmText
    slotContainer = subVmPart.$element('slotContainer1')
    slotDomChildNodes = slotContainer.childNodes[0].childNodes
    expect(slotDomChildNodes[0].attr.value).to.equal(`text: ${dataFrom}`)
    // 样式有效
    if (global.STYLING) {
      const slotContainerNew = global.getStylingNode(slotContainer)
      const slotContainerNewStr = slotContainerNew.toString()
      expect(slotContainerNewStr)
        .to.include('color')
        .include('#ff0000')
    }
  })

  it('一个节点同时存在 if 与 for 指令', async () => {
    // 一个节点同时存在 if 与 for 的情况
    const subVmPart = pageVm.$child('vmInst3')
    slotContainer = subVmPart.$element('slotContainer3')
    slotDomChildNodes = slotContainer.childNodes[0].childNodes
    const dataFrom = pageVm.parentVmText
    const subDataFrom = subVmPart.vmText
    expect(slotDomChildNodes[0].childNodes[0].attr.value).to.equal(`slot 多个节点`)
    expect(slotDomChildNodes[1].attr.value).to.equal(`default slot: ${subDataFrom}`)
    // 样式有效
    if (global.STYLING) {
      const slotContainerNew = global.getStylingNode(slotContainer)
      const slotContainerNewStr = slotContainerNew.toString()
      expect(slotContainerNewStr)
        .to.include('color')
        .include('#1a2d27')
    }

    pageVm.vmInst3Array = ['aa', 'bb']
    await waitForOK()
    expect(slotDomChildNodes[0].childNodes[0].attr.value).to.equal(`slot 多个节点`)
    expect(slotDomChildNodes[1].attr.value).to.equal(`default slot: ${subDataFrom}`)
    // 样式有效
    if (global.STYLING) {
      const slotContainerNew = global.getStylingNode(slotContainer)
      const slotContainerNewStr = slotContainerNew.toString()
      expect(slotContainerNewStr)
        .to.include('color')
        .include('#1a2d27')
    }

    pageVm.toggleVmInst3()
    await waitForOK()

    expect(slotDomChildNodes[0].childNodes[0].childNodes[0].childNodes[0].attr.value).to.equal(
      `vmInst3-${dataFrom}|0:${pageVm.vmInst3Array[0]}`
    )
    expect(slotDomChildNodes[0].childNodes[1].childNodes[0].childNodes[0].attr.value).to.equal(
      `vmInst3-${dataFrom}|1:${pageVm.vmInst3Array[1]}`
    )
    // 样式有效
    if (global.STYLING) {
      const slotContainerNew = global.getStylingNode(slotContainer)
      const slotContainerNewStr = slotContainerNew.toString()
      expect(slotContainerNewStr)
        .to.include('color')
        .include('#ff0000')
    }

    pageVm.toggleVmInst3()
    await waitForOK()
    expect(slotDomChildNodes[0].childNodes[0].attr.value).to.equal(`slot 多个节点`)
    expect(slotDomChildNodes[1].attr.value).to.equal(`default slot: ${subDataFrom}`)

    // 样式有效
    if (global.STYLING) {
      const slotContainerNew = global.getStylingNode(slotContainer)
      const slotContainerNewStr = slotContainerNew.toString()
      expect(slotContainerNewStr)
        .to.include('color')
        .include('#1a2d27')
    }
  })

  it('slot 子节点含有if 与for 指令', async () => {
    // 渲染默认slot
    const subVmPart = pageVm.$child('vmInst4')
    slotContainer = subVmPart.$element('slotContainer3')
    slotDomChildNodes = slotContainer.childNodes[0].childNodes
    const dataFrom = pageVm.parentVmText
    const subDataFrom = subVmPart.vmText
    expect(slotDomChildNodes[0].childNodes[0].attr.value).to.equal(`slot 多个节点`)
    expect(slotDomChildNodes[1].attr.value).to.equal(`default slot: ${subDataFrom}`)

    // 切换child1 为true 显示chil1 定义的内容
    pageVm.toggleChild1()
    await waitForOK()
    expect(slotDomChildNodes[0].childNodes[0].childNodes[0].attr.value).to.equal(
      `child1 : ${dataFrom}`
    )
    expect(slotDomChildNodes[1].childNodes.length).to.equal(0)
    // 切换child2 为true
    pageVm.toggleChild2()
    await waitForOK()
    expect(slotDomChildNodes[0].childNodes[0].childNodes[0].attr.value).to.equal(
      `child1 : ${dataFrom}`
    )
    expect(slotDomChildNodes[1].childNodes[0].childNodes[0].attr.value).to.equal(
      `child2 : ${dataFrom}`
    )
    // 切换for 的数组
    pageVm.toggleFor()
    await waitForOK()
    expect(slotDomChildNodes[0].childNodes[0].childNodes[0].attr.value).to.equal(
      `child1 : ${dataFrom}`
    )
    expect(slotDomChildNodes[1].childNodes[0].childNodes[0].attr.value).to.equal(
      `child2 : ${dataFrom}`
    )
    expect(slotDomChildNodes[2].childNodes[0].childNodes[0].attr.value).to.equal(`for: item1`)
    expect(slotDomChildNodes[2].childNodes[1].childNodes[0].attr.value).to.equal(`for: item2`)
    // 切换child1 为false
    pageVm.toggleChild1()
    await waitForOK()
    expect(slotDomChildNodes[0].childNodes.length).to.equal(0)
    expect(slotDomChildNodes[1].childNodes[0].childNodes[0].attr.value).to.equal(
      `child2 : ${dataFrom}`
    )
    expect(slotDomChildNodes[2].childNodes[0].childNodes[0].attr.value).to.equal(`for: item1`)
    expect(slotDomChildNodes[2].childNodes[1].childNodes[0].attr.value).to.equal(`for: item2`)
    // 切换child2 为false
    pageVm.toggleChild2()
    await waitForOK()
    expect(slotDomChildNodes[0].childNodes.length).to.equal(0)
    expect(slotDomChildNodes[1].childNodes.length).to.equal(0)
    expect(slotDomChildNodes[2].childNodes[0].childNodes[0].attr.value).to.equal(`for: item1`)
    expect(slotDomChildNodes[2].childNodes[1].childNodes[0].attr.value).to.equal(`for: item2`)
    // 切换for 数组为空，显示slot默认值
    pageVm.toggleFor()
    await waitForOK()
    expect(slotDomChildNodes[0].childNodes[0].attr.value).to.equal(`slot 多个节点`)
    expect(slotDomChildNodes[1].attr.value).to.equal(`default slot: ${subDataFrom}`)
  })

  it('组件嵌套组件', () => {
    // 测试组件定义内容
    const subVmPart5 = pageVm.$child('vmInst5')
    slotContainer = subVmPart5.$element('slotContainer1')
    slotDomChildNodes = slotContainer.childNodes[0].childNodes
    const part3 = slotDomChildNodes[1]

    expect(slotDomChildNodes[0].attr.value).to.equal(`part1  inner define from parent`)
    expect(part3.attr.id).to.equal(`slotContainer3`)
    // 测试嵌套组件默认slot
    const part3vmText = part3._vm.vmText
    const part3SlotDomChildNodes = part3.childNodes[0].childNodes
    expect(part3SlotDomChildNodes[0].childNodes[0].attr.value).to.equal(`slot 多个节点`)
    expect(part3SlotDomChildNodes[1].attr.value).to.equal(`default slot: ${part3vmText}`)

    // 测试嵌套组件定义内容
    const subVmPart61 = pageVm.$child('vmInst61')
    const parentVmText = pageVm.parentVmText
    slotContainer = subVmPart61.$element('slotContainer3')
    slotDomChildNodes = slotContainer.childNodes[0].childNodes
    expect(slotDomChildNodes[0].attr.value).to.equal(`part3 inner define ${parentVmText}`)
  })

  it('slot 切换内容 event 仍然有效', () => {
    const subVmPart = pageVm.$child('vmInst7')
    expect(pageVm.eventTriggerNums).to.equal(0)
    pageVm.$emitElement('click', {}, 'elNode71')
    expect(pageVm.eventTriggerNums).to.equal(1)

    subVmPart.toggleShowSlot() // false
    subVmPart.toggleShowSlot() // true
    pageVm.$emitElement('click', {}, 'elNode71')
    expect(pageVm.eventTriggerNums).to.equal(2)
  })

  it('兼容同时存在多个默认slot', async () => {
    const subVmPart = pageVm.$child('vmInst8')
    const dataFrom = pageVm.parentVmText
    const subDataFrom = subVmPart.vmText
    slotContainer = subVmPart.$element('slotContainer1')

    expect(slotContainer.toString()).to.include(`${dataFrom}`)
    pageVm.isShowVmInst8 = false
    await waitForOK()
    expect(slotContainer.toString())
      .to.include(`slot1: ${subDataFrom}`)
      .include(`slot2: ${subDataFrom}`)
    pageVm.isShowVmInst8 = true
    await waitForOK()
    expect(slotContainer.toString()).to.include(`${dataFrom}`)
  })
})
