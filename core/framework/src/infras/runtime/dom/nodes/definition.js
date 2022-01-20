/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * 保留ID
 */
const NodeRef = {
  HTML_ID: '-1'
}

/**
 * 节点类型枚举
 */
const NodeType = {
  UNKNOWN: 0,
  ELEMENT: 1,
  ATTR: 2,
  TEXT: 3,
  COMMENT: 8,
  DOCUMENT: 9,
  DOCUMENT_FRAGMENT: 11,
  FIGMENT: 101
}

export { NodeRef, NodeType }
