/*
 * Copyright (c) 2023, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.widgets.ad;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.hapjs.ad.AdProxy;
import org.hapjs.ad.NativeAdEntity;
import org.hapjs.component.Component;
import org.hapjs.component.Container;

import java.util.ArrayList;
import java.util.List;

public class AdProxyImpl extends AdProxy {
    private Ad mAd;

    public AdProxyImpl(Ad ad) {
        mAd = ad;
    }

    @Override
    public View initAdView(String type, ViewGroup adContainer) {
        Log.d(TAG, "initAdView:adContainer=" + adContainer + ",mType=" + type);
        return null;
    }

    @Override
    public void loadAd(String unitId, String type) {
        Log.d(TAG, "loadAd:mUnitId=" + unitId + ",mType=" + type);
    }

    @Override
    public void bindClickView(String unitId, String type, NativeAdEntity ad) {
        Log.d(TAG, "bindClickView" + unitId + ",mType=" + type);
        List<Component<View>> adClickAreas = new ArrayList<>();
        generateClickAreas(mAd, adClickAreas);
        List<View> views = new ArrayList<>(adClickAreas.size());
        for (int i = 0; i < adClickAreas.size(); i++) {
            Component component = adClickAreas.get(i);
            views.add(component.getHostView());
        }
        //如果未设置点击区域，默认全部区域
        if (views.isEmpty()) {
            Log.w(TAG, "adClickAreas is empty");
            views.add(mAd.getHostView());
        }
    }

    @Override
    public void destroy() {
        Log.d(TAG, "releaseAd");
    }

    private void generateClickAreas(Container container, List<Component<View>> AdClickAreas) {
        if (container == null) {
            return;
        }
        if (container.getChildren() == null || container.getChildren().isEmpty()) {
            return;
        }

        for (int i = 0; i < container.getChildren().size(); i++) {
            Component component = (Component) container.getChildren().get(i);
            if (component == null) {
                continue;
            }
            if (component instanceof AdClickArea) {
                AdClickAreas.add(component);
                continue;
            }
            if (component instanceof Container) {
                generateClickAreas((Container) component, AdClickAreas);
            }
        }
    }
}
