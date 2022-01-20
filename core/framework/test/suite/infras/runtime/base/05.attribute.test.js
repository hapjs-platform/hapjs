/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../imports'

/**
 * 属性操作
 */
describe('基础：5.属性操作', () => {
  let document, nodeHtml, nodeBody
  let nodeBodyAttr, nodeBodyClassList

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

  it('普通属性测试', () => {
    // 清理
    callNativeMessageList.splice(0)

    // 赋值
    config.helper.setElementAttr(nodeBody, 'attr-test-k1', 'attr-test-v1')
    nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr['attr-test-k1']).to.equal('attr-test-v1')
    expect(callNativeMessageList[0]).to.include('attr-test-k1')
    expect(callNativeMessageList[0]).to.include('attr-test-v1')
    callNativeMessageList.splice(0)

    // 赋值
    config.helper.setElementAttr(nodeBody, 'attr-test-k2', 'attr-test-v2')
    nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr['attr-test-k2']).to.equal('attr-test-v2')
    expect(callNativeMessageList[0]).to.include('attr-test-k2')
    expect(callNativeMessageList[0]).to.include('attr-test-v2')
    callNativeMessageList.splice(0)

    // 重新覆盖
    config.helper.setElementAttr(nodeBody, 'attr-test-k1', 'attr-test-v1-2')
    nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr['attr-test-k1']).to.equal('attr-test-v1-2')
    expect(callNativeMessageList[0]).to.include('attr-test-k1')
    expect(callNativeMessageList[0]).to.include('attr-test-v1-2')
    callNativeMessageList.splice(0)

    // 赋值为空串
    config.helper.setElementAttr(nodeBody, 'attr-test-k1', '')
    nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr['attr-test-k1']).to.equal('')
    expect(callNativeMessageList[0]).to.include('attr-test-k1')
    expect(callNativeMessageList[0]).to.not.include('attr-test-v1')
    callNativeMessageList.splice(0)

    // 赋值为null
    config.helper.setElementAttr(nodeBody, 'attr-test-k2', null)
    nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr['attr-test-k2']).to.equal(null)
    expect(callNativeMessageList[0]).to.include('attr-test-k2')
    expect(callNativeMessageList[0]).to.not.include('attr-test-v2')
    callNativeMessageList.splice(0)

    // 赋值为数组
    let arr = ['a']
    config.helper.setElementAttr(nodeBody, 'attr-test-k3', arr)
    nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr['attr-test-k3']).to.equal(arr)
    expect(callNativeMessageList[0]).to.include('attr-test-k3')
    expect(callNativeMessageList[0]).to.include(JSON.stringify(arr))
    callNativeMessageList.splice(0)

    // 再次赋值为原数组
    config.helper.setElementAttr(nodeBody, 'attr-test-k3', arr)
    expect(callNativeMessageList[0]).to.include('attr-test-k3')
    expect(callNativeMessageList[0]).to.include(JSON.stringify(arr))
    callNativeMessageList.splice(0)

    // 赋值为null
    arr = null
    config.helper.setElementAttr(nodeBody, 'attr-test-k3', arr)
    nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr['attr-test-k3']).to.equal(null)
    expect(callNativeMessageList[0]).to.include('attr-test-k3')
    expect(callNativeMessageList[0]).to.not.include(JSON.stringify(arr))
    callNativeMessageList.splice(0)
  })

  it('属性class测试', () => {
    // 清理
    callNativeMessageList.splice(0)

    nodeBodyClassList = config.misc.getNodeAttrClassList(nodeBody)
    expect(nodeBodyClassList.length).to.equal(0)

    // class属性：赋值
    config.helper.setElementAttr(nodeBody, 'class', ' \n class1 class2  class3 ')
    nodeBodyClassList = config.misc.getNodeAttrClassList(nodeBody)
    expect(nodeBodyClassList.length, '赋值后，classList.length为3').to.equal(3)
    expect(callNativeMessageList[0]).to.include('"class"')
    expect(nodeBodyClassList[0]).to.equal('class1')
    expect(nodeBodyClassList[1]).to.equal('class2')
    expect(nodeBodyClassList[2]).to.equal('class3')
    expect(callNativeMessageList[0]).to.include('class1')
    expect(callNativeMessageList[0]).to.include('class2')
    expect(callNativeMessageList[0]).to.include('class3')
    callNativeMessageList.splice(0)

    // class属性：添加重复
    config.helper.setElementAttr(nodeBody, 'class', ' \n class1 class2  class3 class2 ')
    nodeBodyClassList = config.misc.getNodeAttrClassList(nodeBody)
    expect(nodeBodyClassList.length, 'add重复，classList.length仍为3').to.equal(3)
    expect(callNativeMessageList.length, 'class属性变化，生成消息').to.equal(1)
    callNativeMessageList.splice(0)

    // class属性：添加新值
    config.helper.setElementAttr(nodeBody, 'class', ' \n class1 class2  class3 class2 class4 ')
    nodeBodyClassList = config.misc.getNodeAttrClassList(nodeBody)
    expect(nodeBodyClassList.length, 'add新值，classList.length为4').to.equal(4)
    expect(nodeBodyClassList[3]).to.equal('class4')

    expect(callNativeMessageList.length, 'class4不存在，生成消息').to.equal(1)
    expect(callNativeMessageList[0]).to.include('"class"')
    expect(callNativeMessageList[0]).to.include('class1')
    expect(callNativeMessageList[0]).to.include('class2')
    expect(callNativeMessageList[0]).to.include('class3')
    expect(callNativeMessageList[0]).to.include('class4')
    callNativeMessageList.splice(0)

    // class属性：删除class1
    config.helper.setElementAttr(nodeBody, 'class', ' \n class2  class3 class2 class4 ')
    nodeBodyClassList = config.misc.getNodeAttrClassList(nodeBody)
    expect(nodeBodyClassList.length).to.equal(3)
    expect(nodeBodyClassList[0]).to.equal('class2')
    expect(nodeBodyClassList[1]).to.equal('class3')
    expect(nodeBodyClassList[2]).to.equal('class4')
    expect(nodeBodyClassList[3]).to.equal(undefined)
    expect(callNativeMessageList[0]).to.include('"class"')
    expect(callNativeMessageList[0]).to.not.include('class1')
    expect(callNativeMessageList[0]).to.include('class2')
    expect(callNativeMessageList[0]).to.include('class3')
    expect(callNativeMessageList[0]).to.include('class4')
    callNativeMessageList.splice(0)

    // class属性：清空
    config.helper.setElementAttr(nodeBody, 'class', '')
    nodeBodyClassList = config.misc.getNodeAttrClassList(nodeBody)
    expect(nodeBodyClassList.length, '更新后，classList.length为0').to.equal(0)
    expect(callNativeMessageList[0]).to.include('"class"')
    expect(callNativeMessageList[0]).to.not.include('class1')
    expect(callNativeMessageList[0]).to.not.include('class2')
    expect(callNativeMessageList[0]).to.not.include('class3')
    expect(callNativeMessageList[0]).to.not.include('class4')
    expect(callNativeMessageList[0]).to.not.include('class5')
    callNativeMessageList.splice(0)
  })

  it('属性id测试', () => {
    // 清理
    callNativeMessageList.splice(0)

    // 赋值
    config.helper.setElementAttr(nodeBody, 'id', 'idTest1')
    nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr.id).to.equal('idTest1')
    expect(callNativeMessageList[0]).to.include('"id"')
    expect(callNativeMessageList[0]).to.include('idTest1')
    callNativeMessageList.splice(0)

    // 重新赋值
    config.helper.setElementAttr(nodeBody, 'id', 'idTest2')
    nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr.id).to.equal('idTest2')
    expect(callNativeMessageList[0]).to.include('"id"')
    expect(callNativeMessageList[0]).to.include('idTest2')
    callNativeMessageList.splice(0)

    // 赋值为空串
    config.helper.setElementAttr(nodeBody, 'id', '')
    nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr.id).to.equal('')
    expect(callNativeMessageList[0]).to.include('"id"')
    expect(callNativeMessageList[0]).to.not.include('idTest')
    callNativeMessageList.splice(0)

    // 赋值为null
    config.helper.setElementAttr(nodeBody, 'id', null)
    nodeBodyAttr = config.misc.getNodeAttributesAsObject(nodeBody)
    expect(nodeBodyAttr.id).to.equal(null)
    expect(callNativeMessageList[0]).to.include('"id"')
    expect(callNativeMessageList[0]).to.not.include('idTest')
    callNativeMessageList.splice(0)
  })

  it('input标签value/checked属性测试', () => {
    const inputNode = document.createElement('input')
    nodeBody.appendChild(inputNode)
    callNativeMessageList.splice(0)

    // value赋值
    config.helper.setElementAttr(inputNode, 'value', 'inputValue1')
    expect(inputNode.value).to.equal('inputValue1')
    expect(callNativeMessageList[0]).to.include('"value"')
    expect(callNativeMessageList[0]).to.include('inputValue1')
    callNativeMessageList.splice(0)

    // value赋值为空串
    config.helper.setElementAttr(inputNode, 'value', '')
    expect(inputNode.value).to.equal('')
    expect(callNativeMessageList[0]).to.include('"value"')
    callNativeMessageList.splice(0)

    // checked赋值
    config.helper.setElementAttr(inputNode, 'checked', false)
    expect(inputNode.checked).to.equal(false)
    expect(callNativeMessageList[0]).to.include('"checked"')
    expect(callNativeMessageList[0]).to.include('false')
    callNativeMessageList.splice(0)

    // checked赋值为空串
    config.helper.setElementAttr(inputNode, 'checked', '')
    expect(inputNode.checked).to.equal('')
    expect(callNativeMessageList[0]).to.include('"checked"')
    callNativeMessageList.splice(0)
  })
})
