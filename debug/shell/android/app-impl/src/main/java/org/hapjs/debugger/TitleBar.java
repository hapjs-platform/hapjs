/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.hapjs.debugger.app.impl.R;

public class TitleBar extends RelativeLayout implements View.OnClickListener {
    public static final int STYLE_TITLE_BACK = 0;

    private Context mContext;
    private Resources mRes;

    private ImageView mBackIcon;
    private TextView mTitle;
    private int mStyle;

    public TitleBar(Context context) {
        this(context, null);
    }

    public TitleBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mRes = getResources();
        mStyle = STYLE_TITLE_BACK;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    private void initViews() {
        mBackIcon = findViewById(R.id.back_icon);
        mTitle = findViewById(R.id.title);
        mBackIcon.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (!(mContext instanceof Activity)) {
            return;
        }
        if (v.getId() == R.id.back_icon) {
            ((Activity) mContext).onBackPressed();
        }
    }

    public void setStyleOnly(int style) {
        setStyleAndTitle(style, "");
    }

    public void setStyleAndTitle(int style, int resId) {
        setStyleAndTitle(style, mRes.getString(resId));
    }

    public void setStyleAndTitle(int style, String title) {
        mStyle = style;

        switch (style) {
            case STYLE_TITLE_BACK:
                mBackIcon.setVisibility(VISIBLE);
                mTitle.setText(title);
                mTitle.setVisibility(VISIBLE);
                break;
            default:
                break;
        }
    }

    public void setBackListener(OnClickListener backListener) {
        if (mBackIcon != null) {
            mBackIcon.setOnClickListener(backListener);
        }
    }
}
