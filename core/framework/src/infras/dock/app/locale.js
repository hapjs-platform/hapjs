/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const localeObject = {}

const resources = []

function applyLocaleConfig(argLocaleObject, argResources) {
  Object.assign(localeObject, argLocaleObject)
  resources.splice(0, resources.length, ...argResources)

  return getLocaleConfig()
}

function getLocaleConfig() {
  const locale = [localeObject.language, localeObject.countryOrRegion].filter(n => !!n).join('-')
  return {
    localeObject,
    resources,
    locale: locale
  }
}

export { applyLocaleConfig, getLocaleConfig }
