/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// 缓存原始控制台
let _oriConsole = null
// 日志等级
const _logLevels = ['off', 'error', 'warn', 'info', 'log', 'debug', 'trace']
// 日志等级检索
let _levelMap = {}

/**
 * 生成日志等级的矩阵表，用于迅速判断日志消息是否处理
 */
function _makeLevelMap() {
  _logLevels.forEach(level => {
    const levelIndex = _logLevels.indexOf(level)
    _levelMap[level] = {}
    _logLevels.forEach(type => {
      const typeIndex = _logLevels.indexOf(type)
      if (typeIndex <= levelIndex) {
        _levelMap[level][type] = true
      }
    })
  })
}

/**
 * 判断日志消息是否被处理
 * @param  {string} type
 * @return {boolean}
 */
function _checkLevel(type) {
  // 获取当前log等级
  const logLevel = (global.Env && global.Env.logLevel) || 'log'
  return _levelMap[logLevel] && _levelMap[logLevel][type]
}

/**
 * 根据运行环境设置控制台
 */
function setNativeConsole() {
  _oriConsole = global.console
  _makeLevelMap()

  const { trace, debug, log, info, warn, error, time, timeEnd, record } = console
  const globalConsole = console
  console._ori = { trace, debug, log, info, warn, error, time, timeEnd, record }

  if (!console._ori.record) {
    console._ori.record = console._ori.info
  }

  globalConsole.trace = (...args) => {
    if (_checkLevel('trace')) {
      console._ori.debug.apply(console, args)
    }
  }
  globalConsole.debug = (...args) => {
    if (_checkLevel('debug')) {
      ;(console._ori.debug || console._ori.trace).apply(console, args)
    }
  }
  globalConsole.log = (...args) => {
    if (_checkLevel('log')) {
      console._ori.log.apply(console, args)
    }
  }
  globalConsole.info = (...args) => {
    if (_checkLevel('info')) {
      console._ori.info.apply(console, args)
    }
  }
  globalConsole.warn = (...args) => {
    if (_checkLevel('warn')) {
      console._ori.warn.apply(console, args)
    }
  }
  globalConsole.error = (...args) => {
    if (_checkLevel('error')) {
      console._ori.error.apply(console, args)
    }
  }
  globalConsole.time = (...args) => {
    if (global.Env && global.Env.logPerf && _checkLevel('info')) {
      console._ori.time.apply(console, args)
    }
  }
  globalConsole.timeEnd = (...args) => {
    if (global.Env && global.Env.logPerf && _checkLevel('info')) {
      console._ori.timeEnd.apply(console, args)
    }
  }
  globalConsole.record = (...args) => {
    if (global.Env && global.Env.logPerf && _checkLevel('info')) {
      console._ori.record.apply(console, args)
    }
  }
}

/**
 * 恢复原始控制台
 */
function resetNativeConsole() {
  _levelMap = {}
  global.console = _oriConsole
}

export default {
  setNativeConsole,
  resetNativeConsole
}
