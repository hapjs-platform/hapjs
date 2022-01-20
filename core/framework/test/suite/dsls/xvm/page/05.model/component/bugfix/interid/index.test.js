/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../../../imports'

describe('框架：05.自定义子组件', () => {
  const pageId = uniqueId()
  // eslint-disable-next-line
  let page, pageVm

  before(() => {
    callActionJsonList.splice(0)

    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {
    global.destroyPage(pageId)

    callActionJsonList.splice(0)
  })

  beforeEach(() => {})

  afterEach(() => {})

  it('父子组件使用class,但子组件拥有ID时的处理', async () => {
    // 页面创建收集到的消息
    expect(callActionJsonList.length).to.equal(5)
    // 0. createBody
    // 1. addElement新增子组件节点
    expect(callActionJsonList[1])
      .to.include('part1-class')
      .to.include('part1-id')
      .include('#0000FF')
    // 2. useParentStyle属性
    expect(callActionJsonList[2]).to.include('_useParentStyle')
    // 3. 子组件使用父组件的类，但依然保持自己ID的样式
    expect(callActionJsonList[3])
      .to.include('index-part1-class')
      .include('#0000FF')
      .to.not.include('#FF0000')
    // 4. createFinish
  })
})
