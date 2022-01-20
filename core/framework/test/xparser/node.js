/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

var compiler = require('./index.js');
var fs = require('fs');
var path = require('path');
const utils = require('./utils');
const chai = require('chai');
const sinon = require('sinon');
const sinonChai = require('sinon-chai');
const expect = chai.expect;
chai.use(sinonChai);

compiler = compiler.default

function testOnNode () {
  const code = {
    // 转换：没有节点
    html1: `  `,
    // 转换：text节点
    html2: `TEXT内容1`,
    // 转换：text节点下无span
    html3: `<text class="class1" font-size="64px" style="background-color: #FF0000">TEXT内容1</text>`,
    // 转换：text节点下有3个span
    html4: `<text>SPAN内容1<span>SPAN内容2</span>SPAN内容3</text>`,
    // 转换：3个text节点
    html5: `TEXT内容1<text>SPAN内容2</text>TEXT内容3`,
    // 转换：多个节点
    html6: `<div><text>TEXT内容1</text></div>\n<div><text>TEXT内容2</text></div>`,
    // 转换：div下的文本转成text节点
    html7: `<div>TEXT内容1</div>`,
    // 转换：a节点
    html8: `<a>A内容1</a>`,
    // 转换：span节点
    html9: `<span>SPAN内容1</span>`,
    // 转换：文本元素的value属性
    html10: `<text value="TEXT内容1 TEXT内容2"><span>SPAN内容3</span></text>`,
    // 转换：a节点下有3个span
    html11: `<a>SPAN内容1<span>SPAN内容2</span>SPAN内容3</a>`,
    // 转换：text节点中有text
    // html10: `<text>\n<text>TEXT  内容1</text>\n</text>`
  }

  const node = compiler.parseHTML(code.html10)
}

testOnNode()
