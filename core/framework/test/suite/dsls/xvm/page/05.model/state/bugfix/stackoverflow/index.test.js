/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../../../imports'

describe('框架：05.页面ViewModel数据更新', () => {
  const pageId = uniqueId()
  const originDebugMode = global.isRpkDebugMode
  let page, pageVm

  before(() => {
    global.isRpkDebugMode = function() {
      return true
    }
    callActionJsonList.splice(0)

    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {
    global.isRpkDebugMode = originDebugMode
    callActionJsonList.splice(0)

    global.destroyPage(pageId)
  })

  it('test stack overflow', async () => {
    // $element()对象上不会直接挂载watcher对象，不会对watcher对象进行ob，不会出现栈溢出的情况
    const can = pageVm.$element('content')
    try {
      pageVm.mContentNode = can
    } catch (error) {
      expect(error.toString())
        .to.include('mContentNode')
        .include('禁止做数据驱动')
    }

    try {
      pageVm.mContentNode = can.getContext('2d')
    } catch (error) {
      expect(error.toString())
        .to.include('mContentNode')
        .include('禁止做数据驱动')
    }

    try {
      pageVm.mContentNode = {
        a: {
          b: can
        }
      }
    } catch (error) {
      expect(error.toString())
        .to.include('mContentNode')
        .include('禁止做数据驱动')
    }
  })
})
