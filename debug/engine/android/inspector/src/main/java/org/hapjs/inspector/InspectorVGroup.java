/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import java.util.ArrayList;
import java.util.List;
import org.hapjs.render.VDomChangeAction;

public class InspectorVGroup extends InspectorVElement {
    private List<InspectorVElement> mChildren = new ArrayList<>();

    public InspectorVGroup(InspectorVDocument doc, VDomChangeAction action) {
        super(doc, action);
    }

    public InspectorVGroup(InspectorVDocument doc, int id, String tagName) {
        super(doc, id, tagName);
    }

    public List<InspectorVElement> getChildren() {
        return mChildren;
    }

    private void onChildEleAdded(InspectorVElement ele, int index) {
    }

    private void onChildEleRemoved(InspectorVElement ele, int index) {
    }

    public void addChild(InspectorVElement ele) {
        addChild(ele, mChildren.size());
    }


    public void addChild(InspectorVElement ele, int index) {
        if (index < 0 || index >= mChildren.size()) {
            mChildren.add(ele);
        } else {
            mChildren.add(index, ele);
        }

        ele.mParent = this;
        onChildEleAdded(ele, index);
        mDoc.onAddElement(ele);
    }

    public void removeChild(InspectorVElement ele) {
        int index = mChildren.indexOf(ele);
        mChildren.remove(ele);
        ele.mParent = null;

        onChildEleRemoved(ele, index);
        mDoc.onDeleteElement(ele);
    }
}
