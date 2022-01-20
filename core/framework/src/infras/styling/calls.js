/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// 接受消息的列表
const newActionList = []

function saveNewActions(instId, actionList) {
  newActionList.push(...actionList)
}

function cleanNewActions() {
  newActionList.splice(0)
}

function sliceNewActions() {
  return newActionList.slice()
}

export { saveNewActions, cleanNewActions, sliceNewActions }
