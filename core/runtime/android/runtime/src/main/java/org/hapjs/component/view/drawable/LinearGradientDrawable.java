/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view.drawable;

import android.graphics.LinearGradient;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.text.TextUtils;
import android.util.Log;
import java.util.List;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.view.CSSGradientParser;
import org.hapjs.runtime.HapEngine;

public class LinearGradientDrawable extends ShapeDrawable {

    // direction
    static final String TO_TOP = "top";
    static final String TO_LEFT = "left";
    static final String TO_RIGHT = "right";
    static final String TO_BOTTOM = "bottom";
    static final String TAG_DEG = "deg";
    static final int QUADRENT_FIRST = 1;
    static final int QUADRENT_SECOND = 2;
    static final int QUADRENT_THIRD = 4;
    static final int QUADRENT_FOURTH = 8;
    private static final String TAG = "LinearGradientDrawable";
    private List<String> mDirection;
    private List<CSSGradientParser.ColorStop> mColorStop;
    private int[] mColor;
    private float[] mPosition;
    private Shader.TileMode mMode = Shader.TileMode.CLAMP;
    private PointF mStartPoint = new PointF();
    private PointF mEndPoint = new PointF();
    private int mQuadrent = QUADRENT_THIRD;

    private HapEngine mHapEngine;

    public LinearGradientDrawable(
            HapEngine hapEngine, List<String> directions,
            List<CSSGradientParser.ColorStop> colors) {
        mHapEngine = hapEngine;
        this.mColorStop = colors;
        this.mDirection = directions;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        final Rect r = getBounds();
        final int w = r.width();
        final int h = r.height();
        getPaint().setShader(getShader(w, h));
    }

    public Shader getShader(int width, int height) {
        if (width == 0 || height == 0) {
            return null;
        }
        if (mColorStop == null || mColorStop.size() < 2) {
            return null;
        }
        parseGradientDirectionCoordinate(width, height);
        parseGradientColorStopPosition();
        return new LinearGradient(
                mStartPoint.x, mStartPoint.y, mEndPoint.x, mEndPoint.y, mColor, mPosition, mMode);
    }

