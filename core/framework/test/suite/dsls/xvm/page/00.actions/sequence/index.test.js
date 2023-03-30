/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { initPage, callActionJsonList, proxyCallNative, unproxyCallNative } from '../../../imports'
import fs from 'fs'

describe('框架：00. sequence', () => {
  const pageId = 1
  const indexPath = 'test/build/dsls/xvm/page/00.actions/sequence/index.js'
  let page, pageVm, buildRes

  before(function() {
    unproxyCallNative()

    callActionJsonList.splice(0)

    setStyleObjectId()
    initPage(pageId, null, __dirname)
    resetStyleObjectId()

    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(function() {
    proxyCallNative()

    callActionJsonList.splice(0)
    global.destroyPage(pageId)
  })

  // index.ux 打包产物 index.js 中的 styleObjectId 并不总是 2
  // 如果该值不为 2, 会导致后续单测中匹配渲染指令快照失败
  // 所以此处强制将 styleObjectId 设置为 2
  function setStyleObjectId() {
    try {
      buildRes = fs.readFileSync(indexPath, 'utf-8')
      // 匹配示例: `styleObjectId":1`
      const reg = /(styleObjectId":\s*)([\d]+)/g
      // 替换示例: `styleObjectId":1` => `styleObjectId":2`
      const newBuildRes = buildRes.replace(reg, '$12')
      fs.writeFileSync(indexPath, newBuildRes)
    } catch (e) {
      console.log('设置 styleObjectId 失败', e)
    }
  }

  // 还原 styleObjectId
  function resetStyleObjectId() {
    try {
      fs.writeFileSync(indexPath, buildRes)
    } catch (e) {
      console.log('还原 styleObjectId 失败', e)
    }
  }

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
