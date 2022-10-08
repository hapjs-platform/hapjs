/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { $camelize } from 'src/shared/util'

import { NodeRef, NodeType } from './nodes/definition'
import { getListener, getDocumentNodeByRef, removeDocument, getNodeStyleObjectId } from './model'
import { classAttrToClassList } from './util'
import $validateStyle from './validator'

// 原生组件映射表
const nativeComponentMap = {}

/**
 * 注册原生组件（仅注册组件名）
 * @param components
 */
function registerComponents(components) {
  if (typeof components === 'string') {
    components = JSON.parse(components)
  }

  if (!Array.isArray(components)) {
    components = [components]
  }

  components.forEach(comp => {
    if (!comp) {
      return
    }

    if (!comp.name) {
      comp.name = comp.type
      delete comp.type
    }

    comp.methods = comp.methods || []

    console.trace(`### App Runtime ### 注册组件---- `, JSON.stringify(comp))

    let nativeComp = nativeComponentMap[comp.name]

    if (!nativeComp) {
      nativeComp = nativeComponentMap[comp.name] = JSON.parse(JSON.stringify(comp))
      nativeComp.def = {}
    } else {
      nativeComp.methods = Array.from(new Set(nativeComp.methods.concat(comp.methods)))
    }
  })
}

/**
 * 获取Native组件的默认配置
 * @param componentName
 * @return {*}
 */
function getComponentDefaultOptions(componentName) {
  return nativeComponentMap[componentName]
}

/**
 * Native组件实例绑定对应的方法
 * @param node
 */
function bindComponentMethods(node) {
  const nodeType = node.tagName.toLowerCase()
  const component = getComponentDefaultOptions(nodeType)
  if (component && component.methods) {
    const methodNameList = component.methods.filter(name => name !== 'animate')
    methodNameList.forEach(function(methodName) {
      if (!node[methodName]) {
        node[methodName] = function(...args) {
          const listener = getListener(node._docId)
          if (listener) {
            listener.invokeComponentMethod(component.name, node.ref, methodName, args)
            console.trace(
              `### App Runtime ### 调用组件的方法：${component.name}.${methodName}()`,
              JSON.stringify(args)
            )
          } else {
            console.warn(
              `### App Runtime ### 调用组件的方法无效：${component.name}.${methodName}(), 组件已销毁`
            )
          }
        }
      }
    })
    return methodNameList
  }
  return []
}

/**
 * 获取属性的对象
 */
function getNodeAttributesAsObject(node) {
  return node._attr
}

/**
 * 获取内联样式对象
 */
function getNodeInlineStyleAsObject(node) {
  return node._style
}

/**
 * 获取class值数组
 */
function getNodeAttrClassList(node) {
  if (node._classList) {
    return node._classList
  }

  const attr = getNodeAttributesAsObject(node)
  node._classList = classAttrToClassList(attr.class)

  return node._classList
}

/**
 * 获取合并后的样式
 * @param node
 * @return {*|{}}
 */
function getNodeMergedStyleAsObject(node) {
  return node.mergedStyle
}

/**
 * 更新内联样式对象
 * @param node
 * @param value
 */
function setNodeInlineStyle(node, value) {
  node._style = value || {}
}

/**
 * 将样式字符串转换成对象
 * @param {String} cssText - 样式规则字符串
 * @returns {Object} - 样式对象，属性名为驼峰式命名
 */
function cssText2StyleObject(cssText) {
  let styleObj = {}
  const rules = cssText.split(';')
  rules
    .filter(rule => rule.trim()) // 忽略空白的规则
    .forEach(rule => {
      const colonIdx = rule.indexOf(':')
      let key = rule.substring(0, colonIdx).trim()
      key = $camelize(key)
      const value = rule.substring(colonIdx + 1).trim()
      // 校验并转换部分动态更新的样式
      const subStyle = $validateStyle(key, value)
      styleObj = Object.assign(styleObj, subStyle || {})
    })
  console.trace(`### App Runtime ### 元素的样式转换：${cssText} 为${JSON.stringify(styleObj)}`)
  return styleObj
}

/**
 * 转换节点为JSON对象(仅支持布局的节点)
 */
