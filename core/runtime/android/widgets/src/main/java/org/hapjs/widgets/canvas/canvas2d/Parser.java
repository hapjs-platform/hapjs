/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.canvas.canvas2d;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hapjs.common.utils.ColorUtil;
import org.hapjs.common.utils.DisplayUtil;
import org.hapjs.common.utils.FloatUtil;
import org.hapjs.common.utils.IntegerUtil;
import org.hapjs.common.utils.ThreadUtils;
import org.hapjs.runtime.Runtime;
import org.hapjs.widgets.BuildConfig;
import org.hapjs.widgets.canvas.Action;
import org.hapjs.widgets.canvas.CanvasImageLoadRenderAction;
import org.hapjs.widgets.canvas.CanvasJNI;
import org.hapjs.widgets.canvas.CanvasRenderAction;
import org.hapjs.widgets.canvas.CanvasSyncRenderAction;
import org.hapjs.widgets.canvas.image.CanvasBitmap;
import org.hapjs.widgets.canvas.image.CanvasImage;
import org.hapjs.widgets.canvas.image.CanvasImageHelper;
import org.hapjs.widgets.canvas.image.ImageData;

public abstract class Parser {
    private static final String TAG = "Parser";

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private String mName;

    private Parser(String name) {
        mName = name;
    }

    public static Map<Character, Parser> create() {
        Map<Character, Parser> parsers = new HashMap<>();
        parsers.putAll(createRenderActions());
        parsers.putAll(createSyncRenderActions());
        return parsers;
    }

