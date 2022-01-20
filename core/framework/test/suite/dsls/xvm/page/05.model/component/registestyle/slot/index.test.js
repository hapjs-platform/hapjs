/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../../imports'

describe('框架：05.自定义子组件', () => {
  const pageId = uniqueId()
  let page, pageVm

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

  it('子组件的root节点晚初始化', async () => {
    const instPart1 = pageVm.$vm('instPart1')

    // if指令为false时，无rootElement元素
    expect(instPart1.$rootElement()).to.equal(undefined)

    // 更新数据
    instPart1.ifData1 = true
    await waitForOK()

    // 渲染后拥有rootElement元素
    expect(instPart1.$rootElement()).to.not.equal(undefined)
  })

  it('子组件有slot时，class样式传递正确', async () => {
    const instPart2 = pageVm.$vm('instPart2')

    // if指令为false时，无rootElement元素
    expect(instPart2.$rootElement()).to.equal(undefined)

    // 更新数据
    instPart2.ifData1 = true
    await waitForOK()

    // 渲染后拥有rootElement元素
    expect(instPart2.$rootElement()).to.not.equal(undefined)
  })
})
