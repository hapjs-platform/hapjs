/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { initPage, callActionJsonList, proxyCallNative, unproxyCallNative } from '../../../imports'

describe('框架：00. sequence', () => {
  const pageId = 1
  let page, pageVm

  before(function() {
    unproxyCallNative()

    callActionJsonList.splice(0)
    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(function() {
    proxyCallNative()

    callActionJsonList.splice(0)
    global.destroyPage(pageId)
  })

  afterEach(() => {
    callActionJsonList.length = 0
  })

  it('initPage', () => {
    expect(callActionJsonList).to.matchSnapshot()
  })

  it('showArrayMethod', async () => {
    pageVm.showArrayMethod()
    await waitForOK()
    expect(callActionJsonList).to.matchSnapshot()
  })

  it('clickPush', async () => {
    pageVm.clickPush()
    await waitForOK()
    expect(callActionJsonList).to.matchSnapshot()
  })

  it('clickPop', async () => {
    pageVm.clickPop()
    await waitForOK()
    expect(callActionJsonList).to.matchSnapshot()
  })

  it('clickShift', async () => {
    pageVm.clickShift()
    await waitForOK()
    expect(callActionJsonList).to.matchSnapshot()
  })

  it('clickUnshift', async () => {
    pageVm.clickUnshift()
    await waitForOK()
    expect(callActionJsonList).to.matchSnapshot()
  })

  it('clickSplice', async () => {
    pageVm.clickSplice()
    await waitForOK()
    expect(callActionJsonList).to.matchSnapshot()
  })

  it('clickSort', async () => {
    pageVm.clickSort()
    await waitForOK()
    expect(callActionJsonList).to.matchSnapshot()
  })

  it('clickReverse', async () => {
    pageVm.clickReverse()
    await waitForOK()
    expect(callActionJsonList).to.matchSnapshot()
  })
})
