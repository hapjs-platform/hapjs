/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import java.util.HashMap;
import org.hapjs.render.vdom.VDocument;

public class InspectorVDocument extends InspectorVGroup {

    HashMap<Integer, InspectorVElement> mIdToEles = new HashMap<>();
    private VDocument mVDocument;

    public InspectorVDocument(VDocument vdoc) {
        super(null, InspectorVElement.ID_DOC, null);
        mDoc = this;
        mVDocument = vdoc;
    }

    public VDocument getVDocument() {
        return mVDocument;
    }

    public void setVDocument(VDocument vdoc) {
        mVDocument = vdoc;
    }

    public InspectorVElement getElementById(int id) {
        return mIdToEles.get(id);
    }

    void onAddElement(InspectorVElement ele) {
        mIdToEles.put(ele.getVId(), ele);

        if (ele instanceof InspectorVGroup) {
            InspectorVGroup group = (InspectorVGroup) ele;
            for (InspectorVElement e : group.getChildren()) {
                onAddElement(e);
            }
        }
    }

    void onDeleteElement(InspectorVElement ele) {
        mIdToEles.remove(ele.getVId());

        if (ele instanceof InspectorVGroup) {
            InspectorVGroup group = (InspectorVGroup) ele;
            for (InspectorVElement e : group.getChildren()) {
                onDeleteElement(e);
            }
        }
    }
}
