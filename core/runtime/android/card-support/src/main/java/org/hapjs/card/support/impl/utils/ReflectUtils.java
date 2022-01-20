/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.card.support.impl.utils;

import android.content.Context;
import java.lang.reflect.Constructor;
import org.hapjs.card.support.CardView;

public class ReflectUtils {
    private static Constructor sCardViewContructor;

    public static CardView createCardView(String cardViewName, Context context) {
        try {
            if (sCardViewContructor == null) {
                Class clazz = Class.forName(cardViewName);
                sCardViewContructor = clazz.getDeclaredConstructor(Context.class);
            }
            return (CardView) sCardViewContructor.newInstance(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
