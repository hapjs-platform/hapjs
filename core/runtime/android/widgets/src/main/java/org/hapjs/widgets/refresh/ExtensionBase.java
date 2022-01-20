/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.refresh;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.refresh.RefreshExtension;

public abstract class ExtensionBase extends Container<RefreshExtensionView>
        implements RefreshExtensionView.OnMoveListener {

    protected static final String EVENT_MOVE = "move";

    private static final String ATTR_DRAG_RATE = "dragrate";
    private static final String ATTR_TRIGGER_RATIO = "triggerratio";
    private static final String ATTR_TRIGGER_SIZE = "triggersize";
    private static final String ATTR_MAX_DRAG_RATIO = "maxdragratio";
    private static final String ATTR_MAX_DRAG_SIZE = "maxdragsize";
    private static final String ATTR_REFRESH_DISPLAY_RATIO = "refreshdisplayratio";
    private static final String ATTR_REFRESH_DISPLAY_SIZE = "refreshdisplaysize";
    private static final String ATTR_SPINNER_STYLE = "spinnerstyle";
    private static final String ATTR_TRANSLATION_WITH_CONTENT = "translationwithcontent";
    private static final String ATTR_AUTO_REFRESH = "autorefresh";

    private static final String ATTR_STYLE_FRONT = "front";
    private static final String ATTR_STYLE_BEHIND = "behind";
    private static final String ATTR_STYLE_TRANSLATION = "translation";

    public ExtensionBase(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected RefreshExtensionView createViewImpl() {
        RefreshExtensionView headerLayout = new RefreshExtensionView(mContext);
        headerLayout.setComponent(this);
        mNode = headerLayout.getYogaNode();
        return headerLayout;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case ATTR_DRAG_RATE:
                float dragRate =
                        Attributes.getFloat(mHapEngine, attribute,
                                RefreshExtension.DEFAULT_DRAG_RATE);
                setDragRate(dragRate);
                return true;
            case ATTR_TRIGGER_RATIO:
                float triggerRatio =
                        Attributes.getFloat(
                                mHapEngine, attribute,
                                RefreshExtension.DEFAULT_TRIGGER_REFRESH_RATIO);
                setTriggerRatio(triggerRatio);
                return true;
            case ATTR_TRIGGER_SIZE:
                int triggerSize = Attributes.getInt(mHapEngine, attribute, 0);
                setTriggerSize(triggerSize);
                return true;
            case ATTR_MAX_DRAG_RATIO:
                float maxDragRatio =
                        Attributes.getFloat(mHapEngine, attribute,
                                RefreshExtension.DEFAULT_MAX_DRAG_RATIO);
                setMaxDragRatio(maxDragRatio);
                return true;
            case ATTR_MAX_DRAG_SIZE:
                int maxDragSize = Attributes.getInt(mHapEngine, attribute, 0);
                setMaxDragSize(maxDragSize);
                return true;
            case ATTR_REFRESH_DISPLAY_RATIO:
                float displayRatio =
                        Attributes.getFloat(mHapEngine, attribute,
                                RefreshExtension.DEFAULT_DISPLAY_RATIO);
                setRefreshDisplayRatio(displayRatio);
                return true;
            case ATTR_REFRESH_DISPLAY_SIZE:
                int displaySize = Attributes.getInt(mHapEngine, attribute, 0);
                setRefreshDisplaySize(displaySize);
                return true;
            case ATTR_SPINNER_STYLE:
                String style = Attributes.getString(attribute);
                setStyle(style);
                return true;
            case ATTR_TRANSLATION_WITH_CONTENT:
                boolean translationWithContent = Attributes.getBoolean(attribute, true);
                setTranslationWithContent(translationWithContent);
                return true;
            case ATTR_AUTO_REFRESH:
                boolean autoRefresh = Attributes.getBoolean(attribute, false);
                setAutoRefresh(autoRefresh);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    private void setDragRate(float rate) {
        if (mHost != null) {
            mHost.setDragRate(rate);
        }
    }

    public void setTriggerRatio(float ratio) {
        if (mHost != null) {
            mHost.setTriggerRatio(ratio);
        }
    }

    public void setTriggerSize(int size) {
        if (mHost != null) {
            mHost.setTriggerSize(size);
        }
    }

    public void setMaxDragRatio(float ratio) {
        if (mHost != null) {
            mHost.setMaxDragRatio(ratio);
        }
    }

    public void setMaxDragSize(int size) {
        if (mHost != null) {
            mHost.setMaxDragSize(size);
        }
    }

    public void setRefreshDisplayRatio(float ratio) {
        if (mHost != null) {
            mHost.setRefreshDisplayRatio(ratio);
        }
    }

    public void setRefreshDisplaySize(int size) {
        if (mHost != null) {
            mHost.setRefreshDisplaySize(size);
        }
    }

    public void setStyle(String style) {
        if (mHost != null) {
            switch (style) {
                case ATTR_STYLE_FRONT:
                    mHost.setStyle(RefreshExtension.STYLE_FIXED_FRONT);
                    break;
                case ATTR_STYLE_BEHIND:
                    mHost.setStyle(RefreshExtension.STYLE_FIXED_BEHIND);
                    break;
                case ATTR_STYLE_TRANSLATION:
                    mHost.setStyle(RefreshExtension.STYLE_TRANSLATION);
                    break;
                default:
                    break;
            }
        }
    }

    public void setTranslationWithContent(boolean translationWithContent) {
        if (mHost != null) {
            mHost.setTranslationWithContent(translationWithContent);
        }
    }

    public void setAutoRefresh(boolean autoRefresh) {
        if (mHost != null) {
            mHost.setAutoRefresh(autoRefresh);
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (EVENT_MOVE.equals(event) && mHost != null) {
            mHost.setMoveListener(this);
            return true;
        }
        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (EVENT_MOVE.equals(event) && mHost != null) {
            mHost.setMoveListener(null);
            return true;
        }
        return super.removeEvent(event);
    }

    @Override
    public void onMove(float moveDistance, float percent, boolean isDrag, boolean isRefreshing) {
        Map<String, Object> params = new HashMap<>();
        params.put(
                "scrollY",
                DisplayUtil.getDesignPxByWidth(moveDistance, mHapEngine.getDesignWidth()));
        params.put("percent", percent);
        params.put("isDrag", isDrag);
        params.put("refreshing", isRefreshing);
        mCallback.onJsEventCallback(getPageId(), getRef(), EVENT_MOVE, this, params, null);
    }
}
