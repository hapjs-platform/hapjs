/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import path from 'path'

import json from 'rollup-plugin-json' // 通过import引入json文件
import { eslint } from 'rollup-plugin-eslint' // 代码风格检查
import nodeResolve from 'rollup-plugin-node-resolve' // 解析npm包
import commonjs from 'rollup-plugin-commonjs' // commonJS转换为ES6
import buble from 'rollup-plugin-buble' // 转化相应的v8版本的ES6语法特性
import replace from 'rollup-plugin-replace' // Likely with DefinePlugin
import { terser } from 'rollup-plugin-terser' // terser用来混淆ES6代码
import alias from 'rollup-plugin-alias'

import { pushToDevice } from './util'

const packageJSON = require('../package.json')

/**
 * 推送设备插件
 * @param {*} pushOptions
 */
function pushPlugin(pushOptions = { devicePath: '' }) {
  // See https://github.com/rollup/rollup/issues/2717 and
  // https://github.com/rollup/rollup/issues/2617
  let tasks = []
  return {
    name: 'rollup-plugin-push',
    generateBundle(options, bundle) {
      const outputNameList = pushOptions.pushFileList
      if (process.env.ROLLUP_WATCH && (!outputNameList || outputNameList.includes(options.name))) {
        // 相对项目路径
        const fileDistProPath = options.file
        // 绝对路径
        const fileDistAbsPath = path.join(__dirname, '..', fileDistProPath)
        // sdcard的路径
        const fileDestFullPath = fileDistProPath.match(/.*dist\/.*?\/(.*)/)[1]

        const pushTask = () => pushToDevice(fileDistAbsPath, fileDestFullPath)
        tasks.push(pushTask)
      }
    },
    writeBundle(bundle) {
      tasks.map(i => i())
      tasks = []
    }
  }
}

export function getRollConf(config) {
  // 变量替换的工作
  const rollInject = {
    // 平台：na
    ENV_PLATFORM: JSON.stringify(config.NODE_PLATFORM),
    // 阶段: dv|qa|ol
    ENV_PHASE: JSON.stringify(config.NODE_PHASE),
    // OL环境无assert
    'console.assert': `!${config.NODE_PHASE === 'ol'} && console.assert`,
    // 提前判断，函数减少入栈出栈
    'console.trace': `global.Env && global.Env.logLevel === 'trace' && console.trace`,
    // 减少函数的调用，避免性能问题
    'profiler.time': `profiler._isEnabled && profiler.time`,
    'profiler.record': `profiler._isEnabled && profiler.record`
  }

  const plugins = [
    json(),
    eslint({
      exclude: './package.json'
    }),
    alias({
      resolve: ['.js', '/index.js'],
      src: path.resolve(__dirname, './../src/')
    }),
    nodeResolve({
      jsnext: true,
      main: true
    }),
    commonjs(),
    buble({
      target: {
        chrome: 66 // 如果需要支持更高版本的chrome，请升级buble插件
      }
    }),
    replace({
      exclude: ['node_modules/**', 'package.json'],
      values: rollInject
    }),
    pushPlugin({
      pushFileList: config.pushFileList
    })
  ]

  if (config.NODE_PHASE === 'dv') {
  } else {
    // Uglify
    plugins.push(terser())
  }

  return {
    output: {
      banner: `\
      var FRAMEWORK_VERSION = '${packageJSON.version}';
      var global = this;
      `,
      format: 'umd',
      sourcemap: true,
      freeze: false // import * as xxx from 'xxx'; 防止xxx对象被Object.freeze掉
    },
    plugins
  }
}