    /**
     * parse direction(top,bottom,left,right and their combination) and user assign angle.rules of
     * parse direction is base on w3c.
     */
    private void parseGradientDirectionCoordinate(float width, float height) {
        // default direction is top to bottom,base on w3c.
        if (mDirection == null || mDirection.size() == 0 || TextUtils.isEmpty(mDirection.get(0))) {
            mEndPoint.y = height;
            return;
        }
        // to directions
        int quarent = -1;
        int directionQuarent = -1;
        for (int index = 0; index < mDirection.size(); index++) {
            switch (mDirection.get(index)) {
                case TO_LEFT: {
                    mStartPoint.x = width;
                    if (directionQuarent == -1) {
                        quarent = QUADRENT_THIRD;
                        directionQuarent = (QUADRENT_THIRD | QUADRENT_FOURTH);
                    } else {
                        quarent = directionQuarent & (QUADRENT_THIRD | QUADRENT_FOURTH);
                    }
                    break;
                }
                case TO_TOP: {
                    mStartPoint.y = height;
                    if (directionQuarent == -1) {
                        quarent = QUADRENT_FOURTH;
                        directionQuarent = (QUADRENT_FIRST | QUADRENT_FOURTH);
                    } else {
                        quarent = directionQuarent & (QUADRENT_FIRST | QUADRENT_FOURTH);
                    }
                    break;
                }
                case TO_RIGHT: {
                    mEndPoint.x = width;
                    if (directionQuarent == -1) {
                        quarent = QUADRENT_FIRST;
                        directionQuarent = (QUADRENT_FIRST | QUADRENT_SECOND);
                    } else {
                        quarent = directionQuarent & (QUADRENT_FIRST | QUADRENT_SECOND);
                    }
                    break;
                }
                case TO_BOTTOM: {
                    mEndPoint.y = height;
                    if (directionQuarent == -1) {
                        quarent = QUADRENT_SECOND;
                        directionQuarent = (QUADRENT_SECOND | QUADRENT_THIRD);
                    } else {
                        quarent = directionQuarent & (QUADRENT_SECOND | QUADRENT_THIRD);
                    }
                    break;
                }
                default:
                    break;
            }
        }
        if (quarent != -1) {
            mQuadrent = quarent;
        }
        // deg
        boolean isNonDirection =
                (mStartPoint.x == 0 && mStartPoint.y == 0 && mEndPoint.x == 0 && mEndPoint.y == 0);
        if (isNonDirection) {
            String degStr = mDirection.get(0);
            String temp = degStr.toString().trim();
            if (temp.endsWith(TAG_DEG)) {
                temp = temp.substring(0, temp.indexOf(TAG_DEG));
                try {
                    float angle = Float.parseFloat(temp);
                    float halfWidth = width / 2;
                    float halfHeight = height / 2;
                    // deal with bigger than 360 angle and negative angle .
                    angle = angle % 360;
                    if (angle < 0) {
                        angle = (360 + angle);
                    }
                    double diagonalAngle = Math.toDegrees(Math.atan2(width, height));
                    float nonQuadrantAngle = angle;
                    float nonQuadrantCompleAngle = angle;
                    double compleDiagonalAngle = diagonalAngle;
                    int quadrant =
                            angle <= 90
                                    ? QUADRENT_FIRST
                                    : (angle <= 180
                                    ? QUADRENT_SECOND
                                    : (angle <= 270 ? QUADRENT_THIRD :
                                    (angle <= 360 ? QUADRENT_FOURTH : -1)));
                    mQuadrent = quadrant;
                    if (quadrant % 2 == 0) {
                        nonQuadrantAngle = (angle - (quadrant - 1) * 90);
                        nonQuadrantCompleAngle = 90 - nonQuadrantAngle;
                        compleDiagonalAngle = 90 - diagonalAngle;
                    }
                    double angleInRadians = Math.toRadians(nonQuadrantCompleAngle);
                    double remainRadians =
                            Math.toRadians(Math.abs(compleDiagonalAngle - nonQuadrantAngle));
                    double diagonalQuarterLen =
                            Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2)) / 4;
                    double tangentLen = (Math.cos(remainRadians) * diagonalQuarterLen * 2);
                    float endX = (float) (Math.sin(angleInRadians) * tangentLen);
                    float endY = (float) (Math.cos(angleInRadians) * tangentLen);
                    // the first and third or second and fourth has same coordinate.
                    switch (quadrant) {
                        case QUADRENT_FIRST:
                        case QUADRENT_THIRD: {
                            if (nonQuadrantAngle <= compleDiagonalAngle) {
                                mStartPoint.x = (halfWidth - endX);
                                mStartPoint.y = endY + halfHeight;
                                mEndPoint.x = endX + halfWidth;
                                mEndPoint.y = -(endY - halfHeight);
                            } else {
                                mStartPoint.x = -(endX - halfWidth);
                                mStartPoint.y = endY + halfHeight;
                                mEndPoint.x = endX + halfWidth;
                                mEndPoint.y = halfHeight - endY;
                            }
                            break;
                        }
                        case QUADRENT_SECOND:
                        case QUADRENT_FOURTH: {
                            if (nonQuadrantAngle <= compleDiagonalAngle) {
                                mStartPoint.x = -(endX - halfWidth);
                                mStartPoint.y = halfHeight - endY;
                                mEndPoint.x = endX + halfWidth;
                                mEndPoint.y = endY + halfHeight;
                            } else {
                                mStartPoint.x = halfWidth - endX;
                                mStartPoint.y = -(endY - halfHeight);
                                mEndPoint.x = endX + halfWidth;
                                mEndPoint.y = halfHeight + endY;
                            }
                            break;
                        }
                        default:
                            break;
                    }
                    // The First and third quadrant has same coordinate,
                    // but direction is opposite,switch between start and end point.
                    if (quadrant == QUADRENT_THIRD || quadrant == QUADRENT_FOURTH) {
                        float tmpX = mStartPoint.x;
                        mStartPoint.x = mEndPoint.x;
                        mEndPoint.x = tmpX;
                        float tmpY = mStartPoint.y;
                        mStartPoint.y = mEndPoint.y;
                        mEndPoint.y = tmpY;
                    }
                    isNonDirection = false;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "parse gradient direction coordinate error", e);
                }
            }
        }

        // make sure default direction if user not set direction.
        if (isNonDirection) {
            mEndPoint.y = height;
        }
    }

    private void parseGradientColorStopPosition() {
        int colorSize = mColorStop.size();
        int[] colors = new int[colorSize];
        float[] positions = new float[colorSize];
        float distantX = Math.abs(mEndPoint.x - mStartPoint.x);
        float distantY = Math.abs(mEndPoint.y - mStartPoint.y);
        float talentLen = (float) Math.sqrt(Math.pow(distantX, 2) + Math.pow(distantY, 2));
        // parse stop-position and color.
        CSSGradientParser.ColorStop colorStop;
        for (int index = 0; index < colorSize; index++) {
            colorStop = mColorStop.get(index);
            colors[index] = colorStop.mColor;
            String positionStr = colorStop.mPosition;
            if (positionStr.endsWith("%")) {
                String positionNumStr = positionStr.substring(0, positionStr.indexOf("%"));
                positions[index] = Float.parseFloat(positionNumStr) / 100;
            } else if (positionStr.endsWith("px")) {
                String pxNumStr = positionStr.substring(0, positionStr.indexOf("px"));
                float px =
                        DisplayUtil.getRealPxByWidth(Float.parseFloat(pxNumStr),
                                mHapEngine.getDesignWidth());
                positions[index] = px / talentLen;
            } else {
                positions[index] = Float.parseFloat(positionStr);
            }
        }

        CSSGradientParser.ColorStop lastStop = mColorStop.get(colorSize - 1);
        CSSGradientParser.ColorStop firstStop = mColorStop.get(0);
        float firstPosition = positions[0];
        float lastPosition = positions[colorSize - 1];
        if (!lastStop.isDefaultPosition || !firstStop.isDefaultPosition) {
            double angle = Math.toDegrees(Math.atan2(distantX, distantY));
            float talentX = (float) (talentLen * Math.sin(Math.toRadians(angle)));
            float talentY = (float) (talentLen * Math.cos(Math.toRadians(angle)));
            switch (mQuadrent) {
                case QUADRENT_FIRST: {
                    talentY = -talentY;
                    break;
                }
                case QUADRENT_THIRD: {
                    talentX = -talentX;
                    break;
                }
                case QUADRENT_FOURTH: {
                    talentX = -talentX;
                    talentY = -talentY;
                    break;
                }
                default:
                    break;
            }

            // At REPEAT mode, the first stop-position is used by start coordinate and the
            // last stop-position is used by end coordinate,so calculate new start\end
            // coordinate and reset start stop-position to 0,end stop-position to 1.
            if (mMode == Shader.TileMode.REPEAT) {
                if (!lastStop.isDefaultPosition) {
                    mEndPoint.x = mStartPoint.x + lastPosition * talentX;
                    mEndPoint.y = mStartPoint.y + lastPosition * talentY;
                    // reset color stop-position
                    positions[colorSize - 1] = 1;
                }
                if (!firstStop.isDefaultPosition) {
                    mStartPoint.x = mStartPoint.x + firstPosition * talentX;
                    mStartPoint.y = mStartPoint.y + firstPosition * talentY;
                    // reset color-stop position
                    positions[0] = 0;
                }
            }

            if (mMode == Shader.TileMode.CLAMP) {
                if (firstPosition < 0) {
                    mStartPoint.x = mStartPoint.x + firstPosition * talentX;
                    mStartPoint.y = mStartPoint.y + firstPosition * talentY;
                    // reset color-stop position by start and end point increment.
                    float step = -firstPosition;
                    for (int pos = 1; pos < colorSize; pos++) {
                        positions[pos] = (positions[pos] + step) / (1 - firstPosition);
                    }
                    positions[0] = 0;
                }
                if (lastPosition > 1) {
                    float radius = lastPosition - 1;
                    mEndPoint.x = mEndPoint.x + radius * talentX;
                    mEndPoint.y = mEndPoint.y + radius * talentY;
                    // reset color-stop position by start and end point decrement.
                    for (int pos = 0; pos < colorSize - 1; pos++) {
                        positions[pos] = positions[pos] / lastPosition;
                    }
                    positions[colorSize - 1] = 1;
                }
            }
        }

        // If user set the last and first of color stop-position,it will
        // calculate stop-position again what the user not defined that
        // base on the last stop-position radius.
        // Specially,at REPEAT mode, the start and end coordinate is calculated
        // to a new one at begin,so the other stop-position must cal again accord
        // -ing new start and end coordinate.
        float totalPosition = positions[colorSize - 1] + positions[0];
        if (!lastStop.isDefaultPosition || !firstStop.isDefaultPosition) {
            float step = totalPosition / (colorSize - 1);
            for (int index = 1; index < colorSize - 1; index++) {
                colorStop = mColorStop.get(index);
                if (colorStop.isDefaultPosition) {
                    positions[index] = step * index;
                }
                if (mMode == Shader.TileMode.REPEAT && !colorStop.isDefaultPosition) {
                    positions[index] =
                            (positions[index] - firstPosition) / (lastPosition - firstPosition);
                }
            }
        }

        mColor = colors;
        mPosition = positions;
    }

    /**
     * @param mode default mode is {@link Shader.TileMode#CLAMP}
     */
    public Drawable setMode(Shader.TileMode mode) {
        this.mMode = mode;
        return this;
    }

    /**
     * @param direction may be null,default direction is bottom to top.
     */
    public void setDirection(List<String> direction) {
        this.mDirection = direction;
    }

    /**
     * @param color can't not be null and needs >= 2 number of colors.
     */
    public void setColorStop(List<CSSGradientParser.ColorStop> color) {
        this.mColorStop = color;
    }
}
