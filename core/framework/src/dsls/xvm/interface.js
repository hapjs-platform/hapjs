/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { initApp } from './app/interface'

import {
  initPage,
  fireEvent,
  destroyPage,
  invokePageEvent,
  handleMenuPressEvent
} from './page/interface'

import { updatePageActions } from './page/misc'

import XLinker from './vm/linker'

import { APP_KEYS } from 'src/shared/events'

import context from './context'

// 底层传递的事件
const VM_KEYS = {
  onShow: 'onShow',
  onHide: 'onHide',
  onBackPress: 'onBackPress',
  onMenuButtonPress: 'onMenuButtonPress',
  onKey: 'onKey',
  onMenuPress: 'onMenuPress',
  onConfigurationChanged: 'onConfigurationChanged',
  onOrientationChange: 'onOrientationChange',
  onRefresh: 'onRefresh',
  onReachTop: 'onReachTop',
  onReachBottom: 'onReachBottom',
  onPageScroll: 'onPageScroll'
}

function init(quickapp) {
  context.quickapp = quickapp

  quickapp.subscribe(APP_KEYS.initApp, args => {
    return initApp(...args)
  })

  quickapp.subscribe(APP_KEYS.initPage, args => {
    XLinker.resetTarget()
    return initPage(...args)
  })
  quickapp.subscribe(APP_KEYS.destroyPage, args => {
    XLinker.resetTarget()
    destroyPage(...args)
  })

  quickapp.subscribe(APP_KEYS.fireEvent, args => {
    const result = fireEvent(...args)
    updatePageActions(args[0])
    return result
  })

  quickapp.subscribe(APP_KEYS.callbackDone, args => {
    updatePageActions(args[0])
  })

  quickapp.subscribe(APP_KEYS.onShow, args => {
    return invokePageEvent(VM_KEYS.onShow, ...args)
  })

  quickapp.subscribe(APP_KEYS.onHide, args => {
    return invokePageEvent(VM_KEYS.onHide, ...args)
  })

  quickapp.subscribe(APP_KEYS.onBackPress, args => {
    return invokePageEvent(VM_KEYS.onBackPress, ...args)
  })

  // tv menu button
  quickapp.subscribe(APP_KEYS.onMenuButtonPress, args => {
    return invokePageEvent(VM_KEYS.onMenuButtonPress, ...args)
  })

  // tv onKey
  quickapp.subscribe(APP_KEYS.onKey, args => {
    return invokePageEvent(VM_KEYS.onKey, ...args)
  })

  quickapp.subscribe(APP_KEYS.onMenuPress, args => {
    const result = handleMenuPressEvent(...args)
    invokePageEvent(VM_KEYS.onMenuPress, ...args)
    return result
  })

  quickapp.subscribe(APP_KEYS.onConfigurationChanged, args => {
    return invokePageEvent(VM_KEYS.onConfigurationChanged, ...args)
  })

  quickapp.subscribe(APP_KEYS.onOrientationChange, args => {
    return invokePageEvent(VM_KEYS.onOrientationChange, ...args)
  })

  quickapp.subscribe(APP_KEYS.onRefresh, args => {
    return invokePageEvent(VM_KEYS.onRefresh, ...args)
  })

  quickapp.subscribe(APP_KEYS.onReachTop, args => {
    return invokePageEvent(VM_KEYS.onReachTop, ...args)
  })

  quickapp.subscribe(APP_KEYS.onReachBottom, args => {
    return invokePageEvent(VM_KEYS.onReachBottom, ...args)
  })

  quickapp.subscribe(APP_KEYS.onPageScroll, args => {
    return invokePageEvent(VM_KEYS.onPageScroll, ...args)
  })
}

export default {
  init
}
