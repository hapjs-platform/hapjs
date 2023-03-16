/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.video;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hapjs.bridge.ApplicationContext;
import org.hapjs.bridge.Response;
import org.hapjs.bridge.annotation.WidgetAnnotation;
import org.hapjs.common.compat.BuildPlatform;
import org.hapjs.common.executors.Executors;
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
import org.hapjs.render.RootView;
import org.hapjs.render.vdom.DocComponent;
import org.hapjs.runtime.HapEngine;
import org.hapjs.widgets.view.video.FlexVideoView;

@WidgetAnnotation(
        name = Video.WIDGET_NAME,
        methods = {
                Video.METHOD_START,
                Video.METHOD_PAUSE,
                Video.METHOD_SET_CURRENT_TIME,
                Video.METHOD_EXIT_FULLSCREEN,
                Video.METHOD_SNAP_SHOT,
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
    protected static final String METHOD_SNAP_SHOT = "snapshot";
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
    private static final String SPEED = "speed";

    private static final String CURRENT_TIME = "currenttime";

    //pause reason
    private static final String PAUSE_CODE = "pauseCode";
    // 按下视频左下角暂停按钮，导致视频暂停
    private static final int VIDEO_PAUSE_OF_PRESS_PAUSE_BUTTON = 201;
    // 调用快应用官方文档中的视频暂停方法，导致视频暂停
    private static final int VIDEO_PAUSE_OF_CALL_PAUSE_METHOD = 202;
    // 其他情况导致视频暂停
    private static final int VIDEO_PAUSE_OF_OTHER_REASON = 203;

    private String mUri;
    private String mParseUriStr;
    private boolean mAutoPlay;
    private boolean mControlsVisible = true;
    private boolean mOnPreparedRegistered;
    private boolean mPreInPlayingState;
    private boolean mPaused;
    private long mLastPosition = -1;
    public boolean mIsDestroy = false;

    private static final String CALLBACK_KEY_SUCCESS = "success";
    private static final String CALLBACK_KEY_FAIL = "fail";

    private static final String RESULT_URI = "uri";
    private static final String RESULT_NAME = "name";
    private static final String RESULT_SIZE = "size";

    private static final int IMAGE_QUALITY = 100;
    public static final float SPEED_DEFAULT = 1.0f;
    private long mMinLastModified;
    private static final long MAX_ALIVE_TIME_MILLIS = 60 * 60 * 1000L;

    private boolean mCallFromPauseMethod;
    private FlexVideoView mVideoView;

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
        mVideoView = new FlexVideoView(mContext, visible);
        mVideoView.setComponent(this);
        mVideoView.setIsLazyCreate(mLazyCreate);
        mVideoView.setOnPreparedListener(new FlexVideoView.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                if (mHost == null || !mHost.isAttachedToWindow()) {
                    Log.w(TAG, "createViewImpl onPrepared mHost null or !mHost.isAttachedToWindow.");
                    return;
                }
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
                    mVideoView.start();
                } else if (mAutoPlay) {
                    mVideoView.start();
                } else {
                    Log.w(TAG, "createViewImpl onPrepared else  lastPosition : " + lastPosition);
                }
                Log.w(TAG, "createViewImpl onPrepared lastPosition  : " + lastPosition
                        + " mAutoPlay : " + mAutoPlay);
            }
        });

        getOrCreateBackgroundComposer().setBackgroundColor(0xee000000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mVideoView.setOutlineProvider(
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
            mVideoView.setClipToOutline(true);
        }
        return mVideoView;
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
            case SPEED:
                String speedStr = Attributes.getString(attribute, "1");
                float speed = SPEED_DEFAULT;
                try {
                    speed = Float.parseFloat(speedStr);
                } catch (NumberFormatException e) {
                    Log.d(TAG, "parse speed error:" + e);
                    speed = SPEED_DEFAULT;
                }
                setSpeed(speed);
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
                            Map<String, Object> params = new HashMap<>(1);
                            if (mVideoView != null && mVideoView.isPauseButtonPress()) {
                                params.put(PAUSE_CODE, VIDEO_PAUSE_OF_PRESS_PAUSE_BUTTON);
                            } else if (mCallFromPauseMethod) {
                                params.put(PAUSE_CODE, VIDEO_PAUSE_OF_CALL_PAUSE_METHOD);
                            } else {
                                params.put(PAUSE_CODE, VIDEO_PAUSE_OF_OTHER_REASON);
                            }
                            Log.i(TAG, "pauseCode: " + params.get(PAUSE_CODE));
                            mCallback.onJsEventCallback(getPageId(), mRef, PAUSE, Video.this, params,
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
        mCallFromPauseMethod = true;
        mHost.pause();
        mCallFromPauseMethod = false;
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
    private void snapshot(Map<String, Object> args) {
        if (mHost == null || null == args) {
            return;
        }

        DocComponent rootComponent = getRootComponent();
        if (rootComponent == null) {
            return;
        }

        RootView rootView = (RootView) rootComponent.getHostView();
        if (rootView != null) {
            HapEngine hapEngine = HapEngine.getInstance(rootView.getPackage());
            ApplicationContext applicationContext = hapEngine.getApplicationContext();
            File fileDir = new File(applicationContext.getCacheDir() + File.separator + "video_shot");
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }
            File tempFile = new File(fileDir, UUID.randomUUID() + ".jpeg");
            Bitmap bitmap = null;
            try (OutputStream out = new FileOutputStream(tempFile)) {
                bitmap = mHost.getVideoView().getBitmap();
                bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out);

                String internalUri = applicationContext.getInternalUri(tempFile);
                String name = getFileName(internalUri);
                Map<String, Object> data = new ArrayMap<>(3);
                data.put(RESULT_URI, internalUri);
                data.put(RESULT_NAME, name);
                data.put(RESULT_SIZE, tempFile.length());
                mCallback.onJsMethodCallback(getPageId(), (String) args.get(CALLBACK_KEY_SUCCESS), data);
            } catch (FileNotFoundException e) {
                mCallback.onJsMethodCallback(getPageId(), (String) args.get(CALLBACK_KEY_FAIL), e.getMessage(), Response.CODE_FILE_NOT_FOUND);
            } catch (IOException e) {
                mCallback.onJsMethodCallback(getPageId(), (String) args.get(CALLBACK_KEY_FAIL), e.getMessage(), Response.CODE_IO_ERROR);
            } catch (Exception e) {
                mCallback.onJsMethodCallback(getPageId(), (String) args.get(CALLBACK_KEY_FAIL), e.getMessage(), Response.CODE_GENERIC_ERROR);
            }
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }

            synchronized (this) {
                File[] files = fileDir.listFiles();
                if (files != null && files.length > 1) {
                    long currentTimeMillis = System.currentTimeMillis();
                    if (currentTimeMillis - mMinLastModified <= MAX_ALIVE_TIME_MILLIS) {
                        return;
                    }
                    mMinLastModified = Long.MAX_VALUE;

                    Executors.io().execute(new Runnable() {
                        @Override
                        public void run() {
                            for (File file : files) {
                                long lastModified = file.lastModified();
                                if (currentTimeMillis - lastModified > MAX_ALIVE_TIME_MILLIS) {
                                    file.delete();
                                    if (mMinLastModified == Long.MAX_VALUE) {
                                        mMinLastModified = currentTimeMillis;
                                    }
                                } else {
                                    if (mMinLastModified > lastModified) {
                                        mMinLastModified = lastModified;
                                    }
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    private String getFileName(String uri) {
        int index = 0;
        if (uri != null) {
            index = uri.lastIndexOf('/');
        }
        if (index > 0) {
            return uri.substring(index + 1);
        } else {
            return uri;
        }
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
        } else if (METHOD_SNAP_SHOT.equals(methodName)) {
            snapshot(args);
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

    private void setSpeed(float speed) {
        if (speed <= 0) {
            speed = SPEED_DEFAULT;
        }
        mHost.setSpeed(speed);
    }
}
