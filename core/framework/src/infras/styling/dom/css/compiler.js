/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import cssWhat from 'css-what'

import {
  createCssRuleTagInfo,
  createCssRuleClassInfo,
  createCssRuleIdInfo,
  createCssRuleDescInfo,
  isSelectorRuleTypeTag,
  isSelectorRuleTypeClass,
  isSelectorRuleTypeId,
  isSelectorRuleTypePointer
} from './definition'

const styleObjectCache = new WeakMap()

/**
 * 编译样式节点，具备缓存能力;如：自定义组件的重复
 */
function compileStyleObject(styleSheetName, styleObject) {
  if (!styleObjectCache.has(styleObject)) {
    const ret = compileStyleObjectImpl(styleSheetName, styleObject)
    styleObjectCache.set(styleObject, ret)
  }
  return styleObjectCache.get(styleObject)
}

// 样式节点的唯一ID
let styleUniqueId = 1
// 上次选择器定义的索引
let styleSelectorLastIndex = 1
// 匹配后代选择
const regExpDesc = /\s/

/**
 * 编译<style>节点对象，key为选择器，value为css样式定义
 * @param styleSheetName
 * @param styleObject
 */
function compileStyleObjectImpl(styleSheetName, styleObject) {
  const selectorNameList = Object.keys(styleObject || {})

  const styleSheetInst = {
    // 样式定义信息
    id: styleUniqueId++,
    name: styleSheetName,
    from: 0,
    size: 0,
    // 选择器的定义：key为选择器，value为CSS规则
    nameHash: {},
    // 后代选择的最后一条规则的Hash：key为最后一条规则的名称，value.list为以其结尾的后代选择定义
    descLast: {},
    // 后代选择中非最后一条规则的Hash：key为每一个非最后的规则名称，value.list为对应的后代选择定义
    descNotLast: {}
  }

  // 更新信息
  styleSheetInst.from = styleSelectorLastIndex
  styleSheetInst.size = selectorNameList.length
  styleSelectorLastIndex += selectorNameList.length

  for (let i = 0, len = selectorNameList.length; i < len; i++) {
    const selectorName = selectorNameList[i]
    const selectorDefOri = styleObject[selectorName]
    // 定义出来的规则对象
    let selectorDefCssRule

    if (!selectorDefOri || selectorName[0] === '_') {
      continue
    }

    // 提前携带关键帧定义
    if (selectorDefOri.animationName) {
      const kf = _processAnimationName(selectorDefOri.animationName, styleObject)
      if (kf) {
        selectorDefOri.animationKeyframes = kf
      }
    }

    // 携带@fontFace定义
    if (selectorDefOri.fontFamily) {
      const fontFamilyDesc = _processFontFamily(selectorDefOri.fontFamily, styleObject)
      if (fontFamilyDesc) {
        selectorDefOri.fontFamilyDesc = fontFamilyDesc
      }
    }

    // 删除编译时携带的meta，因为是压缩后，不被识别
    if (selectorDefOri._meta) {
      delete selectorDefOri._meta
    }

    // 是否是后代选择器
    if (!selectorDefOri._meta && regExpDesc.test(selectorName)) {
      try {
        const selectorDefList = compileSelectorName(selectorName)
        const selectorDef0 = selectorDefList[0]
        if (selectorDef0.length > 1) {
          selectorDefOri._meta = {}
          selectorDefOri._meta.ruleDef = selectorDef0
        }
      } catch (err) {
        console.warn(`### App Runtime ### 编译CSS后代选择器(${selectorName})出错：${err.message}`)
        continue
      }
      console.trace(`### App Runtime ### 编译CSS后代选择器(${selectorName})`)
    }

    // 处理后代选择器：增加对规则类型的标识(identity)
    if (selectorDefOri._meta && selectorDefOri._meta.ruleDef) {
      const ruleTypeList = selectorDefOri._meta.ruleDef
      for (let j = 0, lenJ = ruleTypeList.length; j < lenJ; j++) {
        const ruleTypeItem = ruleTypeList[j]
        if (isSelectorRuleTypeTag(ruleTypeItem)) {
          ruleTypeItem.idtt = ruleTypeItem.name
        } else if (isSelectorRuleTypeClass(ruleTypeItem)) {
          ruleTypeItem.idtt = `.${ruleTypeItem.value}`
        } else if (isSelectorRuleTypeId(ruleTypeItem)) {
          ruleTypeItem.idtt = `#${ruleTypeItem.value}`
        }
      }
    }

    console.trace(
      `### App Runtime ### 遍历样式节点(${
        styleSheetInst.id
      })的选择器(${selectorName})：${JSON.stringify(selectorDefOri)}`
    )

    // 复制一份
    const selectorDefNew = Object.assign({}, selectorDefOri)
    const selectorDefNewOrder = styleSheetInst.from + i
    if (selectorDefNew._meta) {
      // 后代选择
      const meta = selectorDefNew._meta
      // 删除引用
      delete selectorDefNew._meta

      styleSheetInst.nameHash[selectorName] = selectorDefCssRule = createCssRuleDescInfo({
        name: selectorName,
        score: null,
        order: selectorDefNewOrder,
        style: selectorDefNew,
        // 只有后代选择才有meta信息
        meta: meta,
        _sheetId: styleSheetInst.id
      })

      const ruleTypeList = selectorDefCssRule.meta.ruleDef
      const lastRuleTypeItem = ruleTypeList[ruleTypeList.length - 1]

      // 记录非末尾的ruleType
      for (let j = 0, lenJ = ruleTypeList.length; j < lenJ - 1; j++) {
        const ruleTypeItem = ruleTypeList[j]

        if (!isSelectorRuleTypePointer(ruleTypeItem)) {
          const ruleTypeIdtt = ruleTypeItem.idtt
          styleSheetInst.descNotLast[ruleTypeIdtt] = styleSheetInst.descNotLast[ruleTypeIdtt] || {
            list: []
          }
          styleSheetInst.descNotLast[ruleTypeIdtt].list.push(selectorDefCssRule)
        }
      }

      // 记录最后的ruleType
      if (!isSelectorRuleTypePointer(lastRuleTypeItem)) {
        const ruleTypeIdtt = lastRuleTypeItem.idtt
        styleSheetInst.descLast[ruleTypeIdtt] = styleSheetInst.descLast[ruleTypeIdtt] || {
          list: []
        }
        styleSheetInst.descLast[ruleTypeIdtt].list.push(selectorDefCssRule)
      }
    } else if (selectorName === '@KEYFRAMES') {
      // 动画帧跳过不处理
      continue
    } else {
      // 非后代选择
      if (selectorName[0] === '#') {
        // ID
        styleSheetInst.nameHash[selectorName] = selectorDefCssRule = createCssRuleIdInfo({
          name: selectorName,
          order: selectorDefNewOrder,
          style: selectorDefNew,
          _sheetId: styleSheetInst.id
        })
      } else if (selectorName[0] === '.') {
        // CLASS
        styleSheetInst.nameHash[selectorName] = selectorDefCssRule = createCssRuleClassInfo({
          name: selectorName,
          order: selectorDefNewOrder,
          style: selectorDefNew,
          _sheetId: styleSheetInst.id
        })
      } else {
        // TAG
        styleSheetInst.nameHash[selectorName] = selectorDefCssRule = createCssRuleTagInfo({
          name: selectorName,
          order: selectorDefNewOrder,
          style: selectorDefNew,
          _sheetId: styleSheetInst.id
        })
      }
    }
  }

  return styleSheetInst
}

