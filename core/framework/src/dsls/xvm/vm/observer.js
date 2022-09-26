/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { isObject, isPlainObject } from 'src/shared/util'

import XLinker from './linker'

import { $def, $remove, hasProto, $own, arrayMethods } from '../util'

// 修改后的Array的方法列表
const arrayKeys = Object.getOwnPropertyNames(arrayMethods)

/**
 * 拦截目标对象的原型链
 * @param target
 * @param src
 */
function protoArgment(target, src) {
  /* eslint-disable no-proto */
  target.__proto__ = src
  /* eslint-enable no-proto */
}

/**
 * 在目标对象上定义方法
 */
function copyArgment(target, src, keys) {
  for (let i = 0, l = keys.length; i < l; i++) {
    const key = keys[i]
    $def(target, key, src[key])
  }
}

/**
 * 观察器，修改目标对象属性的set/get, 当属性修改时发送通知
 * @param value
 * @constructor
 */
export class XObserver {
  constructor(value) {
    this.value = value
    // console.trace(`### App Framework ### 创建 XObserver----目标:`, JSON.stringify(value))

    this.dep = new XLinker()
    // 给被观察的对象添加一个'__ob__'属性
    $def(value, '__ob__', this)

    // 如果目标对象是数组，替换数组的方法
    if (Array.isArray(value)) {
      const aug = hasProto ? protoArgment : copyArgment
      aug(value, arrayMethods, arrayKeys)

      this.observeArray(value)
    } else {
      // 如果value是对象类型，则遍历属性
      this.walk(value)
    }
  }

  /**
   * 遍历对象属性，将属性转换为get/set(只能用于对象)
   * @param obj
   */
  walk(obj) {
    for (const key in obj) {
      this.convert(key, obj[key])
    }
  }

  /**
   * 给数组元素添加观察器
   * @param items
   */
  observeArray(items) {
    for (let i = 0, l = items.length; i < l; i++) {
      XObserver.$ob(items[i])
    }
  }

  /**
   * 将属性转换为get/set，当属性被访问或修改时发送事件
   * @param key
   * @param val
   */
  convert(key, val) {
    defineReactive(this.value, key, val)
  }

  /**
   * 关联vm
   * @param vm
   */
  addVm(vm) {
    ;(this.vms || (this.vms = [])).push(vm)
  }

  /**
   * 取消关联vm
   */
  removeVm(vm) {
    $remove(this.vms, vm)
  }
}

/**
 * 观察目标对象，返回观察器
 * @param value
 * @param vm
 * @returns {*}
 */
XObserver.$ob = function(value, vm) {
  // <script>中导出的data只能是对象或者函数（返回值为对象）
  if (!isObject(value)) {
    return
  }

  let ob
  // 检查是否已经有观察器
  if ($own(value, '__ob__') && value.__ob__ instanceof XObserver) {
    ob = value.__ob__
  } else if (
    (Array.isArray(value) || isPlainObject(value)) &&
    Object.isExtensible(value) &&
    !value._isXVm &&
    !value.nodeType
  ) {
    // 如果对象可观察，则创建观察器
    ob = new XObserver(value)
  }
  // 如果有VM,则关联
  if (ob && vm) {
    ob.addVm(vm) // 将vm添加到ob.vms数组中
  }
  return ob
}

/**
 * 递归式反馈
 * @param obj
 * @param key
 * @param val
 * @param customSetter
 */
export function defineReactive(obj, key, val, customSetter) {
  const dep = new XLinker()

  // 获取属性的元信息，跳过不可修改的属性
  const property = Object.getOwnPropertyDescriptor(obj, key)
  if (property && property.configurable === false) {
    return
  }

  // 获取属性当前的get和set函数
  const getter = property && property.get
  const setter = property && property.set

  // 属性值如果是对象的话，递归下去
  let childOb = XObserver.$ob(val)
  // console.trace(`### App Framework ### 观察属性 ${key}`)

  Object.defineProperty(obj, key, {
    enumerable: true,
    configurable: true,
    get: function reactiveGetter() {
      // console.trace(`### App Framework ### 调用被观察属性 Getter ${key}`)
      const value = getter ? getter.call(obj) : val

      // 如果有watcher（函数执行上下文），则与watcher建立联系
      if (XLinker.target) {
        dep.depend()

        if (childOb) {
          childOb.dep.depend()
        }
        // 如果是数组, 数组每个元素建立依赖
        if (Array.isArray(value)) {
          for (let e, i = 0, l = value.length; i < l; i++) {
            e = value[i]
            e && e.__ob__ && e.__ob__.dep.depend()
          }
        }
      }
      return value
    },
    set: function reactiveSetter(newVal) {
      // console.trace(`### App Framework ### 调用被观察属性 Setter ${key}`)

      const value = getter ? getter.call(obj) : val
      if (newVal === value) {
        return
      }

      if (customSetter) {
        customSetter()
      }
      if (setter) {
        setter.call(obj, newVal)
      } else {
        val = newVal
      }
      // 为新值重新创建观察器
      childOb = XObserver.$ob(newVal)
      // 通知所有关联的watcher更新
      dep.notify()
    }
  })
}
