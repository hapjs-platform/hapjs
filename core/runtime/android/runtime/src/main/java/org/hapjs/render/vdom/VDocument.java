/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.vdom;

import android.util.Log;
import java.util.HashMap;

public class VDocument extends VGroup {

    private static final String TAG = "VDocument";

    HashMap<Integer, VElement> mIdToEles = new HashMap<>();
    private DocComponent mDocComponent;

    // The meaning of this member variable is to prevent the CP from calling the hide api or
    // createFinish before the skeleton is rendered.
    private boolean mIsCpHideSkeleton = false;
    private boolean mIsCreateFinish = false;

    public VDocument(DocComponent docComponent) {
        super(null, VElement.ID_DOC, null, docComponent);
        mDoc = this;
        mDocComponent = docComponent;
        mIdToEles.put(getVId(), this);
    }

    public VElement getElementById(int id) {
        return mIdToEles.get(id);
    }

    public void attachChildren(
            boolean open, int animType, DocComponent.PageEnterListener pageEnterListener) {
        mDocComponent.attachChildren(open, animType, pageEnterListener);
    }

    public void detachChildren(
            int animType, DocComponent.PageExitListener pageExitListener, boolean open) {
        mDocComponent.detachChildren(animType, pageExitListener, open);
    }

    void onAddElement(VElement ele) {
        mIdToEles.put(ele.getVId(), ele);

        if (ele instanceof VGroup) {
            VGroup group = (VGroup) ele;
            for (VElement e : group.getChildren()) {
                onAddElement(e);
            }
        }
    }

    void onDeleteElement(VElement ele) {
        mIdToEles.remove(ele.getVId());

        // TODO Container will destroy all children
        if (ele instanceof VGroup) {
            VGroup group = (VGroup) ele;
            for (VElement e : group.getChildren()) {
                onDeleteElement(e);
            }
        }

        ele.destroy();
    }

    public void destroy() {
        mDocComponent.destroy();
    }

    public boolean hasWebComponent() {
        return mDocComponent.hasWebComponent();
    }

    @Override
    public DocComponent getComponent() {
        return mDocComponent;
    }

    public boolean isCpHideSkeleton() {
        return mIsCpHideSkeleton;
    }

    void setCpHideSkeletonFlag(boolean flag) {
        if (flag) {
            Log.d(TAG, "LOG_SKELETON CP call hide skeleton");
        }
        mIsCpHideSkeleton = flag;
    }

    public boolean isCreateFinish() {
        return mIsCreateFinish;
    }

    void setCreateFinishFlag(boolean flag) {
        if (flag) {
            Log.d(TAG, "LOG_SKELETON ACTION_CREATE_FINISH come");
        }
        mIsCreateFinish = flag;
    }
}
