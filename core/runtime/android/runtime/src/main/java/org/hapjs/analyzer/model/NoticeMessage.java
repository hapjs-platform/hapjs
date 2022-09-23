/*
 * Copyright (c) 2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.analyzer.model;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NoticeMessage {
    public static final String LEVEL_COMMON_WARN = "warn";
    public static final String LEVEL_COMMON_ERROR = "error";
    private String mLevel;
    private String mPageName;
    private String mMessage;
    private UIAction mAction;
    private ClickCallback mClickCallback;

    private NoticeMessage(String level, String pageName, String message) {
        this.mLevel = level;
        this.mPageName = pageName;
        this.mMessage = message;
    }

    public String getLevel() {
        return mLevel;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getPageName() {
        return mPageName;
    }

    public void setAction(UIAction action) {
        mAction = action;
    }

    public UIAction getAction() {
        return mAction;
    }

    public ClickCallback getClickCallback() {
        return mClickCallback;
    }

    public void setClickCallback(ClickCallback mClickListener) {
        this.mClickCallback = mClickListener;
    }

    public static NoticeMessage warn(String pageName, String message) {
        return new NoticeMessage(LEVEL_COMMON_WARN, pageName, message);
    }

    public static NoticeMessage error(String pageName, String message) {
        return new NoticeMessage(LEVEL_COMMON_ERROR, pageName, message);
    }

    // Determine whether NoticeMessage is equal according to mLevel and mMessage
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoticeMessage message = (NoticeMessage) o;
        return TextUtils.equals(mLevel, message.mLevel) &&
                Objects.equals(mMessage, message.mMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLevel, mMessage);
    }

    public static class UIAction {
        public int mPageId;
        public List<Integer> mComponentIds;
        public List<View> mViews;
        public int mMaskColor;

        public UIAction(int pageId, List<Integer> componentId, int maskColor) {
            mPageId = pageId;
            mComponentIds = componentId;
            mMaskColor = maskColor;
        }

        private UIAction(Builder builder) {
            mPageId = builder.mPageId;
            mComponentIds = builder.mComponentIds;
            mViews = builder.mViews;
            mMaskColor = builder.mMaskColor;
        }

        public static final class Builder {
            private int mPageId;
            private List<Integer> mComponentIds;
            private List<View> mViews;
            private int mMaskColor = Color.parseColor("#400000FF");

            public Builder() {
            }

            public Builder pageId(int pageId) {
                mPageId = pageId;
                return this;
            }

            public Builder componentIds(List<Integer> componentIds) {
                mComponentIds = componentIds;
                return this;
            }

            public Builder addComponentId(int id) {
                if (mComponentIds == null) {
                    mComponentIds = new ArrayList<>();
                }
                mComponentIds.add(id);
                return this;
            }

            public Builder views(List<View> views) {
                mViews = views;
                return this;
            }

            public Builder maskColor(int maskColor) {
                mMaskColor = maskColor;
                return this;
            }

            public UIAction build() {
                return new UIAction(this);
            }
        }
    }

    public interface ClickCallback {
        void onClick(NoticeMessage message);
    }
}
