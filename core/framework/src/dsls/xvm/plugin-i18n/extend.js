/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * 在VM原型上增加新的方法
 * @param VmClass
 */
export default function extend(VmClass) {
  Object.defineProperty(VmClass.prototype, `$i18n`, {
    get() {
      return this._i18n
    }
  })

  /**
   * 简单的格式化
   * @param {string} key 路径名
   * @param {array} others 额外的参数；第一个元素为格式化时的参数，类型为{object|array}
   * @return {*}
   */
  VmClass.prototype.$t = function(key, ...others) {
    const i18n = this._i18n
    return i18n._t(key, i18n.locale, i18n._getResources(), this, ...others)
  }

  /**
   * 单复数的格式化
   * @param {string} key 路径名
   * @param {number} choice 实际的值
   * @param {array} others 额外的参数
   * @return {*}
   */
  VmClass.prototype.$tc = function(key, choice, ...others) {
    const i18n = this._i18n
    return i18n._tc(key, i18n.locale, i18n._getResources(), this, choice, ...others)
  }
}
