/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import fs from 'fs'
import path from 'path'
import { uniqueId, defaultAppId } from './imports'
import { calculate } from './project/src/Demo/dynamic-js.js'

describe('动态导入测试', () => {
  const pageId = uniqueId()
  let page, pageVm

  before(() => {
    const chunkPath = path.join(__dirname, 'project/build', 'page-chunks.json')
    const pagePath = path.join(__dirname, 'project/build/Demo', 'index.js')
    // 注入依赖的JS缓存
    const chunkHash = fs.readFileSync(chunkPath, {
      encoding: 'UTF-8'
    })
    global.registerBundleChunks(chunkHash)

    const dstFileCont = fs.readFileSync(pagePath)

    global.createPage(pageId, defaultAppId, dstFileCont, {}, { currentPageName: 'undefined' }, {})

    page = global.getPage(pageId)
    pageVm = page.vm
  })

  it('动态导入函数', async () => {
    const count = pageVm.count
    await pageVm.dynamicImportJs()
    expect(pageVm.count).to.equal(calculate(count))
  })
})
