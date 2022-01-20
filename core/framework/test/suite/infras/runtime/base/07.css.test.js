/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../imports'

import data from './07.css.data'

/**
 * CSS简单选择，后代选择操作
 */
describe('基础：7.CSS选择', () => {
  let document, nodeHtml, nodeBody

  beforeEach(() => {
    callNativeMessageList.splice(0)

    document = config.helper.createDocument(1)
    nodeHtml = document.documentElement
    nodeBody = document.createElement('div')
    nodeHtml.appendChild(nodeBody)
  })

  afterEach(() => {
    callNativeMessageList.splice(0)

    config.helper.destroyTagNode(document)
    document = null
    nodeHtml = null
    nodeBody = null
  })

  it('样式计算：简单选择(tag,class,id,inline)', () => {
    // 清理
    callNativeMessageList.splice(0)

    // 定义样式节点
    config.helper.registerStyleObject('testStyleBaseFile', data.simple1, false, nodeBody)

    // 清空样式更新的消息
    expect(callNativeMessageList.length).to.equal(1)
    callNativeMessageList.splice(0)

    // 节点绑定
    const nodeElem1 = document.createElement('div')
    nodeBody.appendChild(nodeElem1)

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-tag-1')
    callNativeMessageList.splice(0)

    // 设置class
    config.helper.setElementAttr(nodeElem1, 'class', ' class-test1 ')

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-class-1')
    callNativeMessageList.splice(0)

    // 设置class
    config.helper.setElementAttr(nodeElem1, 'class', 'class-test2 class-test1')

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-class-2')
    callNativeMessageList.splice(0)

    // 设置id
    config.helper.setElementAttr(nodeElem1, 'id', 'idTest1')

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-id-1')
    callNativeMessageList.splice(0)

    // 设置style
    config.helper.setElementStyle(nodeElem1, 'ca1', '')

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.not.include('ca1-')
    callNativeMessageList.splice(0)

    // 赋值为null前设置新值
    config.helper.setElementStyle(nodeElem1, 'ca1', 'meanless')
    callNativeMessageList.splice(0)

    // 赋值为null
    config.helper.setElementStyle(nodeElem1, 'ca1', null)

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('""')
    callNativeMessageList.splice(0)

    // 删除样式中的CSS属性：ca1，并更新内联样式
    config.helper.setElementStyles(nodeElem1, {})
    config.helper.setElementStyle(nodeElem1, '', null)
    callNativeMessageList.splice(0)

    // 设置id
    config.helper.setElementAttr(nodeElem1, 'id', '')

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-class-2')
    callNativeMessageList.splice(0)

    // 设置class
    config.helper.setElementAttr(nodeElem1, 'class', 'class-test2')

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-class-2')
    callNativeMessageList.splice(0)

    // 设置class: 与定义顺序有关，与声明顺序无关
    config.helper.setElementAttr(nodeElem1, 'class', 'class-test2 class-test1')

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-class-2')
    callNativeMessageList.splice(0)

    // 设置class
    config.helper.setElementAttr(nodeElem1, 'class', '')

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-tag-1')
    callNativeMessageList.splice(0)
  })

  it('样式计算：后代选择', () => {
    // 清理
    callNativeMessageList.splice(0)

    // 定义样式节点
    config.helper.registerStyleObject('testStyleDescFile', data.desc1, false, nodeBody)

    // 清空样式更新的消息
    expect(callNativeMessageList.length).to.equal(1)
    callNativeMessageList.splice(0)

    // 新增元素
    const nodeParent1 = document.createElement('div')
    nodeBody.appendChild(nodeParent1)

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-desc-div-1')
    callNativeMessageList.splice(0)

    // 新增元素
    const nodeText1 = document.createElement('text')
    nodeParent1.appendChild(nodeText1)

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-desc-div-text-1')
    callNativeMessageList.splice(0)

    // 设置class
    config.helper.setElementAttr(nodeBody, 'class', 'doc-page')
    callNativeMessageList.splice(0)

    // 设置class
    config.helper.setElementAttr(nodeText1, 'class', 'class-test1')

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-desc-doc-page-class-test1-1')
    callNativeMessageList.splice(0)

    // 设置ID
    config.helper.setElementAttr(nodeText1, 'id', 'idTest1')

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-desc-doc-page-idTest1-1')
    callNativeMessageList.splice(0)

    // 设置内联：禁用样式
    const ruleNameInline = 'INLINE'
    config.helper.setElementMatchedCssRule(nodeText1, ruleNameInline, data.inlineEditPropList)

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-desc-doc-page-idTest1-1')
    expect(callNativeMessageList[0]).to.include('"ca2"')
    expect(callNativeMessageList[0]).to.include('ca2-inline-1')
    callNativeMessageList.splice(0)

    const inlineCssRule1 = config.helper.getElementMatchedCssRuleList(nodeText1)[0]
    expect(inlineCssRule1.style.length).to.equal(2)
    expect(inlineCssRule1.style[0].disabled).to.equal(true)
    expect(inlineCssRule1.style[1].disabled).to.equal(false)

    // 设置内联，不再禁用
    data.inlineEditPropList[0].disabled = false
    config.helper.setElementMatchedCssRule(nodeText1, ruleNameInline, data.inlineEditPropList)

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-inline-1')
    expect(callNativeMessageList[0]).to.include('"ca2"')
    expect(callNativeMessageList[0]).to.include('ca2-inline-1')
    callNativeMessageList.splice(0)

    const inlineCssRule2 = config.helper.getElementMatchedCssRuleList(nodeText1)[0]
    expect(inlineCssRule2.style.length).to.equal(2)
    expect(inlineCssRule2.style[0].disabled).to.equal(false)
    expect(inlineCssRule2.style[1].disabled).to.equal(false)

    // 暂不支持：恢复设置
    // delete nodeText1._usedCSSPropertyCache['ca2']
    // config.helper.setElementMatchedCssRule(nodeText1, ruleNameInline, [])
    //
    // expect(callNativeMessageList.length).to.equal(1)
    // expect(callNativeMessageList[0]).to.include('"ca1"')
    // expect(callNativeMessageList[0]).to.include('ca1-desc-doc-page-idTest1-1')
    // expect(callNativeMessageList[0]).to.not.include('"ca2"')
    // expect(callNativeMessageList[0]).to.not.include('ca2-inline-1')
    // callNativeMessageList.splice(0)

    // 暂不支持：设置后代选择
    // const ruleNameDesc = '.doc-page #idTest1'
    // config.helper.setElementMatchedCssRule(nodeText1, ruleNameDesc, data.descEditPropList1)
    //
    // expect(callNativeMessageList.length).to.equal(1)
    // expect(callNativeMessageList[0]).to.include('"ca1"')
    // expect(callNativeMessageList[0]).to.not.include('ca1-desc-doc-page-idTest1-1')
    // expect(callNativeMessageList[0]).to.include('"ca2"')
    // expect(callNativeMessageList[0]).to.include('ca2-desc-doc-page-idTest1-2')
    // callNativeMessageList.splice(0)
    //
    // const descCssRule1 = config.helper.getElementMatchedCssRuleList(nodeText1, ruleNameDesc)[0]
    // expect(descCssRule1.styleSheetName).to.equal('testStyleDescFile')
    // expect(descCssRule1.style.length).to.equal(2)
    // expect(descCssRule1.style[0].disabled).to.equal(true)
    // expect(descCssRule1.style[1].disabled).to.equal(false)

    // 暂不支持：设置后代选择
    // data.descEditPropList1[0].disabled = false
    // config.helper.setElementMatchedCssRule(nodeText1, ruleNameDesc, data.descEditPropList1)
    //
    // expect(callNativeMessageList.length).to.equal(1)
    // expect(callNativeMessageList[0]).to.include('"ca1"')
    // expect(callNativeMessageList[0]).to.include('ca1-desc-doc-page-idTest1-2')
    // expect(callNativeMessageList[0]).to.include('"ca2"')
    // expect(callNativeMessageList[0]).to.include('ca2-desc-doc-page-idTest1-2')
    // callNativeMessageList.splice(0)
    //
    // const descCssRule2 = config.helper.getElementMatchedCssRuleList(nodeText1, ruleNameDesc)[0]
    // expect(descCssRule2.styleSheetName).to.equal('testStyleDescFile')
    // expect(descCssRule2.style.length).to.equal(2)
    // expect(descCssRule2.style[0].disabled).to.equal(false)
    // expect(descCssRule2.style[1].disabled).to.equal(false)

    // 暂不支持：恢复设置：恢复到未设置样式对象之前的状态
    // data.descEditPropList1[0].value = 'ca1-desc-doc-page-idTest1-1'
    // delete nodeText1._usedCSSPropertyCache['ca2']
    // config.helper.setElementMatchedCssRule(nodeText1, ruleNameDesc, [data.descEditPropList1[0]])
    //
    // expect(callNativeMessageList.length).to.equal(1)
    // expect(callNativeMessageList[0]).to.include('"ca1"')
    // expect(callNativeMessageList[0]).to.include('ca1-desc-doc-page-idTest1-1')
    // expect(callNativeMessageList[0]).to.not.include('"ca2"')
    // expect(callNativeMessageList[0]).to.not.include('ca2-desc-doc-page-idTest1-2')
    // callNativeMessageList.splice(0)
  })

  it('map组件的测试', () => {
    config.helper.setElementAttr(nodeBody, 'class', 'doc-page')

    // 无样式：不会匹配判断
    config.helper.registerStyleObject(null, {}, false, nodeBody)
    // 新增元素
    callNativeMessageList.splice(0)
    const nodeParent1 = document.createElement('map')
    nodeBody.appendChild(nodeParent1)

    expect(callNativeMessageList.length).to.equal(1)
    callNativeMessageList.splice(0)

    // 有样式：才会匹配判断
    config.helper.registerStyleObject(null, data.styleNodeForMap, false, nodeBody)
    // 新增元素
    callNativeMessageList.splice(0)
    const nodeParent2 = document.createElement('map')
    nodeBody.appendChild(nodeParent2)

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('"ca1"')
    expect(callNativeMessageList[0]).to.include('ca1-desc-doc-page-map-1')
    callNativeMessageList.splice(0)
  })

  it('样式计算：更新CSS规则中的样式对象', () => {
    // 清理
    callNativeMessageList.splice(0)

    // 定义样式节点
    config.helper.registerStyleObject(null, data.desc2, false, nodeBody)

    // 新增元素
    const nodeParent1 = document.createElement('div')
    nodeBody.appendChild(nodeParent1)

    // 新增元素
    const nodeText1 = document.createElement('text')
    nodeParent1.appendChild(nodeText1)

    // 新增元素
    const nodeText2 = document.createElement('text')
    nodeParent1.appendChild(nodeText2)

    // 设置class
    config.helper.setElementAttr(nodeBody, 'class', 'doc-page')

    // 设置class
    config.helper.setElementAttr(nodeText1, 'class', 'class-test1')

    // 设置ID
    config.helper.setElementAttr(nodeText1, 'id', 'idTest1')
    // 设置ID
    config.helper.setElementAttr(nodeText2, 'id', 'idTest1')

    // 清空
    callNativeMessageList.splice(0)

    // 暂不支持：设置后代选择
    // const ruleNameDesc = '.doc-page #idTest1'
    // config.helper.setElementMatchedCssRule(nodeText1, ruleNameDesc, data.descEditPropList2)
    //
    // // 匹配到：nodeText1,nodeText2
    // expect(callNativeMessageList.length).to.equal(2)
    // expect(callNativeMessageList[0]).to.include('"ca1"')
    // expect(callNativeMessageList[0]).to.not.include('ca1-desc-doc-page-idTest1-1')
    // expect(callNativeMessageList[0]).to.include('"ca2"')
    // expect(callNativeMessageList[0]).to.include('ca2-desc-doc-page-idTest1-2')
    //
    // expect(callNativeMessageList[1]).to.include('"ca1"')
    // expect(callNativeMessageList[1]).to.not.include('ca1-desc-doc-page-idTest1-1')
    // expect(callNativeMessageList[1]).to.include('"ca2"')
    // expect(callNativeMessageList[1]).to.include('ca2-desc-doc-page-idTest1-2')
    // callNativeMessageList.splice(0)
    //
    // const descCssRule1 = config.helper.getElementMatchedCssRuleList(nodeText1, ruleNameDesc)[0]
    // expect(descCssRule1.style.length).to.equal(2)
    // expect(descCssRule1.style[0].disabled).to.equal(true)
    // expect(descCssRule1.style[1].disabled).to.equal(false)
    //
    // const descCssRule2 = config.helper.getElementMatchedCssRuleList(nodeText2, ruleNameDesc)[0]
    // expect(descCssRule2.style.length).to.equal(2)
    // expect(descCssRule2.style[0].disabled).to.equal(true)
    // expect(descCssRule2.style[1].disabled).to.equal(false)

    // 暂不支持：设置后代选择
    // data.descEditPropList2[0].disabled = false
    // config.helper.setElementMatchedCssRule(nodeText1, ruleNameDesc, data.descEditPropList2)
    //
    // expect(callNativeMessageList.length).to.equal(2)
    // expect(callNativeMessageList[0]).to.include('"ca1"')
    // expect(callNativeMessageList[0]).to.include('ca1-desc-doc-page-idTest1-2')
    // expect(callNativeMessageList[0]).to.include('"ca2"')
    // expect(callNativeMessageList[0]).to.include('ca2-desc-doc-page-idTest1-2')
    // expect(callNativeMessageList[1]).to.include('"ca1"')
    // expect(callNativeMessageList[1]).to.include('ca1-desc-doc-page-idTest1-2')
    // expect(callNativeMessageList[1]).to.include('"ca2"')
    // expect(callNativeMessageList[1]).to.include('ca2-desc-doc-page-idTest1-2')
    // callNativeMessageList.splice(0)
    //
    // const descCssRule12 = config.helper.getElementMatchedCssRuleList(nodeText1, ruleNameDesc)[0]
    // expect(descCssRule12.style.length).to.equal(2)
    // expect(descCssRule12.style[0].disabled).to.equal(false)
    // expect(descCssRule12.style[1].disabled).to.equal(false)
    //
    // const descCssRule22 = config.helper.getElementMatchedCssRuleList(nodeText2, ruleNameDesc)[0]
    // expect(descCssRule22.style.length).to.equal(2)
    // expect(descCssRule22.style[0].disabled).to.equal(false)
    // expect(descCssRule22.style[1].disabled).to.equal(false)
  })

  it('定义document级别的样式', () => {
    // 清理
    callNativeMessageList.splice(0)

    // 文档级别
    config.helper.registerStyleObject(
      'documentLevelCustom',
      data.docLevelStyle1,
      true,
      document.body
    )

    // 清空样式更新的消息
    expect(callNativeMessageList.length).to.equal(1)
    callNativeMessageList.splice(0)

    // 新增元素
    const nodeParent1 = document.createElement('div')
    nodeBody.appendChild(nodeParent1)

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0])
      .to.include('"ca1"')
      .include('"ca1-div-1"')

    // 暂不支持：定义选择器
    // const ruleNameDesc = 'div'
    // const docLevelCssRule = config.helper.getElementMatchedCssRuleList(nodeParent1, ruleNameDesc)[0]
    // expect(docLevelCssRule.styleSheetName).to.equal('documentLevelCustom')
  })

  it('样式计算：修改非末尾选择器(id)触发的子节点更新', () => {
    // 清理
    callNativeMessageList.splice(0)

    // 定义样式节点
    config.helper.registerStyleObject(null, data.descNotTailSelectorIdUpdateNode, false, nodeBody)

    // 清空样式更新的消息
    expect(callNativeMessageList.length).to.equal(1)
    callNativeMessageList.splice(0)

    // 设置class
    config.helper.setElementAttr(nodeBody, 'class', 'doc-page')

    // 新增元素
    const nodeParent1 = document.createElement('div')
    nodeBody.appendChild(nodeParent1)

    // 触发子节点重新计算
    config.helper.setElementAttr(nodeParent1, 'descRestyling', '')

    // 设置class
    config.helper.setElementAttr(nodeParent1, 'id', 'idDivTest1')

    // 清空
    callNativeMessageList.splice(0)

    // 新增元素
    const nodeChild1 = document.createElement('h1')
    nodeParent1.appendChild(nodeChild1)
    const nodeChild2 = document.createElement('h1')
    nodeParent1.appendChild(nodeChild2)

    expect(callNativeMessageList.length).to.equal(2)
    expect(callNativeMessageList[0])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-idDivTest1-h1-1')
    expect(callNativeMessageList[1])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-idDivTest1-h1-1')
    callNativeMessageList.splice(0)

    // 新增元素
    const nodeChild3 = document.createElement('div')
    nodeParent1.appendChild(nodeChild3)

    // 清空
    callNativeMessageList.splice(0)

    // 新增元素
    const nodeChild31 = document.createElement('h1')
    nodeChild3.appendChild(nodeChild31)
    const nodeChild32 = document.createElement('h1')
    nodeChild3.appendChild(nodeChild32)

    expect(callNativeMessageList.length).to.equal(2)
    expect(callNativeMessageList[0])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-idDivTest1-div-h1-1')
    expect(callNativeMessageList[1])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-idDivTest1-div-h1-1')
    callNativeMessageList.splice(0)

    // 更新父节点的匹配的ID选择器
    config.helper.setElementAttr(nodeParent1, 'id', '')

    // 匹配到：nodeChild1,nodeChild2
    expect(callNativeMessageList.length).to.equal(5)
    expect(callNativeMessageList[1])
      .to.include('"ca1"')
      .not.include('ca1-desc-doc-page-idDivTest1-h1-1')
    expect(callNativeMessageList[2])
      .to.include('"ca1"')
      .not.include('ca1-desc-doc-page-idDivTest1-h1-1')
    expect(callNativeMessageList[3])
      .to.include('"ca1"')
      .not.include('ca1-desc-doc-page-idDivTest1-div-h1-1')
    expect(callNativeMessageList[4])
      .to.include('"ca1"')
      .not.include('ca1-desc-doc-page-idDivTest1-div-h1-1')
    callNativeMessageList.splice(0)

    // 更新父节点的匹配的ID选择器
    config.helper.setElementAttr(nodeParent1, 'id', 'idDivTest2')

    // 匹配到：nodeChild1,nodeChild2
    expect(callNativeMessageList.length).to.equal(5)
    expect(callNativeMessageList[1])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-idDivTest2-h1-1')
    expect(callNativeMessageList[2])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-idDivTest2-h1-1')
    expect(callNativeMessageList[3])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-idDivTest2-div-h1-1')
    expect(callNativeMessageList[4])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-idDivTest2-div-h1-1')
    callNativeMessageList.splice(0)

    // 更新
    config.helper.setElementAttr(nodeChild3, 'id', 'idDivTest2')

    // 匹配到：nodeChild31,nodeChild32
    // 虽然匹配路径变了，但是匹配结果没变，因此不应该变化
    expect(callNativeMessageList.length).to.equal(1)
    callNativeMessageList.splice(0)
  })

  it('样式计算：修改非末尾选择器(class)触发的子节点更新', () => {
    // 清理
    callNativeMessageList.splice(0)

    // 定义样式节点
    config.helper.registerStyleObject(
      null,
      data.descNotTailSelectorClassUpdateNode,
      false,
      nodeBody
    )

    // 清空样式更新的消息
    expect(callNativeMessageList.length).to.equal(1)
    callNativeMessageList.splice(0)

    // 设置class
    config.helper.setElementAttr(nodeBody, 'class', 'doc-page')

    // 新增元素
    const nodeParent1 = document.createElement('div')
    nodeBody.appendChild(nodeParent1)

    // 触发子节点重新计算
    config.helper.setElementAttr(nodeParent1, 'descRestyling', '')

    // 设置class
    config.helper.setElementAttr(nodeParent1, 'class', 'classDivTest1')

    // 清空
    callNativeMessageList.splice(0)

    // 新增元素
    const nodeChild1 = document.createElement('h1')
    nodeParent1.appendChild(nodeChild1)
    const nodeChild2 = document.createElement('h1')
    nodeParent1.appendChild(nodeChild2)

    expect(callNativeMessageList.length).to.equal(2)
    expect(callNativeMessageList[0])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-classDivTest1-h1-1')
    expect(callNativeMessageList[1])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-classDivTest1-h1-1')
    callNativeMessageList.splice(0)

    // 新增元素
    const nodeChild3 = document.createElement('div')
    nodeParent1.appendChild(nodeChild3)

    // 清空
    callNativeMessageList.splice(0)

    // 新增元素
    const nodeChild31 = document.createElement('h1')
    nodeChild3.appendChild(nodeChild31)
    const nodeChild32 = document.createElement('h1')
    nodeChild3.appendChild(nodeChild32)

    expect(callNativeMessageList.length).to.equal(2)
    expect(callNativeMessageList[0])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-classDivTest1-div-h1-1')
    expect(callNativeMessageList[1])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-classDivTest1-div-h1-1')
    callNativeMessageList.splice(0)

    // 更新父节点的匹配的ID选择器
    config.helper.setElementAttr(nodeParent1, 'class', '')

    // 匹配到：nodeChild1,nodeChild2
    expect(callNativeMessageList.length).to.equal(5)
    expect(callNativeMessageList[1])
      .to.include('"ca1"')
      .not.include('ca1-desc-doc-page-classDivTest1-h1-1')
    expect(callNativeMessageList[2])
      .to.include('"ca1"')
      .not.include('ca1-desc-doc-page-classDivTest1-h1-1')
    expect(callNativeMessageList[3])
      .to.include('"ca1"')
      .not.include('ca1-desc-doc-page-classDivTest1-div-h1-1')
    expect(callNativeMessageList[4])
      .to.include('"ca1"')
      .not.include('ca1-desc-doc-page-classDivTest1-div-h1-1')
    callNativeMessageList.splice(0)

    // 更新父节点的匹配的ID选择器
    config.helper.setElementAttr(nodeParent1, 'class', 'classDivTest2')

    // 匹配到：nodeChild1,nodeChild2
    expect(callNativeMessageList.length).to.equal(5)
    expect(callNativeMessageList[1])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-classDivTest2-h1-1')
    expect(callNativeMessageList[2])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-classDivTest2-h1-1')
    expect(callNativeMessageList[3])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-classDivTest2-div-h1-1')
    expect(callNativeMessageList[4])
      .to.include('"ca1"')
      .include('ca1-desc-doc-page-classDivTest2-div-h1-1')
    callNativeMessageList.splice(0)

    // 更新
    config.helper.setElementAttr(nodeChild3, 'class', 'classDivTest2')

    // 匹配到：nodeChild31,nodeChild32
    // 虽然匹配路径变了，但是匹配结果没变，因此不应该变化
    expect(callNativeMessageList.length).to.equal(1)
    callNativeMessageList.splice(0)
  })
})
