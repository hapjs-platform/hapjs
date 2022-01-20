/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * @file 无需Native支持，打包生成web.js，与之前一致；
 */

import './index'
import dsl from 'src/dsls/xvm/index'
global.registerDsl(dsl)
