/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.canvas2d;

import android.graphics.Shader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public abstract class CanvasGradient {

    protected float mDesignRatio = 1f;
    private TreeMap<Float, Integer> mColors;

    protected CanvasGradient() {
        mColors =
                new TreeMap<>(
                        new Comparator<Float>() {
                            @Override
                            public int compare(Float o1, Float o2) {
                                if (o1 == null && o2 == null) {
                                    return 0;
                                }

                                if (o1 == null) {
                                    return -1;
                                }

                                if (o2 == null) {
                                    return 1;
                                }
                                return o1.compareTo(o2);
                            }
                        });
    }

    public void setDesignRatio(float ratio) {
        if (ratio <= 0) {
            return;
        }
        mDesignRatio = ratio;
    }

    public void addColorStop(float offset, int color) {
        mColors.put(offset, color);
    }

    public int[] colors() {
        List<Integer> values = new ArrayList<>(mColors.values());
        int[] colors = new int[values.size()];

        for (int i = 0, size = colors.length; i < size; i++) {
            colors[i] = values.get(i);
        }

        return colors;
    }

    public float[] offsets() {
        List<Float> keys = new ArrayList<>(mColors.keySet());
        float[] offsets = new float[keys.size()];

        for (int i = 0, size = offsets.length; i < size; i++) {
            offsets[i] = keys.get(i);
        }
        return offsets;
    }

    public boolean isValid() {
        return mColors.size() > 0;
    }

    protected int[] reverseColors() {
        List<Integer> values = new ArrayList<>(mColors.values());
        Collections.reverse(values);

        int[] colors = new int[values.size()];
        for (int i = 0, size = colors.length; i < size; i++) {
            colors[i] = values.get(i);
        }
        return colors;
    }

    protected List<Integer> copyOfColor() {
        return new ArrayList<>(mColors.values());
    }

    protected List<Float> copyOfOffset() {
        return new ArrayList<>(mColors.keySet());
    }

    public abstract Shader createShader();

    public abstract void destroy();
}
