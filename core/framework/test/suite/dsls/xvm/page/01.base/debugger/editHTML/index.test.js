/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage } from '../../../../imports'

describe('框架：01.调试器工具', () => {
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

  it('HTML替换节点：空串', async () => {
    const inst = pageVm.$element('inst1')
    const instParentNode = inst.parentNode

    global.replacePageElementWithHtml(page.id, inst.ref, pageVm.html1)

    expect(instParentNode.layoutChildren.length).to.equal(2)
  })

  it('HTML替换节点：纯文本', async () => {
    const inst = pageVm.$element('inst2')
    const instParentNode = inst.parentNode

    global.replacePageElementWithHtml(page.id, inst.ref, pageVm.html2)

    const instNewNodeText = instParentNode.layoutChildren[1]
    expect(instNewNodeText.attr.value).to.equal('TEXT内容1')
  })

  it('HTML替换节点：text节点无span', async () => {
    const inst = pageVm.$element('inst3')
    const instParentNode = inst.parentNode

    global.replacePageElementWithHtml(page.id, inst.ref, pageVm.html3)

    const instNewNodeText = instParentNode.layoutChildren[1]
    expect(instNewNodeText.attr.class).to.equal('class1')
    expect(instNewNodeText.attr.fontSize).to.equal('64px')
    expect(instNewNodeText.attr.value).to.equal('TEXT内容1')
    expect(instNewNodeText._style.backgroundColor).to.equal('#FF0000')
  })

  it('HTML替换节点：text节点既有文本又有span', async () => {
    const inst = pageVm.$element('inst4')
    const instParentNode = inst.parentNode

    global.replacePageElementWithHtml(page.id, inst.ref, pageVm.html4)

    const instNewNodeText = instParentNode.layoutChildren[1]
    expect(instNewNodeText.layoutChildren.length).to.equal(3)
    const instNewNodeSpan0 = instNewNodeText.layoutChildren[0]
    const instNewNodeSpan1 = instNewNodeText.layoutChildren[1]
    const instNewNodeSpan2 = instNewNodeText.layoutChildren[2]
    expect(instNewNodeSpan0.tagName).to.equal('span'.toUpperCase())
    expect(instNewNodeSpan0.attr.value).to.equal('SPAN内容1')
    expect(instNewNodeSpan1.tagName).to.equal('span'.toUpperCase())
    expect(instNewNodeSpan1.attr.value).to.equal('SPAN内容2')
    expect(instNewNodeSpan2.tagName).to.equal('span'.toUpperCase())
    expect(instNewNodeSpan2.attr.value).to.equal('SPAN内容3')
  })

  it('HTML替换节点：既有文本又有text', async () => {
    const inst = pageVm.$element('inst5')
    const instParentNode = inst.parentNode

    global.replacePageElementWithHtml(page.id, inst.ref, pageVm.html5)

    const instNewNodeText0 = instParentNode.layoutChildren[1]
    const instNewNodeText1 = instParentNode.layoutChildren[2]
    const instNewNodeText2 = instParentNode.layoutChildren[3]
    expect(instNewNodeText0.attr.value).to.equal('TEXT内容1')
    expect(instNewNodeText1.attr.value).to.equal('SPAN内容2')
    expect(instNewNodeText2.attr.value).to.equal('TEXT内容3')
  })

  it('HTML替换节点：多个节点', async () => {
    const inst = pageVm.$element('inst6')
    const instParentNode = inst.parentNode

    global.replacePageElementWithHtml(page.id, inst.ref, pageVm.html6)

    const instNewNodeDiv0 = instParentNode.layoutChildren[1]
    const instNewNodeDiv1 = instParentNode.layoutChildren[2]
    const instNewNodeText0 = instNewNodeDiv0.layoutChildren[0]
    const instNewNodeText1 = instNewNodeDiv1.layoutChildren[0]

    expect(instNewNodeDiv0.tagName).to.equal('div'.toUpperCase())
    expect(instNewNodeDiv1.tagName).to.equal('div'.toUpperCase())
    expect(instNewNodeText0.attr.value).to.equal('TEXT内容1')
    expect(instNewNodeText1.attr.value).to.equal('TEXT内容2')
  })

  it('HTML替换节点：div下的文本', async () => {
    const inst = pageVm.$element('inst7')
    const instParentNode = inst.parentNode

    global.replacePageElementWithHtml(page.id, inst.ref, pageVm.html7)

    const instNewNodeDiv0 = instParentNode.layoutChildren[1]
    const instNewNodeText0 = instNewNodeDiv0.layoutChildren[0]

    expect(instNewNodeDiv0.tagName).to.equal('div'.toUpperCase())
    expect(instNewNodeText0.attr.value).to.equal('TEXT内容1')
  })

  it('HTML替换节点：a标签中的纯文本', async () => {
    const inst = pageVm.$element('inst8')
    const instParentNode = inst.parentNode

    global.replacePageElementWithHtml(page.id, inst.ref, pageVm.html8)

    const instNewNodeLink0 = instParentNode.layoutChildren[1]

    expect(instNewNodeLink0.tagName).to.equal('a'.toUpperCase())
    expect(instNewNodeLink0.attr.value).to.equal('A内容1')
  })

  it('HTML替换节点：span标签中的纯文本', async () => {
    const inst = pageVm.$element('inst9')
    const instParentNode = inst.parentNode

    global.replacePageElementWithHtml(page.id, inst.ref, pageVm.html9)

    const instNewNodeLink0 = instParentNode.layoutChildren[1]

    expect(instNewNodeLink0.tagName).to.equal('span'.toUpperCase())
    expect(instNewNodeLink0.attr.value).to.equal('SPAN内容1')
  })

  it('HTML替换节点：text标签上的value属性', async () => {
    const inst = pageVm.$element('inst10')
    const instParentNode = inst.parentNode

    global.replacePageElementWithHtml(page.id, inst.ref, pageVm.html10)

    const instNewNodeText0 = instParentNode.layoutChildren[1]
    const instNewNodeSpan0 = instNewNodeText0.layoutChildren[0]

    expect(instNewNodeText0.tagName).to.equal('text'.toUpperCase())
    expect(instNewNodeText0.attr.value).to.equal(undefined)
    expect(instNewNodeSpan0.tagName).to.equal('span'.toUpperCase())
    expect(instNewNodeSpan0.attr.value).to.equal('SPAN内容3')
  })

  it('HTML替换节点：a节点既有文本又有span', async () => {
    const inst = pageVm.$element('inst11')
    const instParentNode = inst.parentNode

    global.replacePageElementWithHtml(page.id, inst.ref, pageVm.html11)

    const instNewNodeLink = instParentNode.layoutChildren[1]
    expect(instNewNodeLink.layoutChildren.length).to.equal(3)
    const instNewNodeSpan0 = instNewNodeLink.layoutChildren[0]
    const instNewNodeSpan1 = instNewNodeLink.layoutChildren[1]
    const instNewNodeSpan2 = instNewNodeLink.layoutChildren[2]
    expect(instNewNodeSpan0.tagName).to.equal('span'.toUpperCase())
    expect(instNewNodeSpan0.attr.value).to.equal('SPAN内容1')
    expect(instNewNodeSpan1.tagName).to.equal('span'.toUpperCase())
    expect(instNewNodeSpan1.attr.value).to.equal('SPAN内容2')
    expect(instNewNodeSpan2.tagName).to.equal('span'.toUpperCase())
    expect(instNewNodeSpan2.attr.value).to.equal('SPAN内容3')
  })
})
