/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../../imports'

describe('框架：05.JSX基础语法', () => {
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

  it('slot插入', async () => {
    callActionJsonList.splice(0)

    ++pageVm.renderSlotFlag
    await waitForOK()
    expect(callActionJsonList[1])
      .to.include('value')
      .to.include('vmData')
  })

  it('slot插入:传参', async () => {
    callActionJsonList.splice(0)

    ++pageVm.renderSlotFlag
    await waitForOK()
    expect(callActionJsonList[1])
      .to.include('value')
      .to.include('params')
  })

  it('数据绑定', async () => {
    callActionJsonList.splice(0)

    ++pageVm.renderSlotFlag
    await waitForOK()
    expect(callActionJsonList[0])
      .to.include('class')
      .to.include('color-red')
    expect(callActionJsonList[1])
      .to.include('value')
      .to.include('vmData')
  })

  it('事件绑定', async () => {
    callActionJsonList.splice(0)

    ++pageVm.renderSlotFlag
    await waitForOK()
    expect(callActionJsonList[0])
      .to.include('class')
      .to.include('btn-m')
    expect(callActionJsonList[0])
      .to.include('event')
      .to.include('click')
    pageVm.$emitElement('click', {}, 'input1')
    expect(pageVm.params[0])
      .to.include('type')
      .to.include('target')
  })

  it('事件绑定:传参', async () => {
    callActionJsonList.splice(0)

    ++pageVm.renderSlotFlag
    await waitForOK()
    expect(callActionJsonList[0])
      .to.include('class')
      .to.include('btn-m')
    expect(callActionJsonList[0])
      .to.include('event')
      .to.include('click')
    pageVm.$emitElement('click', {}, 'input2')
    expect(pageVm.params[1]).to.equal('click')
  })

  it('对象展开', async () => {
    callActionJsonList.splice(0)

    ++pageVm.renderSlotFlag
    await waitForOK()
    expect(callActionJsonList[0])
      .to.include('id')
      .to.include('input3')
    expect(callActionJsonList[0])
      .to.include('value')
      .to.include('点击显示toast:click')
    expect(callActionJsonList[0])
      .to.include('type')
      .to.include('button')
    expect(callActionJsonList[0])
      .to.include('class')
      .to.include('btn-m')
    expect(callActionJsonList[0])
      .to.include('style')
      .to.include('backgroundColor')
    expect(callActionJsonList[0])
      .to.include('event')
      .to.include('click')
    pageVm.$emitElement('click', {}, 'input3')
    expect(pageVm.params[2]).to.equal('click')
  })

  it('条件渲染', async () => {
    callActionJsonList.splice(0)

    ++pageVm.renderSlotFlag
    await waitForOK()
    expect(callActionJsonList[2])
      .to.include('value')
      .to.include('show=true')
  })

  it('列表渲染', async () => {
    callActionJsonList.splice(0)

    ++pageVm.renderSlotFlag
    await waitForOK()
    expect(callActionJsonList[3])
      .to.include('value')
      .to.include('list-item1')
    expect(callActionJsonList[6])
      .to.include('value')
      .to.include('list-item2')
    expect(callActionJsonList[9])
      .to.include('value')
      .to.include('list-item3')
  })
})
