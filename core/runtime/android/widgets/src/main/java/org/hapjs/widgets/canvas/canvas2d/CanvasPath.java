/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.canvas2d;

import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;
import org.hapjs.widgets.canvas.annotation.CanvasMethod;

public class CanvasPath extends Path {

    private List<Integer> mCommand;

    private PointF mStartPoint;
    private PointF mCurrentPoint;

    public CanvasPath() {
        super();
        mCommand = new ArrayList<>();
        mStartPoint = new PointF();
        mCurrentPoint = new PointF();
    }

    public CanvasPath(CanvasPath src) {
        super(src);
        mCommand = new ArrayList<>();
        mStartPoint = new PointF();
        mCurrentPoint = new PointF();
        mCommand.addAll(src.mCommand);
        mStartPoint.set(src.mStartPoint.x, src.mStartPoint.y);
        mCurrentPoint.set(src.mCurrentPoint.x, src.mCurrentPoint.y);
    }

    @Override
    public void reset() {
        super.reset();
        mCommand.clear();
        mStartPoint.x = mStartPoint.y = 0;
        mCurrentPoint.x = mCurrentPoint.y = 0;
    }

    @CanvasMethod
    public void beginPath() {
        reset();
    }

    @CanvasMethod
    @Override
    public void moveTo(float x, float y) {
        super.moveTo(x, y);
        mCommand.add(PathCommand.MOVE_TO);
        mStartPoint.x = x;
        mStartPoint.y = y;
        mCurrentPoint.x = x;
        mCurrentPoint.y = y;
    }

    @CanvasMethod
    @Override
    public void lineTo(float x, float y) {
        super.lineTo(x, y);
        mCommand.add(PathCommand.LINE_TO);
        mCurrentPoint.x = x;
        mCurrentPoint.y = y;
    }

    @CanvasMethod
    public void arc(
            float cx, float cy, float radius, float startAngle, float endAngle,
            boolean anticlockwise) {
        float startX = (float) (cx + radius * Math.cos(startAngle));
        float startY = (float) (cy + radius * Math.sin(startAngle));

        if (mCommand.size() == 0) {
            moveTo(startX, startY);
        } else {
            lineTo(startX, startY);
        }

        if (startAngle == endAngle) {
            return;
        }

        startAngle = (float) ((startAngle * 180.0f) / Math.PI);
        endAngle = (float) ((endAngle * 180.0f) / Math.PI);

        float sweepAngle = endAngle - startAngle;

        if (anticlockwise) {
            if (sweepAngle <= -360) {
                sweepAngle = 360;
            } else {
                while (sweepAngle >= 0) {
                    sweepAngle -= 360;
                }
            }
        } else {
            if (sweepAngle >= 360) {
                sweepAngle = 360;
            } else {
                while (sweepAngle <= 0) {
                    sweepAngle += 360;
                }
            }
        }

        if (sweepAngle == 0) {
            return;
        }

        float left = cx - radius;
        float top = cy - radius;
        float right = cx + radius;
        float bottom = cy + radius;
        RectF rectF = new RectF(left, top, right, bottom);
        if (sweepAngle % 360 == 0) {
            sweepAngle /= 2;
            arcTo(rectF, startAngle, sweepAngle, false);
            arcTo(rectF, sweepAngle + startAngle, sweepAngle, false);
        } else {
            arcTo(rectF, startAngle, sweepAngle, false);
        }
    }

    @CanvasMethod
    public void arcTo(float x1, float y1, float x2, float y2, float radius) {

        PointF cp = mCurrentPoint;

        if (mCommand.size() == 0) {
            moveTo(x1, y1);
        }

        float a1 = cp.y - y1;
        float b1 = cp.x - x1;
        float a2 = y2 - y1;
        float b2 = x2 - x1;
        float mm = Math.abs(a1 * b2 - b1 * a2);

        if (mm < 1.0e-8 || radius == 0) {
            lineTo(x1, y1);
        } else {
            float dd = a1 * a1 + b1 * b1;
            float cc = a2 * a2 + b2 * b2;
            float tt = a1 * a2 + b1 * b2;
            float k1 = (float) (radius * Math.sqrt(dd) / mm);
            float k2 = (float) (radius * Math.sqrt(cc) / mm);
            float j1 = k1 * tt / dd;
            float j2 = k2 * tt / cc;
            float cx = k1 * b2 + k2 * b1;
            float cy = k1 * a2 + k2 * a1;
            float px = b1 * (k2 + j1);
            float py = a1 * (k2 + j1);
            float qx = b2 * (k1 + j2);
            float qy = a2 * (k1 + j2);
            float startAngle = (float) Math.atan2(py - cy, px - cx);
            float endAngle = (float) Math.atan2(qy - cy, qx - cx);

            lineTo(px + x1, py + y1);
            arc(cx + x1, cy + y1, radius, startAngle, endAngle, b1 * a2 > b2 * a1);
        }
    }

    @Override
    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        if (mCommand.size() == 0) {
            moveTo(x1, y1);
        }
        super.cubicTo(x1, y1, x2, y2, x3, y3);
        mCurrentPoint.x = x3;
        mCurrentPoint.y = y3;
    }

    @Override
    public void quadTo(float x1, float y1, float x2, float y2) {
        if (mCommand.size() == 0) {
            moveTo(x1, y1);
        }
        super.quadTo(x1, y1, x2, y2);
        mCurrentPoint.x = x2;
        mCurrentPoint.y = y2;
    }

    private static class PathCommand {
        public static int MOVE_TO = 1;
        public static int LINE_TO = 2;
        public static int CURVE_TO = 3;
    }
}
