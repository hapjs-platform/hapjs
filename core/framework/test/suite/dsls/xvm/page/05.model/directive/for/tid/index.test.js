/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../../../imports'

describe('框架：05.指令渲染：for指令tid不唯一', () => {
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

  it('在带有重复tid的for指令内使用自定义组件', async () => {
    callActionJsonList.splice(0)
    pageVm.changeList1()

    await waitForOK()
    expect(callActionJsonList.length).to.equal(9)
    let allMessage = callActionJsonList.join('')
    // 某些DOM元素无法复用
    expect(allMessage).to.include('addElement')
    expect(allMessage).to.include('moveElement')
    expect(allMessage).to.include('removeElement')
    callActionJsonList.splice(0)

    pageVm.revertList1()
    await waitForOK()
    expect(callActionJsonList.length).to.equal(7)
    allMessage = callActionJsonList.join('')
    // 某些DOM元素无法复用
    expect(allMessage).to.include('addElement')
    expect(allMessage).to.include('removeElement')
  })

  it('使用特定的tid的for指令内使用自定义组件', async () => {
    callActionJsonList.splice(0)
    pageVm.changeList2()
    await waitForOK()
    expect(callActionJsonList.length).to.equal(5)
    // 单纯地移动，复用
    expect(callActionJsonList[0]).to.include('moveElement')
    expect(callActionJsonList[1]).to.include('moveElement')
    expect(callActionJsonList[2]).to.include('moveElement')
    expect(callActionJsonList[3]).to.include('moveElement')
    callActionJsonList.splice(0)

    pageVm.revertList2()
    await waitForOK()
    expect(callActionJsonList.length).to.equal(5)
    // 单纯地移动，复用
    expect(callActionJsonList[0]).to.include('moveElement')
    expect(callActionJsonList[1]).to.include('moveElement')
    expect(callActionJsonList[2]).to.include('moveElement')
    expect(callActionJsonList[3]).to.include('moveElement')
  })
})
