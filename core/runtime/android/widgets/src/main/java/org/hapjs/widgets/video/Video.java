/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;
import java.util.HashMap;
import java.util.Map;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.net.NetworkReportManager;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.component.Component;
import org.hapjs.component.Container;
import org.hapjs.component.SwipeObserver;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.component.constants.Attributes;
import org.hapjs.component.constants.Corner;
import org.hapjs.component.view.drawable.CSSBackgroundDrawable;
import org.hapjs.model.videodata.VideoCacheData;
import org.hapjs.model.videodata.VideoCacheManager;
import org.hapjs.render.Page;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.video.FlexVideoView;

@WidgetAnnotation(
        name = Video.WIDGET_NAME,
        methods = {
                Video.METHOD_START,
                Video.METHOD_PAUSE,
                Video.METHOD_SET_CURRENT_TIME,
                Video.METHOD_EXIT_FULLSCREEN,
                Component.METHOD_REQUEST_FULLSCREEN,
                Component.METHOD_GET_BOUNDING_CLIENT_RECT,
                Component.METHOD_FOCUS
        })
public class Video extends Component<FlexVideoView> implements SwipeObserver {

    // method
    protected static final String WIDGET_NAME = "video";
    protected static final String METHOD_START = "start";
    protected static final String METHOD_PAUSE = "pause";
    protected static final String METHOD_SET_CURRENT_TIME = "setCurrentTime";
    protected static final String METHOD_EXIT_FULLSCREEN = "exitFullscreen";
    private static final String TAG = "Video";
    // event
    private static final String ERROR = "error";
    private static final String START = "start";
    private static final String PREPARED = "prepared";
    private static final String PLAYING = "playing";
    private static final String PAUSE = "pause";
    private static final String FINISH = "finish";
    private static final String TIMEUPDATE = "timeupdate";
    private static final String SEEKING = "seeking";
    private static final String SEEKED = "seeked";

    // style
    private static final String POSTER = "poster";
    private static final String CONTROLS = "controls";
    private static final String MUTED = "muted";
    private static final String OBJECT_FIT = "objectFit";
    private static final String TITLE_BAR = "titlebar";
    private static final String TITLE = "title";
    private static final String PLAY_COUNT = "playcount";

    private static final String CURRENT_TIME = "currenttime";

    private String mUri;
    private String mParseUriStr;
    private boolean mAutoPlay;
    private boolean mControlsVisible = true;
    private boolean mOnPreparedRegistered;
    private boolean mPreInPlayingState;
    private boolean mPaused;
    private long mLastPosition = -1;
    public boolean mIsDestroy = false;

    public Video(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> savedState) {
        super(hapEngine, context, parent, ref, callback, savedState);
        callback.addActivityStateListener(this);
    }

