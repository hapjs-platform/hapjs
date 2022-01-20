/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MatchedCSSRuleList {
    private CSSStyleRuleWithScore[] mCSSStyleRuleWithScores;

    MatchedCSSRuleList(List<CSSStyleRule> cssStyleRules, Node node) {

        if (cssStyleRules == null) {
            return;
        }

        mCSSStyleRuleWithScores = new CSSStyleRuleWithScore[cssStyleRules.size()];

        for (int i = 0; i < cssStyleRules.size(); i++) {
            CSSStyleRule item = cssStyleRules.get(i);
            long score = CSSCalculator.calculateScore(item, node);
            mCSSStyleRuleWithScores[i] = new CSSStyleRuleWithScore(item, score);
        }

        Arrays.sort(
                mCSSStyleRuleWithScores,
                new Comparator<CSSStyleRuleWithScore>() {
                    @Override
                    public int compare(CSSStyleRuleWithScore o1, CSSStyleRuleWithScore o2) {
                        long first = o1.score;
                        long second = o2.score;
                        if (first > second) {
                            return 1;
                        } else if (first < second) {
                            return -1;
                        }
                        return 0;
                    }
                });
    }

    // For inspector
    public int length() {
        if (mCSSStyleRuleWithScores == null) {
            return 0;
        }
        return mCSSStyleRuleWithScores.length;
    }

    // For inspector
    public CSSStyleRule getCSSStyleRule(int index) {
        if (mCSSStyleRuleWithScores == null) {
            return null;
        }
        return mCSSStyleRuleWithScores[index].cssStyleRule;
    }

    // For inspector
    public long getScore(int index) {
        return mCSSStyleRuleWithScores[index].score;
    }

    private static class CSSStyleRuleWithScore {
        private final CSSStyleRule cssStyleRule;
        private final long score;

        CSSStyleRuleWithScore(CSSStyleRule cssStyleRule, long score) {
            this.cssStyleRule = cssStyleRule;
            this.score = score;
        }
    }
}