function getNodeAsJSON(
  node,
  includeChildren = true,
  includeStyleObject = false,
  includeMergedStyle = false,
  includeCustomDirective = false
) {
  if (node.nodeType === NodeType.ELEMENT) {
    // 元素节点：返回对象
    const hash = {
      ref: node.ref.toString(),
      type: node._type
    }

    // 内置属性
    const styleObjectId = getNodeStyleObjectId(node)
    if (styleObjectId) {
      hash.prop = {
        _styleObjectId: styleObjectId
      }
    }

    // 样式对象
    if (includeStyleObject && node._styleObject) {
      hash.styleObject = node._styleObject
    }

    // 是否使用父级的样式对象
    if (node._useParentStyle) {
      hash.prop = hash.prop || {}
      hash.prop._useParentStyle = true
    }

    // 属性
    const attr = getNodeAttributesAsObject(node)
    if (attr && Object.keys(attr).length) {
      hash.attr = {}
      for (const k in attr) {
        const v = attr[k]
        hash.attr[k] = [null, undefined].indexOf(v) !== -1 ? '' : v
      }
    }

    // 内联样式
    const inlineStyle = getNodeInlineStyleAsObject(node)
    if (inlineStyle && Object.keys(inlineStyle).length) {
      hash.inlineStyle = inlineStyle
    }

    // 合并后的样式
    if (includeMergedStyle) {
      const style = getNodeMergedStyleAsObject(node)
      if (style && Object.keys(style).length) {
        hash.style = style
      }
    }

    // 自定义指令，处理节点上需要触发的钩子函数
    const dirList = node._directives || []
    if (includeCustomDirective && dirList.length) {
      // 节点上存在自定义指令，才设置hooks，减少渲染指令长度
      hash.hooks = []
      // 遍历节点上的自定义指令列表
      for (let i = 0, len = dirList.length; i < len; i++) {
        const callbacks = Object.keys(dirList[i].callbacks)
        for (let j = 0, len = callbacks.length; j < len; j++) {
          // 将需要触发的指令回调名称去重后存入hooks
          if (hash.hooks.indexOf(callbacks[j]) === -1) {
            hash.hooks.push(callbacks[j])
          }
        }
      }
    }

    // 事件
    const eventList = Object.keys(node._eventTargetListeners || {})
    if (eventList && eventList.length) {
      hash.event = eventList
    }

    // 子节点
    const childList = node.layoutChildren || node.childNodes
    if (includeChildren && childList && childList.length) {
      const list = []
      for (let i = 0, len = childList.length; i < len; i++) {
        const childNode = childList[i]
        const childJson = getNodeAsJSON(
          childNode,
          includeChildren,
          includeStyleObject,
          includeMergedStyle
        )
        if (supportRender(childNode)) {
          list.push(childJson)
        } else {
          list.push.apply(list, childJson)
        }
      }
      // 兼容Native底层
      hash.children = list
    }
    return hash
  } else if (node.nodeType === NodeType.FIGMENT) {
    // 逻辑节点：返回数组
    const list = []
    if (includeChildren && node.layoutChildren && node.layoutChildren.length) {
      for (let i = 0, len = node.layoutChildren.length; i < len; i++) {
        const childNode = node.layoutChildren[i]
        const childJson = getNodeAsJSON(
          childNode,
          includeChildren,
          includeStyleObject,
          includeMergedStyle
        )
        if (childNode.nodeType === NodeType.FIGMENT) {
          list.push.apply(list, childJson)
        } else {
          list.push(childJson)
        }
      }
    }
    return list
  } else if (node.nodeType === NodeType.COMMENT) {
    // 注释节点
    return []
  } else {
    console.trace(`### App Runtime ### getNodeAsJSON() 忽略该类型(${node.nodeType})的节点序列化`)
  }
}

/**
 * 获取节点在DOM树中的层级深度
 * @param node
 * @return {number|*|null}
 */
function getNodeDepth(node) {
  return node._depth
}

/**
 * 是否支持布局
 * @param node
 * @return {boolean}
 */
function supportLayout(node) {
  return node._layout
}

/**
 * 是否支持渲染
 * @param node
 * @return {boolean}
 */
function supportRender(node) {
  return node._render
}

/**
 * 支持渲染的父节点，自己或向上查找
 * @param node
 * @param includeSelf
 * @return {*}
 */
function renderParent(node, includeSelf = false) {
  let nodeTmp = includeSelf ? node : node.parentNode
  while (nodeTmp && !supportRender(nodeTmp)) {
    nodeTmp = nodeTmp.parentNode
  }
  return nodeTmp
}

/**
 * 获取渲染父节点数组（从叶子到根）
 * @desc 优化：缓存起来，减少匹配时间
 * @param node
 * @param includeSelf
 * @return {Array}
 */
function renderParents(node, includeSelf = false) {
  const list = []
  let nodeTmp = renderParent(node, includeSelf)
  while (nodeTmp) {
    list.push(nodeTmp)
    nodeTmp = renderParent(nodeTmp)
  }
  return list
}

/**
 * 计算当前节点的可渲染节点(儿子级别的渲染节点)数量，包括自己
 * @return {number}
 */
