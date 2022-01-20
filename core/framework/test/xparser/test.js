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
// 所在目录名
const DIR_NAME = 'case';
// 支持文件扩展名
const FILE_EXT_LIST = ['.html', '.raw'];

/**
 * 编译case目录下案例
 */
;(function build(dir) {
  dir = dir || '.'
  var directory = path.join(__dirname, DIR_NAME, dir);
  var name
  var html
  var json

  const buildPath = path.join(__dirname, 'build')
  !fs.existsSync(buildPath) && fs.mkdirSync(buildPath)

  // 递归遍历目录
  fs.readdirSync(directory)
    .forEach(function (file) {
      var fullpath = path.join(directory, file);
      console.log('### App Parser ### 准备文件', fullpath)

      var stat = fs.statSync(fullpath);
      var extname = path.extname(fullpath);
      // 只处理指定类型的文件
      if (stat.isFile() ) {
        if( (FILE_EXT_LIST.indexOf(extname) >= 0)  ) {
          const buildDestDir = path.join(__dirname, 'build', dir)
          !fs.existsSync(buildDestDir) && fs.mkdirSync(buildDestDir)
          name = path.join(buildDestDir, path.basename(file, extname)) + '.json';
          html = utils.$html(fullpath);
          if (extname === '.html'){
            json = compiler.compile(html, 'html');
          }
          else {
            json = compiler.compile(html);
          }
          fs.writeFileSync(name, JSON.stringify(json))
          console.log('### App Parser ### 生成编译文件', name);
        }
      }
      else if (stat.isDirectory()) {
        var subdir = path.join(dir, file);
        build(subdir);
      }
    });
})('.')

/**
 * Template
 */
describe('XParser编译测试', () => {
  let $app_define$;
  let $app_bootstrap$;
  let components;
  let requireStub;
  let bootstrapStub;

  function $expect(name) {
    const actualJSON = utils.$json('build', name);
    const expectJSON = utils.$json('expect', name);
    expect(actualJSON).eql(expectJSON);

    return actualJSON;
  }

  it('Html模板-简单', () => {
    $expect('simple');
  });

  it('Html模板-虎扑新闻', () => {
    $expect('hupu');
  });

  it('Native模板-基础', () => {
    $expect('base');
  });

  it('Native模板-Text', () => {
    $expect('text');
  });

  it('Native模板-Style校验', () => {
    $expect('validate');
  });
});


