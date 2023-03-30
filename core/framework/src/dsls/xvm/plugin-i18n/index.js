/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { installFactory, getVmClass } from './install'

import BaseFormatter from './format'
import I18nPath from './path'

import { isPlainObject, isNull, parseArgs } from './util'

const defaultFormatter = new BaseFormatter()

class PluginI18n {
  /**
   * @param options 插件支持的选项
   * @param options.locale {string}
   * @param options.resources {array}
   * @param options.resources[0] {object}
   * @param options.resources[0].message {object}
   * @param options.formatter {object} 拥有interpolate方法
   * @param options.root {Vmclass} 页面级Vm实例
   * @param options.fallbackRoot {boolean} 如果value不存在，是否使用页面级Vm的设置;
   */
  constructor(options) {
    const locale = options.locale || 'en'
    const resources = options.resources || []

    this._vm = null
    this._formatter = options.formatter || defaultFormatter
    this._root = options.root || null
    this._fallbackRoot = options.fallbackRoot === undefined ? true : !!options.fallbackRoot

    this._path = new I18nPath()

    this._initVm({
      locale,
      resources
    })
  }

  _initVm(options) {
    const VmClass = getVmClass()
    this._vm = new VmClass({
      data: options
    })
  }

  _destroyVm() {
    this._vm._emit('xlc:onDestroy')
  }

  get locale() {
    return this._vm.locale
  }

  set locale(v) {
    this._vm.$set('locale', v)
  }

  get resources() {
    return this._vm.resources
  }

  set resources(v) {
    this._vm.$set('resources', v)
  }

  _getResources() {
    return this._vm.resources
  }

  _isFallbackRoot(val) {
    const isThisNotRootI18n = this._root && this._root._i18n !== this
    return !isNull(val) && isThisNotRootI18n && this._fallbackRoot
  }

  _warnDefault(locale, key, ret, host, values) {
    if (!isNull(ret)) {
      return ret
    } else {
      console.warn(`### App Framework ### i18n：locale文件中没有找到对应的key：${key}`)
      return key
    }
  }

  _t(key, defLocale, resources, host, ...values) {
    if (!key) {
      return ''
    }
    const parsedArgs = parseArgs(...values)
    const locale = parsedArgs.locale || defLocale
    const ret = this._translate(resources, locale, key, host, 'string', parsedArgs.params)

    if (this._isFallbackRoot(ret)) {
      return this._root.$t(key, ...values)
    } else {
      return this._warnDefault(locale, key, ret, host, values)
    }
  }

  _translate(resources, locale, key, host, interpolateMode, params) {
    if (!resources) {
      return null
    }
    for (let i = 0, len = resources.length; i < len; i++) {
      const ret = this._interpolate(locale, resources[i], key, host, interpolateMode, params, [key])
      if (!isNull(ret)) {
        return ret
      }
    }
    return null
  }

  _interpolate(locale, jsonObject, key, host, interpolateMode, params, visitedLinkStack) {
    if (!jsonObject) {
      return null
    }

    const keyRet = this._path.getPathValue(jsonObject, key)
    if (isPlainObject(keyRet) || Array.isArray(keyRet)) {
      return keyRet
    }

    if (isNull(keyRet)) {
      return null
    }

    if (typeof keyRet === 'string' && keyRet.indexOf('@:') >= 0 && keyRet.indexOf('@.') >= 0) {
      // TODO #Linked locale
    }

    return this._render(keyRet, interpolateMode, params, key)
  }

  _render(message, interpolateMode, params, key) {
    let ret = this._formatter.interpolate(message, params, key)
    if (!ret && this._formatter !== defaultFormatter) {
      ret = defaultFormatter.interpolate(message, params, key)
    }
    return interpolateMode === 'string' ? ret.join('') : ret
  }

  _tc(key, defLocale, resources, host, choice, ...params) {
    if (choice === undefined) {
      choice = 1
    }

    const predefined = {
      count: choice,
      n: choice
    }
    const parsedArgs = parseArgs(...params)
    parsedArgs.params = Object.assign(predefined, parsedArgs.params)
    params =
      parsedArgs.locale === null ? [parsedArgs.params] : [parsedArgs.locale, parsedArgs.params]
    return this._fetchChoice(this._t(key, defLocale, resources, host, ...params), choice)
  }

  _fetchChoice(optionLine, choice) {
    if (typeof optionLine !== 'string') {
      console.warn(`### App Framework ### i18n：处理复数的值定义应该为string类型：${optionLine}`)
      return null
    }

    const choices = optionLine.split('|')
    choice = this._getChoiceIndex(choice, choices.length)
    if (!choices[choice]) {
      return optionLine
    }

    return choices[choice].trim()
  }

  _getChoiceIndex(choice, len) {
    choice = Math.abs(choice)
    if (len === 2) {
      return choice ? (choice > 1 ? 1 : 0) : 1
    }
    return choice ? Math.min(choice, 2) : 0
  }
}

PluginI18n.install = installFactory(`PluginI18n`, PluginI18n)
PluginI18n.version = '__VERSION__'

export default PluginI18n
