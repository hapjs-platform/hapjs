/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.RatingBar;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.utils.BitmapUtils;
import org.hapjs.common.utils.UriUtils;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.SwipeObserver;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.HapRatingBar;

@WidgetAnnotation(
        name = Rating.WIDGET_NAME,
        methods = {
                Component.METHOD_ANIMATE,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_TO_TEMP_FILE_PATH,
                Component.METHOD_FOCUS,
                Component.METHOD_TALKBACK_FOCUS,
                Component.METHOD_TALKBACK_ANNOUNCE
        })
public class Rating extends Component<HapRatingBar> implements SwipeObserver {

    protected static final String WIDGET_NAME = "rating";

    // attr
    private static final String NUM_STARS = "numstars";
    private static final String RATING = "rating";
    private static final String STEP_SIZE = "stepsize";
    private static final String INDICATOR = "indicator";

    // style
    private static final String STAR_BACKGROUND = "starBackground";
    private static final String STAR_FOREGROUND = "starForeground";
    private static final String STAR_SECONDARY = "starSecondary";

    // event
    private static final String CHANGE = "change";

    private static final int DEFAULT_NUM_STARS = 5;
    private static final float DEFAULT_RATING = 0;
    private static final float DEFAULT_STEP_SIZE = 0.5f;
    private static final boolean DEFAULT_INDICATOR = false;

    private static final String KEY_CHECK_EVENT_STATE = "check_event_state";
    private boolean isChangeEventRegistered = false;

    public Rating(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
    }

    @Override
    protected HapRatingBar createViewImpl() {
        HapRatingBar ratingBar = new HapRatingBar(mContext);
        ratingBar.setComponent(this);
        initDefautRating(ratingBar);
        initOnRatingBarChangeListener(ratingBar);

        return ratingBar;
    }

    private void initDefautRating(HapRatingBar ratingBar) {
        ratingBar.setNumStars(DEFAULT_NUM_STARS);
        ratingBar.setRating(DEFAULT_RATING);
        ratingBar.setStepSize(DEFAULT_STEP_SIZE);
        ratingBar.setIsIndicator(DEFAULT_INDICATOR);
    }

    private void initOnRatingBarChangeListener(HapRatingBar ratingBar) {
        if (ratingBar == null) {
            return;
        }
        ratingBar.setOnRatingBarChangeListener(
                new RatingBar.OnRatingBarChangeListener() {
                    @Override
                    public void onRatingChanged(RatingBar ratingBar, float rating,
                                                boolean fromUser) {
                        if (fromUser) {
                            changeAttrDomData(RATING, rating);
                        }
                        if (isChangeEventRegistered) {
                            Map<String, Object> params = new HashMap<>();
                            params.put("rating", rating);
                            params.put(Attributes.EventParams.IS_FROM_USER, fromUser);
                            Map<String, Object> attributes = new HashMap<>();
                            attributes.put("rating", rating);
                            mCallback.onJsEventCallback(
                                    getPageId(), mRef, Attributes.Event.CHANGE, Rating.this, params,
                                    attributes);
                        }
                    }
                });
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case NUM_STARS:
                int numStars = Attributes.getInt(mHapEngine, attribute, DEFAULT_NUM_STARS);
                setNumStars(numStars);
                return true;
            case RATING:
                float rating = Attributes.getFloat(mHapEngine, attribute, DEFAULT_RATING);
                setRating(rating);
                return true;
            case STEP_SIZE:
                float stepSize = Attributes.getFloat(mHapEngine, attribute, DEFAULT_STEP_SIZE);
                setStepSize(stepSize);
                return true;
            case INDICATOR:
                boolean indicator = Attributes.getBoolean(attribute, DEFAULT_INDICATOR);
                setIsIndicator(indicator);
                return true;
            case STAR_BACKGROUND:
                String starBackground = Attributes.getString(attribute);
                setStarBackground(starBackground);
                return true;
            case STAR_FOREGROUND:
                String starForeground = Attributes.getString(attribute);
                setStarForeground(starForeground);
                return true;
            case STAR_SECONDARY:
                String starSecondary = Attributes.getString(attribute);
                setStarSecondary(starSecondary);
                return true;
            default:
                break;
        }

