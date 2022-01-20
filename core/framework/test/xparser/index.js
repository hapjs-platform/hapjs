/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

// 避免Stack信息
console.trace = console.info

module.exports = require('../../lib/xparser/index.js')