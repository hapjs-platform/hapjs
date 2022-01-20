/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { exec } from 'child_process'
import * as path from 'path'
import * as fs from 'fs-extra'
import chokidar from 'chokidar'

// base project dir
const sysCompBaseDir = (subPath = []) => path.join(__dirname, '..', `sys-components`, ...subPath)

let baseDistDir = 'debug'
if (process.env.NODE_PHASE === 'ol') {
  baseDistDir = 'release'
}

const pathBuild = path.resolve(__dirname, `../dist/${baseDistDir}/app`)
fs.ensureDirSync(pathBuild)

function copySysCompFile(filePath, dsl) {
  if (filePath.endsWith('.js')) {
    const baseFileName = path
      .dirname(filePath)
      .split(path.sep)
      .pop()
    fs.ensureDirSync(path.join(pathBuild, dsl))
    fs.copyFileSync(filePath, path.join(pathBuild, dsl, `${baseFileName}.js`))
    console.info(`sys-comp: 复制资源 ${filePath} 到 ${pathBuild}/${dsl}`)
  }
}

function setWatch(buildPath, dsl) {
  return chokidar.watch(buildPath).on('all', (event, filePath) => {
    if (['add', 'change'].includes(event)) {
      copySysCompFile(filePath, dsl)
    }
  })
}

export function buildSysComponent() {
  buildSysComponentDsl('xvm')
}

function buildSysComponentDsl(dsl) {
  const outputComponentPath = sysCompBaseDir([dsl, 'build', 'sys'])
  const watcher = setWatch(outputComponentPath, dsl)

  let cmd = 'npm run build'
  if (baseDistDir === 'release') {
    cmd = 'npm run release'
  }
  if (process.env.ROLLUP_WATCH) {
    cmd = 'npm run start'
  }
  console.log(cmd)
  const build = exec(cmd, {
    cwd: sysCompBaseDir([dsl])
  })

  build.stdout.on('data', data => {
    console.log(`stdout: ${data}`)
  })

  build.stderr.on('data', data => {
    console.error(`stderr: ${data}`)
  })

  build.on('close', code => {
    watcher.close()
    fs.readdirSync(outputComponentPath).map(i => {
      const filePath = path.join(outputComponentPath, i, 'index.js')
      copySysCompFile(filePath, dsl)
    })
    console.log(`子进程退出，状态码为: ${code}`)
  })
}
