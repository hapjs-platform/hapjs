/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { version } from '../../../../package.json'
import platform from 'src/infras/platform/interface'
import dock from './interface'

// 框架版本
global.frameworkVersion = version

// 将各层分别连接起来
const glue = {}

// 基本能力
glue.platform = platform
platform.init()
glue.platform.exposeToNative(platform.exposure)

// 框架能力
glue.dock = dock
dock.init(glue)
glue.platform.exposeToNative(dock.exposure)
