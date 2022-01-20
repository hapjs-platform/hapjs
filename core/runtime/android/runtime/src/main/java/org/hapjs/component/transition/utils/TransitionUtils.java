/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.transition.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TransitionUtils {
    @NonNull
    public static String[] parseString(@NonNull String str) {
        String[] splitStr;
        if (str.contains(",")) {
            splitStr = str.split("\\s*,\\s*");
        } else {
            splitStr = new String[] {str};
        }
        return splitStr;
    }

    @Nullable
    public static Animator mergeAnimators(
            @Nullable Animator animator1, @Nullable Animator animator2) {
        if (animator1 == null) {
            return animator2;
        } else if (animator2 == null) {
            return animator1;
        } else {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animator1, animator2);
            return animatorSet;
        }
    }
}
