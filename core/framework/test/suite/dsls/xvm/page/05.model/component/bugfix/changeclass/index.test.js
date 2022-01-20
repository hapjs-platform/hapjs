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

  it('父组件动态改变class时的处理', async () => {
    const parentElement = pageVm.$element('index-part1-id')

    // 设置class
    const action1 = {
      action: 'edit',
      name: 'class',
      value: 'index-part2-class'
    }
    global.setPageElementAttrs(page.id, parentElement.ref, [action1])

    // expect(parentElement.attr).to.include({ class: 'index-part2-class' })

    // if (global.STYLING) {
    //   const parentElementNew = global.getStylingNode(parentElement)
    //   const parentElementNewStr = parentElementNew.toString()
    //   expect(parentElementNewStr).to.include('#0000FF')
    // }

    // 页面创建收集到的消息
    expect(callActionJsonList.length).to.equal(7)
    // 0. createBody
    // 1. addElement新增子组件节点
    expect(callActionJsonList[1])
      .to.include('part1-class')
      .include('#00FF00')
    // 2. useParentStyle属性
    expect(callActionJsonList[2]).to.include('_useParentStyle')
    // 3. 子组件使用父组件的样式1
    expect(callActionJsonList[3])
      .to.include('index-part1-class')
      .include('#FF0000')
    // 4. createFinish
    // 5. 子组件使用父组件的样式2
    expect(callActionJsonList[5])
      .to.include('index-part2-class')
      .include('#0000FF')
    // 6. updateFinish
  })
})
