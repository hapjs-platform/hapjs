/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../imports'

/**
 * 节点布局渲染操作
 */
describe('基础：3.节点布局渲染操作', () => {
  let document, nodeHtml, nodeBody
  let fnElementBase, fnFigmentBase

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

  it('html与body的默认渲染', () => {
    expect(config.misc.calcRenderCount(nodeHtml)).to.equal(1)
    expect(config.misc.calcRenderCount(nodeBody)).to.equal(1)

    expect(config.misc.renderIndexInRenderParent(nodeHtml)).to.equal(0)
    expect(config.misc.renderIndexInRenderParent(nodeBody)).to.equal(0)
  })

  /**
   * DOM树结构：
   * body
   *    elem10
   *        elem11
   *            figt111
   *        elem12
   *        figt13
   *            elem131
   *            elem132
   *            elem133
   *        elem14
   */
  it(
    '基于Element的操作',
    (fnElementBase = function() {
      // 清理
      callNativeMessageList.splice(0)

      const elem10 = document.createElement('elem10')
      const elem11 = document.createElement('elem11')
      const elem12 = document.createElement('elem12')
      const elem14 = document.createElement('elem14')

      expect(config.helper.getNodeDepth(elem10)).to.equal(null)
      expect(config.helper.getNodeDepth(elem11)).to.equal(null)
      expect(config.helper.getNodeDepth(elem12)).to.equal(null)
      expect(config.helper.getNodeDepth(elem14)).to.equal(null)

      // 所有元素均为1
      nodeBody.appendChild(elem10)
      elem10.appendChild(elem11)
      elem10.appendChild(elem12)
      elem10.appendChild(elem14)

      expect(elem10.childNodes.length).to.equal(3)
      expect(elem10.layoutChildren.length).to.equal(3)
      expect(elem11.parentNode).to.equal(elem10)
      expect(elem12.parentNode).to.equal(elem10)
      expect(elem14.parentNode).to.equal(elem10)

      expect(config.helper.getNodeDepth(elem10)).to.equal(2)
      expect(config.helper.getNodeDepth(elem11)).to.equal(3)
      expect(config.helper.getNodeDepth(elem12)).to.equal(3)
      expect(config.helper.getNodeDepth(elem14)).to.equal(3)

      expect(config.misc.calcRenderCount(elem10)).to.equal(1)
      expect(config.misc.calcRenderCount(elem11)).to.equal(1)
      expect(config.misc.calcRenderCount(elem12)).to.equal(1)
      expect(config.misc.calcRenderCount(elem14)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(elem10)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem11)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem12)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(elem14)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(4)
      expect(callNativeMessageList[0]).to.include('elem10')
      expect(callNativeMessageList[1]).to.include('elem11')
      expect(callNativeMessageList[2]).to.include('elem12')
      expect(callNativeMessageList[3]).to.include('elem14')
      callNativeMessageList.splice(0)

      // 加入空Figment，父元素不变
      const figt111 = config.helper.createFigment(document)
      elem11.appendChild(figt111)

      expect(elem11.childNodes.length).to.equal(1)
      expect(elem11.layoutChildren.length).to.equal(1)
      expect(figt111.parentNode).to.equal(elem11)

      expect(config.helper.getNodeDepth(figt111)).to.equal(4)

      expect(config.misc.calcRenderCount(figt111)).to.equal(0)
      expect(config.misc.calcRenderCount(elem11)).to.equal(1)
      expect(config.misc.calcRenderCount(elem10)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(figt111)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem11)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem12)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(elem14)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(0)
      callNativeMessageList.splice(0)

      // 加入空Figment, 父元素不变
      const figt13 = config.helper.createFigment(document)
      elem10.insertBefore(figt13, elem10.childNodes[2])

      expect(elem10.childNodes.length).to.equal(4)
      expect(elem10.layoutChildren.length).to.equal(4)
      expect(figt13.parentNode).to.equal(elem10)

      expect(config.helper.getNodeDepth(figt13)).to.equal(3)

      expect(config.misc.calcRenderCount(figt13)).to.equal(0)
      expect(config.misc.calcRenderCount(elem10)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(figt13)).to.equal(2)
      expect(config.misc.renderIndexInRenderParent(elem14)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(0)
      callNativeMessageList.splice(0)

      // Figment中加入元素，父元素变化
      const elem131 = document.createElement('elem131')
      figt13.appendChild(elem131)

      expect(figt13.childNodes.length).to.equal(1)
      expect(figt13.layoutChildren.length).to.equal(1)
      expect(elem131.parentNode).to.equal(figt13)

      expect(config.helper.getNodeDepth(elem131)).to.equal(4)

      expect(config.misc.calcRenderCount(elem131)).to.equal(1)
      expect(config.misc.calcRenderCount(figt13)).to.equal(1)
      expect(config.misc.calcRenderCount(elem10)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(elem131)).to.equal(2)
      expect(config.misc.renderIndexInRenderParent(elem14)).to.equal(3)
      expect(config.misc.renderIndexInRenderParent(figt13)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(1)
      expect(callNativeMessageList[0]).to.include('elem131')
      callNativeMessageList.splice(0)

      // Figment中再加入元素，父元素变化
      const elem133 = document.createElement('elem133')
      figt13.appendChild(elem133)

      expect(figt13.childNodes.length).to.equal(2)
      expect(figt13.layoutChildren.length).to.equal(2)
      expect(elem133.parentNode).to.equal(figt13)

      expect(config.helper.getNodeDepth(elem133)).to.equal(4)

      expect(config.misc.calcRenderCount(elem133)).to.equal(1)
      expect(config.misc.calcRenderCount(figt13)).to.equal(2)
      expect(config.misc.calcRenderCount(elem10)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(elem133)).to.equal(3)
      expect(config.misc.renderIndexInRenderParent(elem14)).to.equal(4)
      expect(config.misc.renderIndexInRenderParent(figt13)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(1)
      expect(callNativeMessageList[0]).to.include('elem133')
      callNativeMessageList.splice(0)

      // Figment中再中间插入元素，父元素变化
      const elem132 = document.createElement('elem132')
      figt13.insertBefore(elem132, figt13.childNodes[1])

      expect(figt13.childNodes.length).to.equal(3)
      expect(figt13.layoutChildren.length).to.equal(3)
      expect(elem132.parentNode).to.equal(figt13)

      expect(config.helper.getNodeDepth(elem132)).to.equal(4)

      expect(config.misc.calcRenderCount(elem132)).to.equal(1)
      expect(config.misc.calcRenderCount(figt13)).to.equal(3)
      expect(config.misc.calcRenderCount(elem10)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(elem132)).to.equal(3)
      expect(config.misc.renderIndexInRenderParent(elem14)).to.equal(5)
      expect(config.misc.renderIndexInRenderParent(figt13)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(1)
      expect(callNativeMessageList[0]).to.include('elem132')
      callNativeMessageList.splice(0)

      // Figment中删除中间元素，父元素变化
      figt13.removeChild(elem131)

      expect(figt13.childNodes.length).to.equal(2)
      expect(figt13.layoutChildren.length).to.equal(2)
      expect(elem131.parentNode).to.equal(null)

      expect(config.helper.getNodeDepth(elem131)).to.equal(null)

      expect(config.misc.calcRenderCount(elem131)).to.equal(1)
      expect(config.misc.calcRenderCount(figt13)).to.equal(2)
      expect(config.misc.calcRenderCount(elem10)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(elem132)).to.equal(2)
      expect(config.misc.renderIndexInRenderParent(elem133)).to.equal(3)
      expect(config.misc.renderIndexInRenderParent(elem14)).to.equal(4)
      expect(config.misc.renderIndexInRenderParent(figt13)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(1)
      expect(callNativeMessageList[0]).to.include(elem131.ref)
      callNativeMessageList.splice(0)

      // 删除elem12
      elem10.removeChild(elem12)

      expect(elem10.childNodes.length).to.equal(3)
      expect(elem10.layoutChildren.length).to.equal(3)
      expect(elem12.parentNode).to.equal(null)

      expect(config.helper.getNodeDepth(elem12)).to.equal(null)

      expect(config.misc.calcRenderCount(elem11)).to.equal(1)
      expect(config.misc.calcRenderCount(figt13)).to.equal(2)
      expect(config.misc.calcRenderCount(elem14)).to.equal(1)
      expect(config.misc.calcRenderCount(elem10)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(elem11)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt111)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt13)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(elem132)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(elem133)).to.equal(2)
      expect(config.misc.renderIndexInRenderParent(elem14)).to.equal(3)
      expect(config.misc.renderIndexInRenderParent(elem10)).to.equal(0)

      expect(callNativeMessageList.length).to.equal(1)
      expect(callNativeMessageList[0]).to.include(elem12.ref)
      callNativeMessageList.splice(0)

      // 移动节点到最前，顺序为：elem14, elem11, figt13
      elem10.insertBefore(elem14, elem11)

      expect(elem10.childNodes.length).to.equal(3)
      expect(elem10.layoutChildren.length).to.equal(3)
      expect(elem14.parentNode).to.equal(elem10)
      expect(elem11.parentNode).to.equal(elem10)

      expect(config.helper.getNodeDepth(elem14)).to.equal(3)
      expect(config.helper.getNodeDepth(elem11)).to.equal(3)

      expect(config.misc.calcRenderCount(elem14)).to.equal(1)
      expect(config.misc.calcRenderCount(elem11)).to.equal(1)
      expect(config.misc.calcRenderCount(figt13)).to.equal(2)
      expect(config.misc.calcRenderCount(elem10)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(elem14)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem11)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(figt111)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt13)).to.equal(2)
      expect(config.misc.renderIndexInRenderParent(elem132)).to.equal(2)
      expect(config.misc.renderIndexInRenderParent(elem133)).to.equal(3)
      expect(config.misc.renderIndexInRenderParent(elem10)).to.equal(0)

      expect(callNativeMessageList.length).to.equal(1)
      expect(callNativeMessageList[0]).to.include(elem14.ref)
      callNativeMessageList.splice(0)

      // 移动节点，恢复正常顺序
      elem10.appendChild(elem14)

      expect(elem10.childNodes.length).to.equal(3)
      expect(elem10.layoutChildren.length).to.equal(3)
      expect(elem11.parentNode).to.equal(elem10)
      expect(elem14.parentNode).to.equal(elem10)

      expect(config.helper.getNodeDepth(elem11)).to.equal(3)
      expect(config.helper.getNodeDepth(elem14)).to.equal(3)

      expect(config.misc.calcRenderCount(elem11)).to.equal(1)
      expect(config.misc.calcRenderCount(figt13)).to.equal(2)
      expect(config.misc.calcRenderCount(elem14)).to.equal(1)
      expect(config.misc.calcRenderCount(elem10)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(elem11)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt111)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt13)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(elem132)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(elem133)).to.equal(2)
      expect(config.misc.renderIndexInRenderParent(elem14)).to.equal(3)
      expect(config.misc.renderIndexInRenderParent(elem10)).to.equal(0)

      expect(callNativeMessageList.length).to.equal(1)
      expect(callNativeMessageList[0]).to.include(elem14.ref)
      callNativeMessageList.splice(0)
    })
  )

  /**
   * DOM树结构：
   * body
   *    figt10
   *        figt11
   *            elem111
   *        figt12
   *        elem13
   *            figt131
   *            figt132
   *            figt133
   *        figt14
   */
  it(
    '基于Figment的操作',
    (fnFigmentBase = function() {
      // 清理
      callNativeMessageList.splice(0)

      const figt10 = config.helper.createFigment('figt10')
      const figt11 = config.helper.createFigment('figt11')
      const figt12 = config.helper.createFigment('figt12')
      const figt14 = config.helper.createFigment('figt14')

      expect(config.helper.getNodeDepth(figt10)).to.equal(null)
      expect(config.helper.getNodeDepth(figt11)).to.equal(null)
      expect(config.helper.getNodeDepth(figt12)).to.equal(null)
      expect(config.helper.getNodeDepth(figt14)).to.equal(null)

      // 所有元素均为1
      nodeBody.appendChild(figt10)
      figt10.appendChild(figt11)
      figt10.appendChild(figt12)
      figt10.appendChild(figt14)

      expect(figt10.childNodes.length).to.equal(3)
      expect(figt10.layoutChildren.length).to.equal(3)
      expect(figt11.parentNode).to.equal(figt10)
      expect(figt12.parentNode).to.equal(figt10)
      expect(figt14.parentNode).to.equal(figt10)

      expect(config.helper.getNodeDepth(figt10)).to.equal(2)
      expect(config.helper.getNodeDepth(figt11)).to.equal(3)
      expect(config.helper.getNodeDepth(figt12)).to.equal(3)
      expect(config.helper.getNodeDepth(figt14)).to.equal(3)

      expect(config.misc.calcRenderCount(figt10)).to.equal(0)
      expect(config.misc.calcRenderCount(figt11)).to.equal(0)
      expect(config.misc.calcRenderCount(figt12)).to.equal(0)
      expect(config.misc.calcRenderCount(figt14)).to.equal(0)

      expect(config.misc.renderIndexInRenderParent(figt10)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt11)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt12)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt14)).to.equal(0)

      expect(callNativeMessageList.length).to.equal(0)
      callNativeMessageList.splice(0)

      // 加入空元素，父元素变化
      const elem111 = document.createElement('elem111')
      figt11.appendChild(elem111)

      expect(figt11.childNodes.length).to.equal(1)
      expect(figt11.layoutChildren.length).to.equal(1)
      expect(elem111.parentNode).to.equal(figt11)

      expect(config.helper.getNodeDepth(elem111)).to.equal(4)

      expect(config.misc.calcRenderCount(elem111)).to.equal(1)
      expect(config.misc.calcRenderCount(figt11)).to.equal(1)
      expect(config.misc.calcRenderCount(figt10)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(elem111)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt11)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt12)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(figt14)).to.equal(1)

      expect(callNativeMessageList.length).to.equal(1)
      expect(callNativeMessageList[0]).to.include(elem111.ref)
      callNativeMessageList.splice(0)

      // 加入空元素, 父元素变化
      const elem13 = document.createElement('elem13')
      figt10.insertBefore(elem13, figt10.childNodes[2])

      expect(figt10.childNodes.length).to.equal(4)
      expect(figt10.layoutChildren.length).to.equal(4)
      expect(elem13.parentNode).to.equal(figt10)

      expect(config.helper.getNodeDepth(elem13)).to.equal(3)

      expect(config.misc.calcRenderCount(elem13)).to.equal(1)
      expect(config.misc.calcRenderCount(figt10)).to.equal(2)

      expect(config.misc.renderIndexInRenderParent(elem13)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(figt14)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(1)
      expect(callNativeMessageList[0]).to.include(elem13.ref)
      callNativeMessageList.splice(0)

      // 元素中加入Figment，父元素不变
      const figt131 = config.helper.createFigment(document)
      elem13.appendChild(figt131)

      expect(elem13.childNodes.length).to.equal(1)
      expect(elem13.layoutChildren.length).to.equal(1)
      expect(figt131.parentNode).to.equal(elem13)

      expect(config.helper.getNodeDepth(figt131)).to.equal(4)

      expect(config.misc.calcRenderCount(figt131)).to.equal(0)
      expect(config.misc.calcRenderCount(elem13)).to.equal(1)
      expect(config.misc.calcRenderCount(figt10)).to.equal(2)

      expect(config.misc.renderIndexInRenderParent(figt131)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem13)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(figt14)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(0)
      callNativeMessageList.splice(0)

      // 元素中再加入Figment，父元素不变
      const figt133 = config.helper.createFigment(document)
      elem13.appendChild(figt133)

      expect(elem13.childNodes.length).to.equal(2)
      expect(elem13.layoutChildren.length).to.equal(2)
      expect(figt133.parentNode).to.equal(elem13)

      expect(config.helper.getNodeDepth(figt133)).to.equal(4)

      expect(config.misc.calcRenderCount(figt133)).to.equal(0)
      expect(config.misc.calcRenderCount(elem13)).to.equal(1)
      expect(config.misc.calcRenderCount(figt10)).to.equal(2)

      expect(config.misc.renderIndexInRenderParent(figt133)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem13)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(figt14)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(0)
      callNativeMessageList.splice(0)

      // 元素中再中间插入Figment，父元素不变
      const figt132 = config.helper.createFigment(document)
      elem13.insertBefore(figt132, elem13.childNodes[1])

      expect(elem13.childNodes.length).to.equal(3)
      expect(elem13.layoutChildren.length).to.equal(3)
      expect(figt132.parentNode).to.equal(elem13)

      expect(config.helper.getNodeDepth(figt132)).to.equal(4)

      expect(config.misc.calcRenderCount(figt132)).to.equal(0)
      expect(config.misc.calcRenderCount(elem13)).to.equal(1)
      expect(config.misc.calcRenderCount(figt10)).to.equal(2)

      expect(config.misc.renderIndexInRenderParent(figt132)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem13)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(figt14)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(0)
      callNativeMessageList.splice(0)

      // Figment中删除中间元素，父元素变化
      elem13.removeChild(figt131)

      expect(elem13.childNodes.length).to.equal(2)
      expect(elem13.layoutChildren.length).to.equal(2)
      expect(figt131.parentNode).to.equal(null)

      expect(config.helper.getNodeDepth(figt131)).to.equal(null)

      expect(config.misc.calcRenderCount(figt131)).to.equal(0)
      expect(config.misc.calcRenderCount(elem13)).to.equal(1)
      expect(config.misc.calcRenderCount(figt10)).to.equal(2)

      expect(config.misc.renderIndexInRenderParent(figt132)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt133)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem13)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(figt14)).to.equal(2)

      expect(callNativeMessageList.length).to.equal(0)
      callNativeMessageList.splice(0)

      // 删除figt12
      figt10.removeChild(figt12)

      expect(figt10.childNodes.length).to.equal(3)
      expect(figt10.layoutChildren.length).to.equal(3)
      expect(figt12.parentNode).to.equal(null)

      expect(config.helper.getNodeDepth(figt12)).to.equal(null)

      expect(config.misc.calcRenderCount(figt11)).to.equal(1)
      expect(config.misc.calcRenderCount(elem13)).to.equal(1)
      expect(config.misc.calcRenderCount(figt14)).to.equal(0)
      expect(config.misc.calcRenderCount(figt10)).to.equal(2)

      expect(config.misc.renderIndexInRenderParent(figt11)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem111)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem13)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(figt132)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt133)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt14)).to.equal(2)
      expect(config.misc.renderIndexInRenderParent(figt10)).to.equal(0)

      expect(callNativeMessageList.length).to.equal(0)
      callNativeMessageList.splice(0)

      // 移动节点到最前，顺序为：figt14, figt11, elem13
      figt10.insertBefore(figt14, figt11)

      expect(figt10.childNodes.length).to.equal(3)
      expect(figt10.layoutChildren.length).to.equal(3)
      expect(figt14.parentNode).to.equal(figt10)
      expect(figt11.parentNode).to.equal(figt10)

      expect(config.helper.getNodeDepth(figt14)).to.equal(3)
      expect(config.helper.getNodeDepth(figt11)).to.equal(3)

      expect(config.misc.calcRenderCount(figt14)).to.equal(0)
      expect(config.misc.calcRenderCount(figt11)).to.equal(1)

      expect(config.misc.renderIndexInRenderParent(figt14)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt11)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem111)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem13)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(figt132)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt133)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt10)).to.equal(0)

      expect(callNativeMessageList.length).to.equal(0)
      callNativeMessageList.splice(0)

      // 移动节点，恢复正常顺序
      figt10.appendChild(figt14)

      expect(figt10.childNodes.length).to.equal(3)
      expect(figt10.layoutChildren.length).to.equal(3)
      expect(figt11.parentNode).to.equal(figt10)
      expect(figt14.parentNode).to.equal(figt10)

      expect(config.helper.getNodeDepth(figt11)).to.equal(3)
      expect(config.helper.getNodeDepth(figt14)).to.equal(3)

      expect(config.misc.calcRenderCount(figt11)).to.equal(1)
      expect(config.misc.calcRenderCount(figt14)).to.equal(0)

      expect(config.misc.renderIndexInRenderParent(figt11)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem111)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(elem13)).to.equal(1)
      expect(config.misc.renderIndexInRenderParent(figt132)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt133)).to.equal(0)
      expect(config.misc.renderIndexInRenderParent(figt14)).to.equal(2)
      expect(config.misc.renderIndexInRenderParent(figt10)).to.equal(0)

      expect(callNativeMessageList.length).to.equal(0)
      callNativeMessageList.splice(0)
    })
  )

  /**
   * DOM树结构：
   * body
   *    elem10
   *        elem11
   *        cmmt12
   *        elem13
   *        cmmt14
   *        elem15
   */
  it('对注释节点的支持', () => {
    // 清理
    callNativeMessageList.splice(0)

    const elem10 = document.createElement('elem10')
    const elem11 = document.createElement('elem11')
    const cmmt12 = document.createComment('cmmt12')
    const elem13 = document.createElement('elem13')
    const cmmt14 = document.createComment('cmmt14')
    const elem15 = document.createElement('elem15')

    expect(config.helper.getNodeDepth(elem10)).to.equal(null)
    expect(config.helper.getNodeDepth(elem11)).to.equal(null)
    expect(config.helper.getNodeDepth(cmmt12)).to.equal(null)
    expect(config.helper.getNodeDepth(elem13)).to.equal(null)
    expect(config.helper.getNodeDepth(cmmt14)).to.equal(null)
    expect(config.helper.getNodeDepth(elem15)).to.equal(null)

    // 所有元素均为1
    nodeBody.appendChild(elem10)
    elem10.appendChild(elem11)
    elem10.appendChild(cmmt12)

    expect(elem10.childNodes.length).to.equal(2)
    expect(elem10.layoutChildren.length).to.equal(2)
    expect(elem11.parentNode).to.equal(elem10)
    expect(cmmt12.parentNode).to.equal(elem10)

    expect(config.helper.getNodeDepth(elem10)).to.equal(2)
    expect(config.helper.getNodeDepth(elem11)).to.equal(3)
    expect(config.helper.getNodeDepth(cmmt12)).to.equal(3)

    expect(callNativeMessageList.length).to.equal(2)
    expect(callNativeMessageList[0]).to.include('elem10')
    expect(callNativeMessageList[1]).to.include('elem11')
    callNativeMessageList.splice(0)

    // 增加elem15节点
    elem10.appendChild(elem15)
    expect(elem15.parentNode).to.equal(elem10)
    expect(config.helper.getNodeDepth(elem15)).to.equal(3)

    expect(config.misc.renderIndexInRenderParent(elem10)).to.equal(0)
    expect(config.misc.renderIndexInRenderParent(elem11)).to.equal(0)
    expect(config.misc.renderIndexInRenderParent(cmmt12)).to.equal(1)
    expect(config.misc.renderIndexInRenderParent(elem15)).to.equal(1)

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('elem15')
    callNativeMessageList.splice(0)

    // 插入cmmt14节点
    elem10.insertBefore(cmmt14, elem15)
    expect(cmmt14.parentNode).to.equal(elem10)
    expect(config.helper.getNodeDepth(cmmt14)).to.equal(3)
    expect(config.misc.renderIndexInRenderParent(cmmt14)).to.equal(1)

    expect(callNativeMessageList.length).to.equal(0)
    callNativeMessageList.splice(0)

    // 插入elem13节点
    elem10.insertBefore(elem13, cmmt14)
    expect(elem13.parentNode).to.equal(elem10)
    expect(config.helper.getNodeDepth(elem13)).to.equal(3)
    expect(config.misc.renderIndexInRenderParent(elem13)).to.equal(1)

    expect(callNativeMessageList.length).to.equal(1)
    expect(callNativeMessageList[0]).to.include('elem13')
    callNativeMessageList.splice(0)
  })

  it.skip('基于Element的多级操作', function() {
    fnElementBase()
  })

  it.skip('基于Figment的多级操作', function() {
    fnFigmentBase()
  })
})
