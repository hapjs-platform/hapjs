/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { uniqueId, initPage, commandsList } from '../../../imports'

describe('框架：03.Animation', () => {
  const pageId = uniqueId()
  let page, pageVm, testAnimationNode

  before(() => {
    commandsList.splice(0)

    initPage(pageId, null, __dirname)
    page = global.getPage(pageId)
    pageVm = page.vm
  })

  after(() => {
    commandsList.splice(0)

    global.destroyPage(pageId)
  })

  beforeEach(() => {})

  afterEach(() => {})

  it('Animation 命令发送', async () => {
    const keyframes = [
      {
        width: '120px',
        height: '120px',
        time: 0
      },
      {
        width: '300px',
        height: '300px',
        time: 100
      }
    ]
    const options = {
      duration: 1000,
      easing: 'cubic-bezier(0, 0.6, 0.6, 0)',
      delay: 300,
      fill: 'none',
      iterations: 3,
      needLayout: true
    }
    testAnimationNode = pageVm.$element('test-animation')
    const animateInstance = testAnimationNode.animate(keyframes, options)

    animateInstance.startTime = 1000
    expect(animateInstance.startTime).to.equal(1000)

    animateInstance.play()
    expect(animateInstance.playState).to.equal('running')
    expect(animateInstance.finished).to.equal('false')

    animateInstance.finish()
    expect(animateInstance.playState).to.equal('finished')
    expect(animateInstance.finished).to.equal('true')

    animateInstance.play()
    animateInstance.reverse()
    expect(animateInstance.playState).to.equal('running')
    expect(animateInstance.finished).to.equal('false')

    animateInstance.pause()
    expect(animateInstance.playState).to.equal('paused')
    expect(animateInstance.finished).to.equal('false')

    animateInstance.cancel()
    expect(animateInstance.playState).to.equal('idle')
    expect(animateInstance.finished).to.equal('true')
  })

  it('Animation参数校验', () => {
    const keyframes = []
    const options = {
      iterations: Infinity
    }
    testAnimationNode = pageVm.$element('test-animation')
    const animateInstance = testAnimationNode.animate(keyframes, options)
    expect(animateInstance.initParams.options.iterations).to.equal(-1)
  })
})
