/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import DocumentView from './nodes/document'

export { DocumentView }

export {
  registerFromCssFile,
  getStylingDocumentId,
  setDocument,
  removeDocument,
  getDocument,
  getListener,
  getDocumentNodeByRef,
  defineNodeStyleSheet
} from './model'

export { getNodeAsJSON } from './misc'

export { compileStyleObject } from './css/compiler'

export {
  setElementProp,
  setElementAttr,
  setElementStyle,
  setElementStyles,
  getElementMatchedCssRuleList,
  setElementMatchedCssRule
} from './nodes/element'
