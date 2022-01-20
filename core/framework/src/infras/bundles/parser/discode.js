/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const ENTITIES = new Map([
  /* MATH_SYMBOLES */
  ['forall', '∀'],
  ['part', '∂'],
  ['exists', '∃'],
  ['empty', '∅'],
  ['nabla', '∇'],
  ['isin', '∈'],
  ['notin', '∉'],
  ['ni', '∋'],
  ['prod', '∏'],
  ['sum', '∑'],
  ['minus', '−'],
  ['lowast', '∗'],
  ['radic', '√'],
  ['prop', '∝'],
  ['infin', '∞'],
  ['ang', '∠'],
  ['and', '∧'],
  ['or', '∨'],
  ['cap', '∩'],
  ['cup', '∪'],
  ['int', '∫'],
  ['there4', '∴'],
  ['sim', '∼'],
  ['cong', '≅'],
  ['asymp', '≈'],
  ['ne', '≠'],
  ['le', '≤'],
  ['ge', '≥'],
  ['sub', '⊂'],
  ['sup', '⊃'],
  ['nsub', '⊄'],
  ['sube', '⊆'],
  ['supe', '⊇'],
  ['oplus', '⊕'],
  ['otimes', '⊗'],
  ['perp', '⊥'],
  ['sdot', '⋅'],
  /* GREEK_SYMBOLS */
  /* -uppercase */
  ['Alpha', 'Α'],
  ['Beta', 'Β'],
  ['Gamma', 'Γ'],
  ['Delta', 'Δ'],
  ['Epsilon', 'Ε'],
  ['Zeta', 'Ζ'],
  ['Eta', 'Η'],
  ['Theta', 'Θ'],
  ['Iota', 'Ι'],
  ['Kappa', 'Κ'],
  ['Lambda', 'Λ'],
  ['Mu', 'Μ'],
  ['Nu', 'Ν'],
  ['Xi', 'Ν'],
  ['Omicron', 'Ο'],
  ['Pi', 'Π'],
  ['Rho', 'Ρ'],
  ['Sigma', 'Σ'],
  ['Tau', 'Τ'],
  ['Upsilon', 'Υ'],
  ['Phi', 'Φ'],
  ['Chi', 'Χ'],
  ['Psi', 'Ψ'],
  ['Omega', 'Ω'],
  /* -lowercase */
  ['alpha', 'α'],
  ['beta', 'β'],
  ['gamma', 'γ'],
  ['delta', 'δ'],
  ['epsilon', 'ε'],
  ['zeta', 'ζ'],
  ['eta', 'η'],
  ['theta', 'θ'],
  ['iota', 'ι'],
  ['kappa', 'κ'],
  ['lambda', 'λ'],
  ['mu', 'μ'],
  ['nu', 'ν'],
  ['xi', 'ξ'],
  ['omicron', 'ο'],
  ['pi', 'π'],
  ['rho', 'ρ'],
  ['sigmaf', 'ς'],
  ['sigma', 'σ'],
  ['tau', 'τ'],
  ['upsilon', 'υ'],
  ['phi', 'φ'],
  ['chi', 'χ'],
  ['psi', 'ψ'],
  ['omega', 'ω'],
  ['thetasym', 'ϑ'],
  ['upsih', 'ϒ'],
  ['piv', 'ϖ'],
  ['middot', '·'],
  /* COMMON_ENTITIES */
  ['nbsp', ' '],
  ['quot', "'"],
  ['amp', '&'],
  ['lt', '<'],
  ['gt', '>'],
  /* OTHER_ENTITIES */
  ['OElig', 'Œ'],
  ['oelig', 'œ'],
  ['Scaron', 'Š'],
  ['scaron', 'š'],
  ['Yuml', 'Ÿ'],
  ['fnof', 'ƒ'],
  ['circ', 'ˆ'],
  ['tilde', '˜'],
  ['ensp', ''],
  ['emsp', ''],
  ['thinsp', ''],
  ['zwnj', ''],
  ['zwj', ''],
  ['lrm', ''],
  ['rlm', ''],
  ['ndash', '–'],
  ['mdash', '—'],
  ['lsquo', '‘'],
  ['rsquo', '’'],
  ['sbquo', '‚'],
  ['ldquo', '“'],
  ['rdquo', '”'],
  ['bdquo', '„'],
  ['dagger', '†'],
  ['Dagger', '‡'],
  ['bull', '•'],
  ['hellip', '…'],
  ['permil', '‰'],
  ['prime', '′'],
  ['Prime', '″'],
  ['lsaquo', '‹'],
  ['rsaquo', '›'],
  ['oline', '‾'],
  ['euro', '€'],
  ['trade', '™'],
  ['larr', '←'],
  ['uarr', '↑'],
  ['rarr', '→'],
  ['darr', '↓'],
  ['harr', '↔'],
  ['crarr', '↵'],
  ['lceil', '⌈'],
  ['rceil', '⌉'],
  ['lfloor', '⌊'],
  ['rfloor', '⌋'],
  ['loz', '◊'],
  ['spades', '♠'],
  ['clubs', '♣'],
  ['hearts', '♥'],
  ['diams', '♦'],
  ['#8203', ''] // 防止字符串中出现unicode#8203(zero width space)
])

/**
 * 转义解码
 * @param str
 * @returns {string|XML|*|*}
 */
function decode(str) {
  return str
    .replace(/&([a-zA-Z#0-9]+?);/g, (_, code) => {
      return ENTITIES.get[code] || '&' + code + ';'
    })
    .replace(/\r\n?/g, '')
}
/**
 * url转换, 自动补全http:
 * @param url
 * @param rep
 * @returns {*}
 */
function $url(url, rep) {
  const temp = new RegExp('^//')
  const result = temp.test(url)
  if (result) {
    url = rep + ':' + url
  }
  return url
}

export { decode, $url }
