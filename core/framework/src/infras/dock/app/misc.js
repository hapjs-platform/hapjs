/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * 销毁应用
 * @param app
 */
function destroyApp(app) {
  app._emit('applc:onDestroy')
  app.$clear()

  console.trace(`### App Framework ### 成功销毁应用(${app.id})----`)
}

export { destroyApp }
