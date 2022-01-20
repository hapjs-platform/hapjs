/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import base from './base'

export const config = {
  name: 'system.animation',
  methods: [
    {
      name: 'enable',
      type: 0,
      mode: 0
    },
    {
      name: 'play',
      type: 0,
      mode: 0
    },
    {
      name: 'finish',
      type: 0,
      mode: 0
    },
    {
      name: 'pause',
      type: 0,
      mode: 0
    },
    {
      name: 'cancel',
      type: 0,
      mode: 0
    },
    {
      name: 'reverse',
      type: 0,
      mode: 0
    },
    {
      name: 'getFinished',
      type: 0,
      mode: 0
    },
    {
      name: 'setStartTime',
      type: 0,
      mode: 0
    },
    {
      name: 'getStartTime',
      type: 0,
      mode: 0
    },
    {
      name: 'getPlayState',
      type: 0,
      mode: 0
    }
  ]
}

const moduleOwn = Object.assign(
  {
    name: 'system.animation',
    _currentState: 'idle',
    _isFinished: 'false',
    _startTime: 0,

    enable(options = {}) {
      return this.mockSync(options._data, options._code)
    },

    play() {
      this._currentState = 'running'
      this._isFinished = 'false'
    },

    finish() {
      this._isFinished = 'true'
      this._currentState = 'finished'
    },

    pause() {
      this._currentState = 'paused'
      this._isFinished = 'false'
    },

    cancel() {
      this._isFinished = 'true'
      this._currentState = 'idle'
    },

    reverse() {
      this._currentState = 'running'
      this._isFinished = 'false'
    },

    getFinished() {
      return this.mockSync(this._isFinished)
    },

    getPlayState() {
      return this.mockSync(this._currentState)
    },

    setStartTime(params = {}) {
      this._startTime = params.startTime
    },

    getStartTime() {
      return this.mockSync(this._startTime)
    }
  },
  base
)

export default moduleOwn
