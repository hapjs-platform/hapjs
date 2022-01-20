/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */
module.exports = function(api) {
  api.cache(true)
  return {
    presets: ['@babel/preset-env'],
    plugins: ['@babel/plugin-transform-modules-commonjs'],
    babelrcRoots: ['.', 'node_modules']
  }
}
