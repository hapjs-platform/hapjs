/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

export function setUpPromiseRejectionHook() {
  global.QuickApp = {}
  // todo expose to user later
  global.QuickApp.unhandledrejection = (type, promise, reason) => {
    if (reason.stack) {
      console.warn(`### App Framework ### Unhandled promise rejection: ${reason.stack}`)
    }
  }
}
