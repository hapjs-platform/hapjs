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
  initWorkerPlatform,
  initWorker
} from '../../bridge/platform/index'

// 准备环境
initWorkerPlatform()

export { uniqueId, initWorker, commandsList, callActionJsonList }
