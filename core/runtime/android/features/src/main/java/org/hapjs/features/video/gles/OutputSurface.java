/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.video.gles;

import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

public class OutputSurface implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "OutputSurface";
    HandlerThread frameAvailableThread;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private Object mFrameSyncObject = new Object(); // guards mFrameAvailable
    private boolean mFrameAvailable;
    private TextureRender mTextureRender;
    /**
     * Creates an OutputSurface backed by a pbuffer with the specifed dimensions. The new EGL context
     * and surface will be made current. Creates a Surface that can be passed to
     * MediaCodec.configure().
     */

    /**
     * Creates an OutputSurface using the current EGL context. Creates a Surface that can be passed to
     * MediaCodec.configure().
     */
    public OutputSurface() {
        setup();
    }

    /**
     * Creates instances of TextureRender and SurfaceTexture, and a Surface associated with the
     * SurfaceTexture.
     */
    private void setup() {
        mTextureRender = new TextureRender();
        mTextureRender.surfaceCreated();

        // Even if we don't access the SurfaceTexture after the constructor returns, we
        // still need to keep a reference to it.  The Surface doesn't retain a reference
        // at the Java level, so if we don't either then the object can get GCed, which
        // causes the native finalizer to run.
        mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());
        // ：OpenGL是一组基于状态的系统，向系统申请一个Texture，系统不会直接给你返回一个Texture对象，而是一个编号
        // This doesn't work if OutputSurface is created on the thread that CTS started for
        // these test cases.
        //
        // The CTS-created thread has a Looper, and the SurfaceTexture constructor will
        // create a Handler that uses it.  The "frame available" message is delivered
        // there, but since we're not a Looper-based thread we'll never see it.  For
        // this to do anything useful, OutputSurface must be created on a thread without
        // a Looper, so that SurfaceTexture uses the main application Looper instead.
        //
        // Java language note: passing "this" out of a constructor is generally unwise,
        // but we should be able to get away with it here.
        frameAvailableThread = new HandlerThread("frame-available-handlerthread");
        frameAvailableThread.start();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSurfaceTexture.setOnFrameAvailableListener(
                    this, new Handler(frameAvailableThread.getLooper()));
        } else {
            mSurfaceTexture.setOnFrameAvailableListener(this);
        }

        mSurface = new Surface(mSurfaceTexture);
    }

    public void makeSWRenderMatrix(MediaFormat format, int rotate) {
        mTextureRender.makeSWRenderMatrix(format, rotate);
    }

    /** Prepares EGL. We want a GLES 2.0 context and a surface that supports pbuffer. */

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    public void release() {
        mSurface.release();
        if (frameAvailableThread != null) {
            frameAvailableThread.quit();
            frameAvailableThread = null;
        }
        // this causes a bunch of warnings that appear harmless but might confuse someone:
        //  W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
        // mSurfaceTexture.release();

        // null everything out so future attempts to use this object will cause an NPE
        mTextureRender = null;
        mSurface = null;
        mSurfaceTexture = null;
    }

    /** Makes our EGL context and surface current. */

    /**
     * Returns the Surface that we draw onto.
     */
    public Surface getSurface() {
        return mSurface;
    }

    /**
     * Latches the next buffer into the texture. Must be called from the thread that created the
     * OutputSurface object, after the onFrameAvailable callback has signaled that new data is
     * available.
     */
    public void awaitNewImage() throws Exception {
        final int timeoutMs = 500;
        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                mFrameSyncObject.wait(timeoutMs);
                if (!mFrameAvailable) {
                    throw new RuntimeException("Surface frame wait timed out");
                }
            }
            mFrameAvailable = false;
        }

        // Latch the data.
        mTextureRender.checkGlError("before updateTexImage");
        mSurfaceTexture.updateTexImage();
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    public void drawImage() {
        mTextureRender.drawFrame(mSurfaceTexture);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        synchronized (mFrameSyncObject) {
            if (mFrameAvailable) {
                throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
            }
            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }
}
