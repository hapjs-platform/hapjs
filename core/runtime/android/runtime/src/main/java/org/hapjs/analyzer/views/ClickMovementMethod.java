/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.views;

import android.text.Layout;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;

public class ClickMovementMethod implements View.OnTouchListener {
    private LongClickCallback longClickCallback;

    @Override
    public boolean onTouch(final View v, MotionEvent event) {
        if (longClickCallback == null) {
            longClickCallback = new LongClickCallback(v);
        }

        TextView widget = (TextView) v;
        widget.setMovementMethod(null);
        CharSequence text = widget.getText();
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(text);
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();
            x += widget.getScrollX();
            y += widget.getScrollY();
            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);
            ClickableSpan[] link = spannable.getSpans(off, off, ClickableSpan.class);
            if (link.length != 0) {
                if (action == MotionEvent.ACTION_DOWN) {
                    v.postDelayed(longClickCallback, ViewConfiguration.getLongPressTimeout());
                } else {
                    v.removeCallbacks(longClickCallback);
                    link[0].onClick(widget);
                }
                return true;
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            v.removeCallbacks(longClickCallback);
        }

        return false;
    }

    private static class LongClickCallback implements Runnable {
        private View view;

        LongClickCallback(View view) {
            this.view = view;
        }

        @Override
        public void run() {
            View v = view;
            boolean consumed = v.performLongClick();
            while (!consumed) {
                v = (View) v.getParent();
                if (v == null) {
                    break;
                }
                consumed = v.performLongClick();
            }
        }
    }
}