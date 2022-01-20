/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import DomElement from './element'

/**
 * 表单元素
 */
class DomFormerElement extends DomElement {
  get value() {
    return this._attr && this._attr.value
  }

  set value(val) {
    if (this._attr) {
      this._attr.value = val
    }
  }

  get checked() {
    return this._attr && this._attr.checked
  }

  set checked(checked) {
    if (this._attr) {
      this._attr.checked = checked
    }
  }
}

export default DomFormerElement
