/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureMode;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@code ViewGroup} based on the Yoga layout engine.
 *
 * <p>Under the hood, all views added to this {@code ViewGroup} are laid out using flexbox rules and
 * the Yoga engine.
 */
public class YogaLayout extends ViewGroup {
    private static final String TAG = "YogaLayout";
    private final Map<View, YogaNode> mYogaNodes;
    private final YogaNode mYogaNode;

    public YogaLayout(Context context) {
        this(context, null, 0);
    }

    public YogaLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YogaLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mYogaNode = new YogaNode();
        mYogaNodes = new HashMap<>();

        mYogaNode.setData(this);
        mYogaNode.setMeasureFunction(ViewMeasureFunction.INSTANCE);

        applyLayoutDirection(mYogaNode, this);
    }

    /**
     * If the SDK version is high enough, and the {@code yoga:direction} is not set on the component,
     * the direction (LTR or RTL) is set according to the locale.
     *
     * @param node The destination node
     */
    protected static void applyLayoutDirection(YogaNode node, View view) {
        // do not support RTL by default
        //        Configuration configuration = view.getResources().getConfiguration();
        //        if (configuration.getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
        //            node.setDirection(YogaDirection.RTL);
        //        }
    }

    public YogaNode getYogaNode() {
        return mYogaNode;
    }

    public YogaNode getYogaNodeForView(View view) {
        return mYogaNodes.get(view);
    }

    private YogaNode getYogaNodeOf(View child) {
        if (mYogaNodes.containsKey(child)) {
            return mYogaNodes.get(child);
        }
        if (child instanceof ComponentHost) {
            return ((ComponentHost) child).getComponent().getYogaNode();
        }
        return null;
    }

    /**
     * Adds a child view with the specified layout parameters.
     *
     * <p>In the typical View is added, this constructs a {@code YogaNode} for this child and applies
     * all the {@code yoga:*} attributes. The Yoga node is added to the Yoga tree and the child is
     * added to this ViewGroup.
     *
     * <p>If the child is a {@link YogaLayout} itself, we do not construct a new Yoga node for that
     * child, but use its root node instead.
     *
     * <p><strong>Note:</strong> do not invoke this method from {@code
     * #draw(android.graphics.Canvas)}, {@code onDraw(android.graphics.Canvas)}, {@code
     * #dispatchDraw(android.graphics.Canvas)} or any related method.
     *
     * @param child  the child view to add
     * @param index  the position at which to add the child or -1 to add last
     * @param params the layout parameters to set on the child
     */
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        // Internal nodes (which this is now) cannot have measure functions
        mYogaNode.setMeasureFunction(null);

        super.addView(child, index, params);

        // It is possible that addView is being called as part of a transferal of children, in which
        // case we already know about the YogaNode and only need the Android View tree to be aware
        // that we now own this child.  If so, we don't need to do anything further
        if (mYogaNodes.containsKey(child)) {
            return;
        }

        YogaNode childNode;

        if (child instanceof YogaLayout) {
            childNode = ((YogaLayout) child).getYogaNode();
            childNode.setData(child); // node.setData(null) when YogaLayout#removeView
        } else {

            childNode = getYogaNodeOf(child);

            if (childNode == null) {
                childNode = new YogaNode();
            }

            childNode.setData(child);
            childNode.setMeasureFunction(ViewMeasureFunction.INSTANCE);
        }

        applyLayoutDirection(childNode, child);

        mYogaNodes.put(child, childNode);
        mYogaNode.addChildAt(childNode, mYogaNode.getChildCount());
    }

    /**
     * Adds a view to this {@code ViewGroup} with an already given {@code YogaNode}. Use this function
     * if you already have a Yoga node (and perhaps tree) associated with the view you are adding,
     * that you would like to preserve.
     *
     * @param child The view to add
     * @param node  The Yoga node belonging to the view
     */
    public void addView(View child, YogaNode node) {
        addView(child, node, -1);
    }

    public void addView(View child, YogaNode node, int index) {
        if (index > 0 && mYogaNode.getChildCount() < index) {
            throw new IllegalArgumentException(
                    "addView with illegal index:" + index + ",child count:"
                            + mYogaNode.getChildCount());
        }
        if (child == null) {
            throw new IllegalArgumentException(
                    "YogaLayout Cannot add a null child view to a ViewGroup");
        }
        final int addIndex = index < 0 ? mYogaNode.getChildCount() : index;
        // Internal nodes (which this is now) cannot have measure functions
        mYogaNode.setMeasureFunction(null);
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = generateDefaultLayoutParams();
            if (params == null) {
                throw new IllegalArgumentException(
                        "YogaLayout generateDefaultLayoutParams() cannot return null");
            }
        }
        super.addView(child, addIndex, params);

        if (node == null) {
            if (mYogaNodes.containsKey(child)) {
                node = mYogaNodes.get(child);
                if (node == null) {
                    node = new YogaNode();
                }
            } else {
                node = new YogaNode();
            }
        } else {
            int childIndex = mYogaNode.indexOf(node);
            if (childIndex > -1) {
                mYogaNode.removeChildAt(childIndex);
            }
        }
        node.setData(child);
        if (!(child instanceof YogaLayout)) {
            node.setMeasureFunction(ViewMeasureFunction.INSTANCE);
        }
        applyLayoutDirection(node, child);
        mYogaNodes.put(child, node);
        mYogaNode.addChildAt(node, addIndex);
    }

    @Override
    public void removeView(View view) {
        removeViewFromYogaTree(view, false);
        super.removeView(view);
    }

    @Override
    public void removeViewAt(int index) {
        removeViewFromYogaTree(getChildAt(index), false);
        super.removeViewAt(index);
    }

    @Override
    public void removeViewInLayout(View view) {
        removeViewFromYogaTree(view, true);
        super.removeViewInLayout(view);
    }

    @Override
    public void removeViews(int start, int count) {
        for (int i = start; i < start + count; i++) {
            removeViewFromYogaTree(getChildAt(i), false);
        }
        super.removeViews(start, count);
    }

    @Override
    public void removeViewsInLayout(int start, int count) {
        for (int i = start; i < start + count; i++) {
            removeViewFromYogaTree(getChildAt(i), true);
        }
        super.removeViewsInLayout(start, count);
    }

    @Override
    public void removeAllViews() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            removeViewFromYogaTree(getChildAt(i), false);
        }
        super.removeAllViews();
    }

    @Override
    public void removeAllViewsInLayout() {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            removeViewFromYogaTree(getChildAt(i), true);
        }
        super.removeAllViewsInLayout();
    }

    /**
     * Marks a particular view as "dirty" and to be relaid out. If the view is not a child of this
     * {@link YogaLayout}, the entire tree is traversed to find it.
     *
     * @param view the view to mark as dirty
     */
    public void invalidate(View view) {
        if (mYogaNodes.containsKey(view)) {
            YogaNode node = mYogaNodes.get(view);
            if (node != null) {
                node.dirty();
            }
            return;
        }

        final int childCount = mYogaNode.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final YogaNode yogaNode = mYogaNode.getChildAt(i);
            if (yogaNode.getData() instanceof YogaLayout) {
                ((YogaLayout) yogaNode.getData()).invalidate(view);
            }
        }
        invalidate();
    }

    private void removeViewFromYogaTree(View view, boolean inLayout) {
        final YogaNode node = mYogaNodes.get(view);
        if (node == null) {
            return;
        }

        final YogaNode parent = node.getParent();
        if (parent != null) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                if (parent.getChildAt(i).equals(node)) {
                    parent.removeChildAt(i);
                    break;
                }
            }
        }

        node.setData(null);
        mYogaNodes.remove(view);

        if (inLayout) {
            mYogaNode.calculateLayout(YogaConstants.UNDEFINED, YogaConstants.UNDEFINED);
        }
    }

    protected void applyLayoutRecursive(YogaNode node, float offsetX, float offsetY) {
        View view = (View) node.getData();

        if (view != null && view != this) {
            if (view.getVisibility() == GONE) {
                return;
            }
            view.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            Math.round(node.getLayoutWidth()), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(
                            Math.round(node.getLayoutHeight()), View.MeasureSpec.EXACTLY));
            view.layout(
                    Math.round(offsetX + node.getLayoutX()),
                    Math.round(offsetY + node.getLayoutY()),
                    Math.round(offsetX + node.getLayoutX() + node.getLayoutWidth()),
                    Math.round(offsetY + node.getLayoutY() + node.getLayoutHeight()));
        }

        final int childrenCount = node.getChildCount();
        for (int i = 0; i < childrenCount; i++) {
            if (this.equals(view)) {
                applyLayoutRecursive(node.getChildAt(i), offsetX, offsetY);
            } else if (view instanceof YogaLayout) {
                continue;
            } else {
                applyLayoutRecursive(
                        node.getChildAt(i), offsetX + node.getLayoutX(),
                        offsetY + node.getLayoutY());
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Either we are a root of a tree, or this function is called by our parent's onLayout, in which
        // case our r-l and b-t are the size of our node.
        if (!(getParent() instanceof YogaLayout)) {
            createLayout(
                    MeasureSpec.makeMeasureSpec(r - l, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(b - t, MeasureSpec.EXACTLY));
        }

        applyLayoutRecursive(mYogaNode, 0, 0);
    }

    /**
     * This function is mostly unneeded, because Yoga is doing the measuring. Hence we only need to
     * return accurate results if we are the root.
     *
     * @param widthMeasureSpec  the suggested specification for the width
     * @param heightMeasureSpec the suggested specification for the height
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!(getParent() instanceof YogaLayout)) {
            createLayout(widthMeasureSpec, heightMeasureSpec);
        }

        setMeasuredDimension(
                Math.round(mYogaNode.getLayoutWidth()), Math.round(mYogaNode.getLayoutHeight()));
    }

    protected void createLayout(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (heightMode == MeasureSpec.EXACTLY) {
            mYogaNode.setHeight(heightSize);
        }
        if (widthMode == MeasureSpec.EXACTLY) {
            mYogaNode.setWidth(widthSize);
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            mYogaNode.setMaxHeight(heightSize);
        }
        if (widthMode == MeasureSpec.AT_MOST) {
            mYogaNode.setMaxWidth(widthSize);
        }
        mYogaNode.calculateLayout(YogaConstants.UNDEFINED, YogaConstants.UNDEFINED);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new YogaLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new YogaLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new YogaLayout.LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    /**
     * {@code YogaLayout.LayoutParams} are used by views to tell {@link YogaLayout} how they want to
     * be laid out. More precisely, the specify the yoga parameters of the view.
     *
     * <p>This is actually mostly a wrapper around a {@code SparseArray} that holds a mapping between
     * styleable id's ({@code R.styleable.yoga_yg_*}) and the float of their values. In cases where
     * the value is an enum or an integer, they should first be cast to int (with rounding) before
     * using.
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {

        /**
         * A mapping from attribute keys ({@code R.styleable.yoga_yg_*}) to the float of their values.
         * For attributes like position_percent_left (float), this is the native type. For attributes
         * like align_self (enums), the integer enum value is cast (rounding is used on the other side
         * to prevent precision errors). Dimension attributes are stored as float pixels.
         */
        SparseArray<Float> numericAttributes;

        /**
         * A mapping from attribute keys ({@code R.styleable.yoga_yg_*}) with string values to those
         * strings. This is used for values such as "auto".
         */
        SparseArray<String> stringAttributes;

        /**
         * Constructs a set of layout params from a source set. In the case that the source set is
         * actually a {@link YogaLayout.LayoutParams}, we can copy all the yoga attributes. Otherwise we
         * start with a blank slate.
         *
         * @param source The layout params to copy from
         */
        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        /**
         * Constructs a set of layout params, given width and height specs. In this case, we can set the
         * {@code yoga:width} and {@code yoga:height} if we are given them explicitly. If other options
         * (such as {@code match_parent} or {@code wrap_content} are given, then the parent LayoutParams
         * will store them, and we deal with them during layout. (see {@link YogaLayout#createLayout})
         *
         * @param width  the requested width, either a pixel size, {@code WRAP_CONTENT} or {@code
         *               MATCH_PARENT}.
         * @param height the requested height, either a pixel size, {@code WRAP_CONTENT} or {@code
         *               MATCH_PARENT}.
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * Constructs a set of layout params, given attributes. Grabs all the {@code yoga:*} defined in
         * {@code ALL_YOGA_ATTRIBUTES} and collects the ones that are set in {@code attrs}.
         *
         * @param context the application environment
         * @param attrs   the set of attributes from which to extract the yoga specific attributes
         */
        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }

    /**
     * Wrapper around measure function for yoga leaves.
     */
    public static class ViewMeasureFunction implements YogaMeasureFunction {

        public static final ViewMeasureFunction INSTANCE = new ViewMeasureFunction();

        /**
         * A function to measure leaves of the Yoga tree. Yoga needs some way to know how large elements
         * want to be. This function passes that question directly through to the relevant {@code
         * View}'s measure function.
         *
         * @param node       The yoga node to measure
         * @param width      The suggested width from the parent
         * @param widthMode  The type of suggestion for the width
         * @param height     The suggested height from the parent
         * @param heightMode The type of suggestion for the height
         * @return A measurement output ({@code YogaMeasureOutput}) for the node
         */
        @Override
        public long measure(
                YogaNode node,
                float width,
                YogaMeasureMode widthMode,
                float height,
                YogaMeasureMode heightMode) {
            final View view = (View) node.getData();
            if (view == null || view instanceof YogaLayout) {
                return YogaMeasureOutput.make(0, 0);
            }

            final int widthMeasureSpec =
                    MeasureSpec.makeMeasureSpec(
                            Math.round(width), viewMeasureSpecFromYogaMeasureMode(widthMode));
            final int heightMeasureSpec =
                    MeasureSpec.makeMeasureSpec(
                            Math.round(height), viewMeasureSpecFromYogaMeasureMode(heightMode));

            view.measure(widthMeasureSpec, heightMeasureSpec);

            return YogaMeasureOutput.make(view.getMeasuredWidth(), view.getMeasuredHeight());
        }

        private int viewMeasureSpecFromYogaMeasureMode(YogaMeasureMode mode) {
            if (mode == YogaMeasureMode.AT_MOST) {
                return MeasureSpec.AT_MOST;
            } else if (mode == YogaMeasureMode.EXACTLY) {
                return MeasureSpec.EXACTLY;
            } else {
                return MeasureSpec.UNSPECIFIED;
            }
        }
    }
}
