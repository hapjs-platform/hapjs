/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// import { expect } from 'chai'
import { uniqueId, initPage } from '../../../../imports'

// 需要配合新版打包工具测试
describe('externalClasses测试', () => {
  const pageId = uniqueId()

  before(() => {
    initPage(pageId, null, __dirname)
  })

  after(() => {
    global.destroyPage(pageId)
  })

  it('父组件覆盖子组件样式', () => {})
})
