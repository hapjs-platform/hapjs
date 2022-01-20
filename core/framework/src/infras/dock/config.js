/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Pubsub from 'src/shared/pubsub'

const config = {}

// 拥有事件分发机制
Object.setPrototypeOf(config, new Pubsub())

export default config
