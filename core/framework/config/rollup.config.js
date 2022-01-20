/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import fs from 'fs'
import { getRollConf } from './rollup.base'
import { buildSysComponent } from './sys-comp'

// 配置环境
const config = parse()
config.pushFileList = ['infras', 'dsl', 'animation', 'canvas', 'parser']
// Rollup配置
const rollConf = getRollConf(config)

const confList = config.list.map(item => {
  const output = Object.assign({}, rollConf.output, item.output)
  return Object.assign({}, rollConf, item, { output })
})

export default confList

/**
 * 解析NODE环境的参数
 */
function parse(config) {
  config = config || {}
  // 平台：na|h5
  config.NODE_PLATFORM = process.env.NODE_PLATFORM
  // 阶段: dv|qa|ol
  config.NODE_PHASE = process.env.NODE_PHASE
  // 测试文件来自export
  config.NODE_DIST_EXTRA = process.env.NODE_DIST_EXTRA

  const NODE_ENV = `${config.NODE_PLATFORM}-${config.NODE_PHASE}`

  switch (NODE_ENV) {
    // NA环境
    case 'na-dv':
      config.list = collectExportList('debug', './src/dsls', './src/infras/bundles')
      buildSysComponent()
      break
    case 'na-ol':
      config.list = collectExportList('release', './src/dsls', './src/infras/bundles')
      buildSysComponent()
      break
    // H5环境 待之后完善
    case 'h5-dv':
      config.list = collectExportList('debug')
      break
    case 'h5-ol':
      config.list = collectExportList('release')
      break
    default:
      throw new Error(`Unknown node environment: ${NODE_ENV}`)
  }

  if (config.NODE_DIST_EXTRA === 'infras-ext') {
    config.list = exportInfrasExt()
  }

  console.info('config: ', JSON.stringify(config, null, 4))
  return config
}

/**
 * 收集最终生成的JS文件并生成rollup配置
 * @param {string} env debug|release
 * @param {string} dslsSrc
 * @param {string} bundleSrc bundle文件夹位置
 */
function collectExportList(env, dslsSrc, bundleSrc) {
  const PLATFORM_LISTS = {
    na: [
      {
        input: './src/infras/entry/main/index.js',
        output: {
          file: `./dist/${env}/infras.js`,
          name: 'infras'
        }
      },
      {
        input: './src/infras/entry/worker/index.js',
        output: {
          file: `./dist/${env}/worker.js`,
          name: 'worker'
        }
      },
      {
        input: './src/infras/entry/main/styling.js',
        output: {
          file: `./dist/${env}/styling.js`,
          name: 'styling'
        }
      }
    ],
    h5: [
      {
        input: './src/infras/entry/web/web.js',
        output: {
          file: `./dist/${env}/infras-web.js`,
          name: 'infras-web'
        }
      }
    ]
  }
  const list = PLATFORM_LISTS[process.env.NODE_PLATFORM]
  if (process.env.NODE_PLATFORM === 'na') {
    fs.readdirSync(dslsSrc).forEach(dir => {
      // 收集指定目录下的JS模块
      list.push({
        input: `${dslsSrc}/${dir}/index.js`,
        output: {
          file: `./dist/${env}/dsls/dsl-${dir}.js`,
          format: 'iife',
          name: 'dsl',
          banner: ''
        }
      })
    })

    fs.readdirSync(bundleSrc).forEach(dir => {
      // 收集指定目录下的JS模块
      list.push({
        input: `${bundleSrc}/${dir}/index.js`,
        output: {
          file: `./dist/${env}/bundles/${dir}.js`,
          format: 'iife',
          name: dir,
          banner: ''
        }
      })
    })
  }
  return list
}

/**
 * 为Dsl代码库生成的基础模块文件
 */
function exportInfrasExt() {
  const list = [
    {
      input: './src/infras/entry/main/index.js',
      output: {
        file: './dist/release/output/infras.js',
        name: 'infras',
        banner: ''
      }
    },
    {
      input: './src/infras/entry/main/infras-ext.js',
      output: {
        file: './dist/release/output/infras-ext.js',
        name: 'infras-ext',
        banner: ''
      }
    },
    {
      input: './src/infras/entry/main/styling.js',
      output: {
        file: './dist/release/output/styling.js',
        name: 'styling',
        banner: ''
      }
    }
  ]
  return list
}
