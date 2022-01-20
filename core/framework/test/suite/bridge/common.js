/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const FROM_MINI = process.env.NODE_REPO_FROM === 'minify'

/**
 * 根据key获取对象对应的Value
 * @param {object} target 目标对象
 * @param {string} key 需要获取的key，格式如'a.b'
 */
function getValue(target, key) {
  const keys = key.split('.')
  return keys.reduce(function(o, item) {
    return o[item]
  }, target)
}

/**
 * 从对应的path获取相应的对象
 * @param {string} distPath 从dist中获取的包地址
 * @param {string} distKeys dist包的属性keys
 * @param {string} srcPath 从源码中获取的包地址
 * @param {string} srcKey src包的属性key
 */
export function requireByRepo(distPath, distKeys, srcPath, srcKey) {
  const distPack = FROM_MINI && require(distPath)
  return FROM_MINI
    ? distKeys
      ? getValue(distPack, distKeys)
      : distPack
    : srcKey
    ? require(srcPath)[srcKey]
    : require(srcPath)
}
