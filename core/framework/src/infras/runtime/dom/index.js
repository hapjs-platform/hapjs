/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * @file DOM规范的API汇总
 * @desc 只能被外部文件引用，不能被内部引用
 */

import Event from './nodes/event/event'
import TouchEvent from './nodes/event/touch'
import Node from './nodes/node'
import DomComment from './nodes/comment'
import DomDocument from './nodes/document'
import DomElement from './nodes/element'
import DomFigment from './nodes/figment'
import DomFragment from './nodes/fragment'
import DomHtmlElement from './nodes/html'
import DomTag from './nodes/tag'
import EventTarget from './nodes/target'
import DomText from './nodes/text'

// 导出DOM对象
export { Node, DomDocument, Event, TouchEvent }

export { getListener, genIdByStyleObject } from './model'

export {
  registerComponents,
  getComponentDefaultOptions,
  bindComponentMethods,
  getDocumentNodeByRef,
  getNodeAttributesAsObject,
  getNodeInlineStyleAsObject,
  getNodeAttrClassList,
  setNodeInlineStyle,
  getNodeAsJSON,
  getNodeDepth,
  supportLayout,
  supportRender,
  calcRenderCount,
  destroyTagNode
} from './misc'

export { createEvent } from './nodes/event/index'

export {
  createFigment,
  updatePageTitleBar,
  updatePageStatusBar,
  setMeta,
  scrollTo,
  scrollBy,
  exitFullscreen,
  hideSkeleton,
  callHostFunction
} from './nodes/document'

export {
  setElementProp,
  setElementAttr,
  setElementStyle,
  setElementStyles,
  getElementMatchedCssRuleList,
  setElementMatchedCssRule
} from './nodes/element'

export { resetNodeChildren, restoreNodeChildren } from './nodes/tag'

export { fireTargetEventListener, clearTargetEventListener } from './nodes/target'

function freeze() {
  Object.freeze(Event)
  Object.freeze(Event.prototype)
  Object.freeze(DomComment)
  Object.freeze(DomComment.prototype)
  Object.freeze(DomDocument)
  Object.freeze(DomDocument.prototype)
  Object.freeze(DomElement)
  Object.freeze(DomElement.prototype)
  Object.freeze(DomFigment)
  Object.freeze(DomFigment.prototype)
  Object.freeze(DomFragment)
  Object.freeze(DomFragment.prototype)
  Object.freeze(DomHtmlElement)
  Object.freeze(DomHtmlElement.prototype)
  Object.freeze(Node)
  Object.freeze(Node.prototype)
  Object.freeze(DomTag)
  Object.freeze(DomTag.prototype)
  Object.freeze(EventTarget)
  Object.freeze(EventTarget.prototype)
  Object.freeze(DomText)
  Object.freeze(DomText.prototype)
}

export { freeze }
