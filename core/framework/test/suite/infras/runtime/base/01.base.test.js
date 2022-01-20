/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../imports'

/**
 * 文档节点
 */
describe('基础：1.测试Document', () => {
  beforeEach(() => {
    callNativeMessageList.splice(0)
  })

  afterEach(() => {
    callNativeMessageList.splice(0)
  })

  it('默认document与documentElement数据正常', () => {
    // 创建文档
    const document = config.helper.createDocument(1)

    expect(document.documentElement).to.a('object')
    expect(document.childNodes.length).to.equal(1)
    expect(document.layoutChildren.length).to.equal(1)

    const nodeHtml = document.documentElement
    expect(nodeHtml.childNodes.length).to.equal(0)
    expect(nodeHtml.layoutChildren.length).to.equal(0)
    expect(nodeHtml.parentNode).to.equal(document)
    expect(config.helper.getNodeDepth(nodeHtml)).to.equal(0)

    expect(document.body).to.equal(null)

    const documentElement = document.documentElement

    // 二次销毁不会报错
    config.helper.destroyTagNode(document)
    config.helper.destroyTagNode(document)

    // 销毁后没有对象属性
    for (const k in document) {
      expect(document[k]).not.be.a('object')
      expect(document[k]).not.be.a('array')
    }
    for (const k in documentElement) {
      expect(documentElement[k]).not.be.a('object')
      expect(documentElement[k]).not.be.a('array')
    }
  })
})
