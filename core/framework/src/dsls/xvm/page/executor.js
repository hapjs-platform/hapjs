/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { updatePageActions } from './misc'

// 本此轮询的索引
let execPollUnique = 1

class XExecutor {
  constructor(id, inst) {
    this.id = id
    this.inst = inst

    this.taskList = []
    this.taskHash = new Set()
    // 是否需要排序
    this.needSort = false
    // 下次回调
    this.nextTick = 0
    // 一次最多允许的回调数量
    this.warnSize = 1e4
    // 任务执行模式
    this.execMode = XExecutor.MODE.SYNC
  }

  /**
   * 加入task
   */
  join(tasks) {
    if (!Array.isArray(tasks)) {
      tasks = [tasks]
    }
    // 记录添加
    for (let i = 0, len = tasks.length; i < len; i++) {
      const task = tasks[i]
      if (!this.taskHash.has(task)) {
        this.taskList.push(task)
        this.taskHash.add(task)
        !this.needSort && (this.needSort = true)
        console.trace(`### App Framework ### XExecutor.join() 添加单个task：${task.getter || task}`)
      } else {
        console.trace(`### App Framework ### XExecutor.join() 过滤重复task：${task.getter || task}`)
      }
    }

    // 下次回调
    this._defineNextTick()
  }

  /**
   * 执行task
   */
  exec() {
    let result
    switch (this.execMode) {
      case XExecutor.MODE.SYNC:
        result = this._execSync()
        console.trace(`### App Framework ### XExecutor.exec() 执行：${this.id}:${execPollUnique}`)
        execPollUnique++
        break
    }

    // 检查异常情况
    if (this.taskList.length > 0 || this.taskHash.size > 0) {
      throw new Error(`### App Framework ### XExecutor.exec() 异常:存在未执行的任务`)
    }

    return result
  }

  /**
   * 重置相关数据
   */
  reset() {
    this.taskList.length = 0
    this.taskHash.clear()
    this.needSort && (this.needSort = false)
  }

  _execSync() {
    console.trace(`### App Framework ### XExecutor._execSync() 开始：${this.taskList.length}`)

    let taskLoopIndex = 0
    while (this._hasTask()) {
      // 有新加入task时,重新对任务队列进行排序
      if (this.needSort) {
        this._sort()
      }

      // 原子操作：允许再次添加
      const task = this.taskList.shift()
      this.taskHash.delete(task)

      // 执行任务,避免本次出错影响全局机制
      try {
        task.id ? task.run() : task()
      } catch (err) {
        // 抛错前重置相关数据
        this.reset()
        throw err
      }

      if (++taskLoopIndex > this.warnSize) {
        console.warn(`### App Framework ### 页面元素更新循环次数已达：${taskLoopIndex}`)
      }
    }

    console.trace(`### App Framework ### XExecutor._execSync() 结束：${this.taskList.length}`)
    return this.inst.doc.listener.hasActions()
  }

  /**
   * 对任务队列进行排序
   */
  _sort() {
    const subList = []
    const len = this.taskList.length
    for (let i = 0; i <= len; i++) {
      const task = this.taskList[i]

      if (task && task.id) {
        subList.push(task)
      } else if (subList.length > 0) {
        subList.sort((a, b) => a.id - b.id)
        this.taskList.splice.apply(
          this.taskList,
          [i - subList.length, subList.length].concat(subList)
        )
        subList.splice(0)
      }
    }
    this.needSort = false
  }

  /**
   * 判断任务队列中是否存在待执行任务
   */
  _hasTask() {
    return this.taskList.length > 0
  }

  _defineNextTick() {
    if (!this.nextTick) {
      this.nextTick = Promise.resolve().then(() => {
        this.nextTick = null
        if (this.inst && this.inst.$valid) {
          console.trace(`### App Framework ### XExecutor._defineNextTick() Actions尝试轮询`)
          updatePageActions(this.inst)
        }
      })
    }
  }
}

XExecutor.MODE = {
  SYNC: 1
}

export default XExecutor
