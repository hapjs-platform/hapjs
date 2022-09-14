/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.video;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import org.hapjs.component.constants.Attributes;
import org.hapjs.widgets.video.IMediaPlayer;
import org.hapjs.widgets.video.Player;

public class TextureVideoView extends TextureView {

    protected IMediaPlayer mPlayer;

    // All the stuff we need for playing and showing a video
    protected SurfaceTexture mSurfaceTexture = null;
    protected TextureVideoView.SurfaceTextureListener mSurfaceListener;
    private boolean mShouldReleaseSurface = true;
    TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture surface, final int width, final int height) {
                    boolean isValidState =
                            (mPlayer != null && mPlayer.getTargetState() == Player.STATE_PLAYING);
                    boolean hasValidSize = (width > 0 && height > 0);
                    if (isValidState && hasValidSize) {
                        mPlayer.start();
                    }
                }

                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture surface, final int width, final int height) {
                    if (mSurfaceTexture == null) {
                        mSurfaceTexture = surface;
                        if (mSurfaceListener != null) {
                            mSurfaceListener.onSurfaceTextureAvailable();
                        }
                    } else {
                        setSurfaceTexture(mSurfaceTexture);
                    }
                }

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
                    if (mShouldReleaseSurface) {
                        // after we return from this we can't use the surface any more
                        if (mSurfaceTexture != null) {
                            mSurfaceTexture.release();
                            mSurfaceTexture = null;
                        }
                        if (mSurfaceListener != null) {
                            mSurfaceListener.onSurfaceTextureDestroyed();
                        }
                        release(true);
                    } else {
                        mShouldReleaseSurface = true;
                    }
                    return mSurfaceTexture == null;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture surface) {
                    // do nothing
                }
            };
    private String mObjectFit = "contain";

    public TextureVideoView(Context context) {
        this(context, null);
    }

    public TextureVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextureVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSurfaceTextureListener(mSurfaceTextureListener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int videoWidth = mPlayer == null ? 0 : mPlayer.getVideoWidth();
        int videoHeight = mPlayer == null ? 0 : mPlayer.getVideoHeight();

        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = getDefaultSize(videoWidth, widthMeasureSpec);
        int height = getDefaultSize(videoHeight, heightMeasureSpec);

        if (videoWidth > 0 && videoHeight > 0) {

            float widthRatio = widthSpecSize * 1.0f / videoWidth;
            float heightRatio = heightSpecSize * 1.0f / videoHeight;
            float ratio = 0;

            switch (mObjectFit) {
                case Attributes.ObjectFit.FILL:
                    width = widthSpecSize;
                    height = heightSpecSize;
                    break;
                case Attributes.ObjectFit.NONE:
                    ratio = 1;
                    break;
                case Attributes.ObjectFit.COVER:
                    ratio = Math.max(widthRatio, heightRatio);
                    break;
                case Attributes.ObjectFit.CONTAIN:
                    ratio = Math.min(widthRatio, heightRatio);
                    break;
                case Attributes.ObjectFit.SCALE_DOWN:
                    ratio = Math.min(widthRatio, heightRatio);
                    ratio = Math.min(ratio, 1);
                    break;
                default:
                    ratio = Math.min(widthRatio, heightRatio);
                    break;
            }
            if (ratio != 0) {
                width = (int) (videoWidth * ratio);
                height = (int) (videoHeight * ratio);
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(TextureVideoView.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(TextureVideoView.class.getName());
    }

    public void attachPlayer(IMediaPlayer player) {
        mPlayer = player;
        if (player != null) {
            player.setVideoTextureView(this);
        }
    }

    public void detachPlayer() {
        if (mPlayer != null) {
            mPlayer.setVideoTextureView(null);
        }
        mPlayer = null;
    }

    private void clearSurface() {
        if (mSurfaceTexture == null) {
            return;
        }

        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);

        int[] attribList = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, EGL10.EGL_WINDOW_BIT,
                EGL10.EGL_NONE, 0, // placeholder for recordable [@-3]
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        egl.eglChooseConfig(display, attribList, configs, configs.length, numConfigs);
        EGLConfig config = configs[0];
        EGLContext context =
                egl.eglCreateContext(
                        display, config, EGL10.EGL_NO_CONTEXT,
                        new int[] {12440, 2, EGL10.EGL_NONE});
        EGLSurface eglSurface =
                egl.eglCreateWindowSurface(display, config, mSurfaceTexture,
                        new int[] {EGL10.EGL_NONE});

        egl.eglMakeCurrent(display, eglSurface, eglSurface, context);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        egl.eglSwapBuffers(display, eglSurface);
        egl.eglDestroySurface(display, eglSurface);
        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT);
        egl.eglDestroyContext(display, context);
        egl.eglTerminate(display);
    }

    /*
     * release the media player in any state
     */
    private void release(boolean cleartargetstate) {
        if (cleartargetstate) {
            clearSurface();
        }
    }

    @Override
    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void setShouldReleaseSurface(boolean shouldReleaseSurface) {
        mShouldReleaseSurface = shouldReleaseSurface;
    }

    public void setSurfaceTextureListener(SurfaceTextureListener l) {
        mSurfaceListener = l;
    }

    public void clearSurfaceTextureListener() {
        mSurfaceListener = null;
    }

    public interface SurfaceTextureListener {
        void onSurfaceTextureAvailable();

        void onSurfaceTextureDestroyed();
    }

    public void setObjectFit(String objectFit) {
        mObjectFit = objectFit;
        requestLayout();
    }

}
