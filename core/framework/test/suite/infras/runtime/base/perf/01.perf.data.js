/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const styleNodeCase1 = {
  '#idTest1': {
    ca1: 'ca1-desc-idTest1-1'
  },
  '.class-test1': {
    ca1: 'ca1-desc-class-test1-1'
  },
  div: {
    ca1: 'ca1-desc-div-1'
  },
  'div text': {
    ca1: 'ca1-desc-div-text-1'
  },
  '.doc-page #idTest1': {
    ca1: 'ca1-desc-doc-page-idTest1-1'
  },
  '.doc-page .class-test1': {
    ca1: 'ca1-desc-doc-page-class-test1-1'
  },
  '.doc-page div #idTest1': {
    ca1: 'ca1-desc-doc-page-div-idTest1-1'
  },
  '.doc-page div .class-test1': {
    ca1: 'ca1-desc-doc-page-div-class-test1-1'
  },
  '.doc-page .doc-block #idTest1': {
    ca1: 'ca1-desc-doc-page-doc-block-idTest1-1'
  },
  '.doc-page .doc-block .class-test1': {
    ca1: 'ca1-desc-doc-page-doc-block-class-test1-1'
  }
}

export default {
  styleNodeCase1
}
