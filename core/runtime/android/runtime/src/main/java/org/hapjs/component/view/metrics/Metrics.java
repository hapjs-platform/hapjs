/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.metrics;

import android.text.TextUtils;
import android.util.Log;
import com.facebook.yoga.YogaNode;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;

/**
 * Metrics class for Specifying Component's minWidth, minHeight, maxWidth, maxHeight size limit.
 */
public class Metrics {
    public static final String PERCENT = Attributes.Unit.PERCENT;
    public static final String PX = Attributes.Unit.PX;
    private static final String TAG = "Metrics";
    private static final int MIN_FLAG = 0b1;
    private static final int MAX_FLAG = 0b1 << 1;
    private static final int WIDTH_FLAG = 0b1 << 2;
    private static final int HEIGHT_FLAG = 0b1 << 3;
    private static final int UNIT_PX = 1;
    private static final int UNIT_PERCENT = 2;
    private SizeLimit minWidth;
    private SizeLimit minHeight;
    private SizeLimit maxWidth;
    private SizeLimit maxHeight;

    public Metrics() {
    }

    public void applyMetrics(
            HapEngine hapEngine, String attributeStr, String limitStr, YogaNode node) {
        switch (attributeStr) {
            case Attributes.Style.MIN_WIDTH:
                if (minWidth == null) {
                    minWidth =
                            SizeLimit.createSizeLimit(hapEngine, MIN_FLAG | WIDTH_FLAG, limitStr);
                } else {
                    minWidth.parseSizeLimit(hapEngine, MIN_FLAG | WIDTH_FLAG, limitStr);
                }
                minWidth.apply(node);
                break;
            case Attributes.Style.MIN_HEIGHT:
                if (minHeight == null) {
                    minHeight =
                            SizeLimit.createSizeLimit(hapEngine, MIN_FLAG | HEIGHT_FLAG, limitStr);
                } else {
                    minHeight.parseSizeLimit(hapEngine, MIN_FLAG | HEIGHT_FLAG, limitStr);
                }
                minHeight.apply(node);
                break;
            case Attributes.Style.MAX_WIDTH:
                if (maxWidth == null) {
                    maxWidth =
                            SizeLimit.createSizeLimit(hapEngine, MAX_FLAG | WIDTH_FLAG, limitStr);
                } else {
                    maxWidth.parseSizeLimit(hapEngine, MAX_FLAG | WIDTH_FLAG, limitStr);
                }
                maxWidth.apply(node);
                break;
            case Attributes.Style.MAX_HEIGHT:
                if (maxHeight == null) {
                    maxHeight =
                            SizeLimit.createSizeLimit(hapEngine, MAX_FLAG | HEIGHT_FLAG, limitStr);
                } else {
                    maxHeight.parseSizeLimit(hapEngine, MAX_FLAG | HEIGHT_FLAG, limitStr);
                }
                maxHeight.apply(node);
                break;
            default:
                Log.e(TAG, "applyMetrics: unsupported attribute argument " + attributeStr);
                break;
        }
    }

    public static class SizeLimit {

        private int mFlag;
        private float mSizeLimit;
        private int mUnit;

        private SizeLimit() {
        }

        public static SizeLimit createSizeLimit(HapEngine hapEngine, int typeFlag,
                                                String limitStr) {
            SizeLimit sizeLimit = new SizeLimit();
            sizeLimit.parseSizeLimit(hapEngine, typeFlag, limitStr);
            return sizeLimit;
        }

