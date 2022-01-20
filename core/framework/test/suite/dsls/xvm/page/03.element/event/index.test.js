/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
import { uniqueId, initPage } from '../../../imports'

describe('框架：03.事件分发支持', () => {
  // 获取manifest
  let manifest

  const pageId = uniqueId()
  let page, pageVm

  before(() => {
    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {})

  beforeEach(() => {
    manifest = global.loadManifestJSON()
  })

  afterEach(() => {
    global.registerManifest(JSON.stringify(global.loadManifestJSON()))
  })

  it('1040以下 所有事件不支持分发', () => {
    manifest.minPlatformVersion = '1030'
    global.registerManifest(JSON.stringify(manifest))

    // 监听函数与结果
    const evtListenList = []

    const evtListener = function(evt) {
      const msg = `正常:${evt.type}:${evt._supportW3C}`

      evtListenList.push(msg)
    }

    const elNode = pageVm.$element('elNode')

    elNode.addEventListener('click', evtListener)
    elNode.addEventListener('focus', evtListener)

    pageVm.$emitElement('click', {}, 'elNode')
    pageVm.$emitElement('focus', {}, 'elNode')

    elNode.removeEventListener('click', evtListener)
    elNode.removeEventListener('focus', evtListener)

    expect(evtListenList.length).to.equal(2)
    expect(evtListenList[0]).to.equal('正常:click:false')
    expect(evtListenList[1]).to.equal('正常:focus:false')
  })

  it('1040及以上 touch事件支持分发', () => {
    manifest.minPlatformVersion = '1040'
    global.registerManifest(JSON.stringify(manifest))

    // 监听函数与结果
    const evtListenList = []

    const evtListener = function(evt) {
      const msg = `正常:${evt.type}:${evt._supportW3C}`

      evtListenList.push(msg)
    }

    const elNode = pageVm.$element('elNode')

    elNode.addEventListener('click', evtListener)
    elNode.addEventListener('focus', evtListener)

    pageVm.$emitElement('click', {}, 'elNode')
    pageVm.$emitElement('focus', {}, 'elNode')

    elNode.removeEventListener('click', evtListener)
    elNode.removeEventListener('focus', evtListener)

    expect(evtListenList.length).to.equal(2)
    expect(evtListenList[0]).to.equal('正常:click:true')
    expect(evtListenList[1]).to.equal('正常:focus:false')
  })
})