    private static Map<Character, Parser> createRenderActions() {
        Map<Character, Parser> parsers = new HashMap<>();
        // 属性
        parsers.put(
                'A',
                new Parser("fillStyle") {

                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "fillStyle:" + parameter);
                        }
                        switch (type) {
                            case 'A': // CSSColor
                                int color = ColorUtil.getColor(parameter);
                                Log.i(TAG, "parse fillStyleColor:" + Integer.toHexString(color));
                                return new CanvasRenderAction(getName(), parameter) {
                                    @Override
                                    public void render(@NonNull CanvasContextRendering2D context) {
                                        context.setFillStyle(color);
                                    }
                                };
                            case 'B': // pattern
                                if (TextUtils.isEmpty(parameter)) {
                                    Log.e(TAG, "parse fillStyle pattern,param is empty!");
                                    return null;
                                }

                                String[] patternParams = extractCommand(parameter);
                                if (patternParams.length != 2) {
                                    Log.e(
                                            TAG,
                                            "parse fillStyle pattern,invalid param,"
                                                    + "specify an json array which length is 2, and index 0 is image id,"
                                                    + " index 1 is pattern type!");
                                    return null;
                                }

                                int id = IntegerUtil.parse(patternParams[0]);
                                String pattern = patternParams[1];
                                return new CanvasImageLoadRenderAction(getName(), parameter) {

                                    @Override
                                    public void render(@NonNull CanvasContextRendering2D context) {
                                        CanvasImageHelper imageHelper =
                                                CanvasImageHelper.getInstance();
                                        CanvasImage image = imageHelper.getImage(id);
                                        if (image == null) {
                                            Log.e(TAG, "setFillStyle pattern error,image is null");
                                            return;
                                        }

                                        Bitmap bmp = imageHelper.getImageBitmap(image);
                                        if (bmp == null && !ThreadUtils.isInMainThread()) {
                                            bmp = imageHelper.recoverImage(image);
                                        }

                                        if (bmp != null) {
                                            context.setFillStyle(bmp, pattern);
                                            return;
                                        }

                                        if (isLoading()) {
                                            return;
                                        }

                                        markLoading(context.getPageId(),
                                                context.getCanvasElementId());

                                        imageHelper.recoverImage(image, this);
                                    }
                                };
                            case 'C': // linearGradient
                                if (TextUtils.isEmpty(parameter)) {
                                    Log.e(TAG, "parse fillStyle linearGradient,param is empty!");
                                    return null;
                                }

                                String[] linearGradientParams = extractCommand(parameter);
                                if (linearGradientParams.length < 4
                                        || linearGradientParams.length % 2 != 0) {
                                    Log.e(TAG, "parse fillStyle linearGradient,invalid param");
                                    return null;
                                }

                                float x0 = FloatUtil.parse(linearGradientParams[0]);
                                float y0 = FloatUtil.parse(linearGradientParams[1]);
                                float x1 = FloatUtil.parse(linearGradientParams[2]);
                                float y1 = FloatUtil.parse(linearGradientParams[3]);

                                LinearGradient linearGradient = new LinearGradient(x0, y0, x1, y1);
                                if (linearGradientParams.length > 4) {
                                    for (int i = 4, length = linearGradientParams.length;
                                            i < length; i += 2) {
                                        linearGradient.addColorStop(
                                                FloatUtil.parse(linearGradientParams[i]),
                                                ColorUtil.getColor(linearGradientParams[i + 1]));
                                    }
                                }

                                return new CanvasRenderAction(getName(), parameter) {
                                    @Override
                                    public void render(@NonNull CanvasContextRendering2D context) {
                                        context.setFillStyle(linearGradient);
                                    }
                                };
                            case 'D': // RadialGradient
                                if (TextUtils.isEmpty(parameter)) {
                                    Log.e(TAG, "parse fillStyle radialGradient,param is empty!");
                                    return null;
                                }

                                String[] radialGradientParams = extractCommand(parameter);
                                if (radialGradientParams.length < 6
                                        || radialGradientParams.length % 2 != 0) {
                                    Log.e(TAG, "parse fillStyle radialGradient,invalid param");
                                    return null;
                                }

                                float m0 = FloatUtil.parse(radialGradientParams[0]);
                                float n0 = FloatUtil.parse(radialGradientParams[1]);
                                float r0 = FloatUtil.parse(radialGradientParams[2]);
                                float m1 = FloatUtil.parse(radialGradientParams[3]);
                                float n1 = FloatUtil.parse(radialGradientParams[4]);
                                float r1 = FloatUtil.parse(radialGradientParams[5]);

                                RadialGradient radialGradient =
                                        new RadialGradient(m0, n0, r0, m1, n1, r1);
                                if (radialGradientParams.length > 6) {
                                    for (int i = 6, length = radialGradientParams.length;
                                            i < length; i += 2) {
                                        radialGradient.addColorStop(
                                                FloatUtil.parse(radialGradientParams[i]),
                                                ColorUtil.getColor(radialGradientParams[i + 1]));
                                    }
                                }

                                return new CanvasRenderAction(getName(), parameter) {
                                    @Override
                                    public void render(@NonNull CanvasContextRendering2D context) {
                                        context.setFillStyle(radialGradient);
                                    }
                                };
                            default:
                                break;
                        }
                        return null;
                    }
                });
        parsers.put(
                'B',
                new Parser("font") {
                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "font:" + parameter);
                        }

                        if (TextUtils.isEmpty(parameter)) {
                            return null;
                        }

                        CSSFont font = CSSFont.parse(parameter);

                        return new CanvasRenderAction(getName(), parameter) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setFont(font);
                            }
                        };
                    }
                });
        parsers.put(
                'C',
                new NoTypeParser("globalAlpha", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float alpha = FloatUtil.parse(params[0]);
                        if (FloatUtil.isUndefined(alpha)) {
                            Log.e(TAG, "parse globalAlpha error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setGlobalAlpha(alpha);
                            }
                        };
                    }
                });
        parsers.put(
                'D',
                new NoTypeParser("globalCompositeOperation", 1) {

                    private boolean isValid(String globalCompositeOperation) {
                        return "source-over".equals(globalCompositeOperation)
                                || "source-atop".equals(globalCompositeOperation)
                                || "source-in".equals(globalCompositeOperation)
                                || "source-out".equals(globalCompositeOperation)
                                || "destination-over".equals(globalCompositeOperation)
                                || "destination-atop".equals(globalCompositeOperation)
                                || "destination-in".equals(globalCompositeOperation)
                                || "destination-out".equals(globalCompositeOperation)
                                || "lighter".equals(globalCompositeOperation)
                                || "copy".equals(globalCompositeOperation)
                                || "xor".equals(globalCompositeOperation);
                    }

                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        String globalCompositeOperation = params[0];
                        if (!isValid(globalCompositeOperation)) {
                            Log.w(TAG, "globalCompositeOperation:" + globalCompositeOperation
                                    + " is invalid");
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {

                            //                    @Override
                            //                    public boolean supportHardware() {
                            //
                            // //https://developer.android.com/guide/topics/graphics/hardware-accel.html
                            //                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P &&
                            // "lighter".equalsIgnoreCase(globalCompositeOperation)) {
                            //                            return false;
                            //                        }
                            //                        return true;
                            //                    }

                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setGlobalCompositeOperation(globalCompositeOperation);
                            }

                            @Override
                            public boolean useCompositeCanvas() {
                                // source-over为默认的xfermode
                                return true;
                            }
                        };
                    }
                });
        parsers.put(
                'E',
                new NoTypeParser("lineCap", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        String lineCap = params[0];
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setLineCap(lineCap);
                            }
                        };
                    }
                });
        parsers.put(
                'F',
                new NoTypeParser("lineDashOffset", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float lineDashOffset = FloatUtil.parse(params[0]);
                        if (FloatUtil.isUndefined(lineDashOffset)) {
                            Log.e(TAG, "parse lineDashOffset error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setLineDashOffset(lineDashOffset);
                            }
                        };
                    }
                });
        parsers.put(
                'G',
                new NoTypeParser("lineJoin", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        String lineJoin = params[0];
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setLineJoin(lineJoin);
                            }

                            @Override
                            public boolean supportHardware(
                                    @NonNull CanvasContextRendering2D context) {
                                // miter在android9以下绘制，会被截掉，与bevel效果一样
                                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                                        || TextUtils.equals(lineJoin, "bevel")
                                        || TextUtils.equals(lineJoin, "round");
                            }
                        };
                    }
                });
        parsers.put(
                'H',
                new NoTypeParser("lineWidth", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float lineWidth = FloatUtil.parse(params[0]);
                        if (FloatUtil.isUndefined(lineWidth)) {
                            Log.e(TAG, "parse lineWidth error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setLineWidth(lineWidth);
                            }
                        };
                    }
                });
        parsers.put(
                'I',
                new NoTypeParser("miterLimit", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float miterLimit = FloatUtil.parse(params[0]);
                        if (FloatUtil.isUndefined(miterLimit)) {
                            Log.e(TAG, "parse miterLimit error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setMiterLimit(miterLimit);
                            }

                            @Override
                            public boolean supportHardware(
                                    @NonNull CanvasContextRendering2D context) {
                                // android9以下，使用硬件绘制miter的效果与bevel一样
                                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                                        || context.currentState().mStrokePaint.getStrokeJoin()
                                        != Paint.Join.MITER;
                            }
                        };
                    }
                });
        parsers.put(
                'J',
                new NoTypeParser("shadowBlur", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float blur = FloatUtil.parse(params[0]);
                        if (FloatUtil.isUndefined(blur)) {
                            Log.e(TAG, "parse shadowBlur error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {

                            @Override
                            public boolean supportHardware(
                                    @NonNull CanvasContextRendering2D context) {
                                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
                            }

                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setShadowBlur(blur);
                            }
                        };
                    }
                });
        parsers.put(
                'K',
                new NoTypeParser("shadowColor", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        int color = ColorUtil.getColor(params[0]);
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public boolean supportHardware(
                                    @NonNull CanvasContextRendering2D context) {
                                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
                            }

                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setShadowColor(color);
                            }
                        };
                    }
                });
        parsers.put(
                'L',
                new NoTypeParser("shadowOffsetX", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float offsetX = FloatUtil.parse(params[0]);
                        if (FloatUtil.isUndefined(offsetX)) {
                            Log.e(TAG, "parse shadowOffsetX error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public boolean supportHardware(
                                    @NonNull CanvasContextRendering2D context) {
                                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
                            }

                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setShadowOffsetX(offsetX);
                            }
                        };
                    }
                });
        parsers.put(
                'M',
                new NoTypeParser("shadowOffsetY", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float offsetY = FloatUtil.parse(params[0]);
                        if (FloatUtil.isUndefined(offsetY)) {
                            Log.e(TAG, "parse shadowOffsetY error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public boolean supportHardware(
                                    @NonNull CanvasContextRendering2D context) {
                                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
                            }

                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setShadowOffsetY(offsetY);
                            }
                        };
                    }
                });
        parsers.put(
                'N',
                new Parser("strokeStyle") {
                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {

                        if (DEBUG) {
                            Log.i(TAG, "strokeStyle:" + parameter);
                        }

                        switch (type) {
                            case 'A': // CSSColor
                                int color = ColorUtil.getColor(parameter);
                                Log.i(TAG, "parse strokeStyleColor:" + Integer.toHexString(color));
                                return new CanvasRenderAction(getName(), parameter) {
                                    @Override
                                    public void render(@NonNull CanvasContextRendering2D context) {
                                        context.setStrokeStyle(color);
                                    }
                                };
                            case 'B': // pattern
                                if (TextUtils.isEmpty(parameter)) {
                                    Log.e(TAG, "parse strokeStyle pattern,param is empty!");
                                    return null;
                                }

                                String[] patternParams = extractCommand(parameter);
                                if (patternParams.length != 2) {
                                    Log.e(
                                            TAG,
                                            "parse strokeStyle pattern,invalid param,"
                                                    +
                                                    "specify an json array which length is 2, and index 0 is image id, "
                                                    + " index 1 is pattern type!");
                                    return null;
                                }

                                int id = IntegerUtil.parse(patternParams[0]);
                                String pattern = patternParams[1];
                                return new CanvasImageLoadRenderAction(getName(), parameter) {

                                    @Override
                                    public void render(@NonNull CanvasContextRendering2D context) {
                                        CanvasImageHelper imageHelper =
                                                CanvasImageHelper.getInstance();
                                        CanvasImage image = imageHelper.getImage(id);
                                        if (image == null) {
                                            Log.e(TAG, "setFillStyle pattern error,image is null");
                                            return;
                                        }

                                        Bitmap bmp = imageHelper.getImageBitmap(image);
                                        if (bmp == null && !ThreadUtils.isInMainThread()) {
                                            bmp = imageHelper.recoverImage(image);
                                        }

                                        if (bmp != null) {
                                            context.setStrokeStyle(bmp, pattern);
                                            return;
                                        }

                                        if (isLoading()) {
                                            return;
                                        }

                                        markLoading(context.getPageId(),
                                                context.getCanvasElementId());

                                        imageHelper.recoverImage(image, this);
                                    }
                                };

                            case 'C': // linearGradient
                                if (TextUtils.isEmpty(parameter)) {
                                    Log.e(TAG, "parse strokeStyle linearGradient,param is empty!");
                                    return null;
                                }

                                String[] linearGradientParams = extractCommand(parameter);
                                if (linearGradientParams.length < 4
                                        || linearGradientParams.length % 2 != 0) {
                                    Log.e(TAG, "parse strokeStyle linearGradient,invalid param");
                                    return null;
                                }

                                float x0 = FloatUtil.parse(linearGradientParams[0]);
                                float y0 = FloatUtil.parse(linearGradientParams[1]);
                                float x1 = FloatUtil.parse(linearGradientParams[2]);
                                float y1 = FloatUtil.parse(linearGradientParams[3]);

                                LinearGradient linearGradient = new LinearGradient(x0, y0, x1, y1);
                                if (linearGradientParams.length > 4) {
                                    for (int i = 4, length = linearGradientParams.length;
                                            i < length; i += 2) {
                                        linearGradient.addColorStop(
                                                FloatUtil.parse(linearGradientParams[i]),
                                                ColorUtil.getColor(linearGradientParams[i + 1]));
                                    }
                                }

                                return new CanvasRenderAction(getName(), parameter) {
                                    @Override
                                    public void render(@NonNull CanvasContextRendering2D context) {
                                        context.setStrokeStyle(linearGradient);
                                    }
                                };
                            case 'D': // RadialGradient
                                if (TextUtils.isEmpty(parameter)) {
                                    Log.e(TAG, "parse strokeStyle radialGradient,param is empty!");
                                    return null;
                                }

                                String[] radialGradientParams = extractCommand(parameter);
                                if (radialGradientParams.length < 6
                                        || radialGradientParams.length % 2 != 0) {
                                    Log.e(TAG, "parse strokeStyle radialGradient,invalid param");
                                    return null;
                                }

                                float m0 = FloatUtil.parse(radialGradientParams[0]);
                                float n0 = FloatUtil.parse(radialGradientParams[1]);
                                float r0 = FloatUtil.parse(radialGradientParams[2]);
                                float m1 = FloatUtil.parse(radialGradientParams[3]);
                                float n1 = FloatUtil.parse(radialGradientParams[4]);
                                float r1 = FloatUtil.parse(radialGradientParams[5]);

                                RadialGradient radialGradient =
                                        new RadialGradient(m0, n0, r0, m1, n1, r1);
                                if (radialGradientParams.length > 6) {
                                    for (int i = 6, length = radialGradientParams.length;
                                            i < length; i += 2) {
                                        radialGradient.addColorStop(
                                                FloatUtil.parse(radialGradientParams[i]),
                                                ColorUtil.getColor(radialGradientParams[i + 1]));
                                    }
                                }

                                return new CanvasRenderAction(getName(), parameter) {
                                    @Override
                                    public void render(@NonNull CanvasContextRendering2D context) {
                                        // context.setStrokeStyle(radialGradient);
                                        context.setStrokeStyle(radialGradient);
                                    }
                                };
                            default:
                                break;
                        }
                        return null;
                    }
                });
        parsers.put(
                'O',
                new NoTypeParser("textAlign", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        String textAlign = params[0];
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setTextAlign(textAlign);
                            }
                        };
                    }
                });
        parsers.put(
                'P',
                new NoTypeParser("textBaseline", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        String textBaseLine = params[0];
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setTextBaseline(textBaseLine);
                            }
                        };
                    }
                });

        // 方法
        parsers.put(
                'Q',
                new NoTypeParser("arc", 6) {
                    @Override
                    public Action parse(int pageId, int canvasId, String[] params, int num,
                                        String origin) {
                        float x = FloatUtil.parse(params[0]);
                        float y = FloatUtil.parse(params[1]);
                        float radius = FloatUtil.parse(params[2]);
                        float startAngle = FloatUtil.parse(params[3]);
                        float endAngle = FloatUtil.parse(params[4]);
                        int v = IntegerUtil.parse(params[5]);
                        boolean anticlockwise = v != 0;

                        if (FloatUtil.isUndefined(x)
                                || FloatUtil.isUndefined(y)
                                || FloatUtil.isUndefined(radius)
                                || FloatUtil.isUndefined(startAngle)
                                || FloatUtil.isUndefined(endAngle)) {
                            Log.e(TAG, "parse [arc],parameter is error:" + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.arc(x, y, radius, startAngle, endAngle, anticlockwise);
                            }
                        };
                    }
                });
        parsers.put(
                'R',
                new NoTypeParser("arcTo", 5) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float x1 = FloatUtil.parse(params[0]);
                        float y1 = FloatUtil.parse(params[1]);
                        float x2 = FloatUtil.parse(params[2]);
                        float y2 = FloatUtil.parse(params[3]);
                        float radius = FloatUtil.parse(params[4]);
                        if (FloatUtil.isUndefined(x1)
                                || FloatUtil.isUndefined(y1)
                                || FloatUtil.isUndefined(x2)
                                || FloatUtil.isUndefined(y2)
                                || FloatUtil.isUndefined(radius)) {
                            Log.e(TAG, "parse arcTo error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.arcTo(x1, y1, x2, y2, radius);
                            }
                        };
                    }
                });
        parsers.put(
                'S',
                new Parser("beginPath") {
                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "beginPath");
                        }

                        return new CanvasRenderAction(getName(), parameter) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.beginPath();
                            }
                        };
                    }
                });
        parsers.put(
                'T',
                new NoTypeParser("bezierCurveTo", 6) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float cpx1 = FloatUtil.parse(params[0]);
                        float cpy1 = FloatUtil.parse(params[1]);
                        float cpx2 = FloatUtil.parse(params[2]);
                        float cpy2 = FloatUtil.parse(params[3]);
                        float x = FloatUtil.parse(params[4]);
                        float y = FloatUtil.parse(params[5]);

                        if (FloatUtil.isUndefined(cpx1)
                                || FloatUtil.isUndefined(cpy1)
                                || FloatUtil.isUndefined(cpx2)
                                || FloatUtil.isUndefined(cpy2)
                                || FloatUtil.isUndefined(x)
                                || FloatUtil.isUndefined(y)) {
                            Log.e(TAG, "parse bezierCurveTo error,parameter is invalid," + origin);
                            return null;
                        }

                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.bezierCurveTo(cpx1, cpy1, cpx2, cpy2, x, y);
                            }
                        };
                    }
                });
        parsers.put(
                'U',
                new NoTypeParser("clearRect", 4) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float x = FloatUtil.parse(params[0]);
                        float y = FloatUtil.parse(params[1]);
                        float width = FloatUtil.parse(params[2]);
                        float height = FloatUtil.parse(params[3]);

                        if (FloatUtil.isUndefined(x)
                                || FloatUtil.isUndefined(y)
                                || FloatUtil.isUndefined(width)
                                || FloatUtil.isUndefined(height)) {
                            Log.e(TAG, "parse clearRect,invalid param:" + origin);
                            return null;
                        }

                        return new CanvasRenderAction(getName(), origin) {

                            @Override
                            public boolean canClear(@NonNull CanvasContextRendering2D context) {
                                RectF region = context.getDesignDisplayRegion();
                                if (region == null) {
                                    return false;
                                }
                                if (x <= region.left
                                        && y <= region.top
                                        && (x + width) >= region.right
                                        && (y + height) >= region.bottom) {
                                    return true;
                                }

                                Bitmap bitmap = context.dumpBitmap();
                                if (bitmap == null) {
                                    return false;
                                }

                                Rect rect = context.getOrCreateClipWhiteArea();
                                CanvasJNI.computeClipWhiteArea(bitmap, rect);
                                bitmap.recycle();

                                int xTemp = (int) Math.ceil(x);
                                int yTemp = (int) Math.ceil(y);
                                int widthTemp = (int) Math.floor(width);
                                int heightTemp = (int) Math.floor(height);
                                return xTemp <= rect.left
                                        && yTemp <= rect.top
                                        && (xTemp + widthTemp) >= rect.right
                                        && (yTemp + heightTemp) >= rect.bottom;
                            }

                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.clearRect(x, y, width, height);
                            }
                        };
                    }
                });
        parsers.put(
                'V',
                new Parser("clip") {
                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "clip");
                        }

                        return new CanvasRenderAction(getName(), parameter) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.clip();
                            }
                        };
                    }
                });
        parsers.put(
                'W',
                new Parser("closePath") {
                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "closePath:");
                        }
                        return new CanvasRenderAction(getName(), parameter) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.closePath();
                            }
                        };
                    }
                });
        parsers.put(
                'X',
                new Parser("drawImage") {
                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "drawImage:" + parameter);
                        }
                        if (TextUtils.isEmpty(parameter)) {
                            return null;
                        }
                        String[] params = extractCommand(parameter);
                        if (params.length != 9) {
                            return null;
                        }

                        int id = Integer.parseInt(params[0]);
                        float sx = FloatUtil.parse(params[1]);
                        float sy = FloatUtil.parse(params[2]);
                        float sWidth = FloatUtil.parse(params[3]);
                        float sHeight = FloatUtil.parse(params[4]);
                        float dx = FloatUtil.parse(params[5]);
                        float dy = FloatUtil.parse(params[6]);
                        float dWidth = FloatUtil.parse(params[7]);
                        float dHeight = FloatUtil.parse(params[8]);

                        return new CanvasImageLoadRenderAction(getName(), parameter) {

                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                CanvasImageHelper imageHelper = CanvasImageHelper.getInstance();
                                CanvasImage image = imageHelper.getImage(id);
                                if (image == null) {
                                    Log.e(TAG, "drawImage error error,image is null");
                                    return;
                                }

                                CanvasBitmap canvasBitmap = imageHelper.getCanvasBitmap(image);
                                Bitmap bmp = imageHelper.getImageBitmap(image);
                                if (bmp == null && !ThreadUtils.isInMainThread()) {
                                    bmp = imageHelper.recoverImage(image);
                                    canvasBitmap = imageHelper.getCanvasBitmap(image);
                                }

                                if (bmp != null) {
                                    context.drawImage(
                                            bmp,
                                            sx * canvasBitmap.getScaleX(),
                                            sy * canvasBitmap.getScaleY(),
                                            sWidth * canvasBitmap.getScaleX(),
                                            sHeight * canvasBitmap.getScaleY(),
                                            dx,
                                            dy,
                                            dWidth,
                                            dHeight);
                                    return;
                                }

                                if (isLoading()) {
                                    return;
                                }

                                markLoading(context.getPageId(), context.getCanvasElementId());

                                imageHelper.recoverImage(image, this);
                            }
                        };
                    }
                });
        parsers.put(
                'Y',
                new Parser("fill") {
                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "fill:");
                        }
                        return new CanvasRenderAction(getName(), parameter) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.fill();
                            }
                        };
                    }
                });
        parsers.put(
                'Z',
                new NoTypeParser("fillRect", 4) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float x = FloatUtil.parse(params[0]);
                        float y = FloatUtil.parse(params[1]);
                        float width = FloatUtil.parse(params[2]);
                        float height = FloatUtil.parse(params[3]);

                        if (FloatUtil.isUndefined(x)
                                || FloatUtil.isUndefined(y)
                                || FloatUtil.isUndefined(width)
                                || FloatUtil.isUndefined(height)) {
                            Log.e(TAG, "parse fillRect error,invalid param:" + origin);
                            return null;
                        }

                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.fillRect(x, y, width, height);
                            }
                        };
                    }
                });
        parsers.put(
                'a',
                new Parser("fillText") {
                    @Override
                    public Action parse(int pageId, int canvasId, char type, String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "fillText:" + parameter);
                        }
                        if (TextUtils.isEmpty(parameter)) {
                            Log.e(TAG, "parse fillText error,parameter is empty!");
                            return null;
                        }

                        String[] params = extractCommand(parameter);
                        if (params.length < 3) {
                            Log.e(TAG, "parse fillText error,paramter num is must be 3 or 4,"
                                    + parameter);
                            return null;
                        }

                        final String text;
                        try {
                            text = new String(Base64.decode(params[0], Base64.DEFAULT),
                                    StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            Log.e(TAG, "parse fillText error,invalid text");
                            return null;
                        }

                        float x = FloatUtil.parse(params[1]);
                        float y = FloatUtil.parse(params[2]);
                        if (FloatUtil.isUndefined(x) || FloatUtil.isUndefined(y)) {
                            Log.e(TAG, "parse fillText error,parameter is invalid," + parameter);
                            return null;
                        }

                        if (params.length == 4) {
                            float maxWidth = FloatUtil.parse(params[3]);
                            if (FloatUtil.isUndefined(maxWidth)) {
                                Log.e(TAG, "parse fillText error,maxWidth is invalid," + parameter);
                                return null;
                            }
                            return new CanvasRenderAction(getName(), parameter) {
                                @Override
                                public void render(@NonNull CanvasContextRendering2D context) {
                                    context.fillText(text, x, y, maxWidth);
                                }
                            };
                        }

                        return new CanvasRenderAction(getName(), parameter) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.fillText(text, x, y);
                            }
                        };
                    }
                });
        parsers.put(
                'b',
                new NoTypeParser("lineTo", 2) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float x = FloatUtil.parse(params[0]);
                        float y = FloatUtil.parse(params[1]);
                        if (FloatUtil.isUndefined(x) || FloatUtil.isUndefined(y)) {
                            Log.e(TAG, "parse lineTo error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.lineTo(x, y);
                            }
                        };
                    }
                });
        parsers.put(
                'c',
                new NoTypeParser("moveTo", 2) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float x = FloatUtil.parse(params[0]);
                        float y = FloatUtil.parse(params[1]);

                        if (FloatUtil.isUndefined(x) || FloatUtil.isUndefined(y)) {
                            Log.e(TAG, "parse moveTo error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.moveTo(x, y);
                            }
                        };
                    }
                });

        parsers.put(
                'd',
                new NoTypeParser("putImageData", 5) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        // 数据在js中进行裁剪
                        int width = Integer.parseInt(params[0]);
                        int height = Integer.parseInt(params[1]);
                        String imageData = params[2];
                        float dx = FloatUtil.parse(params[3]);
                        float dy = FloatUtil.parse(params[4]);

                        if (IntegerUtil.isUndefined(width)
                                || IntegerUtil.isUndefined(height)
                                || FloatUtil.isUndefined(dx)
                                || FloatUtil.isUndefined(dy)) {
                            Log.e(TAG, "parse putImageData error,parameter is invalid," + origin);
                            return null;
                        }

                        CanvasImageHelper imageHelper = CanvasImageHelper.getInstance();
                        // 标识同一位置绘制的imagedata，新的imagedata将会覆盖旧的imagedata，以便回收bitmap
                        String key = imageHelper.generateImageDataKey(imageData);
                        imageHelper.loadImageData(key, width, height, imageData);

                        return new CanvasImageLoadRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                CanvasImageHelper helper = CanvasImageHelper.getInstance();
                                CanvasBitmap canvasBitmap = helper.getCanvasBitmap(key);
                                Bitmap bitmap = helper.getImageDataBitmap(key);
                                if (bitmap == null && !ThreadUtils.isInMainThread()) {
                                    bitmap = helper.recoverImageData(key);
                                    canvasBitmap = helper.getCanvasBitmap(key);
                                }
                                if (bitmap != null) {
                                    context.putImageData(
                                            bitmap, dx * canvasBitmap.getScaleX(),
                                            dy * canvasBitmap.getScaleY());
                                    return;
                                }

                                if (isLoading()) {
                                    return;
                                }

                                markLoading(context.getPageId(), context.getCanvasElementId());
                                imageHelper.recoverImageData(key, this);
                            }
                        };
                    }
                });

        parsers.put(
                'e',
                new NoTypeParser("quadraticCurveTo", 4) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float cpx = FloatUtil.parse(params[0]);
                        float cpy = FloatUtil.parse(params[1]);
                        float x = FloatUtil.parse(params[2]);
                        float y = FloatUtil.parse(params[3]);

                        if (FloatUtil.isUndefined(cpx)
                                || FloatUtil.isUndefined(cpy)
                                || FloatUtil.isUndefined(x)
                                || FloatUtil.isUndefined(y)) {
                            Log.e(TAG, "parse lineTo error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.quadraticCurveTo(cpx, cpy, x, y);
                            }
                        };
                    }
                });
        parsers.put(
                'f',
                new NoTypeParser("rect", 4) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float x = FloatUtil.parse(params[0]);
                        float y = FloatUtil.parse(params[1]);
                        float width = FloatUtil.parse(params[2]);
                        float height = FloatUtil.parse(params[3]);

                        if (FloatUtil.isUndefined(x)
                                || FloatUtil.isUndefined(y)
                                || FloatUtil.isUndefined(width)
                                || FloatUtil.isUndefined(height)) {
                            Log.e(TAG, "parse rect error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.rect(x, y, width, height);
                            }
                        };
                    }
                });
        parsers.put(
                'g',
                new Parser("restore") {
                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "restore:" + parameter);
                        }
                        return new CanvasRenderAction(getName(), parameter) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.restore();
                            }
                        };
                    }
                });
        parsers.put(
                'h',
                new NoTypeParser("rotate", 1) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float angle = FloatUtil.parse(params[0]);
                        if (FloatUtil.isUndefined(angle)) {
                            Log.e(TAG, "parse rotate error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.rotate(angle);
                            }
                        };
                    }
                });
        parsers.put(
                'i',
                new Parser("save") {
                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "save:" + parameter);
                        }
                        return new CanvasRenderAction(getName(), parameter) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.save();
                            }
                        };
                    }
                });
        parsers.put(
                'j',
                new NoTypeParser("scale", 2) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float x = FloatUtil.parse(params[0]);
                        float y = FloatUtil.parse(params[1]);
                        if (FloatUtil.isUndefined(x) || FloatUtil.isUndefined(y)) {
                            Log.e(TAG, "parse scale error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.scale(x, y);
                            }

                            @Override
                            public boolean supportHardware(
                                    @NonNull CanvasContextRendering2D context) {
                                // android9以下使用硬件绘制出现模糊的情况
                                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
                            }
                        };
                    }
                });
        parsers.put(
                'k',
                new Parser("setLineDash") {
                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "setLineDash:" + parameter);
                        }
                        float[] segments;
                        if (TextUtils.isEmpty(parameter)) {
                            segments = null;
                        } else {
                            String[] params = extractCommand(parameter);
                            int len = params.length;
                            segments = new float[len];
                            for (int i = 0; i < len; i++) {
                                segments[i] = FloatUtil.parse(params[i]);
                                if (FloatUtil.isUndefined(segments[i])) {
                                    Log.e(TAG, "parse setLineDash error,parameter is invalid,"
                                            + parameter);
                                    return null;
                                }
                            }
                        }

                        return new CanvasRenderAction(getName(), parameter) {

                            @Override
                            public boolean supportHardware(
                                    @NonNull CanvasContextRendering2D context) {
                                if (segments == null || segments.length <= 0) {
                                    return true;
                                }
                                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
                            }

                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setLineDash(segments);
                            }
                        };
                    }
                });
        parsers.put(
                'l',
                new NoTypeParser("setTransform", 6) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float a = FloatUtil.parse(params[0]);
                        float b = FloatUtil.parse(params[1]);
                        float c = FloatUtil.parse(params[2]);
                        float d = FloatUtil.parse(params[3]);
                        float e = FloatUtil.parse(params[4]);
                        float f = FloatUtil.parse(params[5]);
                        if (FloatUtil.isUndefined(a)
                                || FloatUtil.isUndefined(b)
                                || FloatUtil.isUndefined(c)
                                || FloatUtil.isUndefined(d)
                                || FloatUtil.isUndefined(e)
                                || FloatUtil.isUndefined(f)) {
                            Log.e(TAG, "parse setTransform error,paramter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.setTransform(a, b, c, d, e, f);
                            }
                        };
                    }
                });
        parsers.put(
                'm',
                new Parser("stroke") {
                    @Override
                    public CanvasRenderAction parse(int pageId, int canvasId, char type,
                                                    String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "stroke");
                        }
                        return new CanvasRenderAction(getName(), parameter) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.stroke();
                            }
                        };
                    }
                });
        parsers.put(
                'n',
                new NoTypeParser("strokeRect", 4) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float x = FloatUtil.parse(params[0]);
                        float y = FloatUtil.parse(params[1]);
                        float width = FloatUtil.parse(params[2]);
                        float height = FloatUtil.parse(params[3]);

                        if (FloatUtil.isUndefined(x)
                                || FloatUtil.isUndefined(y)
                                || FloatUtil.isUndefined(width)
                                || FloatUtil.isUndefined(height)) {
                            Log.e(TAG, "parse strokeRect error,invalid param:" + origin);
                            return null;
                        }

                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.strokeRect(x, y, width, height);
                            }
                        };
                    }
                });
        parsers.put(
                'o',
                new Parser("strokeText") {
                    @Override
                    public Action parse(int pageId, int canvasId, char type, String parameter) {
                        if (DEBUG) {
                            Log.i(TAG, "strokeText:" + parameter);
                        }
                        if (TextUtils.isEmpty(parameter)) {
                            Log.e(TAG, "parse strokeText error,parameter is empty!");
                            return null;
                        }

                        String[] params = extractCommand(parameter);
                        if (params.length < 3) {
                            Log.e(TAG, "parse strokeText error,paramter num is must be 3 or 4,"
                                    + parameter);
                            return null;
                        }

                        final String text;
                        try {
                            text = new String(Base64.decode(params[0], Base64.DEFAULT),
                                    StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            Log.e(TAG, "parse fillText error,invalid text");
                            return null;
                        }

                        float x = FloatUtil.parse(params[1]);
                        float y = FloatUtil.parse(params[2]);
                        if (FloatUtil.isUndefined(x) || FloatUtil.isUndefined(y)) {
                            Log.e(TAG, "parse strokeText error,parameter is invalid," + parameter);
                            return null;
                        }

                        if (params.length == 4) {
                            float maxWidth = FloatUtil.parse(params[3]);
                            if (FloatUtil.isUndefined(maxWidth)) {
                                Log.e(TAG,
                                        "parse strokeText error,maxWidth is invalid," + parameter);
                                return null;
                            }
                            return new CanvasRenderAction(getName(), parameter) {
                                @Override
                                public void render(@NonNull CanvasContextRendering2D context) {
                                    context.strokeText(text, x, y, maxWidth);
                                }
                            };
                        }

                        return new CanvasRenderAction(getName(), parameter) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.strokeText(text, x, y);
                            }
                        };
                    }
                });
        parsers.put(
                'p',
                new NoTypeParser("transform", 6) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float a = FloatUtil.parse(params[0]);
                        float b = FloatUtil.parse(params[1]);
                        float c = FloatUtil.parse(params[2]);
                        float d = FloatUtil.parse(params[3]);
                        float e = FloatUtil.parse(params[4]);
                        float f = FloatUtil.parse(params[5]);
                        if (FloatUtil.isUndefined(a)
                                || FloatUtil.isUndefined(b)
                                || FloatUtil.isUndefined(c)
                                || FloatUtil.isUndefined(d)
                                || FloatUtil.isUndefined(e)
                                || FloatUtil.isUndefined(f)) {
                            Log.e(TAG, "parse setTransform error,paramter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.transform(a, b, c, d, e, f);
                            }
                        };
                    }
                });
        parsers.put(
                'q',
                new NoTypeParser("translate", 2) {
                    @Override
                    public CanvasRenderAction parse(
                            int pageId, int canvasId, String[] params, int num, String origin) {
                        float x = FloatUtil.parse(params[0]);
                        float y = FloatUtil.parse(params[1]);
                        if (FloatUtil.isUndefined(x) || FloatUtil.isUndefined(y)) {
                            Log.e(TAG, "parse translate error,parameter is invalid," + origin);
                            return null;
                        }
                        return new CanvasRenderAction(getName(), origin) {
                            @Override
                            public void render(@NonNull CanvasContextRendering2D context) {
                                context.translate(x, y);
                            }
                        };
                    }
                });
        return parsers;
    }

    private static Map<Character, Parser> createSyncRenderActions() {
        Map<Character, Parser> parsers = new HashMap<>();
        parsers.put(
                '!',
                new NoTypeParser("measureText", 2) {
                    @Override
                    public Action parse(int pageId, int canvasId, String[] params, int num,
                                        String origin) {
                        String text = "";
                        try {
                            text = new String(Base64.decode(params[0], Base64.DEFAULT),
                                    StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            Log.e(TAG, "measureText,parse text error");
                        }
                        String font = params[1];
                        String finalText = text;
                        return new CanvasSyncRenderAction(getName()) {
                            @Override
                            public void render(
                                    @NonNull CanvasContextRendering2D context,
                                    @NonNull Map<String, Object> result)
                                    throws Exception {
                                if (TextUtils.isEmpty(finalText)) {
                                    result.put("width", 0);
                                } else {
                                    CSSFont cssFont = null;
                                    if (!TextUtils.isEmpty(font)) {
                                        cssFont = CSSFont.parse(font);
                                    }
                                    float width = context.measureText(finalText, cssFont);
                                    result.put("width", width);
                                }
                            }
                        };
                    }
                });

        parsers.put(
                '@',
                new NoTypeParser("getImageData", 4) {
                    @Override
                    public Action parse(int pageId, int canvasId, String[] params, int num,
                                        String origin) {
                        float x = FloatUtil.parse(params[0]);
                        float y = FloatUtil.parse(params[1]);
                        float sw = FloatUtil.parse(params[2]);
                        float sh = FloatUtil.parse(params[3]);
                        if (FloatUtil.isUndefined(x)
                                || FloatUtil.isUndefined(y)
                                || FloatUtil.isUndefined(sw)
                                || FloatUtil.isUndefined(sh)) {
                            Log.e(TAG, "parse getImageData error,parameter is invalid," + origin);
                            return null;
                        }

                        return new CanvasSyncRenderAction(getName()) {
                            @Override
                            public void render(
                                    @NonNull CanvasContextRendering2D context,
                                    @NonNull Map<String, Object> result)
                                    throws Exception {
                                int screenWidth = DisplayUtil
                                        .getScreenWidth(Runtime.getInstance().getContext());
                                int screenHeight = DisplayUtil
                                        .getScreenHeight(Runtime.getInstance().getContext());
                                float width = sw;
                                float height = sh;
                                if (width > screenWidth) {
                                    width = screenWidth;
                                }
                                if (height > screenHeight) {
                                    height = screenHeight;
                                }
                                ImageData imageData = context.getImageData(x, y, width, height);
                                if (imageData == null) {
                                    return;
                                }
                                result.put("width", imageData.width);
                                result.put("height", imageData.height);
                                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(imageData.data.length);
                                byteBuffer.put(imageData.data);
                                byteBuffer.rewind();
                                result.put("data", byteBuffer);
                            }
                        };
                    }
                });
        return parsers;
    }

    private static String[] extractCommand(String command) {
        if (command.indexOf('(') != -1 && command.indexOf(')') != -1) {
            // 包含了括号，例如rgba(255,255,255,255),使用非贪婪模式匹配
            String regex = "(\\s*\\w*\\s*\\(.*?\\))";

            Pattern compile = Pattern.compile(regex);
            Matcher matcher = compile.matcher(command);
            int preIndex = 0;
            List<String> args = new ArrayList<>();
            while (matcher.find()) {
                if (preIndex < matcher.start()) {
                    String split = command.substring(preIndex, matcher.start());
                    String[] c = split.split(",");
                    for (String s : c) {
                        if (!TextUtils.isEmpty(s)) {
                            args.add(s);
                        }
                    }
                }
                args.add(command.substring(matcher.start(), matcher.end()));
                preIndex = matcher.end();
            }
            String[] params = new String[args.size()];
            args.toArray(params);
            return params;
        } else {
            return command.split(",");
        }
    }

    public String getName() {
        return mName;
    }

    public abstract Action parse(int pageId, int canvasId, char type, String parameter);

    public abstract static class NoTypeParser extends Parser {

        private int mParamNumber;

        private NoTypeParser(String name, int paramNum) {
            super(name);
            mParamNumber = paramNum;
        }

        @Override
        public Action parse(int pageId, int canvasId, char type, String parameter) {
            if (DEBUG) {
                Log.i(TAG, getName() + ":" + parameter);
            }
            if (TextUtils.isEmpty(parameter)) {
                Log.e(TAG, "parse " + getName() + " error,parameter is empty!");
                return null;
            }

            String[] params = extractCommand(parameter);
            if (params.length != mParamNumber) {
                Log.e(TAG, "parse " + getName() + " error,paramter num is not " + mParamNumber);
                return null;
            }
            return parse(pageId, canvasId, params, mParamNumber, parameter);
        }

        public abstract Action parse(int pageId, int canvasId, String[] params, int num,
                                     String origin);
    }
}
