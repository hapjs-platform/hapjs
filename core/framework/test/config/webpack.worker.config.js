/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const path = require('path')
const fs = require('fs')

const uxLoader = require.resolve('@hap-toolkit/dsl-xvm/lib/loaders/ux-loader.js')
const moduleLoader = require.resolve('@hap-toolkit/packager/lib/loaders/module-loader.js')

// 支持文件扩展名
const FILE_EXT_LIST = ['.ux']
// 所在目录名
const pathSource = path.resolve(__dirname, '../suite/infras')
const pathBuild = path.resolve(__dirname, '../../')
// 页面文件
const zipPages = {}

// 提取脚本文件，资源文件
parse(pathSource, '.', false)

module.exports = {
  entry: zipPages,
  mode: 'development',
  output: {
    path: pathBuild,
    filename: '[name]'
  },
  devtool: 'inline-source-map',
  module: {
    rules: [
      {
        test: new RegExp(`(${FILE_EXT_LIST.map(k => '\\' + k).join('|')})(\\?[^?]+)?$`),
        loaders: uxLoader
      },
      {
        test: /\.js/,
        loaders: [moduleLoader, 'babel-loader']
      }
    ]
  },
  plugins: [],
  resolve: {
    extensions: ['.webpack.js', '.web.js', '.js', '.json'].concat(FILE_EXT_LIST)
  },
  stats: {
    children: false,
    chunks: false,
    chunkModules: false,
    chunkOrigins: false,
    modules: false,
    version: false,
    assets: false
  },
  node: {
    global: false,
    console: false,
    process: false
  }
}

/**
 * 查找所有的ux文件
 */
function parse(baseDir, partDir, common) {
  let name
  const dir = path.join(baseDir, partDir)
  // 递归遍历目录
  fs.readdirSync(dir).forEach(function(filename) {
    const fullpath = path.join(dir, filename)
    // console.log('### App Loader ### 准备文件', fullpath)

    const stat = fs.statSync(fullpath)
    const extname = path.extname(fullpath)
    // 只处理指定类型的文件，添加到entry中( Common目录下的脚本文件不处理 )
    if (stat.isFile()) {
      const isEntryWorker = /.*\.worker\.js$/.test(filename)
      if (isEntryWorker && !common) {
        name = path.join('test', 'build/infras', partDir, path.basename(filename, extname)) + '.js'
        const relativePath = path.relative(path.resolve(__dirname, '../../'), fullpath)
        const uxType = filename === 'app.ux' ? 'app' : 'page'
        zipPages[name] = `./${relativePath}?uxType=${uxType}`
        console.log('### App Loader ### 添加 entry: ', name)
      }
    } else if (stat.isDirectory()) {
      const subdir = path.join(partDir, filename)
      const isCommon = common || filename.toLowerCase() === 'common'
      parse(baseDir, subdir, isCommon)
    }
  })
}
