/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { execSync } from 'child_process'

/**
 * 通过ADB命令复制到设备中
 * @param fileFromFullPath 本地绝对路径
 * @param fileDestFootPath 复制到设备指定路径后的末尾相对路径
 */
function pushToDevice(fileFromFullPath, fileDestFootPath) {
  try {
    const fileDestFullPath = `/sdcard/quickapp/assets/js/${fileDestFootPath}`
    const commandAdbPush = `adb push "${fileFromFullPath}" "${fileDestFullPath}"`
    console.info(`执行命令: ${commandAdbPush}`)

    const out = execSync(commandAdbPush)
    console.info(out.toString())
  } catch (e) {
    console.warn(e.stdout.toString())
    console.warn(e.stderr.toString())
  }
}

export { pushToDevice }
