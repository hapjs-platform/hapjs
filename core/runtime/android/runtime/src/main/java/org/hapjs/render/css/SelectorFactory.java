/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.render.css;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.hapjs.component.Component;
import org.hapjs.component.view.state.State;
import org.hapjs.render.DebugUtils;
import org.hapjs.render.action.RenderActionNode;
import org.hapjs.render.css.property.CSSPropertyBuilder;
import org.hapjs.render.css.value.CSSValueFactory;
import org.hapjs.render.css.value.CSSValues;

class SelectorFactory {
    private static final String TAG = "SelectorFactory";

    static SimpleSelector createIdSelector(String id) {
        return new SelectorFactory.IdSelector(id);
    }

    static SimpleSelector createClassSelector(String cls) {
        return new SelectorFactory.ClassSelector(cls);
    }

    static SimpleSelector createElementSelector(String tag) {
        return new SelectorFactory.ElementSelector(tag);
    }

    static SimpleSelector createStateSelector(SimpleSelector simpleSelector, String state) {
        return new SelectorFactory.StateSelector(simpleSelector, state);
    }

    static Selector createChildSelector(Selector parent, SimpleSelector child) {
        return new SelectorFactory.ChildSelector(parent, child);
    }

    static Selector createDescendantSelector(
            Selector ancestorSelector, SimpleSelector simpleSelector) {
        return new SelectorFactory.DescendantSelector(ancestorSelector, simpleSelector);
    }

    private static boolean handleMatch(
            CSSStyleRule cssStyleRule,
            Node lastChild,
            CSSStyleDeclaration declaration,
            Node curr,
            DescendantSelector descendantSelector) {
        Node finalCurr = curr;
        if (!curr.getPseudoListener().containsKey(descendantSelector)) {
            curr.putPseudoListener(
                    descendantSelector,
                    new Node.PseudoListener() {
                        @Override
                        public boolean stateChanged(Map<String, Boolean> newStates) {
                            if (lastChild != null) {
                                for (Iterator<Map.Entry<String, Boolean>> iterator =
                                        newStates.entrySet().iterator();
                                        iterator.hasNext(); ) {
                                    Map.Entry<String, Boolean> next = iterator.next();
                                    if (DebugUtils.DBG) {
                                        Log.d(
                                                TAG,
                                                "stateChanged: state: "
                                                        + next.getKey()
                                                        + " value: "
                                                        + next.getValue()
                                                        + " current handling selector: "
                                                        + descendantSelector);
                                    }
                                    if (next.getValue()) {
                                        break;
                                    }
                                }
                            }
                            if (descendantSelector.mAncestorSelector
                                    .match(cssStyleRule, finalCurr, lastChild)) {
                                CSSCalculator.addExtraDeclaration(finalCurr, declaration);
                                Map<String, CSSValues> cssValuesMap =
                                        CSSValueFactory.createCSSValuesMap(declaration);
                                if (lastChild != null && finalCurr.getComponent() != null) {
                                    lastChild.setPendingCssValuesMap(null);
                                    lastChild.setPendingCssValuesKey(null);
                                    Component component = lastChild.getComponent();
                                    if (DebugUtils.DBG) {
                                        Log.d(
                                                TAG,
                                                "match: lastChild.getComponent(): "
                                                        + component
                                                        + " lastChild: "
                                                        + lastChild.getTagName()
                                                        + " declaration: "
                                                        + declaration.convertStyleProps()
                                                        + " selector: "
                                                        + descendantSelector);
                                    }
                                    if (component != null) {
                                        component.applyPseoudoStyles(descendantSelector.toString(),
                                                cssValuesMap);
                                    } else {
                                        lastChild.setPendingState(Component.PSEUDO_STATE);
                                        lastChild.setPendingCssValuesKey(
                                                descendantSelector.toString());
                                        lastChild.setPendingCssValuesMap(cssValuesMap);
                                    }
                                }
                                return true;
                            } else {
                                return false;
                            }
                        }

                        @Override
                        public void makeNormalState() {
                            if (lastChild != null) {
                                lastChild.setPendingCssValuesMap(null);
                                lastChild.setPendingCssValuesKey(null);
                                Component component = lastChild.getComponent();
                                if (DebugUtils.DBG) {
                                    Log.d(
                                            TAG,
                                            "make normal state: lastChild.getComponent(): "
                                                    + component
                                                    + " lastChild: "
                                                    + lastChild.getTagName()
                                                    + " declaration: "
                                                    + declaration.convertStyleProps()
                                                    + " selector: "
                                                    + descendantSelector);
                                }
                                if (component != null) {
                                    component.restoreStyles();
                                } else {
                                    lastChild.setPendingState(State.NORMAL);
                                }
                            }
                        }
                    });
            if (DebugUtils.DBG) {
                if (curr instanceof RenderActionNode) {
                    RenderActionNode renderActionNode = (RenderActionNode) curr;
                    Log.d(
                            TAG,
                            "only node with state will setLisener node: "
                                    + renderActionNode.getTagName()
                                    + " : "
                                    + renderActionNode.getVId()
                                    + " selector: "
                                    + descendantSelector
                                    + " simple: "
                                    + descendantSelector.mSimpleSelector
                                    + " ans: "
                                    + descendantSelector.mAncestorSelector);
                }
            }

            List<CSSProperty> cssProperties = new ArrayList<>();
            if (declaration.getLength() > 0) {
                Iterator<String> cssPropertyIterator = declaration.iterator();
                while (cssPropertyIterator.hasNext()) {
                    String entryKey = cssPropertyIterator.next();
                    CSSProperty item = declaration.getProperty(entryKey);
                    String state = item.getState();
                    if (state != null) {
                        if (state.isEmpty() || !state.startsWith(Component.PSEUDO_STATE)) {
                            CSSProperty cssProperty =
                                    new CSSPropertyBuilder(item)
                                            .setValue(item.getValue())
                                            .setState(Component.PSEUDO_STATE + "+"
                                                    + descendantSelector.toString())
                                            .build();
                            cssProperties.add(cssProperty);
                            cssPropertyIterator.remove();
                            if (DebugUtils.DBG) {
                                Log.d(TAG, "matchWithState: " + cssProperty);
                            }
                        }
                    }
                }
                for (CSSProperty cssProperty : cssProperties) {
                    declaration.setProperty(cssProperty);
                }
            }
            return true;
        }
        return false;
    }

