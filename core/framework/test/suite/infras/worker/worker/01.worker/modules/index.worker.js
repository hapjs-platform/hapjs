/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import './part1'
import './part2'

// 引入全部接口
import system from '@system'
// 示例接口
import sample from '@system.sample'

// 全局引用
const globalRef = Object.getPrototypeOf(global)

globalRef.model = {
  system: system,
  sample: sample,
  // 读写
  opsTestReadwrite() {
    sample.readwrite = 'readwrite-v2'
    const readwrite = sample.readwrite
    return readwrite
  },
  opsTestAttrReadwrite() {
    return sample.getAttr('readwrite')
  },
  // 只读
  opsTestReadonly() {
    sample.readonly = 'readonly-v2'
    const readonly = sample.readonly
    return readonly
  },
  opsTestAttrReadonly() {
    return sample.getAttr('readonly')
  },
  // 只写
  opsTestWriteonly() {
    sample.writeonly = 'writeonly-v2'
    const writeonly = sample.writeonly
    return writeonly
  },
  opsTestAttrWriteonly() {
    return sample.getAttr('writeonly')
  },

  // 事件
  opsTestCustomEvent1() {
    return (sample.onCustomEvent1 = function(ret) {
      this.onCustomEvent1Result = ret
    }.bind(this))
  },
  // 事件：删除回调
  opsTestCustomEvent1Clear() {
    return (sample.onCustomEvent1 = null)
  },
  // 事件：赋值对象会警告并不生效
  opsTestCustomEvent1Object() {
    return (sample.onCustomEvent1 = {})
  }
}
