/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.list;

import android.view.View;
import androidx.recyclerview.widget.FlexRecyclerView;
import androidx.recyclerview.widget.RecyclerView;

public interface FlexLayoutManager {
    int DEFAULT_COLUMN_COUNT = 1;

    FlexRecyclerView getFlexRecyclerView();

    void setFlexRecyclerView(FlexRecyclerView flexRecyclerView);

    RecyclerView.LayoutManager getRealLayoutManager();

    void setScrollPage(boolean scrollPage);

    int findFlexFirstVisibleItemPosition();

    int findFlexLastVisibleItemPosition();

    int findFlexFirstCompletelyVisibleItemPosition();

    int findFlexLastCompletelyVisibleItemPosition();

    int getFlexItemCount();

    int getStateItemCount();

    int getOverScrolledY();

    int getFlexOrientation();

    void setFlexOrientation(int orientation);

    void setFlexReverseLayout(boolean reverseLayout);

    boolean canFlexScrollHorizontally();

    boolean canFlexScrollVertically();

    int getFlexSpanCount();

    void setFlexSpanCount(int spanCount);

    void scrollToFlexPositionWithOffset(int position, int offset);

    View getFlexChildAt(int position);

    int getFlexChildPosition(View view);
}
