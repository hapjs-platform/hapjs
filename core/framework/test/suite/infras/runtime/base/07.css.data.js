/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// 简单的CSS样式
const simple1 = {
  '#idTest1': {
    ca1: 'ca1-id-1'
  },
  '.class-test1': {
    ca1: 'ca1-class-1'
  },
  '.class-test2': {
    ca1: 'ca1-class-2'
  },
  div: {
    ca1: 'ca1-tag-1'
  }
}

// 后代选择
const desc1 = {
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
  }
}

const inlineEditPropList = [
  {
    name: 'ca1',
    value: 'ca1-inline-1',
    disabled: true
  },
  {
    name: 'ca2',
    value: 'ca2-inline-1',
    disabled: false
  }
]

const descEditPropList1 = [
  {
    name: 'ca1',
    value: 'ca1-desc-doc-page-idTest1-2',
    disabled: true
  },
  {
    name: 'ca2',
    value: 'ca2-desc-doc-page-idTest1-2',
    disabled: false
  }
]

const styleNodeForMap = {
  map: {
    ca1: 'ca1-tag-1'
  },
  '.map': {
    ca1: 'ca1-class-map-1'
  },
  '.doc-page map': {
    ca1: 'ca1-desc-doc-page-map-1'
  }
}

const desc2 = {
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
  }
}

const descEditPropList2 = [
  {
    name: 'ca1',
    value: 'ca1-desc-doc-page-idTest1-2',
    disabled: true
  },
  {
    name: 'ca2',
    value: 'ca2-desc-doc-page-idTest1-2',
    disabled: false
  }
]

// 文档级别的样式
const docLevelStyle1 = {
  div: {
    ca1: 'ca1-div-1'
  }
}

// 修改非末尾的ID
const descNotTailSelectorIdUpdateNode = {
  '.doc-page #idDivTest1 h1': {
    ca1: 'ca1-desc-doc-page-idDivTest1-h1-1'
  },
  '.doc-page #idDivTest2 h1': {
    ca1: 'ca1-desc-doc-page-idDivTest2-h1-1'
  },
  '.doc-page #idDivTest1 div h1': {
    ca1: 'ca1-desc-doc-page-idDivTest1-div-h1-1'
  },
  '.doc-page #idDivTest2 div h1': {
    ca1: 'ca1-desc-doc-page-idDivTest2-div-h1-1'
  }
}

// 修改非末尾的class
const descNotTailSelectorClassUpdateNode = {
  '.doc-page .classDivTest1 h1': {
    ca1: 'ca1-desc-doc-page-classDivTest1-h1-1'
  },
  '.doc-page .classDivTest2 h1': {
    ca1: 'ca1-desc-doc-page-classDivTest2-h1-1'
  },
  '.doc-page .classDivTest1 div h1': {
    ca1: 'ca1-desc-doc-page-classDivTest1-div-h1-1'
  },
  '.doc-page .classDivTest2 div h1': {
    ca1: 'ca1-desc-doc-page-classDivTest2-div-h1-1'
  }
}

export default {
  simple1,
  desc1,
  inlineEditPropList,
  descEditPropList1,
  styleNodeForMap,
  desc2,
  descEditPropList2,
  docLevelStyle1,
  descNotTailSelectorIdUpdateNode,
  descNotTailSelectorClassUpdateNode
}
