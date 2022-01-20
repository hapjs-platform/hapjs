/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../imports'

describe('框架：01.加载自定义组件', () => {
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

  it('数据初始化正确', () => {
    expect(pageVm.$child('vm1Id')).to.not.equal(undefined)
    expect(pageVm.$child('vm1Id').d1Str).to.equal('v1Str')

    const d1Comp = pageVm.$child('vm1Id').$element('d1Id')
    expect(d1Comp.attr.value).to.equal('v1Str')
  })

  it('ViewModel销毁时清理引用', async () => {
    // 销毁 VM
    pageVm.renderPart2 = false
    // 执行异步方法，在异步回调中访问已销毁 VM
    pageVm.$child('vm2Id').accessVmAsync()
    await waitForOK()
    pageVm.$broadcast('part2-evt1')
    // Vm销毁后，调用Vm方法不报错
    pageVm.$child('vm2Id').$emit('evt2')
  })

  it('使用自定义组件时的DOM事件绑定', () => {
    // 无参数触发
    const detail3 = 'detail value 3'
    pageVm.$child('vm3Id').$emitElement('click', detail3)
    expect(pageVm.detail).to.equal(detail3)
    // 有参数触发
    const detail4 = 'detail value 4'
    pageVm.$child('vm4Id').$emitElement('click', detail4)
    expect(pageVm.arg0).to.equal('val0')
    expect(pageVm.detail).to.equal(detail4)
  })
})
