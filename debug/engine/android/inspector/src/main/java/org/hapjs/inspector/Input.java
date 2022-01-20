/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.ObjectMapper;
import com.facebook.stetho.json.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

public class Input implements ChromeDevtoolsDomain {

    public static float sScale = 1;
    private static Input sInstance;
    private final ObjectMapper mObjectMapper;
    private int mMaxWidth;
    private int mMaxHeight;

    public Input() {
        sInstance = this;
        mObjectMapper = new ObjectMapper();
    }

    public static Input getInstance() {
        return sInstance;
    }

    @ChromeDevtoolsMethod
    public void dispatchKeyEvent(JsonRpcPeer peer, JSONObject params) {
        final KeyEventRequest request = mObjectMapper.convertValue(params, KeyEventRequest.class);
        final View view = getRootView();
        if (view != null) {
            view.post(
                    new Runnable() {
                        public void run() {
                            final long now = SystemClock.uptimeMillis();
                            int action = getKeyEventAction(request);
                            if (action == -1) {
                                return;
                            }
                            KeyEvent keyEvent = new KeyEvent(action, getKeyEventCode(request));
                            view.dispatchKeyEvent(keyEvent);
                        }
                    });
        }
    }

    @ChromeDevtoolsMethod
    public void dispatchMouseEvent(JsonRpcPeer peer, JSONObject params) {
    }

    @ChromeDevtoolsMethod
    public void dispatchTouchEvent(JsonRpcPeer peer, JSONObject params) {
    }

    @ChromeDevtoolsMethod
    public void emulateTouchFromMouseEvent(JsonRpcPeer peer, JSONObject params) {
        final MouseEventRequest request =
                mObjectMapper.convertValue(params, MouseEventRequest.class);

        final View view = getRootView();
        if (view != null) {
            view.post(
                    new Runnable() {
                        public void run() {
                            final long now = SystemClock.uptimeMillis();
                            final MotionEvent evt =
                                    MotionEvent.obtain(
                                            now,
                                            now,
                                            getMotionEventAction(request),
                                            (float) request.x / sScale,
                                            (float) request.y / sScale,
                                            request.modifiers);
                            view.dispatchTouchEvent(evt);
                            evt.recycle();
                        }
                    });
        }
    }

    private View getRootView() {
        VDocumentProvider provider = VDocumentProvider.getCurrent();
        if (provider != null) {
            View view = provider.getRootView();
            if (view == null) {
                return null;
            }
            while (view != null) {
                ViewParent vp = view.getParent();
                if (!(vp instanceof ViewGroup)) {
                    break;
                }
                view = (View) vp;
            }
            return view;
        }
        return null;
    }

    private int getMotionEventAction(MouseEventRequest request) {
        String type = request.type;
        int action = 0;
        if ("mousePressed".equals(type)) {
            action |= MotionEvent.ACTION_DOWN;
        } else if ("mouseReleased".equals(type)) {
            action |= MotionEvent.ACTION_UP;
        } else if ("mouseMoved".equals(type)) {
            action |= MotionEvent.ACTION_MOVE;
        } else if ("mouseWheel".equals(type)) {
            action |= MotionEvent.ACTION_SCROLL;
        }

        return action;
    }

    private int getKeyEventAction(KeyEventRequest request) {
        String type = request.type;
        if ("keyDown".equals(type)) {
            return KeyEvent.ACTION_DOWN;
        } else if ("keyUp".equals(type)) {
            return KeyEvent.ACTION_UP;
        }
        return -1;
    }

    private int getKeyEventCode(KeyEventRequest request) {

        return KeyMapper.getCode(request.code);
    }

    public void setScreencastSize(int maxWidth, int maxHeight) {
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        final View view = getRootView();
        if (view != null) {
            int viewWidth = view.getWidth();
            int viewHeight = view.getHeight();
            sScale =
                    Math.min((float) mMaxWidth / (float) viewWidth,
                            (float) mMaxHeight / (float) viewHeight);
        }
    }

    @SuppressLint({"UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse"})
    public static class TouchPoint {
        @JsonProperty(required = true)
        public String state;

        @JsonProperty(required = true)
        public int x;

        @JsonProperty(required = true)
        public int y;

        @JsonProperty
        public int radiusX;

        @JsonProperty
        public int radiusY;

        @JsonProperty
        public double rotationAngle;

        @JsonProperty
        public double force;

        @JsonProperty
        public int id;
    }

    private static class KeyEventRequest {
        @JsonProperty(required = true)
        public String type;

        @JsonProperty
        public int modifiers;

        @JsonProperty
        public double timestamp;

        @JsonProperty
        public String unmodifiedText;

        @JsonProperty
        public String key;

        @JsonProperty
        public String code;

        @JsonProperty
        public int windowsVirtualKeyCode;

        @JsonProperty
        public int nativeVirtualKeyCode;

        @JsonProperty
        public boolean autoRepeat;

        @JsonProperty
        public boolean isKeypad;

        @JsonProperty
        public boolean isSystemKey;
    }

    private static class MouseEventRequest {
        @JsonProperty(required = true)
        public String type;

        @JsonProperty(required = true)
        public int x;

        @JsonProperty(required = true)
        public int y;

        @JsonProperty
        public int modifiers;

        @JsonProperty
        public double timestamp;

        @JsonProperty
        public String button;

        @JsonProperty
        public int clickCount;
    }

    private static class EmulateTouchFromMouseEventRequest {
        @JsonProperty(required = true)
        public String type;