function calcRenderCount(node) {
  if (node._renderCount !== null) {
    return node._renderCount
  }

  let count = supportRender(node) ? 1 : 0
  if (supportLayout(node) && !supportRender(node)) {
    for (let i = 0, len = node.layoutChildren.length; i < len; i++) {
      count += calcRenderCount(node.layoutChildren[i])
    }
  }

  node._renderCount = count
  return count
}

/**
 * 重置当前节点以上的可渲染数量
 * @param node
 */
function resetRenderCount(node) {
  let curr = node
  while (curr) {
    curr._renderCount = null
    curr = curr.parentNode
  }
}

/**
 * 计算当前节点相对于可渲染父节点的渲染索引
 * @returns {Number}
 * @private
 */
function renderIndexInRenderParent(node) {
  // 只有支持布局的节点才有效
  const parent = node.parentNode

  if (node.nodeType === NodeType.DOCUMENT || node.ref === NodeRef.HTML_ID) {
    return 0
  }

  if (!parent || !supportLayout(node)) {
    return -1
  }

  // 如果发生，说明数据异常
  const localIndex = renderIndexInParent(node, parent)
  // 父节点是非法节点
  let parentIndex = 0
  if (!supportRender(parent)) {
    parentIndex = renderIndexInRenderParent(parent)
  }

  if (localIndex < 0 || parentIndex < 0) {
    return -1
  }

  return localIndex + parentIndex
}

/**
 * 计算当前节点相对于父节点下的渲染索引
 * @param node
 * @param parent
 * @return {*}
 */
function renderIndexInParent(node, parent) {
  // 只有支持布局的节点才有效
  if (!parent.layoutChildren || parent.layoutChildren.length <= 0) {
    return -1
  }
  const index = parent.layoutChildren.indexOf(node)
  if (index > 0) {
    let count = 0
    for (let i = 0, len = index; i < len; i++) {
      count += calcRenderCount(parent.layoutChildren[i])
    }
    return count
  } else {
    return index
  }
}

/**
 * 销毁继承自Tag的节点对象
 * @param node
 */
function destroyTagNode(node) {
  switch (node.nodeType) {
    case NodeType.DOCUMENT:
      destroyDocument(node)
      break
    case NodeType.ELEMENT:
      destroyElement(node)
      break
    default:
      destroyTag(node)
  }
}

function destroyNode(node) {
  delete node._docId
  delete node._layout
  delete node._render
  delete node._renderCount

  delete node.nextSibling
  delete node.previousSibling
  delete node.parentNode

  // 防止销毁多次报错
  if (node.childNodes) {
    for (let i = node.childNodes.length - 1; i >= 0; i--) {
      destroyElement(node.childNodes[i])
    }
  }

  delete node.layoutChildren
  delete node.childNodes

  delete node._content
  delete node._ownerDocument

  console.trace(
    `### App Runtime ### 销毁节点：节点(${node.ref})剩余属性有：[${Object.keys(node).join(', ')}]`
  )
}

function destroyTag(node) {
  delete node._depth
  delete node._tmpRenderIndexInParent
  delete node._eventTargetListeners

  // 优化：框架层面的对象
  delete node._bindWatcherList
  delete node._vm

  destroyNode(node)
}

function destroyElement(node) {
  console.trace(
    `### App Runtime ### 销毁元素：${JSON.stringify(getNodeAsJSON(node, false, false, false))}`
  )

  delete node._classList

  delete node._styleObject
  delete node._styleObjectId
  delete node._useParentStyle
  delete node._usedCSSPropertyCache
  delete node._matchedCssRuleCache
  delete node._mergedStyleCache
  delete node._ownCssRuleCache

  delete node._attr
  delete node._style
  delete node._dataset

  destroyTag(node)
}

function destroyDocument(node) {
  console.trace(`### App Runtime ### 销毁文档`)
  delete node._nodeMap
  delete node.listener
  delete node._styleSheetHash

  if (node._styleObjectMap) {
    node._styleObjectMap.clear()
  }
  delete node._styleObjectMap

  if (node.documentElement) {
    destroyElement(node.documentElement)
  }
  delete node.documentElement
  delete node.body
  delete node.childNodes
  delete node.layoutChildren

  removeDocument(node._docId)
  destroyTag(node)
}

export {
  registerComponents,
  getComponentDefaultOptions,
  bindComponentMethods,
  getDocumentNodeByRef,
  getNodeAttributesAsObject,
  getNodeInlineStyleAsObject,
  getNodeAttrClassList,
  getNodeMergedStyleAsObject,
  setNodeInlineStyle,
  getNodeAsJSON,
  getNodeDepth,
  supportLayout,
  supportRender,
  renderParent,
  renderParents,
  calcRenderCount,
  resetRenderCount,
  renderIndexInRenderParent,
  renderIndexInParent,
  destroyTagNode,
  cssText2StyleObject
}
