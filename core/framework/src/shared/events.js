/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * 定义dock与dsl通讯时的事件接口
 */
const APP_KEYS = {
  // APP加载
  initApp: 'quickapp.app.initApp',
  // 页面加载
  initPage: 'quickapp.page.initPage',
  // 页面销毁
  destroyPage: 'quickapp.page.destroyPage',
  // 页面中的组件事件
  fireEvent: 'quickapp.page.fireEvent',
  // 页面onShow
  onShow: 'quickapp.page.onShow',
  // 页面onHide
  onHide: 'quickapp.page.onHide',
  // 页面返回响应
  onBackPress: 'quickapp.page.onBackPress',
  // 遥控器menu键响应
  onMenuButtonPress: 'quickapp.page.onMenuButtonPress',
  // 页面onKey，响应遥控器等键位导航
  onKey: 'quickapp.page.onKey',
  // 页面菜单响应
  onMenuPress: 'quickapp.page.onMenuPress',
  // 页面朝向响应
  onOrientationChange: 'quickapp.page.onOrientationChange',
  // 页面onConfigurationChanged
  onConfigurationChanged: 'quickapp.page.onConfigurationChanged',
  // 页面朝向响应
  onRefresh: 'quickapp.page.onRefresh',
  // 页面：组件方法的callback()执行完毕，Native接口的回调完毕，定时器完毕
  callbackDone: 'quickapp.page.callbackDone',
  // 页面：页面滚动到顶部时响应
  onReachTop: 'quickapp.page.onReachTop',
  // 页面：页面滚动到底部时响应
  onReachBottom: 'quickapp.page.onReachBottom',
  // 页面：页面滚动时触发响应
  onPageScroll: 'quickapp.page.onPageScroll'
}

export { APP_KEYS }
