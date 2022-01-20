/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import '../../../bridge/imports'

import { uniqueId, initPage, boot, defaultAppId } from '../../../bridge/platform/index'

boot('xvm')

export { uniqueId, initPage, defaultAppId }
