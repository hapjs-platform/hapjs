/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

interface Selector {

    // E > F
    int SAC_CHILD_SELECTOR = 1;
    // E F
    int SAC_DESCENDANT_SELECTOR = 2;
    // .myClass #myId
    int SAC_CONDITIONAL_SELECTOR = 4;
    // H1
    int SAC_ELEMENT_NODE_SELECTOR = 3;

    int getSelectorType();

    long getScore();

    boolean match(CSSStyleRule cssStyleRule, Node node, Node lastChild);
}
