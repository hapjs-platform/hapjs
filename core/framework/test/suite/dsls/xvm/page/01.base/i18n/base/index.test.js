/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, callActionJsonList } from '../../../../imports'

describe('框架：01.国际化的基础测试', () => {
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

  it('i18n基本功能', () => {
    const app = pageVm.$app
    const localeConfig = app.getLocaleConfig()

    expect(typeof localeConfig).to.equal('object')
    expect(typeof localeConfig.localeObject).to.equal('object')
    expect(localeConfig.resources.length).to.above(0)
    expect(typeof localeConfig.locale).to.equal('string')
  })

  it('$t()在模板中使用', () => {
    const elNode101 = pageVm.$element('elNode101')
    const elNode102 = pageVm.$element('elNode102')
    const elNode103 = pageVm.$element('elNode103')
    const elNode104 = pageVm.$element('elNode104')
    const elNode105 = pageVm.$element('elNode105')
    const elNode106 = pageVm.$element('elNode106')
    const elNode107 = pageVm.$element('elNode107')
    const elNode108 = pageVm.$element('elNode108')
    const elNode109 = pageVm.$element('elNode109')
    const elNode110 = pageVm.$element('elNode110')

    expect(elNode101.attr.value).to.equal('纯文本内容')
    expect(elNode102.attr.value).to.equal('')
    expect(elNode103.attr.value).to.equal('')
    expect(elNode104.attr.value).to.equal('not.exist')
    expect(elNode105.attr.value).to.equal('类型-object')
    expect(elNode106.attr.value).to.equal('类型-array')
    expect(elNode107.attr.value).to.equal('[object Object]')
    expect(elNode108.attr.value).to.equal('元素-类型-字符串')
    expect(elNode109.attr.value).to.equal('元素-类型-对象-属性')
    expect(elNode110.attr.value).to.equal('元素-类型-数组')

    const vmChild2 = pageVm.$child('vmChild201')
    const elNode201 = vmChild2.$element('elNode201')
    expect(elNode201.attr.value).to.equal('纯文本内容')
  })

  it('$t()在脚本中使用', () => {
    // 无参数
    const case101Ret1 = pageVm.$t('message.format')
    expect(case101Ret1).to.be.an('object')

    // 传递参数
    const case102Ret1 = pageVm.$t('message.format.object', { name: 'object' })
    expect(case102Ret1).to.equal('类型-object')

    // 不支持：传递locale
    const case103Ret1 = pageVm.$t('message.text', 'fu')
    expect(case103Ret1).to.equal('纯文本内容')

    // 不支持：传递locale，但可以额外传递参数
    const case104Ret1 = pageVm.$t('message.format.object', 'fu', {
      name: 'object'
    })
    expect(case104Ret1).to.equal('类型-object')

    // 传递的表达式在locale中没有
    const case105Ret1 = pageVm.$t('message.fallback')
    expect(case105Ret1).to.equal('fallback-value')

    // 传递locale不存在
    const case106Ret1 = pageVm.$t('message.fallback', 'fu')
    expect(case106Ret1).to.equal('fallback-value')
  })

  it('$tc()的功能', () => {
    // 模板中渲染
    const elNode101 = pageVm.$element('elNode101')

    expect(elNode101.attr.value).to.equal('纯文本内容')

    // 脚本中调用

    // 2个选择：不传递默认为1;
    expect(pageVm.$tc('message.plurals.double')).to.equal('车')
    expect(pageVm.$tc('message.plurals.double', 0)).to.equal('车s')
    expect(pageVm.$tc('message.plurals.double', 1)).to.equal('车')
    expect(pageVm.$tc('message.plurals.double', 2)).to.equal('车s')
    expect(pageVm.$tc('message.plurals.double', 1, 'en')).to.equal('车')

    // 多个选择
    expect(pageVm.$tc('message.plurals.three')).to.equal('one 苹果')
    expect(pageVm.$tc('message.plurals.three', 0)).to.equal('no 苹果s')
    expect(pageVm.$tc('message.plurals.three', 1)).to.equal('one 苹果')
    expect(pageVm.$tc('message.plurals.three', 2)).to.equal('2 苹果s')
    expect(pageVm.$tc('message.plurals.three', 10, 'en')).to.equal('10 苹果s')
    expect(pageVm.$tc('message.plurals.three', 2, 'en', { count: 10 })).to.equal('10 苹果s')
    expect(pageVm.$tc('message.plurals.three', 2, 'en', { count: 'Many' })).to.equal('Many 苹果s')

    // 替换参数
    expect(pageVm.$tc('message.plurals.format.object', 1, { name: 'object' })).to.equal(
      '类型-object'
    )
    expect(pageVm.$tc('message.plurals.format.array', 1, ['array'])).to.equal('类型-array')

    expect(pageVm.$tc('message.plurals.fallback')).to.equal('fallback-value')
    expect(pageVm.$tc('message.plurals.fallback', 1, 'fu')).to.equal('fallback-value')
  })

  it('系统更新locale', async () => {
    const currLocaleConfig = pageVm.$app.getLocaleConfig()
    const newLocaleConfig = JSON.parse(JSON.stringify(currLocaleConfig))

    // 修改为英文
    newLocaleConfig.localeObject.language = 'en'
    newLocaleConfig.localeObject.countryOrRegion = 'US'
    newLocaleConfig.resources.shift()

    // 触发更改
    global.changeAppLocale(newLocaleConfig.localeObject, newLocaleConfig.resources)
    global.notifyConfigurationChanged(pageId, { type: 'locale' })

    expect(pageVm.evtOnConfigurationChanged).to.equal(true)
    expect(pageVm.evtOnConfigurationChangedEvt.type).to.equal('locale')
    await waitForOK()

    // 模板中使用
    const elNode101 = pageVm.$element('elNode101')
    const elNode102 = pageVm.$element('elNode102')
    const elNode103 = pageVm.$element('elNode103')
    const elNode104 = pageVm.$element('elNode104')
    const elNode105 = pageVm.$element('elNode105')
    const elNode106 = pageVm.$element('elNode106')
    const elNode107 = pageVm.$element('elNode107')
    const elNode108 = pageVm.$element('elNode108')
    const elNode109 = pageVm.$element('elNode109')
    const elNode110 = pageVm.$element('elNode110')

    expect(elNode101.attr.value).to.equal('pure-text-content')
    expect(elNode102.attr.value).to.equal('')
    expect(elNode103.attr.value).to.equal('')
    expect(elNode104.attr.value).to.equal('not.exist')
    expect(elNode105.attr.value).to.equal('type-object')
    expect(elNode106.attr.value).to.equal('type-array')
    expect(elNode107.attr.value).to.equal('[object Object]')
    expect(elNode108.attr.value).to.equal('item-type-string')
    expect(elNode109.attr.value).to.equal('item-type-object-attribute')
    expect(elNode110.attr.value).to.equal('item-type-array')

    const vmChild2 = pageVm.$child('vmChild201')
    const elNode201 = vmChild2.$element('elNode201')
    expect(elNode201.attr.value).to.equal('pure-text-content')
  })
})
