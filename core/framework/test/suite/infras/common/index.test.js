/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/* eslint-disable no-unused-expressions */

import './imports'

import common from 'src/infras/common'

describe('公共库测试', () => {
  it('Object.assign', () => {
    expect(typeof Object.assign).to.be.equal('function')
  })

  it('setTimeout', () => {
    expect(typeof setTimeout).to.be.equal('function')
  })

  it('console.log', () => {
    expect(typeof console.log).to.be.equal('function')
  })

  it('setNativeConsole', () => {
    expect(typeof common.setNativeConsole).to.be.equal('function')
  })

  it('resetNativeConsole', () => {
    expect(typeof common.setNativeConsole).to.be.equal('function')
  })

  it('setNativeTimer', () => {
    expect(typeof common.setNativeConsole).to.be.equal('function')
  })

  it('resetNativeTimer', () => {
    expect(typeof common.setNativeConsole).to.be.equal('function')
  })

  it('freezePrototype', () => {
    expect(typeof common.setNativeConsole).to.be.equal('function')
  })
})

describe('对象原型冻结测试', function() {
  before(() => {
    common.freezePrototype()
  })

  it('Object原型', () => {
    expect(Object).to.be.frozen
    expect(Object.prototype).to.be.frozen
  })

  it('Array原型', () => {
    expect(Array).to.be.frozen
    expect(Array.prototype).to.be.frozen
  })

  it('String原型', () => {
    expect(String.prototype).to.be.frozen
  })
  it('Number原型', () => {
    expect(Number.prototype).to.be.frozen
  })
  it('Boolean原型', () => {
    expect(Boolean.prototype).to.be.frozen
  })
  it('Error原型', () => {
    expect(Error.prototype).to.be.frozen
  })
  it('Date原型', () => {
    expect(Date.prototype).to.be.frozen
  })
  it('Math原型', () => {
    expect(Math.prototype).to.be.frozen
  })
  it('RegExp原型', () => {
    expect(RegExp.prototype).to.be.frozen
  })
})