/**
 * 返回动画帧的定义JSON
 * @param key
 * @param styleObject
 */
function _processAnimationName(key, styleObject) {
  const keyframes = styleObject['@KEYFRAMES']
  // 处理动画信息, 替换为关键帧
  if (keyframes) {
    const kf = keyframes[key]
    if (kf) {
      return JSON.stringify(kf)
    }
  }
  return null
}

/**
 * 返回字体的定义JSON
 * @param {string} key
 * @param styleObject
 */
function _processFontFamily(key, styleObject) {
  const fontFamilyDesc = styleObject['@FONT-FACE']
  // 剔除外层引号
  const fontFamilies = key.replace(/["']+/g, '').split(',')
  const result = []
  if (fontFamilies.length > 0) {
    fontFamilies.forEach(item => {
      item = item.trim()
      // 处理字体信息
      if (item) {
        if (fontFamilyDesc && fontFamilyDesc[item]) {
          result.push(fontFamilyDesc[item])
        } else {
          result.push({ fontName: item })
        }
      }
    })

    return JSON.stringify(result)
  }
  return null
}

// 选择器名称的缓存
const selectorDefCache = new Map()

/**
 * 编译选择器，缓存编译后的定义
 * @param selectorName
 * @return {V}
 */
function compileSelectorName(selectorName) {
  if (!selectorDefCache.has(selectorName)) {
    selectorDefCache.set(selectorName, cssWhat(selectorName))
  }
  return selectorDefCache.get(selectorName)
}

export { compileStyleObject }
