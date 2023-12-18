/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.widget.PopupWindow;
import androidx.annotation.Size;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaUnit;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.Floating;
import org.hapjs.component.FloatingHelper;
import org.hapjs.component.Placement;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.view.flexbox.PercentFlexboxLayout;
import org.hapjs.runtime.GrayModeManager;
import org.hapjs.runtime.HapEngine;
import org.hapjs.runtime.Runtime;
import org.hapjs.widgets.text.Text;

@WidgetAnnotation(
        name = Popup.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Popup extends Container<PercentFlexboxLayout> implements Floating {

    protected static final String WIDGET_NAME = "popup";

    // attribute
    private static final String PLACEMENT = "placement";
    // style
    private static final String MASK_COLOR = "maskColor";
    // event
    private static final String EVENT_VISIBILITY_CHANGE = "visibilitychange";
    private static int sScreenWidth =
            DisplayUtil.getScreenWidth(Runtime.getInstance().getContext());
    private static int sScreenHeight =
            DisplayUtil.getScreenHeight(Runtime.getInstance().getContext());
    private PopupWindow mPopup;
    private String mTargetId;
    private Placement mPlacement = Placement.BOTTOM;
    private int mMaskColor = 0;
    private boolean mVisibilityChange;
    private int mContentWidth;
    private int mContentHeight;
    private int mAnchorWidth;
    private int mAnchorHeight;
    private int[] mAnchorLocation = new int[2];
    private boolean mHasCustomBackground;
    private Drawable mTransparentDrawable;
    private int mPaddingH;
    private int mPaddingV;

    public Popup(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        mTransparentDrawable = new ColorDrawable(Color.TRANSPARENT);
    }

    @Override
    protected PercentFlexboxLayout createViewImpl() {
        PercentFlexboxLayout flexboxLayout = new PercentFlexboxLayout(mContext);
        if (GrayModeManager.getInstance().shouldApplyGrayMode()) {
            GrayModeManager.getInstance().applyGrayMode(flexboxLayout, true);
        }
        flexboxLayout.setComponent(this);
        return flexboxLayout;
    }

    @Override
    public void addChild(Component child, int index) {
        if (child instanceof Text) {
            if (GrayModeManager.getInstance().shouldApplyGrayMode()) {
                GrayModeManager.getInstance().applyGrayMode(child.getHostView(), true);
            }
            super.addChild(child, index);
        }
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.TARGET:
                String targetId = Attributes.getString(attribute);
                setTargetId(targetId);
                return true;
            case PLACEMENT:
                String placementStr = Attributes.getString(attribute);
                setPlacement(placementStr);
                return true;
            case MASK_COLOR:
                String maskColorStr = Attributes.getString(attribute, "transparent");
                setMaskColor(maskColorStr);
                return true;
            case Attributes.Style.POSITION:
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    public void setTargetId(String targetId) {
        mTargetId = targetId;
        FloatingHelper helper = getRootComponent().getFloatingHelper();
        helper.put(mTargetId, this);
    }

    public void setPlacement(String placementStr) {
        mPlacement = Placement.fromString(placementStr);
    }

    public void setMaskColor(String maskColorStr) {
        if (TextUtils.isEmpty(maskColorStr)) {
            return;
        }
        boolean hasAlpha = ColorUtil.hasAlpha(maskColorStr);
        int color = ColorUtil.getColor(maskColorStr);
        if (!hasAlpha) {
            // set alpha to 30%.
            color &= 0x4cffffff;
        }
        mMaskColor = color;
    }

    @Override
    public void setWidth(String widthStr) {
        super.setWidth(widthStr);
        if (mHost == null) {
            return;
        }
        YogaNode node = mHost.getYogaNode();
        if (node.getWidth().unit == YogaUnit.PERCENT) {
            node.setWidth(sScreenWidth * node.getWidth().value / 100);
        }
    }

    @Override
    public void setHeight(String heightStr) {
        super.setHeight(heightStr);
        if (mHost == null) {
            return;
        }
        YogaNode node = mHost.getYogaNode();
        if (node.getHeight().unit == YogaUnit.PERCENT) {
            node.setHeight(sScreenHeight * node.getHeight().value / 100);
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (EVENT_VISIBILITY_CHANGE.equals(event)) {
            mVisibilityChange = true;
            return true;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (EVENT_VISIBILITY_CHANGE.equals(event)) {
            mVisibilityChange = false;
            return true;
        }

        return super.removeEvent(event);
    }

    @Override
    public void show(View anchor) {
        if (anchor == null || mHost == null) {
            return;
        }
        if (mPopup == null) {
            mPopup = new PopupWindow(mContext, null, 0, R.style.Widget_AppCompat_PopupWindow);
            mPopup.setBackgroundDrawable(
                    mContext.getResources().getDrawable(R.drawable.popup_background));
            mPopup.setOutsideTouchable(true);
            mPopup.setFocusable(true);
            mPopup.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            mPopup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            Rect rect = new Rect();
            mPopup.getBackground().getPadding(rect);
            mPaddingH = rect.left + rect.right;
            mPaddingV = rect.top + rect.bottom;
            mPopup.setOnDismissListener(
                    new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            if (mVisibilityChange) {
                                Map<String, Object> params = new HashMap();
                                params.put("visibility", false);
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, EVENT_VISIBILITY_CHANGE, Popup.this,
                                        params, null);
                            }
                            clearDim();
                        }
                    });
        }

        if (mPopup.isShowing()) {
            return;
        }

        if (mVisibilityChange) {
            Map<String, Object> params = new HashMap();
            params.put("visibility", true);
            mCallback.onJsEventCallback(getPageId(), mRef, EVENT_VISIBILITY_CHANGE, this, params,
                    null);
        }

        mPopup.setContentView(mHost);

        mHost.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mContentWidth = mHost.getMeasuredWidth();
        mContentHeight = mHost.getMeasuredHeight();

        if (mHasCustomBackground) {
            mPopup.setBackgroundDrawable(mTransparentDrawable);
        } else {
            mContentWidth += mPaddingH;
            mContentHeight += mPaddingV;
        }

        mAnchorWidth = anchor.getWidth();
        mAnchorHeight = anchor.getHeight();
        anchor.getLocationOnScreen(mAnchorLocation);

        int[] offset = new int[2];
        getOffset(mPlacement, offset, false);

        mPopup.showAsDropDown(anchor, offset[0], offset[1]);
        dimBehind();
    }

    private void getOffset(Placement placement, @Size(2) int[] offset, boolean fallback) {
        if (fallback && mPlacement == placement) {
            Placement next = placement.next();
            if (next == null) {
                // fallback process over.
                offset[0] = 0;
                offset[1] = 0;
            } else {
                getOffset(next, offset, true);
            }
            return;
        }

        int offsetX = 0;
        int offsetY = 0;
        boolean success = false;
        switch (placement) {
            case LEFT:
                if (mAnchorLocation[0] >= mContentWidth) {
                    offsetX = -mContentWidth;
                    offsetY = -(mContentHeight + mAnchorHeight) / 2;
                    offsetY = Math.min(offsetY, -mContentHeight);
                    success = true;
                }
                break;
            case RIGHT:
                if (sScreenWidth - mAnchorLocation[0] - mAnchorWidth >= mContentWidth) {
                    offsetX = mAnchorWidth;
                    offsetY = -(mContentHeight + mAnchorHeight) / 2;
                    offsetY = Math.min(offsetY, -mContentHeight);
                    success = true;
                }
                break;
            case TOP:
                if (mAnchorLocation[1] >= mContentHeight) {
                    offsetX = (mAnchorWidth - mContentWidth) / 2;
                    ;
                    offsetY = -(mContentHeight + mAnchorHeight);
                    success = true;
                }

                break;
            case BOTTOM:
                if (sScreenHeight - mAnchorLocation[1] - mAnchorHeight >= mContentHeight) {
                    offsetX = (mAnchorWidth - mContentWidth) / 2;
                    offsetY = 0;
                    success = true;
                }
                break;
            case TOP_LEFT:
                if (mAnchorLocation[0] > mContentWidth && mAnchorLocation[1] >= mContentHeight) {
                    offsetX = -mContentWidth;
                    offsetY = -(mContentHeight + mAnchorHeight);
                    success = true;
                }
                break;
            case TOP_RIGHT:
                if (mAnchorLocation[1] >= mContentHeight
                        && sScreenWidth - mAnchorLocation[0] - mAnchorWidth >= mContentWidth) {
                    offsetX = mAnchorWidth;
                    offsetY = -(mContentHeight + mAnchorHeight);
                    success = true;
                }
                break;
            case BOTTOM_LEFT:
                if (mAnchorLocation[0] >= mContentWidth
                        && sScreenHeight - mAnchorLocation[1] - mAnchorHeight >= mContentHeight) {
                    offsetX = -mContentWidth;
                    offsetY = 0;
                    success = true;
                }
                break;
            case BOTTOM_RIGHT:
                if (sScreenWidth - mAnchorLocation[0] - mAnchorWidth >= mContentWidth
                        && sScreenHeight - mAnchorLocation[1] - mAnchorHeight >= mContentHeight) {
                    offsetX = mAnchorWidth;
                    offsetY = 0;
                    success = true;
                }
                break;
            default:
                break;
        }
        if (success) {
            offset[0] = offsetX;
            offset[1] = offsetY;
        } else {
            final Placement next;
            if (fallback) {
                next = placement.next();
            } else {
                next = Placement.BOTTOM;
            }
            if (next != null) {
                getOffset(next, offset, true);
            }
        }
    }

    @Override
    public void dismiss() {
        if (mPopup != null) {
            mPopup.dismiss();
        }
    }

    @Override
    public void setBackgroundColor(String colorStr) {
        super.setBackgroundColor(colorStr);
        mHasCustomBackground = true;
    }

    @Override
    public void setBackgroundImage(String backgroundImage) {
        super.setBackgroundImage(backgroundImage);
        mHasCustomBackground = true;
    }

    @Override
    public void setBackground(String background) {
        super.setBackground(background);
        mHasCustomBackground = true;
    }

    private void dimBehind() {
        if (mPopup == null || mMaskColor == 0 || !(mContext instanceof Activity)) {
            return;
        }
        Activity activity = (Activity) mContext;
        ViewGroup parent = (ViewGroup) activity.getWindow().getDecorView().getRootView();
        Drawable dim = new ColorDrawable(mMaskColor);
        if (GrayModeManager.getInstance().shouldApplyGrayMode()) {
            GrayModeManager.getInstance().applyGrayMode(dim);
        }
        dim.setBounds(0, 0, parent.getWidth(), parent.getHeight());
        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.add(dim);
    }

    private void clearDim() {
        if (!(mContext instanceof Activity)) {
            return;
        }
        Activity activity = (Activity) mContext;
        ViewGroup parent = (ViewGroup) activity.getWindow().getDecorView().getRootView();
        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.clear();
    }
}
