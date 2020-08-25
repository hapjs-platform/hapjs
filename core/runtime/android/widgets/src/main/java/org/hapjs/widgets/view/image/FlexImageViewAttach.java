/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.image;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.DraweeView;
import com.facebook.imagepipeline.core.PriorityThreadFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imageutils.BitmapUtil;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.hapjs.component.view.ScrollView;
import org.hapjs.widgets.view.image.provider.TileManager;

public class FlexImageViewAttach {

    private static final String TAG = "FlexImageViewAttach";

    private static ExecutorService sDecodeExecutor;

    private int mImageWidth;
    private int mImageHeight;
    private boolean mHostViewAttached;
    private volatile boolean mSubscribing;

    private Rect mTempRect;
    private RectF mDisplayRect;
    private Matrix mDrawMatrix;
    private Matrix mTempMatrix;

    private TileManager mTileManager;
    private ImageRequest mImageRequest;
    private WeakReference<DraweeView<GenericDraweeHierarchy>> mHostView;
    private DataSource<CloseableReference<PooledByteBuffer>> mEncodedSource;
    private DataSubscriber<CloseableReference<PooledByteBuffer>> mDataSubscriber;

    // listener
    private View.OnLayoutChangeListener mLayoutChangeListener;
    private ScrollView.ScrollViewListener mScrollViewListener;
    private RecyclerView.OnScrollListener mRecyclerViewScrollListener;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;
    private FlexImageView.OnLoadStatusListener mOnLoadStatusListener;
    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;

    public FlexImageViewAttach(final DraweeView<GenericDraweeHierarchy> hostView) {
        mHostView = new WeakReference<>(hostView);
    }

    public void releaseSource() {
        if (mImageRequest != null) {
            releaseInternal();
            removeChangeListener();
        }
        mImageHeight = 0;
        mImageWidth = 0;
        mImageRequest = null;
    }

    public void onDraw(Canvas canvas) {
        if (mTileManager == null || mImageRequest == null) {
            return;
        }
        final Matrix temp = getOrCreateTempMatrix();
        getOrCreateDrawMatrix().invert(temp);
        canvas.concat(temp);
        mTileManager.draw(canvas, null);
    }

    public void onDetach() {
        mHostViewAttached = false;
        removeOnPreDrawListenerFromObserver();
        if (mImageRequest != null) {
            removeChangeListener();
            releaseInternal();
        }
    }

    public void onAttach() {
        mHostViewAttached = true;
        addOnPreDrawListenerToObserver();
        if (mImageRequest != null) {
            addChangeListener();
        }
    }

    private void addChangeListener() {
        View hostView = mHostView.get();
        if (hostView == null) {
            return;
        }

        if (mLayoutChangeListener == null) {
            mLayoutChangeListener =
                    new View.OnLayoutChangeListener() {
                        @Override
                        public void onLayoutChange(
                                View v,
                                int left,
                                int top,
                                int right,
                                int bottom,
                                int oldLeft,
                                int oldTop,
                                int oldRight,
                                int oldBottom) {
                            if ((right - left) > BitmapUtil.MAX_BITMAP_SIZE
                                    || (bottom - top) > BitmapUtil.MAX_BITMAP_SIZE) {
                                notifyTileViewInvalidate();
                            }
                        }
                    };
        }
        hostView.addOnLayoutChangeListener(mLayoutChangeListener);

        ViewGroup scrollableView = getScrollableView();
        if (scrollableView instanceof ScrollView) {
            if (mScrollViewListener == null) {
                mScrollViewListener =
                        new ScrollView.ScrollViewListener() {
                            @Override
                            public void onScrollChanged(ScrollView scrollView, int x, int y,
                                                        int oldx, int oldy) {
                                getOrCreateDrawMatrix().postTranslate(oldx - x, oldy - y);
                                notifyTileViewInvalidate();
                            }
                        };
            }
            ((ScrollView) scrollableView).addScrollViewListener(mScrollViewListener);
        } else if (scrollableView instanceof RecyclerView) {
            if (mRecyclerViewScrollListener == null) {
                mRecyclerViewScrollListener =
                        new RecyclerView.OnScrollListener() {
                            @Override
                            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                                super.onScrolled(recyclerView, dx, dy);
                                getOrCreateDrawMatrix().postTranslate(dx, dy);
                                notifyTileViewInvalidate();
                            }
                        };
            }
            RecyclerView recyclerView = (RecyclerView) scrollableView;
            recyclerView.removeOnScrollListener(mRecyclerViewScrollListener);
            recyclerView.addOnScrollListener(mRecyclerViewScrollListener);
        }

