/*
 * Copyright (c) 2021-2022, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hapjs.component.Component;
import org.hapjs.component.RecyclerDataItem;
import org.hapjs.render.css.value.CSSValues;

public abstract class Node {
    private String mTagName;
    private Node mParent;
    private List<Node> mChildren = Collections.synchronizedList(new ArrayList<>());
    private CSSInlineStyleRule mInlineStyle = new CSSInlineStyleRule();
    private CSSStyleDeclaration mFinalStyle = new CSSStyleDeclaration();

    private String mCSSId = null;
    private String[] mCSSClass = null;
    private boolean mRestyling = false;
    private boolean mDirty = false;

    private Component mComponent;
    private RecyclerDataItem mRecyclerDataItem;
    private Map<String, CSSValues> mPendingCssValuesMap;
    private String mPendingCssValuesKey;
    private String mPendingState;
    private Map<Selector, PseudoListener> mPseudoListener = new ConcurrentHashMap<>();

    protected Node(String tagName) {
        mTagName = tagName;
    }

    public void handleStateChanged(Map<String, Boolean> newStates) {
        Map<String, Boolean> simpleSelectorMap = new HashMap<>();
        for (Iterator<Map.Entry<Selector, PseudoListener>> iterator =
                getPseudoListener().entrySet().iterator();
                iterator.hasNext(); ) {
            Map.Entry<Selector, Node.PseudoListener> next = iterator.next();
            SelectorFactory.DescendantSelector descendantSelector =
                    (SelectorFactory.DescendantSelector) next.getKey();
            boolean match = next.getValue().stateChanged(newStates);
            String simpleSelectorString = descendantSelector.mSimpleSelector.toString();
            if (match) {
                simpleSelectorMap.put(simpleSelectorString, true);
            } else {
                if (!simpleSelectorMap.containsKey(simpleSelectorString)) {
                    simpleSelectorMap.put(simpleSelectorString, false);
                }
                if (!simpleSelectorMap.get(simpleSelectorString)) {
                    simpleSelectorMap.put(simpleSelectorString, false);
                }
            }
        }
        for (Iterator<Map.Entry<Selector, PseudoListener>> iterator =
                getPseudoListener().entrySet().iterator();
                iterator.hasNext(); ) {
            Map.Entry<Selector, Node.PseudoListener> next = iterator.next();
            SelectorFactory.DescendantSelector descendantSelector =
                    (SelectorFactory.DescendantSelector) next.getKey();
            String simpleSelectorString = descendantSelector.mSimpleSelector.toString();
            if (!simpleSelectorMap.get(simpleSelectorString)) {
                Node.PseudoListener pseudoListener = getPseudoListener().get(next.getKey());
                pseudoListener.makeNormalState();
            }
        }
    }

    public void putPseudoListener(Selector selector, PseudoListener pseudoListener) {
        mPseudoListener.put(selector, pseudoListener);
    }

    public void clearPseudoListener() {
        mPseudoListener.clear();
    }

    public Map<Selector, PseudoListener> getPseudoListener() {
        return mPseudoListener;
    }

    public synchronized boolean isDirty() {
        return mDirty;
    }

    public synchronized void setDirty(boolean dirty) {
        mDirty = dirty;
    }

    public synchronized String getTagName() {
        return mTagName;
    }

    public synchronized void setTagName(String tagName) {
        mTagName = tagName;
    }

    public synchronized Node getParent() {
        return mParent;
    }

    public synchronized void setParent(Node parent) {
        mParent = parent;
    }

    synchronized String getCSSId() {
        return mCSSId;
    }

    protected synchronized void setCSSId(String idPlain) {
        mCSSId = idPlain.trim();
    }

    public synchronized String[] getCSSClass() {
        return mCSSClass;
    }

    protected synchronized void setCSSClass(String classPlain) {
        String classTrimed = classPlain.trim();
        mCSSClass = null;
        if (classTrimed.contains(" ")) {
            mCSSClass = classTrimed.split("\\s+");
        } else {
            mCSSClass = new String[] {classTrimed};
        }
    }

    public synchronized void updateInlineStyles(CSSStyleDeclaration style) {
        mInlineStyle.getDeclaration().setAllProperty(style);
    }

    public synchronized CSSStyleRule getInlineStyle() {
        return mInlineStyle;
    }

    public synchronized CSSStyleDeclaration getFinalStyle() {
        return mFinalStyle;
    }

    public CSSStyleDeclaration calFinalStyle(MatchedCSSRuleList matchedStyles) {
        return CSSCalculator.calFinalStyle(this, matchedStyles);
    }

    public MatchedCSSRuleList calMatchedStyles() {
        return CSSCalculator.calMatchedStyles(this);
    }

    protected abstract MatchedCSSStyleSheet getMatchedStyleSheet();

    public List<? extends Node> getChildren() {
        return mChildren;
    }

    public void appendChild(Node node) {
        synchronized (mChildren) {
            if (!mChildren.contains(node)) {
                mChildren.add(node);
            }
        }
    }

    public void removeChild(Node node) {
        synchronized (mChildren) {
            mChildren.remove(node);
        }
    }

    public synchronized void setRestyling(boolean restyling) {
        mRestyling = restyling;
    }

    public synchronized boolean shouldRestyling() {
        return mRestyling;
    }

    public Component getComponent() {
        return mComponent;
    }

    public void setComponent(Component component) {
        mComponent = component;
    }

    public RecyclerDataItem getRecyclerDataItem() {
        return mRecyclerDataItem;
    }

    public void setRecyclerDataItem(RecyclerDataItem recyclerDataItem) {
        mRecyclerDataItem = recyclerDataItem;
    }

    public Map<String, CSSValues> getPendingCssValuesMap() {
        return mPendingCssValuesMap;
    }

    public void setPendingCssValuesMap(Map<String, CSSValues> pendingCssValuesMap) {
        mPendingCssValuesMap = pendingCssValuesMap;
    }

    public String getPendingState() {
        return mPendingState;
    }

    public void setPendingState(String pendingState) {
        mPendingState = pendingState;
    }

    public String getPendingCssValuesKey() {
        return mPendingCssValuesKey;
    }

    public void setPendingCssValuesKey(String pendingCssValuesKey) {
        mPendingCssValuesKey = pendingCssValuesKey;
    }

    public interface PseudoListener {
        boolean stateChanged(Map<String, Boolean> newStates);

        void makeNormalState();
    }
}
