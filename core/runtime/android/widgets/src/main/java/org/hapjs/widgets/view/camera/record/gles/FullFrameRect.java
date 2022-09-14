/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record.gles;

/**
 * This class essentially represents a viewport-sized sprite that will be rendered with a texture,
 * usually from an external source like the camera or video decoder.
 */
public class FullFrameRect {
    private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE);
    private Texture2dProgram mProgram;
    private float[] mMvpMatrix = null;
    public boolean mIsCurrentError = false;
    /**
     * Prepares the object.
     *
     * @param program The program to use. FullFrameRect takes ownership, and will release the program
     *                when no longer needed.
     */
    public FullFrameRect(Texture2dProgram program) {
        mProgram = program;
    }

    /**
     * Releases resources.
     *
     * <p>This must be called with the appropriate EGL context current (i.e. the one that was current
     * when the constructor was called). If we're about to destroy the EGL context, there's no value
     * in having the caller make it current just to do this cleanup, so you can pass a flag that will
     * tell this function to skip any EGL-context-specific cleanup.
     */
    public void release(boolean doEglCleanup) {
        if (mProgram != null) {
            if (doEglCleanup) {
                mProgram.release();
            }
            mProgram = null;
        }
    }

    /**
     * Returns the program currently in use.
     */
    public Texture2dProgram getProgram() {
        return mProgram;
    }

    /**
     * Changes the program. The previous program will be released.
     *
     * <p>The appropriate EGL context must be current.
     */
    public void changeProgram(Texture2dProgram program) {
        mProgram.release();
        mProgram = program;
    }

    /**
     * Creates a texture object suitable for use with drawFrame().
     */
    public int createTextureObject() {
        return mProgram.createTextureObject();
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object.
     */
    public void drawFrame(int textureId, float[] texMatrix) {
        if (null == mRectDrawable || null == mProgram) {
            return;
        }
        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        mProgram.draw(
                GlUtil.IDENTITY_MATRIX,
                mRectDrawable.getVertexArray(),
                0,
                mRectDrawable.getVertexCount(),
                mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix,
                mRectDrawable.getTexCoordArray(),
                textureId,
                mRectDrawable.getTexCoordStride());
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object.
     */
    public void drawCameraFrame(float[] mvpMatrix, int textureId, float[] texMatrix) {
        if (null == mRectDrawable || mProgram == null) {
            return;
        }
        if (mvpMatrix != null) {
            mMvpMatrix = mvpMatrix;
        }
        if (null == mMvpMatrix) {
            return;
        }
        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        mIsCurrentError = mProgram.draw(mMvpMatrix, mRectDrawable.getVertexArray(), 0,
                mRectDrawable.getVertexCount(), mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix, mRectDrawable.getTexCoordArray(), textureId,
                mRectDrawable.getTexCoordStride());
    }
}
