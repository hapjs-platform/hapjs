/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
!function () {
  if ("undefined" == typeof window) (function (e) {
    var p = {};

    function t (o) {
      if (p[o])return p[o].exports;
      var n = p[o] = {exports: {}, id: o, loaded: !1};
      return e[o].call(n.exports, n, n.exports, t), n.loaded = !0, n.exports
    }

    t.m = e, t.c = p, t.p = "", t(0)
  })({
    0: function (e, p, t) {
      var o = t(142);
      $app_define$("@app-application/app", [], function (e, p, t) {
        o(t, p, e), p.__esModule && p.default && (t.exports = p.default)
      }), $app_bootstrap$("@app-application/app", {packagerVersion: "0.0.5"})
    }, 142: function (e, p) {
      e.exports = function (e, p, t) {
        "use strict";
        Object.defineProperty(p, "__esModule", {value: !0}), p.default = {}
      }
    }
  })
}();