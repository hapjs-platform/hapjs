/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../imports'

describe('框架：05.executor', () => {
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

  it('executor合并task', async () => {
    callActionJsonList.splice(0)

    // 连续触发两次同一watcher的update
    pageVm.d1 = 'd1-modified-first'
    pageVm.d1 = 'd1-modified-second'
    await waitForOK()
    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('updateAttrs')
    expect(callActionJsonList[0]).to.include('d1-modified-second')
    expect(callActionJsonList[1]).to.include('updateFinish')
  })

  it('executor排序task', async () => {
    callActionJsonList.splice(0)

    // 连续触发不同watcher的update
    pageVm.d2 = 'd2-modified'
    pageVm.d1 = 'd1-modified'
    await waitForOK()
    expect(callActionJsonList.length).to.equal(3)
    expect(callActionJsonList[0]).to.include('updateAttrs')
    expect(callActionJsonList[0]).to.include('d1-modified')
    expect(callActionJsonList[1]).to.include('updateAttrs')
    expect(callActionJsonList[1]).to.include('d2-modified')
    expect(callActionJsonList[2]).to.include('updateFinish')
  })

  it('executor排序task+if', async () => {
    callActionJsonList.splice(0)

    // 先修改if节点子元素的数据,再修改if的数据为false
    pageVm.d3 = 'd3-modified'
    pageVm.s1 = false
    await waitForOK()
    expect(callActionJsonList.length).to.equal(2)
    expect(callActionJsonList[0]).to.include('removeElement')
    expect(callActionJsonList[1]).to.include('updateFinish')
  })

  it('executor排序task+for', async () => {
    callActionJsonList.splice(0)

    // 先修改for节点子元素的数据,再修改if的数据为false
    pageVm.d4 = 'd4-modified'
    pageVm.arr1 = ['a']
    await waitForOK()
    expect(callActionJsonList.length).to.equal(5)
    expect(callActionJsonList[0]).to.include('removeElement')
    expect(callActionJsonList[1]).to.include('removeElement')
    // 跳过中间的moveElement
    expect(callActionJsonList[3]).to.include('updateAttrs')
    expect(callActionJsonList[3]).to.include('d4-modified')
    expect(callActionJsonList[4]).to.include('updateFinish')
  })
})