        ViewPager viewPager = getViewPager();
        if (viewPager != null) {
            if (mOnPageChangeListener == null) {
                mOnPageChangeListener =
                        new ViewPager.OnPageChangeListener() {

                            @Override
                            public void onPageScrolled(
                                    int position, float positionOffset, int positionOffsetPixels) {
                            }

                            @Override
                            public void onPageSelected(int position) {
                            }

                            @Override
                            public void onPageScrollStateChanged(int state) {
                                if (state == ViewPager.SCROLL_STATE_SETTLING) {
                                    notifyTileViewInvalidate();
                                }
                            }
                        };
            }
            viewPager.removeOnPageChangeListener(mOnPageChangeListener);
            viewPager.addOnPageChangeListener(mOnPageChangeListener);
        }
    }

    private void removeChangeListener() {
        View hostView = mHostView.get();
        if (hostView == null) {
            return;
        }
        if (mLayoutChangeListener != null) {
            hostView.removeOnLayoutChangeListener(mLayoutChangeListener);
        }

        ViewGroup scrollableView = getScrollableView();
        if (scrollableView instanceof ScrollView) {
            if (mScrollViewListener != null) {
                ((ScrollView) scrollableView).removeScrollViewListener(mScrollViewListener);
            }
        } else if (scrollableView instanceof RecyclerView) {
            ((RecyclerView) scrollableView).removeOnScrollListener(mRecyclerViewScrollListener);
        }

        ViewPager viewPager = getViewPager();
        if (viewPager != null) {
            viewPager.removeOnPageChangeListener(mOnPageChangeListener);
        }
    }

    private void releaseInternal() {
        if (mEncodedSource != null && !mSubscribing) {
            synchronized (this) {
                mEncodedSource.close();
            }
        }
        mSubscribing = false;
        if (mTileManager != null) {
            mTileManager.clearUp();
        }
    }

    public void setOnPreDrawListener(ViewTreeObserver.OnPreDrawListener listener) {
        mOnPreDrawListener = listener;
    }

    private void addOnPreDrawListenerToObserver() {
        View hostView = mHostView.get();
        if (hostView == null || mOnPreDrawListener == null) {
            return;
        }
        hostView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
    }

    private void removeOnPreDrawListenerFromObserver() {
        View hostView = mHostView.get();
        if (hostView == null || mOnPreDrawListener == null) {
            return;
        }
        hostView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
    }

    private DraweeView<GenericDraweeHierarchy> getHostView() {
        return mHostView.get();
    }

    private Rect getLocalVisibleRect() {
        DraweeView<GenericDraweeHierarchy> hostView = getHostView();
        if (hostView != null) {
            final Rect temp = getOrCreateTempRect();
            hostView.getLocalVisibleRect(temp);
            return temp;
        }
        return null;
    }

    private boolean isHostViewVisible() {
        boolean result = false;
        final DraweeView<GenericDraweeHierarchy> hostView = getHostView();
        final Rect temp = getOrCreateTempRect();
        if (hostView != null) {
            result = hostView.getGlobalVisibleRect(temp);
        }
        result =
                result
                        && !(temp.width() >= hostView.getMeasuredWidth()
                        && temp.height() >= hostView.getMeasuredHeight());
        return result && mHostViewAttached;
    }

    private RectF getDisplayRect(Matrix matrix) {
        int measureWidth = getHostViewMeasureWidth();
        int measureHeight = getHostViewMeasureHeight();
        if (measureWidth == -1 || measureHeight == -1) {
            return null;
        }
        final RectF displayRect = getOrCreateDisplayRectF();
        displayRect.set(0.0F, 0.0F, measureWidth, measureHeight);
        matrix.mapRect(displayRect);
        return displayRect;
    }

    private int getHostViewMeasureWidth() {
        DraweeView<GenericDraweeHierarchy> draweeView = getHostView();
        if (draweeView == null) {
            return -1;
        }
        return draweeView.getMeasuredWidth();
    }

    private int getHostViewMeasureHeight() {
        DraweeView<GenericDraweeHierarchy> draweeView = getHostView();
        if (draweeView == null) {
            return -1;
        }
        return draweeView.getMeasuredHeight();
    }

    private ViewGroup getScrollableView() {
        View view = getHostView();
        if (view == null) {
            return null;
        }
        ViewParent parent = view.getParent();
        if (parent instanceof ScrollView || parent instanceof RecyclerView) {
            return (ViewGroup) parent;
        }
        for (; ; ) {
            if (parent == null) {
                return null;
            }
            parent = parent.getParent();
            if (parent instanceof ScrollView || parent instanceof RecyclerView) {
                return (ViewGroup) parent;
            }
        }
    }

    private ViewPager getViewPager() {
        View view = getHostView();
        if (view == null) {
            return null;
        }
        ViewParent parent = view.getParent();
        if (parent instanceof ViewPager) {
            return (ViewPager) parent;
        }
        for (; ; ) {
            if (parent == null) {
                return null;
            }
            parent = parent.getParent();
            if (parent instanceof ViewPager) {
                return (ViewPager) parent;
            }
        }
    }

    private Matrix getOrCreateDrawMatrix() {
        if (mDrawMatrix == null) {
            mDrawMatrix = new Matrix();
        }
        return mDrawMatrix;
    }

    private Matrix getOrCreateTempMatrix() {
        if (mTempMatrix == null) {
            mTempMatrix = new Matrix();
        }
        return mTempMatrix;
    }

    private RectF getOrCreateDisplayRectF() {
        if (mDisplayRect == null) {
            mDisplayRect = new RectF();
        }
        return mDisplayRect;
    }

    private Rect getOrCreateTempRect() {
        if (mTempRect == null) {
            mTempRect = new Rect();
        }
        return mTempRect;
    }

    private void notifyTileViewInvalidate() {
        org.hapjs.common.executors.Executors.ui()
                .execute(
                        () -> {
                            if (mTileManager == null) {
                                return;
                            }
                            Rect visibleRect = getLocalVisibleRect();
                            if (visibleRect != null) {
                                mTileManager.setViewPort(visibleRect);
                            }
                            RectF displayRect = getDisplayRect(getOrCreateDrawMatrix());
                            boolean isVisible = isHostViewVisible();
                            if (!isVisible) {
                                releaseInternal();
                            } else {
                                subscribeIfNeeded();
                                mTileManager.invalidate(displayRect);
                            }
                        });
    }

    private ExecutorService getOrCreateExecutor() {
        if (sDecodeExecutor == null) {
            sDecodeExecutor =
                    Executors.newFixedThreadPool(
                            4,
                            new PriorityThreadFactory(
                                    Process.THREAD_PRIORITY_BACKGROUND, "ImageAttachExecutor",
                                    true));
        }
        return sDecodeExecutor;
    }

    private void subscribeIfNeeded() {
        if (mImageRequest == null || mSubscribing || mTileManager.isDecoderRunning()) {
            return;
        }
        if (mEncodedSource != null) {
            synchronized (this) {
                mEncodedSource.close();
            }
        }
        mSubscribing = true;
        mEncodedSource = Fresco.getImagePipeline().fetchEncodedImage(mImageRequest, null);
        if (mDataSubscriber == null) {
            mDataSubscriber = new SimpleDataSubscriber(this);
        }
        mEncodedSource.subscribe(mDataSubscriber, getOrCreateExecutor());
    }

    public void handleAttachedImage(ImageRequest imageRequest) {
        mImageRequest = imageRequest;
        if (mTileManager == null) {
            mTileManager = new TileManager(getHostView());
        }
        addChangeListener();
        subscribeIfNeeded();
    }

    public void setOnLoadStatusListener(FlexImageView.OnLoadStatusListener listener) {
        mOnLoadStatusListener = listener;
    }

    private void onDataSubscriberNewResult(DataSource<CloseableReference<PooledByteBuffer>> dataSource) {
        EncodedImage tmpEncodedImage = null;
        CloseableReference<PooledByteBuffer> ref = null;
        synchronized (FlexImageViewAttach.this) {
            if (!dataSource.isFinished()) {
                return;
            }
            if (dataSource.isClosed() || !CloseableReference.isValid(dataSource.getResult())) {
                mSubscribing = false;
                return;
            }
            ref = dataSource.getResult();
            tmpEncodedImage = new EncodedImage(ref);
        }
        final EncodedImage encodedImage = tmpEncodedImage;
        try {
            encodedImage.parseMetaData();

            final int width = encodedImage.getWidth();
            final int height = encodedImage.getHeight();
            if (mImageWidth == 0 && mImageHeight == 0 && mOnLoadStatusListener != null) {
                mImageWidth = width;
                mImageHeight = height;
                mOnLoadStatusListener.onComplete(mImageWidth, mImageHeight);
            }

            if ((width > BitmapUtil.MAX_BITMAP_SIZE ||
                    height > BitmapUtil.MAX_BITMAP_SIZE) &&
                    (getHostViewMeasureHeight() > BitmapUtil.MAX_BITMAP_SIZE ||
                            getHostViewMeasureWidth() > BitmapUtil.MAX_BITMAP_SIZE)) {
                mTileManager.setTileDataStream(encodedImage.getInputStream());
                // must set data stream before notify.
                notifyTileViewInvalidate();
                if (mSubscribing) {
                    mTileManager.runDecoder();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "SimpleDataSubscriber onNewResult exception: ", e);
        } finally {
            synchronized (FlexImageViewAttach.this) {
                if (encodedImage != null) {
                    encodedImage.close();
                }
                if (!dataSource.isClosed()) {
                    dataSource.close();
                }
                if (ref != null) {
                    CloseableReference.closeSafely(ref);
                }
                mSubscribing = false;
            }
        }
    }

    private void onDataSubscriberFailure(DataSource dataSource) {
        synchronized (FlexImageViewAttach.this) {
            dataSource.close();
            mSubscribing = false;
        }
    }

    private static class SimpleDataSubscriber implements DataSubscriber<CloseableReference<PooledByteBuffer>> {
        private WeakReference<FlexImageViewAttach> mFlexImageViewAttachRef;

        public SimpleDataSubscriber(FlexImageViewAttach flexImageViewAttach) {
            mFlexImageViewAttachRef = new WeakReference<>(flexImageViewAttach);
        }

        @Override
        public void onNewResult(DataSource<CloseableReference<PooledByteBuffer>> dataSource) {
            FlexImageViewAttach flexImageViewAttach = mFlexImageViewAttachRef.get();
            if (flexImageViewAttach != null) {
                flexImageViewAttach.onDataSubscriberNewResult(dataSource);
            }
        }

        @Override
        public void onFailure(DataSource dataSource) {
            FlexImageViewAttach flexImageViewAttach = mFlexImageViewAttachRef.get();
            if (flexImageViewAttach != null) {
                flexImageViewAttach.onDataSubscriberFailure(dataSource);
            }
        }

        @Override
        public void onCancellation(DataSource dataSource) {
        }

        @Override
        public void onProgressUpdate(DataSource dataSource) {
        }
    }
}