    @Override
    protected FlexVideoView createViewImpl() {
        boolean visible = Attributes.getBoolean(mAttrsDomData.get(CONTROLS), true);
        final FlexVideoView videoView = new FlexVideoView(mContext, visible);
        videoView.setComponent(this);
        videoView.setIsLazyCreate(mLazyCreate);
        videoView.setOnPreparedListener(new FlexVideoView.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                if (mHost == null || !mHost.isAttachedToWindow()) {
                    Log.w(TAG, "createViewImpl onPrepared mHost null or !mHost.isAttachedToWindow.");
                    return;
                }
                VideoCacheManager.getInstance().putPageObtainPlayer(getPageId(), true);
                if (mOnPreparedRegistered) {
                    Map<String, Object> params = new HashMap();
                    params.put("duration", mp.getDuration() / 1000f);
                    mCallback.onJsEventCallback(getPageId(), mRef, PREPARED, Video.this, params,
                            null);
                }
                setVideoSize(mp.getVideoWidth(), mp.getVideoHeight());
                long lastPosition = getLastPosition();
                if (lastPosition < 0) {
                    VideoCacheData videoCacheData = VideoCacheManager.getInstance().getVideoData(getPageId(), mParseUriStr);
                    if (null != videoCacheData) {
                        lastPosition = videoCacheData.lastPosition;
                    }
                }
                if (lastPosition > 0) {
                    mp.seek(lastPosition);
                    setLastPosition(-1);
                    videoView.start();
                } else if (mAutoPlay) {
                    videoView.start();
                } else {
                    Log.w(TAG, "createViewImpl onPrepared else  lastPosition : " + lastPosition);
                }
                Log.w(TAG, "createViewImpl onPrepared lastPosition  : " + lastPosition
                        + " mAutoPlay : " + mAutoPlay);
            }
        });

        getOrCreateBackgroundComposer().setBackgroundColor(0xee000000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            videoView.setOutlineProvider(
                    new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            if (view != null
                                    && view.getBackground() instanceof CSSBackgroundDrawable) {
                                float borderRadius =
                                        ((CSSBackgroundDrawable) view.getBackground()).getRadius();
                                if (!Float.isNaN(borderRadius)) {
                                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                                            borderRadius);
                                } else {
                                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                                            0);
                                }
                            }
                        }
                    });
            videoView.setClipToOutline(true);
        }
        return videoView;
    }

    @Override
    protected boolean setAttribute(String key, Object attribute) {
        switch (key) {
            case Attributes.Style.SRC:
                String uri = Attributes.getString(attribute);
                setVideoURI(uri);
                return true;
            case Attributes.Style.AUTO_PLAY:
                boolean autoPlay = Attributes.getBoolean(attribute, false);
                setAutoPlay(autoPlay);
                return true;
            case POSTER:
                String posterUri = Attributes.getString(attribute);
                setPoster(posterUri);
                return true;
            case CONTROLS:
                mControlsVisible = Attributes.getBoolean(attribute, true);
                switchControlsVisibility(mControlsVisible);
                return true;
            case MUTED:
                boolean muted = Attributes.getBoolean(attribute, false);
                setMuted(muted);
                return true;
            case OBJECT_FIT:
                String objectFit = Attributes.getString(attribute, Attributes.ObjectFit.CONTAIN);
                setObjectFit(objectFit);
                return true;
            case Attributes.Style.ORIENTATION:
                String screenOrientation =
                        Attributes.getString(attribute, Page.ORIENTATION_LANDSCAPE);
                setScreenOrientation(screenOrientation);
                return true;
            case TITLE_BAR:
                boolean titleBarEnabled = Attributes.getBoolean(attribute, true);
                setTitleBarEnabled(titleBarEnabled);
                return true;
            case TITLE:
                String title = Attributes.getString(attribute);
                setTitle(title);
                return true;
            case PLAY_COUNT:
                String playCount = Attributes.getString(attribute, Attributes.PlayCount.ONCE);
                setPlayCount(playCount);
                return true;
            case Attributes.Style.SHOW:
                mShow = parseShowAttribute(attribute);
                if (!mShow && mHost.mIsFullScreen) {
                    boolean isSameUriStr = false;
                    if (null != mHost && mHost.mIsFullScreen) {
                        Uri cacheUri = mHost.mCacheFullScreenUri;
                        String cacheUriStr = "";
                        if (null != cacheUri) {
                            cacheUriStr = cacheUri.toString();
                        }
                        if (!TextUtils.isEmpty(cacheUriStr)) {
                            isSameUriStr = cacheUriStr.equals(mUri);
                        }
                    }
                    if (isSameUriStr) {
                        exitFullscreen();
                    }
                }
                super.setAttribute(key, attribute);
                return true;
            default:
                break;
        }
        return super.setAttribute(key, attribute);
    }

    @Override
    protected boolean addEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }
        if (ERROR.equals(event)) {
            mHost.setOnErrorListener(
                    new FlexVideoView.OnErrorListener() {
                        @Override
                        public boolean onError(int what, int extra) {
                            Log.w(TAG, "Error, what:" + what + " extra:" + extra);
                            Map<String, Object> params = new HashMap();
                            params.put("what", what);
                            params.put("extra", extra);
                            mCallback
                                    .onJsEventCallback(getPageId(), mRef, ERROR, Video.this, params,
                                            null);
                            return true;
                        }
                    });
            return true;
        } else if (START.equals(event)) {
            mHost.setOnStartListener(
                    new FlexVideoView.OnStartListener() {
                        @Override
                        public void onStart() {
                            mCallback.onJsEventCallback(getPageId(), mRef, START, Video.this, null,
                                    null);
                        }
                    });
            return true;
        } else if (PREPARED.equals(event)) {
            mOnPreparedRegistered = true;
            return true;
        } else if (PLAYING.equals(event)) {
            mHost.setOnPlayingListener(
                    new FlexVideoView.OnPlayingListener() {
                        @Override
                        public void onPlaying() {
                            mCallback
                                    .onJsEventCallback(getPageId(), mRef, PLAYING, Video.this, null,
                                            null);
                        }
                    });
            return true;
        } else if (PAUSE.equals(event)) {
            mHost.setOnPauseListener(
                    new FlexVideoView.OnPauseListener() {
                        @Override
                        public void onPause() {
                            mCallback.onJsEventCallback(getPageId(), mRef, PAUSE, Video.this, null,
                                    null);
                        }
                    });
            return true;
        } else if (FINISH.equals(event)) {
            mHost.setOnCompletionListener(
                    new FlexVideoView.OnCompletionListener() {
                        @Override
                        public void onCompletion() {
                            mCallback.onJsEventCallback(getPageId(), mRef, FINISH, Video.this, null,
                                    null);
                        }
                    });
            return true;
        } else if (TIMEUPDATE.equals(event)) {
            mHost.setOnTimeUpdateListener(
                    new FlexVideoView.OnTimeUpdateListener() {
                        @Override
                        public void onTimeUpdate() {
                            if (mHost != null) {
                                Map<String, Object> params = new HashMap<>();
                                params.put(CURRENT_TIME, mHost.getCurrentPosition() / 1000f);
                                mCallback.onJsEventCallback(
                                        getPageId(), mRef, TIMEUPDATE, Video.this, params, null);
                            }
                        }
                    });
            return true;
        } else if (SEEKING.equals(event)) {
            mHost.setOnSeekingListener(
                    new FlexVideoView.OnSeekingListener() {
                        @Override
                        public void onSeeking(long position) {
                            Map<String, Object> params = new HashMap<>();
                            params.put(CURRENT_TIME, position / 1000f);
                            mCallback.onJsEventCallback(getPageId(), mRef, SEEKING, Video.this,
                                    params, null);
                        }
                    });
            return true;
        } else if (SEEKED.equals(event)) {
            mHost.setOnSeekedListener(
                    new FlexVideoView.OnSeekedListener() {
                        @Override
                        public void onSeeked(long position) {
                            Map<String, Object> params = new HashMap<>();
                            params.put(CURRENT_TIME, position / 1000f);
                            mCallback.onJsEventCallback(getPageId(), mRef, SEEKED, Video.this,
                                    params, null);
                        }
                    });
            return true;
        } else if (Attributes.Event.CLICK.equals(event)) {
            mHost.setClickable(true);
            return super.addEvent(event);
        }
        return super.addEvent(event);
    }

    @Override
    protected boolean removeEvent(String event) {
        if (TextUtils.isEmpty(event) || mHost == null) {
            return true;
        }

        if (ERROR.equals(event)) {
            mHost.setOnErrorListener(null);
            return true;
        } else if (START.equals(event)) {
            mHost.setOnStartListener(null);
            return true;
        } else if (PREPARED.equals(event)) {
            mOnPreparedRegistered = false;
            return true;
        } else if (PLAYING.equals(event)) {
            mHost.setOnPlayingListener(null);
            return true;
        } else if (PAUSE.equals(event)) {
            mHost.setOnPauseListener(null);
            return true;
        } else if (FINISH.equals(event)) {
            mHost.setOnCompletionListener(null);
            return true;
        } else if (TIMEUPDATE.equals(event)) {
            mHost.setOnTimeUpdateListener(null);
            return true;
        } else if (SEEKING.equals(event)) {
            mHost.setOnSeekingListener(null);
            return true;
        } else if (SEEKED.equals(event)) {
            mHost.setOnSeekedListener(null);
            return true;
        }
        return super.removeEvent(event);
    }

    public void setVideoURI(String uri) {
        if (mHost == null) {
            return;
        }

        if (uri == null) {
            if (mUri != null) {
                resetState();
            }
        } else if (uri != null) {
            if (!uri.equals(mUri)) {
                resetState();
            }
        }
        mUri = uri;

        if (TextUtils.isEmpty(uri)) {
            mHost.setVideoURI(null);
            return;
        }
        Uri tmpUri = tryParseUri(uri);
        if (null != tmpUri) {
            mParseUriStr = tmpUri.toString();
        } else {
            mParseUriStr = null;
        }
        mHost.setVideoURI(tmpUri);
        NetworkReportManager.getInstance().reportNetwork(NetworkReportManager.KEY_VIDEO, uri.toString());
    }

    public void setAutoPlay(boolean autoPlay) {
        mAutoPlay = autoPlay;

        if (mHost != null) {
            mHost.setAutoPlay(autoPlay);
        }
    }

    public void setPoster(String poster) {
        if (mHost == null) {
            return;
        }

        if (TextUtils.isEmpty(poster)) {
            mHost.setPoster(null);
            return;
        }
        mHost.setPoster(tryParseUri(poster));
    }

    public void setMuted(boolean muted) {
        if (mHost == null) {
            return;
        }
        mHost.setMuted(muted);
    }

    public void setCurrentTime(int position) {
        if (mHost == null) {
            return;
        }
        mHost.setCurrentTime(position);
    }

    public void setScreenOrientation(String orientation) {
        if (TextUtils.isEmpty(orientation) || mHost == null) {
            return;
        }
        mHost.setScreenOrientation(orientation);
    }

    public void start() {
        if (mHost == null) {
            return;
        }
        mHost.start();
    }

    public void pause() {
        if (mHost == null) {
            return;
        }
        mHost.pause();
    }

    private void requestFullscreen(int screenOrientation) {
        if (mHost == null) {
            return;
        }
        mHost.requestFullscreen(screenOrientation);
    }

    private void exitFullscreen() {
        if (mHost == null) {
            return;
        }
        mHost.exitFullscreen();
    }

    @Override
    public void invokeMethod(String methodName, Map<String, Object> args) {
        if (METHOD_START.equals(methodName)) {
            start();
        } else if (METHOD_PAUSE.equals(methodName)) {
            pause();
        } else if (METHOD_SET_CURRENT_TIME.equals(methodName)) {
            if (args == null || args.get(CURRENT_TIME) == null) {
                return;
            }
            float position = Attributes.getFloat(mHapEngine, args.get(CURRENT_TIME));
            setCurrentTime((int) (position * 1000));
        } else if (Component.METHOD_REQUEST_FULLSCREEN.equals(methodName)) {

            int screenOrientation =
                    BuildPlatform.isTV()
                            ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            if (getMinPlatformVersion() < 1050) { // SR-224 change default value after 1050
                screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            }

            if (args != null) {
                Object value = args.get(Attributes.Style.SCREEN_ORIENTATION);
                if (Page.ORIENTATION_PORTRAIT.equals(value)) {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else if (Page.ORIENTATION_LANDSCAPE.equals(value)) {
                    screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                }
            }
            requestFullscreen(screenOrientation);
        } else if (METHOD_EXIT_FULLSCREEN.equals(methodName)) {
            exitFullscreen();
        } else if (METHOD_GET_BOUNDING_CLIENT_RECT.equals(methodName)) {
            super.invokeMethod(methodName, args);
        }
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        mPaused = false;
        if (mHost != null) {
            mHost.onActivityResume();
        }
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
        mPaused = true;
        if (mHost != null) {
            mHost.onActivityPaused();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        mIsDestroy = true;
        mLastPosition = -1;
        mCallback.removeActivityStateListener(this);
        if (mHost != null) {
            mHost.exitFullscreen();
            mHost.release();
            if (mHost.mIsEverCacheVideo) {
                VideoCacheManager.getInstance().removeCacheVideoData(getPageId(), mParseUriStr);
            }
        }
    }

    public void switchControlsVisibility(boolean visible) {
        if (mHost != null) {
            mHost.switchControlsVisibility(visible);
        }
    }

    public boolean isControlsVisible() {
        return mControlsVisible;
    }

    public void setControlsVisible(boolean visible) {
        mControlsVisible = visible;
    }

    public boolean getPreIsInPlayingState() {
        return mPreInPlayingState;
    }

    public void setPreIsInPlayingState(boolean isInPlayingState) {
        mPreInPlayingState = isInPlayingState;
    }

    public long getLastPosition() {
        return mLastPosition;
    }

    public void setLastPosition(long lastPosition) {
        mLastPosition = lastPosition;
    }

    public boolean isPaused() {
        return mPaused;
    }

    private void resetState() {
        mPreInPlayingState = false;
        mLastPosition = -1;
    }

    protected IMediaPlayer createPlayer() {
        return PlayerInstanceManager.getInstance().createMediaPlayer();
    }

    private void setVideoSize(int videoWidth, int videoHeight) {
        if (videoWidth == 0 || videoHeight == 0 || mHost == null || mHost.isFullscreen()) {
            return;
        }

        final boolean isWidthDefined = isWidthDefined();
        final boolean isHeightDefined = isHeightDefined();
        if ((isWidthDefined && isHeightDefined)) {
            return;
        }

        float objectWidth = -1;
        float objectHeight = -1;
        final float aspectRatio = (float) videoWidth / videoHeight;
        boolean needRequestLayout = false;

        if (isParentYogaLayout()) {
            YogaFlexDirection parentDirection = null;
            boolean isParentStretch = false;
            YogaNode yogaNode = mNode.getParent();
            if (yogaNode != null) {
                parentDirection = yogaNode.getFlexDirection();
                isParentStretch = (yogaNode.getAlignItems() == YogaAlign.STRETCH);
            }
            final float layoutWidth = mNode.getLayoutWidth();
            final float layoutHeight = mNode.getLayoutHeight();

            if (!isWidthDefined && !isHeightDefined) {
                if (parentDirection == YogaFlexDirection.ROW
                        || parentDirection == YogaFlexDirection.ROW_REVERSE) {
                    // consider main-axis first
                    if (layoutWidth > 0) {
                        // main-axis grow
                        objectWidth = layoutWidth;
                        if (!isParentStretch) {
                            objectHeight = layoutWidth / aspectRatio;
                        }
                    } else if (layoutHeight > 0) {
                        // cross-axis stretched
                        objectHeight = layoutHeight;
                        objectWidth = layoutHeight * aspectRatio;
                    } else {
                        // default to video dimension
                        objectWidth = videoWidth;
                        objectHeight = videoHeight;
                    }
                } else if (parentDirection == YogaFlexDirection.COLUMN
                        || parentDirection == YogaFlexDirection.COLUMN_REVERSE) {
                    if (layoutHeight > 0) {
                        // main-axis grow
                        objectHeight = layoutHeight;
                        if (!isParentStretch) {
                            objectWidth = layoutHeight * aspectRatio;
                        }
                    } else if (layoutWidth > 0) {
                        // cross-axis stretched
                        objectWidth = layoutWidth;
                        objectHeight = layoutWidth / aspectRatio;
                    } else {
                        // default to video dimension
                        objectWidth = videoWidth;
                        objectHeight = videoHeight;
                    }
                }
            } else if (!isWidthDefined && isHeightDefined) {
                objectWidth = layoutHeight * aspectRatio;
            } else if (!isHeightDefined && isWidthDefined) {
                objectHeight = layoutWidth / aspectRatio;
            }
            if (objectWidth != -1) {
                mNode.setWidth(objectWidth);
                needRequestLayout = true;
            }
            if (objectHeight != -1) {
                mNode.setHeight(objectHeight);
                needRequestLayout = true;
            }
        } else {
            ViewGroup.LayoutParams lp = mHost.getLayoutParams();
            if (!isWidthDefined && !isHeightDefined) {
                objectWidth = videoWidth;
                objectHeight = videoHeight;
            } else if (!isWidthDefined() && isHeightDefined()) {
                int height =
                        (mHost.getMeasuredHeight() > videoHeight || lp.height < 0)
                                ? mHost.getMeasuredHeight()
                                : lp.height;
                objectWidth = Math.round(height * aspectRatio);

            } else if (isWidthDefined() && !isHeightDefined()) {
                int width =
                        (mHost.getMeasuredWidth() > videoWidth || lp.width < 0)
                                ? mHost.getMeasuredWidth()
                                : lp.width;
                objectHeight = Math.round(width / aspectRatio);
            }
            if (objectWidth != -1) {
                lp.width = (int) objectWidth;
                needRequestLayout = true;
            }
            if (objectHeight != -1) {
                lp.height = (int) objectHeight;
                needRequestLayout = true;
            }
        }

        if (needRequestLayout) {
            mHost.requestLayout();
        }
    }

    private void setObjectFit(String objectFit) {
        if (TextUtils.isEmpty(objectFit)) {
            return;
        }
        mHost.setObjectFit(objectFit);
    }

    private void setTitleBarEnabled(boolean titleBarEnabled) {
        mHost.setTitleBarEnabled(titleBarEnabled);
    }

    private void setTitle(String title) {
        mHost.setTitle(title);
    }

    private void setPlayCount(String playCount) {
        mHost.setPlayCount(playCount);
    }

    @Override
    public void setBorderRadius(String position, float borderRadius) {
        super.setBorderRadius(position, borderRadius);
        if (FloatUtil.isUndefined(borderRadius) || borderRadius < 0 || mHost == null) {
            return;
        }

        switch (position) {
            case Attributes.Style.BORDER_RADIUS:
                mHost.setBorderRadius(borderRadius);
                break;
            case Attributes.Style.BORDER_TOP_LEFT_RADIUS:
                mHost.setBorderCornerRadii(Corner.TOP_LEFT, borderRadius);
                break;
            case Attributes.Style.BORDER_TOP_RIGHT_RADIUS:
                mHost.setBorderCornerRadii(Corner.TOP_RIGHT, borderRadius);
                break;
            case Attributes.Style.BORDER_BOTTOM_LEFT_RADIUS:
                mHost.setBorderCornerRadii(Corner.BOTTOM_LEFT, borderRadius);
                break;
            case Attributes.Style.BORDER_BOTTOM_RIGHT_RADIUS:
                mHost.setBorderCornerRadii(Corner.BOTTOM_RIGHT, borderRadius);
                break;
            default:
                break;
        }
    }
}
