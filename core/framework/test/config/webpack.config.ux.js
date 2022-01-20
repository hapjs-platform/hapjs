/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const path = require('path')
const fs = require('fs')

const webpack = require('webpack')
const HandlerPlugin = require('@hap-toolkit/packager/lib/plugins/handler-plugin')
const ExtractCssPlugin = require('@hap-toolkit/dsl-xvm/lib/plugins/extract-css-plugin.js')
const moduleLoader = require.resolve('@hap-toolkit/packager/lib/loaders/module-loader.js')

const { initCompileOptionsObject } = require('@hap-toolkit/shared-utils/compilation-config')

initCompileOptionsObject({
  enableExtractCss: true,
  removeUxStyle: true
})

// 支持文件扩展名
const FILE_EXT_LIST = ['.ux']
// 所在目录名
const pathSource = path.resolve(__dirname, '../suite/dsls')
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
    filename: '[name].js'
  },
  devtool: 'inline-source-map',
  module: {
    rules: [
      {
        test: new RegExp(`(${FILE_EXT_LIST.map(k => '\\' + k).join('|')})(\\?[^?]+)?$`),
        oneOf: [
          {
            resourceQuery: /uxType=app/,
            use: require.resolve('@hap-toolkit/dsl-xvm/lib/loaders/app-loader.js')
          },
          {
            resourceQuery: /uxType=(page|comp|card)/,
            use: require.resolve('@hap-toolkit/dsl-xvm/lib/loaders/ux-loader.js')
          }
        ]
      },
      {
        test: /\.js/,
        loaders: [moduleLoader, 'babel-loader']
      },
      {
        test: /\.json/,
        loader: 'json-loader',
        type: 'javascript/auto'
      }
    ]
  },
  plugins: [
    new webpack.DefinePlugin({
      QUICKAPP_TOOLKIT_VERSION: JSON.stringify(require('hap-toolkit/package.json').version)
    }),
    new HandlerPlugin({
      pathSrc: pathSource
    }),
    new ExtractCssPlugin()
  ],
  resolve: {
    extensions: ['.webpack.js', '.web.js', '.js', '.json'].concat(FILE_EXT_LIST),
    alias: {
      src: path.join(process.cwd(), 'src')
    }
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
    // 临时：TODO：xvms/importthen不参与查找编译
    if (fullpath === path.join(baseDir, 'xvms')) {
      return
    }
    // console.log('### App Loader ### 准备文件', fullpath)

    const stat = fs.statSync(fullpath)
    const extname = path.extname(fullpath)
    // 只处理指定类型的文件，添加到entry中( Common目录下的脚本文件不处理 )
    if (stat.isFile()) {
      const isEntryPage = FILE_EXT_LIST.indexOf(extname) >= 0
      if (isEntryPage && !common) {
        name = path.join('test', 'build/dsls', partDir, path.basename(filename, extname))
        const relativePath = path.relative(path.resolve(__dirname, '../../'), fullpath)
        const uxType = filename === 'app.ux' ? 'app' : 'page'
        zipPages[name] = `./${relativePath}?uxType=${uxType}`
        console.log('### App Loader ### 添加 entry: ', name)

        // 复制一份文件供应用创建
        if (filename === 'app.ux') {
          const manifestFrom = relativePath.replace('app.ux', 'manifest.json')
          const manifestDest = name.replace(/app$/, 'manifest.json')
          mkdirp(path.dirname(manifestDest))
          fs.writeFileSync(manifestDest, fs.readFileSync(manifestFrom))
        }
      }
      // 临时：负责复制i18n
      if (partDir === 'xvm/app/i18n') {
        console.log('### App Loader ### 复制国际化文件')
        if (extname === '.json') {
          name = path.join('test', 'build/dsls', partDir, filename)
          mkdirp(path.dirname(name))
          fs.writeFileSync(name, fs.readFileSync(fullpath))
        }
      }
    } else if (stat.isDirectory()) {
      const subdir = path.join(partDir, filename)
      const isCommon = common || filename.toLowerCase() === 'common'
      parse(baseDir, subdir, isCommon)
    }
  })
}

function mkdirp(dir) {
  if (!fs.existsSync(dir)) {
    mkdirp(path.dirname(dir))
    fs.mkdirSync(dir)
  }
}
