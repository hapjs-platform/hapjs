/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
module.exports = {
  extension: ['.ux'],
  // 覆盖以匹配到ux文件
  exclude: ['test/**/*.js'],
  reporter: ['html'],
  // remap之后不要排除操作
  excludeAfterRemap: false
}
