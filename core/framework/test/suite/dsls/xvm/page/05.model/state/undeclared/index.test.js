/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../../imports'

describe('框架：05.debug模式下页面ViewModel数据未声明给出提示', () => {
  const pageId = uniqueId()
  let page, pageVm
  const warnFn = console.warn
  const warnMessageList = []
  const originDebugMode = global.isRpkDebugMode

  before(() => {
    global.isRpkDebugMode = function() {
      return true
    }

    console.warn = function(...args) {
      warnMessageList.push(...args)
      warnFn(...args)
    }
    callActionJsonList.splice(0)

    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {
    console.warn = warnFn
    global.isRpkDebugMode = originDebugMode
    callActionJsonList.splice(0)

    global.destroyPage(pageId)
  })

  it('数据在data/private/protected/public中未定义', async () => {
    expect(warnMessageList.length).equal(4)
    // computed 和 {{}}指令触发
    expect(warnMessageList[0]).to.include('computed为1050版本中新增的计算属性，不再当做Vm方法')
    expect(warnMessageList[1]).to.include('请使用其它名称，后续版本不再兼容')
    expect(warnMessageList[2]).equal(
      '### App Framework ### 请确认VM的data/public/protected/private中定义了属性：b'
    )
    expect(warnMessageList[3]).equal(
      '### App Framework ### 请确认VM的data/public/protected/private中定义了属性：b'
    )
    warnMessageList.splice(0)

    // 第一次依赖收集的数据会有警告
    await pageVm.showLoading1()
    // 这里不要通过attr.value直接获取value，会触发console.warn
    expect(pageVm.$element('d')._attr.value).equal('propD')
    expect(warnMessageList.length).equal(1)
    // {{}}指令
    expect(warnMessageList[0]).equal(
      '### App Framework ### 请确认VM的data/public/protected/private中定义了属性：d'
    )
    warnMessageList.splice(0)

    // 隐藏UI
    pageVm.loading1 = false
    await waitForOK()
    expect(warnMessageList.length).equal(0)
    warnMessageList.splice(0)

    // 再次依赖收集应该还会提示
    await pageVm.showLoading1()
    expect(warnMessageList.length).equal(1)
    expect(warnMessageList[0]).equal(
      '### App Framework ### 请确认VM的data/public/protected/private中定义了属性：d'
    )
    warnMessageList.splice(0)

    // 检测数据UI一致性
    await pageVm.showLoading2()
    // 第一个tick内未获取到pageVm.b的值，value为undefined。
    expect(pageVm.$element('b1')._attr.value).equal(undefined)
    expect(pageVm.b).equal('propB') // 数据与UI不一致，所以如果没有在data/private/protected/public中未定义需要注意。
    expect(warnMessageList.length).equal(1)
    expect(warnMessageList[0]).equal(
      '### App Framework ### 请确认VM的data/public/protected/private中定义了属性：b'
    )
    warnMessageList.splice(0)

    // 由于在VM上已经有属性b，依赖收集时可以获取到。
    await pageVm.showLoading3()
    expect(pageVm.$element('b2')._attr.value).equal('propB')
    // 因为pageVm.c依赖于pageVm.a和pageVm.b，而pageVm.b没有在data里定义，不会被劫持
    // 所以不会触发computed属性的变化，需要手动触发pageVm.a的属性才能改变pageVm.c的值。
    expect(pageVm.$element('c1')._attr.value).equal('propA,undefined')
    expect(warnMessageList.length).equal(1)
    expect(warnMessageList[0]).equal(
      '### App Framework ### 请确认VM的data/public/protected/private中定义了属性：b'
    )
    warnMessageList.splice(0)

    // 手动改变pageVm.a的值，触发pageVm.c的值改变
    pageVm.a = 'propA1'
    await waitForOK()
    expect(pageVm.$element('c1')._attr.value).equal('propA1,propB')
    warnMessageList.splice(0)

    await pageVm.showLoading4()
    expect(warnMessageList.length).equal(0)
    warnMessageList.splice(0)
  })
})
