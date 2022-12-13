/*
 * Copyright (c) 2023-present,  the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hapjs.component.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.GenericDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;

import org.hapjs.common.utils.FloatUtil;

import java.util.Arrays;

public class RoundCornerImageView extends GenericDraweeView {

    private float[] mComputedCornerRadii = new float[4];
    private float[] mBorderCornerRadii;
    private ScalingUtils.ScaleType mScaleType = ScalingUtils.ScaleType.CENTER_CROP;
    private Uri mSource;
    private boolean mProgressiveRenderingEnabled;
    private final AbstractDraweeControllerBuilder mDraweeControllerBuilder;
    private final RoundedCornerPostprocessor mRoundedCornerPostprocessor;
    private float mBorderRadius = FloatUtil.UNDEFINED;
    private static final Matrix sMatrix = new Matrix();
    private static final Matrix sInverse = new Matrix();
    private static final String TAG = "RoundCornerImageView";

    private class RoundedCornerPostprocessor extends BasePostprocessor {

        void getRadii(Bitmap source, float[] computedCornerRadii, float[] mappedRadii) {
            mScaleType.getTransform(
                    sMatrix,
                    new Rect(0, 0, source.getWidth(), source.getHeight()),
                    source.getWidth(),
                    source.getHeight(),
                    0.0f,
                    0.0f);
            sMatrix.invert(sInverse);

            mappedRadii[0] = sInverse.mapRadius(computedCornerRadii[0]);
            mappedRadii[1] = mappedRadii[0];

            mappedRadii[2] = sInverse.mapRadius(computedCornerRadii[1]);
            mappedRadii[3] = mappedRadii[2];

            mappedRadii[4] = sInverse.mapRadius(computedCornerRadii[2]);
            mappedRadii[5] = mappedRadii[4];

            mappedRadii[6] = sInverse.mapRadius(computedCornerRadii[3]);
            mappedRadii[7] = mappedRadii[6];
        }

        @Override
        public void process(Bitmap output, Bitmap source) {
            cornerRadii(mComputedCornerRadii);

            output.setHasAlpha(true);
            if (FloatUtil.floatsEqual(mComputedCornerRadii[0], 0f) &&
                    FloatUtil.floatsEqual(mComputedCornerRadii[1], 0f) &&
                    FloatUtil.floatsEqual(mComputedCornerRadii[2], 0f) &&
                    FloatUtil.floatsEqual(mComputedCornerRadii[3], 0f)) {
                super.process(output, source);
                return;
            }
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            Canvas canvas = new Canvas(output);

            float[] radii = new float[8];

            getRadii(source, mComputedCornerRadii, radii);

            Path pathForBorderRadius = new Path();

            pathForBorderRadius.addRoundRect(
                    new RectF(0, 0, source.getWidth(), source.getHeight()),
                    radii,
                    Path.Direction.CW);

            canvas.drawPath(pathForBorderRadius, paint);
        }
    }

    private static GenericDraweeHierarchy buildHierarchy(Context context) {
        return new GenericDraweeHierarchyBuilder(context.getResources())
                .build();
    }

    public RoundCornerImageView(Context context) {
        super(context, buildHierarchy(context));
        mRoundedCornerPostprocessor = new RoundedCornerPostprocessor();
        mDraweeControllerBuilder = Fresco.newDraweeControllerBuilder();
    }

    public RoundCornerImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoundedCornerPostprocessor = new RoundedCornerPostprocessor();
        mDraweeControllerBuilder = Fresco.newDraweeControllerBuilder();
    }

    /**
     * same border
     *
     * @param borderRadius
     */
    public void setBorderRadius(float borderRadius) {
        if (!FloatUtil.floatsEqual(mBorderRadius, borderRadius)) {
            mBorderRadius = borderRadius;
            maybeUpdateView();
        }
    }

    public void setBorderRadius(int position, float borderRadius) {
        if (mBorderCornerRadii == null) {
            mBorderCornerRadii = new float[4];
            Arrays.fill(mBorderCornerRadii, FloatUtil.UNDEFINED);
        }

        if (!FloatUtil.floatsEqual(mBorderCornerRadii[position], borderRadius)) {
            mBorderCornerRadii[position] = borderRadius;
            maybeUpdateView();
        }
    }

    public void setSource(Uri uri) {
        mSource = uri;
        maybeUpdateView();
    }

    private void cornerRadii(float[] computedCorners) {
        float defaultBorderRadius = !FloatUtil.isUndefined(mBorderRadius) ? mBorderRadius : 0;
        computedCorners[0] = mBorderCornerRadii != null && !FloatUtil.isUndefined(mBorderCornerRadii[0]) ? mBorderCornerRadii[0] : defaultBorderRadius;
        computedCorners[1] = mBorderCornerRadii != null && !FloatUtil.isUndefined(mBorderCornerRadii[1]) ? mBorderCornerRadii[1] : defaultBorderRadius;
        computedCorners[2] = mBorderCornerRadii != null && !FloatUtil.isUndefined(mBorderCornerRadii[2]) ? mBorderCornerRadii[2] : defaultBorderRadius;
        computedCorners[3] = mBorderCornerRadii != null && !FloatUtil.isUndefined(mBorderCornerRadii[3]) ? mBorderCornerRadii[3] : defaultBorderRadius;
    }

    public void maybeUpdateView() {
        if (mSource == null) {
            return;
        }
        Postprocessor postprocessor = mRoundedCornerPostprocessor;
        int width = -1;
        int height = -1;
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (null != layoutParams) {
            width = layoutParams.width;
            height = layoutParams.height;
        }
        boolean doResize = (width > 0 && height > 0)
                && mScaleType != ScalingUtils.ScaleType.CENTER;
        ResizeOptions resizeOptions = doResize ? new ResizeOptions(width, height) : null;
        // resize  postprocessor
        ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(mSource)
                .setPostprocessor(postprocessor)
                .setResizeOptions(resizeOptions)
                .setAutoRotateEnabled(true)
                .setProgressiveRenderingEnabled(mProgressiveRenderingEnabled)
                .build();
        // This builder is reused
        mDraweeControllerBuilder.reset();
        mDraweeControllerBuilder
                .setAutoPlayAnimations(true)
                .setOldController(getController())
                .setImageRequest(imageRequest);
        setController(mDraweeControllerBuilder.build());
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
