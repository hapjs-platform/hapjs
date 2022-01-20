/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const fs = require('fs-extra')
const path = require('path')
const argv = require('yargs').argv
const chalk = require('chalk')

const dirs = [
  {
    from: 'babel.config.js'
  },
  {
    from: '.eslintrc'
  },
  {
    from: 'src/shared',
    exclude: []
  },
  {
    from: 'dist/release/output',
    to: 'dist/release/output',
    exclude: []
  },
  {
    from: 'test/suite',
    exclude: ['dsls/xvm', 'infras']
  },
  {
    from: 'test/config',
    exclude: ['webpack.worker.config.js']
  }
]

function copyDirs(dest) {
  dirs.forEach(function(dir) {
    dir.to = dir.to || dir.from
    fs.copy(dir.from, path.join(dest, dir.to), function(err) {
      if (err) return console.log(chalk.red('copy fail'), err)
      console.log(chalk.green('复制成功:'), dir.to)
      removeFiles(dest, dir)
    })
  })
}

function removeFiles(dest, dir) {
  for (let i = 0, len = (dir.exclude || []).length; i < len; i++) {
    const removed = path.join(dest, dir.to, dir.exclude[i])
    fs.remove(removed, function(err) {
      if (err) return console.log(chalk.red('copy fail'), err)
      console.log(chalk.green('删除成功:'), removed)
    })
  }
}

function main() {
  let deployPath = argv.path
  if (!deployPath) {
    deployPath = './deploy'
    console.log(chalk.green('未指定打包地址，导入到当前文件夹的deploy文件夹下'))
  }
  const dest = path.join(process.cwd(), deployPath)
  console.log(chalk.green('复制路径为 => '), dest)
  fs.ensureDirSync(dest)
  copyDirs(dest)
}

main()
