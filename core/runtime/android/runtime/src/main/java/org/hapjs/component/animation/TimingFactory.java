/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.animation;

import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

public class TimingFactory {

    private TimingFactory() {
    }

    public static Interpolator getTiming(String timing) {
        if ("linear".equals(timing)) {
            return new LinearInterpolator();
        } else if ("ease".equals(timing)) {
            return new EaseInterpolator();
        } else if ("ease-in".equals(timing)) {
            return new EaseInInterpolator();
        } else if ("ease-out".equals(timing)) {
            return new EaseOutInterpolator();
        } else if ("ease-in-out".equals(timing)) {
            return new EaseInOutInterpolator();
        } else if (timing != null && timing.startsWith("cubic-bezier")) {
            int paramStartIndex = timing.indexOf("(");
            int paramEndIndex = timing.indexOf(")");
            if (paramStartIndex == -1 || paramEndIndex == -1 || paramStartIndex > paramEndIndex) {
                return new EaseInterpolator();
            }
            String param = timing.substring(paramStartIndex + 1, paramEndIndex);
            String[] paramList = param.split(",");
            if (paramList.length == 4) {
                return new CubicBezierInterpolator(
                        Float.parseFloat(paramList[0].trim()),
                        Float.parseFloat(paramList[1].trim()),
                        Float.parseFloat(paramList[2].trim()),
                        Float.parseFloat(paramList[3].trim()));
            }
        } else if ("step-start".equals(timing)) {
            return new StepStartInterpolator();
        } else if ("step-end".equals(timing)) {
            return new StepEndInterpolator();
        } else if (timing != null && timing.startsWith("step")) {
            int paramStartIndex = timing.indexOf("(");
            int paramEndIndex = timing.indexOf(")");
            if (paramStartIndex == -1 || paramEndIndex == -1 || paramStartIndex > paramEndIndex) {
                return new EaseInterpolator();
            }
            String param = timing.substring(paramStartIndex + 1, paramEndIndex);
            String[] paramList = param.split(",");
            if (paramList.length == 1) {
                return new StepInterpolator(Integer.parseInt(paramList[0].trim()));
            } else if (paramList.length == 2) {
                return new StepInterpolator(Integer.parseInt(paramList[0].trim()),
                        paramList[1].trim());
            }
        }

        return new LinearInterpolator();
    }
}
