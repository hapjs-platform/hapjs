/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Session from '../../../../../src/infras/platform/session'
import { expect } from 'chai'

it('test session', () => {
  function Connection() {
    this.disconnect = () => {}
  }
  global.Connection = Connection
  const session = new Session()
  session.connect()
  expect(() => session.connect()).to.throw('The inspector session has already connected')
  expect(() => session.post({})).to.throw('method must be a string')
  session.disconnect()
  expect(() => session.post('profiler.start')).to.throw()
})
