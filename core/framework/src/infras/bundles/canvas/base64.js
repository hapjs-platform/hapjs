/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const CHARTS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/='

function arrayBufferToBase64(arraybuffer) {
  // Use a lookup table to find the index.
  const lookup = new Uint8Array(256)
  for (let i = 0; i < CHARTS.length; i++) {
    lookup[CHARTS.charCodeAt(i)] = i
  }

  const bytes = new Uint8Array(arraybuffer)
  const len = bytes.length
  let base64 = ''

  for (let i = 0; i < len; i += 3) {
    base64 += CHARTS[bytes[i] >> 2]
    base64 += CHARTS[((bytes[i] & 3) << 4) | (bytes[i + 1] >> 4)]
    base64 += CHARTS[((bytes[i + 1] & 15) << 2) | (bytes[i + 2] >> 6)]
    base64 += CHARTS[bytes[i + 2] & 63]
  }

  if (len % 3 === 2) {
    base64 = base64.substring(0, base64.length - 1) + '='
  } else if (len % 3 === 1) {
    base64 = base64.substring(0, base64.length - 2) + '=='
  }

  return base64
}

function base64ToArrayBuffer(base64) {
  // Use a lookup table to find the index.
  const lookup = new Uint8Array(256)
  for (let i = 0; i < CHARTS.length; i++) {
    lookup[CHARTS.charCodeAt(i)] = i
  }

  let bufferLength = base64.length * 0.75
  const len = base64.length
  let i
  let p = 0
  let encoded1
  let encoded2
  let encoded3
  let encoded4

  if (base64[base64.length - 1] === '=') {
    bufferLength--
    if (base64[base64.length - 2] === '=') {
      bufferLength--
    }
  }

  const arraybuffer = new ArrayBuffer(bufferLength)
  const bytes = new Uint8Array(arraybuffer)

  for (i = 0; i < len; i += 4) {
    encoded1 = lookup[base64.charCodeAt(i)]
    encoded2 = lookup[base64.charCodeAt(i + 1)]
    encoded3 = lookup[base64.charCodeAt(i + 2)]
    encoded4 = lookup[base64.charCodeAt(i + 3)]

    bytes[p++] = (encoded1 << 2) | (encoded2 >> 4)
    bytes[p++] = ((encoded2 & 15) << 4) | (encoded3 >> 2)
    bytes[p++] = ((encoded3 & 3) << 6) | (encoded4 & 63)
  }

  return arraybuffer
}

function btoa(input) {
  const str = String(input)
  let map = CHARTS
  let block = 0
  let output = ''
  for (
    let code, idx = 3 / 4, idx0 = 3 / 4, uarr, cntChar = 0;
    // 能取到字符时、block未处理完时、长度不足时
    !isNaN((code = str.codePointAt(Math.min(cntChar, idx)))) ||
    255 & block ||
    ((map = '='), (idx0 - 3 / 4) % 1);
    idx += 3 / 4, idx0 += 3 / 4, cntChar++
  ) {
    if (code > 0x7f) {
      // utf8字符处理
      ;(uarr = encodeURI(String.fromCodePoint(code)).split('%')).shift()
      let cntShift = 0
      for (let hex, idx2 = idx0 % 1; uarr[idx2 | 0]; idx2 += 3 / 4, cntShift++) {
        hex = uarr[idx2 | 0]
        block = (block << 8) | parseInt(hex, 16)
        output += map.charAt(63 & (block >> (8 - (idx2 % 1) * 8)))
      }
      idx = idx === 3 / 4 ? 0 : idx // 修复首字符为utf8字符时出错的BUG
      idx += ((3 / 4) * uarr.length) % 1 // idx补偿
      idx0 += (3 / 4) * (cntShift - 1) // 修复特殊字符编码bug
      // code大于0xffff的情况
      if (code > 0xffff) {
        cntChar++
        idx += 3 / 2
      }
    } else {
      block = (block << 8) | code
      output += map.charAt(63 & (block >> (8 - (idx0 % 1) * 8)))
      // 修复特殊字符中混入ASCII字符时编码bug
      if (idx0 % 1 === 0 && 255 & block && map !== '=') {
        idx += 3 / 4
        idx0 += 3 / 4
        output += map.charAt(63 & (block >> 2))
      }
    }
  }
  return output
}

export default {
  arrayBufferToBase64,
  base64ToArrayBuffer,
  btoa
}
