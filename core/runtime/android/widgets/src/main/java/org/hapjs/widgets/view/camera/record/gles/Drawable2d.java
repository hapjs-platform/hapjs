/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.view.camera.record.gles;

import java.nio.FloatBuffer;

/**
 * Base class for stuff we like to draw.
 */
public class Drawable2d {
    private static final int SIZEOF_FLOAT = 4;
    /**
     * A "full" square, extending from -1 to +1 in both dimensions. When the model/view/projection
     * matrix is identity, this will exactly cover the viewport.
     *
     * <p>The texture coordinates are Y-inverted relative to RECTANGLE. (This seems to work out right
     * with external textures from SurfaceTexture.)
     */
    private static final float[] FULL_RECTANGLE_COORDS = {
            -1.0f, -1.0f, // 0 bottom left
            1.0f, -1.0f, // 1 bottom right
            -1.0f, 1.0f, // 2 top left
            1.0f, 1.0f, // 3 top right
    };

    private static final float[] FULL_RECTANGLE_TEX_COORDS = {
            0.0f, 0.0f, // 0 bottom left
            1.0f, 0.0f, // 1 bottom right
            0.0f, 1.0f, // 2 top left
            1.0f, 1.0f // 3 top right
    };
    private static final FloatBuffer FULL_RECTANGLE_BUF =
            GlUtil.createFloatBuffer(FULL_RECTANGLE_COORDS);
    private static final FloatBuffer FULL_RECTANGLE_TEX_BUF =
            GlUtil.createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);
    private static FloatBuffer mTexCoordArray;
    private FloatBuffer mVertexArray;
    private int mVertexCount;
    private int mCoordsPerVertex;
    private int mVertexStride;
    private int mTexCoordStride;
    private Prefab mPrefab;

    /**
     * Prepares a drawable from a "pre-fabricated" shape definition.
     *
     * <p>Does no EGL/GL operations, so this can be done at any time.
     */
    public Drawable2d(Prefab shape) {
        switch (shape) {
            case TRIANGLE:
                break;
            case RECTANGLE:
                break;
            case FULL_RECTANGLE:
                mVertexArray = FULL_RECTANGLE_BUF;
                if (mTexCoordArray == null) {
                    mTexCoordArray = FULL_RECTANGLE_TEX_BUF;
                }
                mCoordsPerVertex = 2;
                mVertexStride = mCoordsPerVertex * SIZEOF_FLOAT;
                mVertexCount = FULL_RECTANGLE_COORDS.length / mCoordsPerVertex;
                break;
            default:
                break;
        }
        mTexCoordStride = 2 * SIZEOF_FLOAT;
        mPrefab = shape;
    }

    /**
     * Returns the array of vertices.
     *
     * <p>To avoid allocations, this returns internal state. The caller must not modify it.
     */
    public FloatBuffer getVertexArray() {
        return mVertexArray;
    }

    /**
     * Returns the array of texture coordinates.
     *
     * <p>To avoid allocations, this returns internal state. The caller must not modify it.
     */
    public FloatBuffer getTexCoordArray() {
        return mTexCoordArray;
    }

    public static void setTexCoordArray(FloatBuffer floatBuffer) {
        mTexCoordArray = floatBuffer;
    }

    /**
     * Returns the number of vertices stored in the vertex array.
     */
    public int getVertexCount() {
        return mVertexCount;
    }

    /**
     * Returns the width, in bytes, of the data for each vertex.
     */
    public int getVertexStride() {
        return mVertexStride;
    }

    /**
     * Returns the width, in bytes, of the data for each texture coordinate.
     */
    public int getTexCoordStride() {
        return mTexCoordStride;
    }

    /**
     * Returns the number of position coordinates per vertex. This will be 2 or 3.
     */
    public int getCoordsPerVertex() {
        return mCoordsPerVertex;
    }

    @Override
    public String toString() {
        if (mPrefab != null) {
            return "[Drawable2d: " + mPrefab + "]";
        } else {
            return "[Drawable2d: ...]";
        }
    }

    /**
     * Enum values for constructor.
     */
    public enum Prefab {
        TRIANGLE,
        RECTANGLE,
        FULL_RECTANGLE
    }
}