    interface SimpleSelector extends Selector {
    }

    /**
     * [#id]
     */
    static class IdSelector implements SimpleSelector {
        private final String mId;

        IdSelector(String id) {
            mId = id;
        }

        @Override
        public int getSelectorType() {
            return Selector.SAC_CONDITIONAL_SELECTOR;
        }

        @Override
        public long getScore() {
            return CSSCalculator.ID_SCORE;
        }

        @Override
        public boolean match(CSSStyleRule cssStyleRule, Node node, Node lastChild) {
            return mId.equals(node.getCSSId());
        }

        @Override
        public String toString() {
            return "#:" + mId;
        }
    }

    /**
     * [.cls]
     */
    static class ClassSelector implements SimpleSelector {
        private final String mCls;

        ClassSelector(String cls) {
            mCls = cls;
        }

        @Override
        public int getSelectorType() {
            return Selector.SAC_CONDITIONAL_SELECTOR;
        }

        @Override
        public long getScore() {
            return CSSCalculator.CLASS_SCORE;
        }

        @Override
        public boolean match(CSSStyleRule cssStyleRule, Node node, Node lastChild) {
            return arrayContains(node.getCSSClass(), mCls);
        }

        boolean arrayContains(String[] a, String s) {
            if (a == null) {
                return false;
            }
            for (String item : a) {
                if (s.equals(item)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "." + mCls;
        }
    }

    /**
     * [text]
     */
    static class ElementSelector implements SimpleSelector {
        private final String mTag;

        ElementSelector(String tag) {
            mTag = tag;
        }

        @Override
        public int getSelectorType() {
            return Selector.SAC_ELEMENT_NODE_SELECTOR;
        }

        @Override
        public long getScore() {
            return CSSCalculator.ELEMENT_SCORE;
        }

        @Override
        public boolean match(CSSStyleRule cssStyleRule, Node node, Node lastChild) {
            return mTag.equals(node.getTagName());
        }

        @Override
        public String toString() {
            return "tag:" + mTag;
        }
    }

    /**
     * [div > text]
     */
    static class ChildSelector extends DescendantSelector {

        ChildSelector(Selector parent, SimpleSelector child) {
            super(parent, child);
        }

        @Override
        public int getSelectorType() {
            return Selector.SAC_CHILD_SELECTOR;
        }

        @Override
        public boolean match(CSSStyleRule cssStyleRule, Node node, Node lastChild) {
            if (!mSimpleSelector.match(cssStyleRule, node, lastChild)) {
                return false;
            }
            RenderActionNode renderActionNode = null;
            if (node instanceof RenderActionNode) {
                renderActionNode = (RenderActionNode) node;
            }
            if (renderActionNode == null) {
                Log.e(TAG,
                        "SelectorFactory ChildSelector match error: node is not render action node");
                return false;
            }
            synchronized (renderActionNode.getDocument()) {
                if (node.getParent() == null) {
                    // set dirty and calculate it later
                    node.setDirty(true);
                    return false;
                }

                Selector selector = mAncestorSelector;
                while (selector instanceof DescendantSelector) {
                    DescendantSelector descendantSelector = (DescendantSelector) selector;
                    if (descendantSelector.mSimpleSelector instanceof StateSelector) {
                        selector = descendantSelector.mSimpleSelector;
                        break;
                    } else {
                        // 再看前面的
                        selector = descendantSelector.mAncestorSelector;
                    }
                }
                if (selector instanceof StateSelector) {
                    CSSStyleDeclaration declaration = cssStyleRule.getDeclaration();
                    Node curr = node.getParent();
                    if (handleMatch(cssStyleRule, lastChild, declaration, curr, this)) {
                        return true;
                    }
                    ;
                } else {
                    if (mAncestorSelector.match(cssStyleRule, node.getParent(), lastChild)) {
                        return true;
                    } else {
                        // set dirty and calculate it later
                        if (node.getParent().isDirty()) {
                            node.setDirty(true);
                        }
                        return false;
                    }
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return mAncestorSelector + " > " + mSimpleSelector;
        }
    }

    static class DescendantSelector implements Selector {
        Selector mAncestorSelector;
        SimpleSelector mSimpleSelector;

        DescendantSelector(Selector ancestorSelector, SimpleSelector simpleSelector) {
            mAncestorSelector = ancestorSelector;
            mSimpleSelector = simpleSelector;
        }

        @Override
        public int getSelectorType() {
            return Selector.SAC_DESCENDANT_SELECTOR;
        }

        @Override
        public long getScore() {
            return mAncestorSelector.getScore() + mSimpleSelector.getScore();
        }

        @Override
        public boolean match(CSSStyleRule cssStyleRule, Node node, Node lastChild) {
            boolean endMatch = mSimpleSelector.match(cssStyleRule, node, lastChild);
            if (!endMatch) {
                return false;
            }

            Selector selector = mAncestorSelector;
            while (selector instanceof DescendantSelector) {
                DescendantSelector descendantSelector = (DescendantSelector) selector;
                if (descendantSelector.mSimpleSelector instanceof StateSelector) {
                    selector = descendantSelector.mSimpleSelector;
                    break;
                } else {
                    // 再看前面的
                    selector = descendantSelector.mAncestorSelector;
                }
            }
            if (selector instanceof StateSelector) {
                CSSStyleDeclaration declaration = cssStyleRule.getDeclaration();
                Node curr = node.getParent();
                while (curr != null) {
                    if (mAncestorSelector.match(cssStyleRule, curr, lastChild)) {
                        if (handleMatch(cssStyleRule, lastChild, declaration, curr, this)) {
                            return true;
                        }
                    }
                    curr = curr.getParent();
                }
            } else {
                // 前面没有stateselector，走老逻辑
                Node curr = node.getParent();
                while (curr != null) {
                    if (mAncestorSelector.match(cssStyleRule, curr, lastChild)) {
                        return true;
                    }
                    curr = curr.getParent();
                }
            }
            node.setDirty(true);
            return false;
        }

        @Override
        public String toString() {
            return mAncestorSelector + " " + mSimpleSelector;
        }
    }

    static class StateSelector implements SimpleSelector {
        SimpleSelector mSimpleSelector;
        String mState;

        StateSelector(SimpleSelector simpleSelector, String state) {
            mSimpleSelector = simpleSelector;
            mState = state;
        }

        @Override
        public int getSelectorType() {
            return mSimpleSelector.getSelectorType();
        }

        @Override
        public long getScore() {
            return mSimpleSelector.getScore();
        }

        @Override
        public boolean match(CSSStyleRule cssStyleRule, Node node, Node lastChild) {
            Component component = node.getComponent();
            if (component == null || component.getHostView() == null) {
                return mSimpleSelector.match(cssStyleRule, node, lastChild);
            } else {
                Map<String, Boolean> mStateMap = component.getStateMap();
                HashSet<String> hashSet = new HashSet<>();
                for (String key : mStateMap.keySet()) {
                    Boolean value = mStateMap.get(key);
                    if (value != null && value) {
                        hashSet.add(key);
                    }
                }
                if (mSimpleSelector.match(cssStyleRule, node, lastChild)
                        && hashSet.contains(mState)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return mSimpleSelector.toString() + " state:" + mState;
        }
    }
}
