/*
 * SPDX-License-Identifier: MIT
 *
 * Copyright (c) 2016 kazuya kawaguchi
 */

/**
 * COPY FROM: https://github.com/kazupon/vue-i18n
 */

export const numberFormatKeys = [
  'style',
  'currency',
  'currencyDisplay',
  'useGrouping',
  'minimumIntegerDigits',
  'minimumFractionDigits',
  'maximumFractionDigits',
  'minimumSignificantDigits',
  'maximumSignificantDigits',
  'localeMatcher',
  'formatMatcher'
]

export function isObject(obj) {
  return obj !== null && typeof obj === 'object'
}

const toString = Object.prototype.toString
const OBJECT_STRING = '[object Object]'
export function isPlainObject(obj) {
  return toString.call(obj) === OBJECT_STRING
}

export function isNull(val) {
  return val === null || val === undefined
}

export function parseArgs(...args) {
  let locale = null
  let params = null
  if (args.length === 1) {
    if (isObject(args[0]) || Array.isArray(args[0])) {
      params = args[0]
    } else if (typeof args[0] === 'string') {
      locale = args[0]
    }
  } else if (args.length === 2) {
    if (typeof args[0] === 'string') {
      locale = args[0]
    }
    /* istanbul ignore if */
    if (isObject(args[1]) || Array.isArray(args[1])) {
      params = args[1]
    }
  }

  if (locale) {
    console.warn(
      `### App Framework ### 当前多语言国际化的版本：不支持参数传递具体某个locale：${locale}`
    )
  }

  return { locale, params }
}
