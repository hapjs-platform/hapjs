/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import fs from 'fs'
import prettyFormat from 'pretty-format'

const { DOMCollection, DOMElement, Immutable, AsymmetricMatcher } = prettyFormat.plugins

let PLUGINS = [DOMElement, DOMCollection, Immutable, AsymmetricMatcher]

// Prepend to list so the last added is the first tested.
const addSerializer = plugin => {
  PLUGINS = [plugin].concat(PLUGINS)
}
const getSerializers = () => PLUGINS

function serialize(data) {
  return prettyFormat(data, {
    escapeRegex: true,
    plugins: PLUGINS,
    printFunctionName: false
  })
}

// Remove double quote marks and unescape double quotes and backslashes.
const escapeBacktickString = str => str.replace(/`|\\|\${/g, '\\$&')

const printBacktickString = str => '`' + escapeBacktickString(str) + '`'

const normalizeNewlines = str => str.replace(/\r\n|\r/g, '\n')

class Manager {
  constructor() {
    this.file = null
    this.snapName = null
    this.extension = '.snapshot'
    this.diffExtension = '.diff'
    this.snapData = Object.create(null)
  }

  get snapFile() {
    return this.file + this.extension
  }

  get diffSnapFile() {
    return this.snapFile + this.diffExtension
  }

  initConfig(ctx) {
    this.file = ctx.file
    this.snapName = ctx.fullTitle()
  }

  /**
   * 从snapshot文件读取data数据
   */
  getSnapShotData() {
    const SnapData = Object.create(null)
    if (fs.existsSync(this.snapFile)) {
      try {
        const snapshotContents = fs.readFileSync(this.snapFile, 'utf8')
        const populate = new Function('exports', snapshotContents)
        populate(SnapData)
      } catch (e) {}
    }

    return SnapData[this.snapName]
  }

  /**
   * 保存snapshot data
   * @param {Object} obj - 当前snapshot 的data
   * @param {*} itSnapData - 从snapshot文件取出来的data
   */
  saveSnapData(obj, itSnapData) {
    const snapData = {
      snapName: this.snapName,
      data: obj
    }

    if (itSnapData === undefined) {
      ;(this.snapData[this.snapFile] || (this.snapData[this.snapFile] = [])).push(snapData)
    } else if (itSnapData !== obj) {
      ;(this.snapData[this.diffSnapFile] || (this.snapData[this.diffSnapFile] = [])).push(snapData)
    }
  }

  /**
   * 将snapshot data 存储到文件
   */
  setSnapShot() {
    Object.keys(this.snapData).forEach(snapFilePath => {
      this.setSnapShotFile(this.snapData[snapFilePath], snapFilePath)
    })
  }

  setSnapShotFile(snapData, snapFile) {
    const snapContent = snapData.map(
      snapshot =>
        'exports[' +
        printBacktickString(snapshot.snapName) +
        '] = ' +
        printBacktickString(normalizeNewlines(snapshot.data)) +
        ';'
    )

    fs.writeFileSync(snapFile, '\n\n' + snapContent.join('\n\n') + '\n')
  }

  tryDelDiffFile() {
    const diffFile = this.diffSnapFile
    if (fs.existsSync(diffFile)) {
      fs.unlinkSync(diffFile)
    }
  }
}

/**
 * chai 插件，增加快照对比的能力
 * @param {Object} chai
 * @param {Object} utils
 */
function snapshotChai(chai, utils) {
  let manager
  before(function() {
    manager = new Manager()
  })
  beforeEach(function() {
    if (this.currentTest) manager.initConfig(this.currentTest)
  })
  after(function() {
    manager.setSnapShot()
    manager = null
  })
  chai.Assertion.addMethod('matchSnapshot', function() {
    const obj = serialize(utils.flag(this, 'object'))
    const itSnapData = manager.getSnapShotData()
    const isValid = itSnapData === undefined || itSnapData === obj
    manager.saveSnapData(obj, itSnapData)
    manager.tryDelDiffFile()
    this.assert(
      isValid,
      `expected value to match snapshot ${manager.snapName}`,
      `expected value to not match snapshot ${manager.snapName}`,
      itSnapData,
      obj,
      true
    )
  })
}

export { snapshotChai, addSerializer, getSerializers }
