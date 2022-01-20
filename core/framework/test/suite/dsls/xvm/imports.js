/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../../bridge/imports'

import {
  commandsList,
  callActionJsonList,
  uniqueId,
  initApp,
  initPage,
  boot,
  proxyCallNative,
  unproxyCallNative
} from '../../bridge/platform/index'

boot('xvm')

export {
  uniqueId,
  initApp,
  initPage,
  commandsList,
  callActionJsonList,
  proxyCallNative,
  unproxyCallNative
}
