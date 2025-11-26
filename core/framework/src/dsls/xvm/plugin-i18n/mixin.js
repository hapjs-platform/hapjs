/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { isPlainObject } from './util'

/**
 * 生命周期的混入处理
 */
export default {
  /**
   * 初始化i18n选项
   */
  onCreate() {
    const options = this._options

    // 仅限页面Vm
    if (this.$root() === this && options.i18n) {
      const I18nClass = this.PluginI18n

      if (isPlainObject(options.i18n)) {
        // 传递的是对象
        options.i18n.root = this

        this._i18n = new I18nClass(options.i18n)
      } else if (options.i18n instanceof I18nClass) {
        // 传递的是类实例
        this._i18n = options.i18n
      }
    } else if (this._parent && this._parent._i18n) {
      this._i18n = this._parent._i18n
    } else if (this._root && this._root !== this && this._root._i18n) {
      this._i18n = this._root._i18n
    }
  },
  onConfigurationChanged(evt) {
    // 页面级Vm
    if (evt.type === 'locale') {
      if (this.$root() === this && this._i18n && this._page && this._page.app) {
        const i18n = this._i18n
        const ret = this._page.app.getLocaleConfig()
        const retNew = JSON.parse(JSON.stringify(ret))
        i18n.locale = retNew.locale
        i18n.resources = retNew.resources
      }
    }
  },
  onDestroy() {
    // 页面级Vm
    if (this.$root() === this && this._i18n) {
      const i18n = this._i18n
      i18n._destroyVm()
    }
    this._i18n = null
  }
}