        private void parseSizeLimit(HapEngine hapEngine, int typeFlag, String limitStr) {
            mFlag = typeFlag;
            if (TextUtils.isEmpty(limitStr)) {
                // 设置默认值
                if (isMin()) {
                    limitStr = Attributes.Style.AUTO;
                } else if (isMax()) {
                    limitStr = Attributes.Style.NONE;
                }
            } else {
                limitStr = limitStr.trim();
                if (isMax() && Attributes.Style.AUTO.equals(limitStr)) {
                    // max 不支持 auto
                    throw new IllegalArgumentException(
                            TAG + " parseSizeLimit: illegal value " + limitStr
                                    + " for max property");
                }
                if (isMin() && Attributes.Style.NONE.equals(limitStr)) {
                    // min 不支持 none
                    throw new IllegalArgumentException(
                            TAG + " parseSizeLimit: illegal value " + limitStr
                                    + " for min property");
                }
            }
            switch (limitStr) {
                case Attributes.Style.AUTO:
                    // TODO W3C草稿 https://drafts.csswg.org/css-sizing-3/#width-height-keywords
                    mSizeLimit = 0;
                    mUnit = UNIT_PX;
                    return;
                case Attributes.Style.NONE:
                    // 不做限制
                    mSizeLimit = Float.POSITIVE_INFINITY;
                    mUnit = UNIT_PX;
                    return;
                case Attributes.Style.MIN_CONTENT:
                    // TODO W3C草稿
                    break;
                case Attributes.Style.MAX_CONTENT:
                    // TODO W3C草稿
                    break;
                default:
                    break;
            }
            if (limitStr.startsWith(Attributes.Style.FIT_CONTENT)) {
                // TODO W3C草稿
            }
            float tempValue;
            if (limitStr.endsWith(PERCENT)) {
                mUnit = UNIT_PERCENT;
                tempValue = FloatUtil
                        .parse(limitStr.substring(0, limitStr.length() - PERCENT.length()));
            } else if (limitStr.endsWith(PX)) {
                mUnit = UNIT_PX;
                tempValue = Attributes.getFloat(hapEngine, limitStr);
            } else {
                // 若可解析为浮点数，视作PX值
                tempValue = Attributes.getFloat(hapEngine, limitStr + PX);
                if (FloatUtil.isUndefined(tempValue) || tempValue < 0) {
                    Log.e(TAG, "SizeLimit: illegal argument " + limitStr);
                } else {
                    mUnit = UNIT_PX;
                    mSizeLimit = tempValue;
                }
                return;
            }
            if (FloatUtil.isUndefined(tempValue) || tempValue < 0) {
                Log.e(TAG, "SizeLimit: illegal argument " + limitStr);
            } else {
                mSizeLimit = tempValue;
            }
        }

        void apply(YogaNode node) {
            if (node == null) {
                return;
            }

            boolean isWidth = isWidth();
            boolean isHeight = isHeight();
            boolean isMin = isMin();
            boolean isMax = isMax();
            if (isMin) {
                switch (mUnit) {
                    case UNIT_PX:
                        if (isWidth) {
                            node.setMinWidth(mSizeLimit);
                        } else if (isHeight) {
                            node.setMinHeight(mSizeLimit);
                        }
                        break;
                    case UNIT_PERCENT:
                        if (isWidth) {
                            node.setMinWidthPercent(mSizeLimit);
                        } else if (isHeight) {
                            node.setMinHeightPercent(mSizeLimit);
                        }
                        break;
                    default:
                        break;
                }
            } else if (isMax) {
                switch (mUnit) {
                    case UNIT_PX:
                        if (isWidth) {
                            node.setMaxWidth(mSizeLimit);
                        } else if (isHeight) {
                            node.setMaxHeight(mSizeLimit);
                        }
                        break;
                    case UNIT_PERCENT:
                        if (isWidth) {
                            node.setMaxWidthPercent(mSizeLimit);
                        } else if (isHeight) {
                            node.setMaxHeightPercent(mSizeLimit);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        public boolean isMin() {
            return (mFlag & MIN_FLAG) == MIN_FLAG;
        }

        public boolean isMax() {
            return (mFlag & MAX_FLAG) == MAX_FLAG;
        }

        public boolean isWidth() {
            return (mFlag & WIDTH_FLAG) == WIDTH_FLAG;
        }

        public boolean isHeight() {
            return (mFlag & HEIGHT_FLAG) == HEIGHT_FLAG;
        }
    }
}
