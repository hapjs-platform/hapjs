/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../../../imports'

describe('框架：05.指令渲染：for：', () => {
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

  it('element作为item的操作', async () => {
    callActionJsonList.splice(0)

    const nodeParentDiv = pageVm.$element('inst1')
    const nodeParentFig = nodeParentDiv.layoutChildren[0]

    expect(nodeParentFig.childNodes.length).to.equal(2)
    expect(nodeParentFig.layoutChildren.length).to.equal(2)

    // 头部插入
    pageVm.list1.unshift({ name: '102', uuid: 102 })
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(3)
    expect(nodeParentFig.layoutChildren[0].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[1].attr.value).to.equal('103')
    expect(nodeParentFig.layoutChildren[2].attr.value).to.equal('104')
    expect(callActionJsonList.length).to.equal(4)
    expect(callActionJsonList[0])
      .to.include('addElement')
      .include('102')
    expect(callActionJsonList[1]).to.include('moveElement')
    expect(callActionJsonList[2]).to.include('moveElement')
    callActionJsonList.splice(0)

    // 头部插入
    pageVm.list1.unshift({ name: '101', uuid: 101 })
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(4)
    expect(nodeParentFig.layoutChildren[0].attr.value).to.equal('101')
    expect(nodeParentFig.layoutChildren[1].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[2].attr.value).to.equal('103')
    expect(nodeParentFig.layoutChildren[3].attr.value).to.equal('104')
    expect(callActionJsonList.length).to.equal(5)
    expect(callActionJsonList[0])
      .to.include('addElement')
      .include('101')
    expect(callActionJsonList[1]).to.include('moveElement')
    expect(callActionJsonList[2]).to.include('moveElement')
    expect(callActionJsonList[3]).to.include('moveElement')
    callActionJsonList.splice(0)

    // 尾部插入
    pageVm.list1.push({ name: '105', uuid: 105 })
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(5)
    expect(nodeParentFig.layoutChildren[0].attr.value).to.equal('101')
    expect(nodeParentFig.layoutChildren[1].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[2].attr.value).to.equal('103')
    expect(nodeParentFig.layoutChildren[3].attr.value).to.equal('104')
    expect(nodeParentFig.layoutChildren[4].attr.value).to.equal('105')
    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0])
      .to.include('addElement')
      .include('105')
    callActionJsonList.splice(0)

    // 尾部删除
    pageVm.list1.pop()
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(4)
    expect(nodeParentFig.layoutChildren[0].attr.value).to.equal('101')
    expect(nodeParentFig.layoutChildren[1].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[2].attr.value).to.equal('103')
    expect(nodeParentFig.layoutChildren[3].attr.value).to.equal('104')
    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('removeElement')
    callActionJsonList.splice(0)

    // 中间删除
    pageVm.list1.splice(2, 1)
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(3)
    expect(nodeParentFig.layoutChildren[0].attr.value).to.equal('101')
    expect(nodeParentFig.layoutChildren[1].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[2].attr.value).to.equal('104')
    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('removeElement')
    callActionJsonList.splice(0)

    // 中间删除
    pageVm.list1.splice(2, 1)
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(2)
    expect(nodeParentFig.layoutChildren[0].attr.value).to.equal('101')
    expect(nodeParentFig.layoutChildren[1].attr.value).to.equal('102')
    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('removeElement')
    callActionJsonList.splice(0)

    // 头尾翻转
    pageVm.list1.reverse()
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(2)
    expect(nodeParentFig.layoutChildren[0].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[1].attr.value).to.equal('101')
    expect(callActionJsonList.length).to.equal(3)
    expect(callActionJsonList[0]).to.include('moveElement')
    expect(callActionJsonList[1]).to.include('moveElement')
    callActionJsonList.splice(0)
  })

  it('figment作为item的操作', async () => {
    callActionJsonList.splice(0)

    const nodeParentDiv = pageVm.$element('inst2')
    const nodeParentFig = nodeParentDiv.layoutChildren[0]

    expect(nodeParentFig.childNodes.length).to.equal(2)
    expect(nodeParentFig.layoutChildren.length).to.equal(2)

    // 头部插入
    pageVm.list2.unshift({ name: '102', uuid: 102 })
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(3)
    expect(nodeParentFig.layoutChildren[0].layoutChildren[0].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[1].layoutChildren[0].attr.value).to.equal('103')
    expect(nodeParentFig.layoutChildren[2].layoutChildren[0].attr.value).to.equal('104')
    expect(callActionJsonList.length).to.equal(4)
    expect(callActionJsonList[0])
      .to.include('addElement')
      .include('102')
    expect(callActionJsonList[1]).to.include('moveElement')
    expect(callActionJsonList[2]).to.include('moveElement')
    callActionJsonList.splice(0)

    // 头部插入
    pageVm.list2.unshift({ name: '101', uuid: 101 })
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(4)
    expect(nodeParentFig.layoutChildren[0].layoutChildren[0].attr.value).to.equal('101')
    expect(nodeParentFig.layoutChildren[1].layoutChildren[0].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[2].layoutChildren[0].attr.value).to.equal('103')
    expect(nodeParentFig.layoutChildren[3].layoutChildren[0].attr.value).to.equal('104')
    expect(callActionJsonList.length).to.equal(5)
    expect(callActionJsonList[0])
      .to.include('addElement')
      .include('101')
    expect(callActionJsonList[1]).to.include('moveElement')
    expect(callActionJsonList[2]).to.include('moveElement')
    expect(callActionJsonList[3]).to.include('moveElement')
    callActionJsonList.splice(0)

    // 尾部插入
    pageVm.list2.push({ name: '105', uuid: 105 })
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(5)
    expect(nodeParentFig.layoutChildren[0].layoutChildren[0].attr.value).to.equal('101')
    expect(nodeParentFig.layoutChildren[1].layoutChildren[0].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[2].layoutChildren[0].attr.value).to.equal('103')
    expect(nodeParentFig.layoutChildren[3].layoutChildren[0].attr.value).to.equal('104')
    expect(nodeParentFig.layoutChildren[4].layoutChildren[0].attr.value).to.equal('105')
    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0])
      .to.include('addElement')
      .include('105')
    callActionJsonList.splice(0)

    // 尾部删除
    pageVm.list2.pop()
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(4)
    expect(nodeParentFig.layoutChildren[0].layoutChildren[0].attr.value).to.equal('101')
    expect(nodeParentFig.layoutChildren[1].layoutChildren[0].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[2].layoutChildren[0].attr.value).to.equal('103')
    expect(nodeParentFig.layoutChildren[3].layoutChildren[0].attr.value).to.equal('104')
    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('removeElement')
    callActionJsonList.splice(0)

    // 中间删除
    pageVm.list2.splice(2, 1)
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(3)
    expect(nodeParentFig.layoutChildren[0].layoutChildren[0].attr.value).to.equal('101')
    expect(nodeParentFig.layoutChildren[1].layoutChildren[0].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[2].layoutChildren[0].attr.value).to.equal('104')
    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('removeElement')
    callActionJsonList.splice(0)

    // 中间删除
    pageVm.list2.splice(2, 1)
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(2)
    expect(nodeParentFig.layoutChildren[0].layoutChildren[0].attr.value).to.equal('101')
    expect(nodeParentFig.layoutChildren[1].layoutChildren[0].attr.value).to.equal('102')
    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('removeElement')
    callActionJsonList.splice(0)

    // 头尾翻转
    pageVm.list2.reverse()
    await waitForOK()
    expect(nodeParentFig.layoutChildren.length).to.equal(2)
    expect(nodeParentFig.layoutChildren[0].layoutChildren[0].attr.value).to.equal('102')
    expect(nodeParentFig.layoutChildren[1].layoutChildren[0].attr.value).to.equal('101')
    expect(callActionJsonList.length).to.equal(3)
    expect(callActionJsonList[0]).to.include('moveElement')
    expect(callActionJsonList[1]).to.include('moveElement')
    callActionJsonList.splice(0)
  })

  it('ViewModel的data属性与方法重名', async () => {
    callActionJsonList.splice(0)

    const nodeParentDiv = pageVm.$element('inst3')
    const nodeParentFig = nodeParentDiv.layoutChildren[0]

    expect(nodeParentFig.childNodes.length).to.equal(1)
    expect(nodeParentFig.layoutChildren.length).to.equal(1)
  })

  it('for循环固定值', async () => {
    callActionJsonList.splice(0)

    const nodeParentDiv = pageVm.$element('inst4')
    const nodeParentFig = nodeParentDiv.layoutChildren[0]

    expect(nodeParentFig.layoutChildren.length).to.equal(3)
    expect(nodeParentFig.layoutChildren[0].attr.value).to.equal(1)
    expect(nodeParentFig.layoutChildren[1].attr.value).to.equal(2)
    expect(nodeParentFig.layoutChildren[2].attr.value).to.equal(3)
  })
})
