/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import extend from './extend'
import mixin from './mixin'

let VmClassInner

export function installFactory(pluginClassName, pluginClass) {
  return function install(VmClass) {
    // 相互持有引用
    VmClass.prototype[pluginClassName] = pluginClass
    VmClassInner = VmClass

    extend(VmClass)
    VmClass.mixin(mixin)
  }
}

export function getVmClass() {
  if (!VmClassInner) {
    throw new Error(`i18n：找不到Vm实例所属的类，请在插件安装后再实例化`)
  }
  return VmClassInner
}
