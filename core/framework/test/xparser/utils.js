/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const fs = require('fs');
const path =require('path');

/**
 * 获取期望的json输出
 * @param name
 */
function $json(dir, name) {
  const filepath = path.resolve(__dirname, dir, `${name}.json`);
  const result = fs.readFileSync(filepath, 'utf-8');
  return JSON.parse(result.toString());
}

/**
 * 字符串格式化
 * @param json
 */
function $stringify(json) {
  return JSON.stringify(json, function(key, value) {
    if (typeof value === 'function') {  // 如果是函数, 打印函数实现
      value = value.toString();
    }
    return value;
  }, '  ');
}

/**
 * 读取html文件
 * @param name
 */
function $html(filepath) {
  const result = fs.readFileSync(filepath, 'utf-8');
  return result.toString();
}

module.exports = {
  $json,
  $stringify,
  $html
}