        @JsonProperty(required = true)
        public int x;

        @JsonProperty(required = true)
        public int y;

        @JsonProperty(required = true)
        public String button;

        @JsonProperty
        public double deltaX;

        @JsonProperty
        public double deltaY;

        @JsonProperty
        public int modifiers;
    }

    private static class TouchEventRequest {
        @JsonProperty(required = true)
        public String type;

        @JsonProperty(required = true)
        public List<TouchPoint> touchPoints;

        @JsonProperty
        public int modifiers;
    }

    private static class KeyMapper {
        static KeyMapper sInstance = new KeyMapper();
        Map<String, Integer> mapper = new HashMap<String, Integer>();

        KeyMapper() {
            add("Enter", KeyEvent.KEYCODE_ENTER);
            add("Escape", KeyEvent.KEYCODE_ESCAPE);
            add("ShiftRight", KeyEvent.KEYCODE_SHIFT_RIGHT);
            add("ShiftLeft", KeyEvent.KEYCODE_SHIFT_LEFT);
            add("ControlLeft", KeyEvent.KEYCODE_CTRL_LEFT);
            add("ControlRight", KeyEvent.KEYCODE_CTRL_RIGHT);
            add("MetaLeft", KeyEvent.KEYCODE_META_LEFT);
            add("MetaRight", KeyEvent.KEYCODE_META_RIGHT);
            add("Escape", KeyEvent.KEYCODE_ESCAPE);
            add("AltLeft", KeyEvent.KEYCODE_ALT_LEFT);
            add("AltRight", KeyEvent.KEYCODE_ALT_RIGHT);
            add("Backspace", KeyEvent.KEYCODE_DEL);
            add("Delete", KeyEvent.KEYCODE_FORWARD_DEL);
            add("Insert", KeyEvent.KEYCODE_INSERT);
            add("PageDown", KeyEvent.KEYCODE_PAGE_DOWN);
            add("PageUp", KeyEvent.KEYCODE_PAGE_UP);
            add("Home", KeyEvent.KEYCODE_HOME);
            // add("End", KeyEvent.KEYCODE_END);
            add("KeyA", KeyEvent.KEYCODE_A);
            add("KeyB", KeyEvent.KEYCODE_B);
            add("KeyC", KeyEvent.KEYCODE_C);
            add("KeyD", KeyEvent.KEYCODE_D);
            add("KeyE", KeyEvent.KEYCODE_E);
            add("KeyF", KeyEvent.KEYCODE_F);
            add("KeyG", KeyEvent.KEYCODE_G);
            add("KeyH", KeyEvent.KEYCODE_H);
            add("KeyI", KeyEvent.KEYCODE_I);
            add("KeyJ", KeyEvent.KEYCODE_J);
            add("KeyK", KeyEvent.KEYCODE_K);
            add("KeyL", KeyEvent.KEYCODE_L);
            add("KeyM", KeyEvent.KEYCODE_M);
            add("KeyN", KeyEvent.KEYCODE_N);
            add("KeyO", KeyEvent.KEYCODE_O);
            add("KeyP", KeyEvent.KEYCODE_P);
            add("KeyQ", KeyEvent.KEYCODE_Q);
            add("KeyR", KeyEvent.KEYCODE_R);
            add("KeyS", KeyEvent.KEYCODE_S);
            add("KeyT", KeyEvent.KEYCODE_T);
            add("KeyU", KeyEvent.KEYCODE_U);
            add("KeyV", KeyEvent.KEYCODE_V);
            add("KeyW", KeyEvent.KEYCODE_W);
            add("KeyX", KeyEvent.KEYCODE_X);
            add("KeyY", KeyEvent.KEYCODE_Y);
            add("KeyZ", KeyEvent.KEYCODE_Z);
            add("KeyZ", KeyEvent.KEYCODE_Z);
            add("Digit1", KeyEvent.KEYCODE_1);
            add("Digit2", KeyEvent.KEYCODE_2);
            add("Digit3", KeyEvent.KEYCODE_3);
            add("Digit4", KeyEvent.KEYCODE_4);
            add("Digit5", KeyEvent.KEYCODE_5);
            add("Digit6", KeyEvent.KEYCODE_6);
            add("Digit7", KeyEvent.KEYCODE_7);
            add("Digit8", KeyEvent.KEYCODE_8);
            add("Digit9", KeyEvent.KEYCODE_9);
            add("Digit0", KeyEvent.KEYCODE_0);
            add("F1", KeyEvent.KEYCODE_F1);
            add("F2", KeyEvent.KEYCODE_F2);
            add("F3", KeyEvent.KEYCODE_F3);
            add("F4", KeyEvent.KEYCODE_F4);
            add("F5", KeyEvent.KEYCODE_F5);
            add("F6", KeyEvent.KEYCODE_F6);
            add("F7", KeyEvent.KEYCODE_F7);
            add("F8", KeyEvent.KEYCODE_F8);
            add("F9", KeyEvent.KEYCODE_F9);
            add("F10", KeyEvent.KEYCODE_F10);
            add("F11", KeyEvent.KEYCODE_F11);
            add("F12", KeyEvent.KEYCODE_F12);
        }

        public static int getCode(String name) {
            Integer code = sInstance.mapper.get(name);
            return code == null ? 0 : code.intValue();
        }

        void add(String keyName, int keycode) {
            mapper.put(keyName, keycode);
        }
    }
}
