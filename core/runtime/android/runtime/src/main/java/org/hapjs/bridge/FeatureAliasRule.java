/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeatureAliasRule {
    private String mName;
    private String mTarget;
    private boolean mRegex;
    private Pattern mPattern;

    public FeatureAliasRule(String name, String target, boolean regex) {
        mName = name;
        mTarget = target;
        mRegex = regex;
    }

    public String getName() {
        return mName;
    }

    public String getTarget() {
        return mTarget;
    }

    public boolean isRegex() {
        return mRegex;
    }

    public String resolveAlias(String name) {
        if (mRegex) {
            if (mPattern == null) {
                mPattern = Pattern.compile(mName);
            }
            Matcher matcher = mPattern.matcher(name);
            if (matcher.matches()) {
                return matcher.replaceAll(mTarget);
            }
        } else {
            if (mName.equals(name)) {
                return mTarget;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FeatureAliasRule that = (FeatureAliasRule) o;

        if (mRegex != that.mRegex) {
            return false;
        }
        if (mName != null ? !mName.equals(that.mName) : that.mName != null) {
            return false;
        }
        return mTarget != null ? mTarget.equals(that.mTarget) : that.mTarget == null;
    }

    @Override
    public int hashCode() {
        int result = mName != null ? mName.hashCode() : 0;
        result = 31 * result + (mTarget != null ? mTarget.hashCode() : 0);
        result = 31 * result + (mRegex ? 1 : 0);
        return result;
    }
}
