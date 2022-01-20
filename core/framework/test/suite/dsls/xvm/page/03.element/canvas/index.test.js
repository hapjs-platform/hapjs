/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, commandsList } from '../../../imports'

describe('框架：03.Canvas', () => {
  const pageId = uniqueId()
  let page, pageVm

  before(() => {
    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {
    commandsList.splice(0)

    global.destroyPage(pageId)
  })

  beforeEach(() => {
    commandsList.splice(0)
  })

  afterEach(() => {})

  it('Canvas命令发送', async () => {
    const canvas = pageVm.$element('canvasInst1')
    const ctx = canvas.getContext('2d')

    ctx.fillText('hello', 99, 100)
    await waitForOK()
    expect(commandsList[0]).to.include('a?')

    ctx.strokeText('hello', 99, 100)
    await waitForOK()
    expect(commandsList[1]).to.include('o?')

    ctx.rect(1, 2, 3, 4)
    await waitForOK()
    expect(commandsList[2]).to.include('f?')

    ctx.putImageData(
      {
        width: 10,
        height: 10,
        data: new Uint8ClampedArray(10 * 10 * 4)
      },
      0,
      0
    )
    await waitForOK()
    expect(commandsList[3]).to.include('d?')

    ctx.getImageData(0, 0, 100, 100)
    expect(commandsList[4]).to.include('@?')

    // setLineDash 参数值为数组，且元素为数字类型，不能为 NaN 且元素值不小于0
    ctx.setLineDash(0)
    expect(ctx.getLineDash().length).to.equal(0)
    await waitForOK()
    expect(commandsList[5]).to.be.an('undefined')

    ctx.setLineDash([1, false])
    expect(ctx.getLineDash().length).to.equal(0)
    await waitForOK()
    expect(commandsList[5]).to.be.an('undefined')

    ctx.setLineDash([1, -1])
    expect(ctx.getLineDash().length).to.equal(0)
    await waitForOK()
    expect(commandsList[5]).to.be.an('undefined')

    ctx.setLineDash([1, NaN])
    expect(ctx.getLineDash().length).to.equal(0)
    await waitForOK()
    expect(commandsList[5]).to.be.an('undefined')

    ctx.setLineDash([1, 2])
    expect(ctx.getLineDash().length).to.equal(2)
    expect(ctx.getLineDash()[0]).to.equal(1)
    expect(ctx.getLineDash()[1]).to.equal(2)
    await waitForOK()
    expect(commandsList[5]).to.equal('k?1,2;')

    ctx.setLineDash([1])
    expect(ctx.getLineDash().length).to.equal(2)
    expect(ctx.getLineDash()[0]).to.equal(1)
    expect(ctx.getLineDash()[1]).to.equal(1)
    await waitForOK()
    expect(commandsList[6]).to.equal('k?1,1;')
  })

  it('CreateImageData返回值', () => {
    const canvas = pageVm.$element('canvasInst1')
    const ctx = canvas.getContext('2d')

    const imagedata = ctx.createImageData(10, 10)
    expect(imagedata).to.have.all.keys('width', 'height', 'data')
    expect(imagedata.width).to.equal(10)
    expect(imagedata.height).to.equal(10)
    expect(imagedata.data).to.be.a('Uint8ClampedArray')
    expect(imagedata.data).to.have.lengthOf(10 * 10 * 4)

    const imagedata2 = ctx.createImageData({
      width: 10,
      height: 20,
      data: new Uint8ClampedArray(10 * 20 * 4)
    })
    expect(imagedata2).to.have.all.keys('width', 'height', 'data')
    expect(imagedata2.width).to.equal(10)
    expect(imagedata2.height).to.equal(20)
    expect(imagedata2.data).to.be.a('Uint8ClampedArray')
    expect(imagedata2.data).to.have.lengthOf(10 * 20 * 4)
  })

  it('Canvas lineDashOffset', () => {
    const canvas = pageVm.$element('canvasInst1')
    const ctx = canvas.getContext('2d')

    // lineDashOffset 属性值必须为数组类型且不能为 NaN
    expect(ctx.lineDashOffset).to.equal(0)

    ctx.lineDashOffset = '22'
    expect(ctx.lineDashOffset).to.equal(0)

    ctx.lineDashOffset = NaN
    expect(ctx.lineDashOffset).to.equal(0)

    ctx.lineDashOffset = 5
    expect(ctx.lineDashOffset).to.equal(5)
  })

  it('toTempFilePath方法', async () => {
    const canvas = pageVm.$element('canvasInst1')
    const ctx = canvas.getContext('2d')

    // 确保在保存图片之前发送绘制指令
    ctx.fillText('hello', 99, 100)
    expect(commandsList.length).to.equal(0)
    canvas.toTempFilePath()
    expect(commandsList.length).to.equal(1)
    expect(commandsList[0]).to.include('a?')
    await waitForOK()
    expect(commandsList.length).to.equal(1)
  })

  it('Canvas绘制中文及特殊字符', async () => {
    const canvas = pageVm.$element('canvasInst1')
    const ctx = canvas.getContext('2d')

    ctx.fillText('一', 99, 100)
    await waitForOK()
    expect(commandsList[0]).to.include('5LiA')

    ctx.fillText('ВГДЕЖ', 99, 100)
    await waitForOK()
    expect(commandsList[1]).to.include('0JLQk9CU0JXQlg==')

    ctx.strokeText('ηθλ ικ', 99, 100)
    await waitForOK()
    expect(commandsList[2]).to.include('zrfOuM67IM65zro=')

    ctx.fillText('𠮷𠾷𠿸丽侮', 99, 100)
    await waitForOK()
    expect(commandsList[3]).to.include('8KCut/CgvrfwoL+48K+ggPCvoIU=')
  })
})