        return super.setAttribute(key, attribute);
    }

    public void setNumStars(int numStars) {
        if (mHost == null) {
            return;
        }

        float oldRating = mHost.getRating();
        float oldStepSize = mHost.getStepSize();

        mHost.setNumStars(numStars);

        // reset rating and stepsize when num stars changed
        setRating(oldRating);
        setStepSize(oldStepSize);
    }

    public void setRating(float rating) {
        if (mHost == null || rating < 0) {
            return;
        }

        mHost.setRating(rating);
    }

    public void setStepSize(float stepSize) {
        if (mHost == null) {
            return;
        }

        mHost.setStepSize(stepSize);
    }

    public void setIsIndicator(boolean indicator) {
        if (mHost == null) {
            return;
        }

        mHost.setIsIndicator(indicator);
    }

    public void setStarBackground(String starBackground) {
        if (mHost == null || TextUtils.isEmpty(starBackground)) {
            return;
        }

        final Uri background = mCallback.getCache(starBackground);
        if (background != null) {
            BitmapUtils.fetchLocalDrawable(
                    getHostView().getContext(),
                    background,
                    new BitmapUtils.OnDrawableDecodedListener() {
                        @Override
                        public void onDrawableDecoded(Drawable drawable, Uri uri) {
                            if (drawable != null && UriUtils.equals(background, uri)) {
                                mHost.setStarBackground(drawable);
                            }
                        }
                    });
        }
    }

    public void setStarForeground(String starForeground) {
        if (mHost == null || TextUtils.isEmpty(starForeground)) {
            return;
        }

        final Uri foreground = mCallback.getCache(starForeground);
        if (foreground != null) {
            BitmapUtils.fetchLocalDrawable(
                    getHostView().getContext(),
                    foreground,
                    new BitmapUtils.OnDrawableDecodedListener() {
                        @Override
                        public void onDrawableDecoded(Drawable drawable, Uri uri) {
                            if (drawable != null && UriUtils.equals(foreground, uri)) {
                                mHost.setStarForeground(drawable);
                            }
                        }
                    });
        }
    }

    public void setStarSecondary(String starSecondary) {
        if (mHost == null || TextUtils.isEmpty(starSecondary)) {
            return;
        }

        final Uri secondary = mCallback.getCache(starSecondary);
        if (secondary != null) {
            BitmapUtils.fetchLocalDrawable(
                    getHostView().getContext(),
                    secondary,
                    new BitmapUtils.OnDrawableDecodedListener() {
                        @Override
                        public void onDrawableDecoded(Drawable drawable, Uri uri) {
                            if (drawable != null && UriUtils.equals(secondary, uri)) {
                                mHost.setStarSecondary(drawable);
                            }
                        }
                    });
        }
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        switch (event) {
            case CHANGE:
                isChangeEventRegistered = true;
                return true;
            default:
                break;
        }

        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (CHANGE.equals(event)) {
            isChangeEventRegistered = false;
            return true;
        }
        return super.removeEvent(event);
    }

    @Override
    protected void onSaveInstanceState(Map<String, Object> outState) {
        super.onSaveInstanceState(outState);
        if (mHost == null || outState == null) {
            return;
        }
        outState.put(KEY_CHECK_EVENT_STATE, isChangeEventRegistered);
    }

    @Override
    protected void onRestoreInstanceState(Map<String, Object> savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState == null) {
            return;
        }
        if (savedState.get(KEY_CHECK_EVENT_STATE) != null) {
            isChangeEventRegistered = (boolean) savedState.get(KEY_CHECK_EVENT_STATE);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mHost != null) {
            mHost.setOnRatingBarChangeListener(null);
        }
    }
}
