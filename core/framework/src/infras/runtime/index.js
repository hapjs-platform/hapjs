/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { Node, Event, TouchEvent, freeze } from './dom/index'

import * as helper from './helper'

function init() {
  freeze()
}

export default {
  init,
  Node,
  Event,
  TouchEvent,
  helper,
  exposure: {
    registerComponents: helper.registerComponents
  }
}
